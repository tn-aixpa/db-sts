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

package it.smartcommunitylab.dbsts.db;

import it.smartcommunitylab.dbsts.jwt.WebIdentity;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DbManager implements InitializingBean {

    private final org.springframework.security.crypto.keygen.StringKeyGenerator pwdGenerator;

    private DbAdapter adapter;

    private Long defaultDuration = 3600L;
    private Set<String> defaultRoles = Collections.emptySet();

    public DbManager(@Value("${sts.credentials.password-length}") Integer pwdLength) {
        Assert.notNull(pwdLength, "pwd length must be set");

        this.pwdGenerator = new KeyGenerator(pwdLength);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(adapter, "db adapter can not be null");
    }

    @Autowired
    public void setDuration(@Value("${sts.credentials.duration}") Long duration) {
        if (duration != null && duration > 120) {
            this.defaultDuration = duration;
        }
    }

    @Autowired
    public void setRoles(@Value("${sts.credentials.roles}") String roles) {
        if (StringUtils.hasText(roles)) {
            this.defaultRoles = StringUtils.commaDelimitedListToSet(roles);
        }
    }

    @Autowired
    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    public DbUser exchange(@NotNull WebIdentity webIdentity, Collection<String> requestedRoles) {
        if (webIdentity == null) {
            throw new IllegalArgumentException("invalid web identity");
        }

        log.info("exchange webIdentity for db user");
        if (log.isTraceEnabled()) {
            log.trace("web identity: {}", webIdentity);
        }

        //validity
        Instant now = Instant.now(); //(include)
        Instant expiration = webIdentity.getExpiresAt() != null
            ? webIdentity.getExpiresAt()
            : now.plus(defaultDuration, ChronoUnit.SECONDS);

        //roles
        Set<String> roles = defaultRoles;
        if (webIdentity.getRoles() != null && !webIdentity.getRoles().isEmpty()) {
            //web roles are derived from token
            roles = new HashSet<>(webIdentity.getRoles());
        }
        if (requestedRoles != null && !requestedRoles.isEmpty()) {
            //requested have the highest priority
            roles = new HashSet<>(requestedRoles);
        }

        //generate secure credentials
        String username = pwdGenerator.generateKey();
        String password = pwdGenerator.generateKey();

        //convert
        DbUser user = DbUser.builder()
            .username(username)
            .password(password)
            .roles(roles)
            .validUntil(expiration)
            .build();

        //create in database
        user = adapter.create(user);

        log.debug("created db user {}", user.getUsername());
        if (log.isTraceEnabled()) {
            log.trace("user: {}", user);
        }

        //return
        return user;
    }

    // Delete roles for users
    public void delete(String roles) {
        adapter.delete(roles);
    }

}
