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

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.dbsts.jwt.JwtService;

@RestController
@Slf4j
public class StsEndpoint implements InitializingBean {

    public static final String TOKEN_URL = "/sts/web";

    @Autowired
    private JwtService jwtService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(jwtService, "jwtService is required");
    }

    @RequestMapping(value = TOKEN_URL, method = { RequestMethod.POST, RequestMethod.GET })
    public TokenResponse token(
        @RequestParam Map<String, String> parameters,
        @CurrentSecurityContext SecurityContext securityContext
    ) {}
}
