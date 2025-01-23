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

package it.smartcommunitylab.dbsts.api;

import it.smartcommunitylab.dbsts.db.DbManager;
import it.smartcommunitylab.dbsts.db.DbUser;
import it.smartcommunitylab.dbsts.db.User;
import it.smartcommunitylab.dbsts.db.UserRepository;
import it.smartcommunitylab.dbsts.jwt.JwtService;
import it.smartcommunitylab.dbsts.jwt.WebIdentity;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class StsEndpoint implements InitializingBean {

    public static final String TOKEN_URL = "/sts/web";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DbManager dbManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(jwtService, "jwt service is required");
        Assert.notNull(dbManager, "db manager is required");
    }

    @RequestMapping(value = TOKEN_URL, method = { RequestMethod.POST, RequestMethod.GET })
    public TokenResponse exchange(
        @RequestParam Map<String, String> params,
        @CurrentSecurityContext SecurityContext securityContext
    ) {
        Authentication authentication = securityContext.getAuthentication();

        //resolve client authentication
        if (authentication == null || !(authentication.isAuthenticated())) {
            throw new InsufficientAuthenticationException("Invalid or missing authentication");
        }

        if (params == null) {
            throw new IllegalArgumentException("invalid request");
        }

        TokenRequest request = TokenRequest.builder()
            .token(params.get("token"))
            .duration(params.get("duration") != null ? Integer.parseInt(params.get("duration")) : null)
            .roles(params.get("roles") != null ? StringUtils.commaDelimitedListToSet(params.get("roles")) : null)
            .build();

        String client = authentication.getName();
        log.debug("request token exchange for client {}", client);

        //resolve token
        String token = request.getToken();
        Integer duration = request.getDuration();
        if (log.isTraceEnabled()) {
            log.trace("token: {}", token);
        }

        log.debug("assume web identity for client {}", client);

        WebIdentity webIdentity = jwtService.assumeWebIdentity(token, duration);

        //obtain db user
        Set<String> roles = request.getRoles();
        log.debug("generate db user for client {} requested roles {}", client, roles);

        DbUser dbUser = dbManager.exchange(webIdentity, roles);
        log.debug(
            "generated db user {} with roles {} valid until {}",
            dbUser.getUsername(),
            dbUser.getRoles(),
            dbUser.getValidUntil()
        );

        //build response
        Long expiration = dbUser.getValidUntil() != null
            ? Duration.between(Instant.now(), dbUser.getValidUntil()).toSeconds()
            : null;

        return TokenResponse.builder()
            .clientId(client)
            .expiration(expiration)
            .database(dbUser.getDatabase())
            .username(dbUser.getUsername())
            .password(dbUser.getPassword())
            .build();
    }
}
