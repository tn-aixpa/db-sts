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

package it.smartcommunitylab.dbsts.postgresql;

import it.smartcommunitylab.dbsts.db.DbAdapter;
import it.smartcommunitylab.dbsts.db.DbUser;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "spring.datasource", name = "platform", havingValue = "postgresql")
@Slf4j
public class PostgresqlAdapter implements DbAdapter {

    private static final String CREATE_SQL = "CREATE ROLE %s WITH LOGIN PASSWORD %s ";
    private static final String VALID_UNTIL = " VALID UNTIL %s ";
    private static final String IN_ROLE = " IN ROLE %s ";

    private static final String DELETE_SQL = "DROP ROLE IF EXISTS %s";

    private static final String TIMESTAMP_FORMAT = "";

    private final JdbcTemplate jdbcTemplate;
    private final DateFormat dateFormatter;

    public PostgresqlAdapter(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    }

    @Override
    public DbUser create(DbUser user) {
        String role = user.getUsername();
        String password = hash(user.getPassword());
        String inRoles = user.getRoles() != null ? String.join(",", user.getRoles()) : null;
        String until = user.getValidUntil() != null ? dateFormatter.format(Date.from(user.getValidUntil())) : null;

        if (!StringUtils.hasText(role) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("invalid user");
        }

        String sql = CREATE_SQL;
        List<Object> params = new ArrayList<>();
        params.add(role);
        params.add(quote(password));

        if (until != null) {
            sql += VALID_UNTIL;
            params.add(quote(until));
        }

        if (inRoles != null && !inRoles.isEmpty()) {
            sql += IN_ROLE;
            params.add(inRoles);
        }

        //need to raw execute query to create roles...
        String query = String.format(sql, params.toArray());

        log.debug("create role for {}", role);
        if (log.isTraceEnabled()) {
            log.trace("sql: {}", query);
        }

        jdbcTemplate.execute(query);

        return user;
    }

    @Override
    public void delete(String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("invalid user");
        }

        //need to raw execute query to drop roles...
        String query = String.format(DELETE_SQL, role);
        jdbcTemplate.execute(query);
    }

    private String hash(String password) {
        //TODO md5 hash the password
        return password;
    }

    private String quote(String value) {
        return "'" + value + "'";
    }
}
