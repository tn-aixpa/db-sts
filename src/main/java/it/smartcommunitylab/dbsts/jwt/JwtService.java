/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.dbsts.jwt;

import it.smartcommunitylab.dbsts.api.TokenRequest;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class JwtService {

    private static final String ROLE_PREFIX = "PG_";
    private final String issuerUri;

    private JwtAuthenticationProvider jwtAuthProvider;

    private int defaultDuration = 3600;

    public JwtService(
        @Value("${sts.jwt.issuer-uri}") String issuerUri,
        @Value("${sts.jwt.audience}") String audience,
        @Value("${sts.jwt.claim}") String claim
    ) {
        this.issuerUri = issuerUri;
        if (StringUtils.hasText(issuerUri)) {
            //build auth provider to validate web jwt
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder(issuerUri, audience));
            provider.setJwtAuthenticationConverter(jwtAuthConverter(claim));
            this.jwtAuthProvider = provider;
        }
    }

    @Autowired
    public void setDuration(@Value("${sts.credentials.duration}") Integer duration) {
        if (duration != null && duration > 120) {
            this.defaultDuration = duration;
        }
    }

    public WebIdentity assumeWebIdentity(@NotNull TokenRequest request) {
        //resolve token if available
        String token = request.getToken();
        Integer duration = request.getDuration();

        if (StringUtils.hasText(token)) {
            //use info from token
            return assumeWebIdentity(token, duration);
        }

        //fallback to request params
        String username = request.getUsername();
        String database = request.getDatabase();
        List<String> roles = request.getRoles() != null ? new ArrayList<>(request.getRoles()) : null;

        if (StringUtils.hasText(username)) {
            return assumeWebIdentity(username, database, roles, duration);
        }

        throw new IllegalArgumentException("invalid request: missing params");
    }

    public WebIdentity assumeWebIdentity(
        @NotNull String username,
        @Nullable String database,
        @Nullable List<String> roles,
        @Nullable Integer duration
    ) {
        log.info("assume web identity request");
        if (log.isTraceEnabled()) {
            log.trace("user: {}", username);
        }

        try {
            //evaluate token expiration
            Instant now = Instant.now();
            Instant expiration = now.plus(defaultDuration, ChronoUnit.SECONDS);
            Instant expd = duration != null ? now.plus(duration, ChronoUnit.SECONDS) : null;
            if (expd != null && expd.isBefore(expiration)) {
                //request duration is smaller than token expiration, use it
                expiration = expd;
            }

            //build identity
            WebIdentity id = WebIdentity.builder()
                .issuer(issuerUri)
                .createdAt(now)
                .expiresAt(expiration)
                .username(username)
                .roles(roles)
                .database(database)
                .build();

            if (log.isTraceEnabled()) {
                log.trace("web identity: {}", id);
            }

            return id;
        } catch (AuthenticationException ae1) {
            throw new IllegalArgumentException("invalid or missing token");
        }
    }

    public WebIdentity assumeWebIdentity(@NotNull String token, Integer duration) {
        log.info("assume web identity request");
        if (jwtAuthProvider == null) {
            throw new IllegalArgumentException("token exchange not supported, jwt provider not configured");
        }

        if (log.isTraceEnabled()) {
            log.trace("token: {}", token);
        }

        try {
            //autenticate jwt token via provider
            BearerTokenAuthenticationToken request = new BearerTokenAuthenticationToken(token);
            Authentication webAuth = jwtAuthProvider.authenticate(request);
            if (!webAuth.isAuthenticated()) {
                throw new IllegalArgumentException("invalid or missing token");
            }

            log.debug("token request resolved for {} via jwt provider", webAuth.getName());

            //token is valid, use as context for generation
            List<String> roles = webAuth
                .getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .toList();
            Instant exp = null;
            if (webAuth instanceof ExpiringJwtAuthenticationToken) {
                exp = ((ExpiringJwtAuthenticationToken) webAuth).getExpiration();
            }

            //evaluate token expiration
            Instant now = Instant.now();
            Instant expiration = exp != null ? exp : now.plus(defaultDuration, ChronoUnit.SECONDS);
            Instant expd = duration != null ? now.plus(duration, ChronoUnit.SECONDS) : null;
            if (expd != null && expd.isBefore(expiration)) {
                //request duration is smaller than token expiration, use it
                expiration = expd;
            }

            Collection<String> webRoles = roles != null ? roles : Collections.emptyList();

            //check if scoped request
            String database = null;
            if (webAuth instanceof JwtAuthenticationToken) {
                database = ((JwtAuthenticationToken) webAuth).getToken().getClaimAsString("database");
            }

            //build identity
            WebIdentity id = WebIdentity.builder()
                .issuer(issuerUri)
                .createdAt(now)
                .expiresAt(expiration)
                .username(webAuth.getName())
                .roles(webRoles)
                .database(database)
                .build();

            if (log.isTraceEnabled()) {
                log.trace("web identity: {}", id);
            }

            return id;
        } catch (AuthenticationException ae1) {
            throw new IllegalArgumentException("invalid or missing token");
        }
    }

    /*
     * JWT decoder
     */
    private JwtDecoder jwtDecoder(String issuer, String audience) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        jwtDecoder.setJwtValidator(withIssuer);

        if (StringUtils.hasText(audience)) {
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                (aud -> aud != null && aud.contains(audience))
            );

            OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                audienceValidator
            );
            jwtDecoder.setJwtValidator(withAudience);
        }

        return jwtDecoder;
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter(String rolesClaimName) {
        return (Jwt jwt) -> {
            Set<GrantedAuthority> authorities = new HashSet<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            //read roles from token
            if (StringUtils.hasText(rolesClaimName) && jwt.hasClaim(rolesClaimName)) {
                List<String> roles = jwt.getClaimAsStringList(rolesClaimName);
                if (roles != null) {
                    roles.forEach(r ->
                        //derive a scoped PG role
                        authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + r))
                    );
                }
            }

            return new ExpiringJwtAuthenticationToken(jwt, authorities, jwt.getExpiresAt());
        };
    }
}
