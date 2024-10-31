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

package it.smartcommunitylab.dbsts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

    @Value("${sts.client.client-id}")
    private String clientId;

    @Value("${sts.client.client-secret}")
    private String clientSecret;

    @Bean("securityFilterChain")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AntPathRequestMatcher reqMatcher = new AntPathRequestMatcher("/**");
        HttpSecurity securityChain = http
            .securityMatcher(reqMatcher)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(reqMatcher).hasRole("CLIENT").anyRequest().authenticated();
            })
            // disable request cache
            .requestCache(requestCache -> requestCache.disable())
            //disable csrf
            .csrf(csrf -> csrf.disable())
            // we don't want a session for these endpoints, each request should be evaluated
            .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // disallow cors
        securityChain.cors(cors -> cors.disable());

        //client authentication (when configured)
        if (StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)) {
            //enable basic for client auth
            //TODO support additional methods
            securityChain
                .httpBasic(basic -> basic.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .userDetailsService(userDetailsService(clientId, clientSecret));

            //disable anonymous
            securityChain.anonymous(anon -> anon.disable());
        } else {
            //assign both USER and ADMIN to anon user to bypass all scoped permission checks
            securityChain.anonymous(anon -> {
                anon.authorities("ROLE_CLIENT");
                anon.principal("anonymous");
            });
        }

        securityChain.exceptionHandling(handling -> {
            handling
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .accessDeniedHandler(new AccessDeniedHandlerImpl()); // use 403
        });

        return securityChain.build();
    }

    /**
     * Basic auth provider
     */
    public static UserDetailsService userDetailsService(String username, String password) {
        UserDetails client = User.withDefaultPasswordEncoder()
            .username(username)
            .password(password)
            .roles("CLIENT")
            .build();

        return new InMemoryUserDetailsManager(client);
    }
}
