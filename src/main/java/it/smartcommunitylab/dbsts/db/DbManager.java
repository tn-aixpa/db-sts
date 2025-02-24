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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DbManager implements InitializingBean {

    private final StringKeyGenerator pwdGenerator;
    private final StringKeyGenerator usernameGenerator;

    private DbAdapter adapter;
    private UserRepository userRepository;
    private String policy = "expire";

    private Long defaultDuration = 3600l;
    private Set<String> defaultRoles = Collections.emptySet();

    public DbManager(@Value("${sts.credentials.password-length}") Integer pwdLength) {
        Assert.notNull(pwdLength, "pwd length must be set");

        this.pwdGenerator = new HumanStringKeyGenerator(pwdLength.intValue());
        //use lowercase for usernames
        this.usernameGenerator = new CombinedKeyGenerator(
            new HumanStringKeyGenerator(4, "abcdefghijklmnopqrstuvwxyz".toCharArray()),
            new HumanStringKeyGenerator(8, "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray())
        );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(adapter, "db adapter can not be null");
    }

    @Autowired(required = false)
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    @Autowired
    public void setPolicy(@Value("${adapter.connection.policy}") String policy) {
        this.policy = policy;
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
        Instant now = Instant.now();
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

        //database
        String database = webIdentity.getDatabase();

        //generate secure credentials
        String username = usernameGenerator.generateKey();
        String password = pwdGenerator.generateKey();

        //convert
        DbUser user = DbUser.builder()
            .database(database)
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

        if (userRepository != null) {
            //store user
            User u = User.builder()
                .id(UUID.randomUUID().toString())
                .webIssuer(webIdentity.getIssuer())
                .webUser(webIdentity.getUsername())
                .dbDatabase(user.getDatabase())
                .dbUser(user.getUsername())
                .dbRoles(user.getRoles() != null ? user.getRoles().toArray(new String[0]) : null)
                .dbValidUntil(Date.from(user.getValidUntil()))
                .status("active")
                .build();

            userRepository.store(u);
        }

        //return
        return user;
    }

    public void delete(DbUser user) {
        if (user != null && StringUtils.hasText(user.getUsername())) {
            log.debug("delete db user {}", user.getUsername());

            adapter.delete(user);
        }
    }

    public void cleanupExpired() {
        log.debug("cleanup expired users");
        if (userRepository != null) {
            List<User> users = userRepository.findExpired();
            users.forEach(user -> {
                //remove from adapter
                try {
                    DbUser dbUser = DbUser.builder()
                        .database(user.getDbDatabase())
                        .username(user.getDbUser())
                        .roles(user.getDbRoles() != null ? Arrays.asList(user.getDbRoles()) : null)
                        .build();

                    adapter.delete(dbUser);
                } catch (Exception e) {
                    log.error("Error removing user: {}", e);
                }

                if ("expire".equals(policy)) {
                    //expire
                    userRepository.expire(user.getId());
                } else {
                    //delete
                    userRepository.remove(user.getId());
                }
            });
        }
    }
}
