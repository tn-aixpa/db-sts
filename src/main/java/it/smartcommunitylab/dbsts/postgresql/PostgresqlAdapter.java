/**
 * Copyright 2024 the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
public class PostgresqlAdapter implements DbAdapter {

    private static final String CREATE_SQL = "CREATE ROLE %s WITH LOGIN PASSWORD %s ";
    private static final String VALID_UNTIL = " VALID UNTIL %s ";
    private static final String IN_ROLE = " IN ROLE %s ";

    private static final String GRANT_SQL = "GRANT CONNECT ON DATABASE %s TO %s";
    private static final String ALTER_ROLE_SQL = "ALTER ROLE %s SET ROLE %s";

    private static final String REVOKE_CONNECT_SQL = "REVOKE CONNECT ON DATABASE %s FROM %s";
    private static final String REVOKE_ROLE_SQL = "REVOKE %s FROM %s;";

    private static final String DISABLE_SQL = "ALTER USER %s WITH NOLOGIN";

    private static final String DROP_SQL = "DROP ROLE IF EXISTS %s";

    private final JdbcTemplate jdbcTemplate;
    private final DateFormat dateFormatter;

    private final PostgresqlProperties properties;
    private Set<String> databases;

    public PostgresqlAdapter(DataSourceProperties dataSourceProperties, PostgresqlProperties properties) {
        Assert.notNull(dataSourceProperties, "properties are required");
        Assert.hasText(dataSourceProperties.getUrl(), "url is required");

        this.properties = properties;

        //create dedicated dataSource and template
        DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        if (StringUtils.hasText(properties.getDatabase())) {
            //use selected
            this.databases = StringUtils.commaDelimitedListToSet(properties.getDatabase());
        } else {
            //extract single from connection
            try {
                String value = "http://" + dataSourceProperties.getUrl().replaceFirst("jdbc:postgresql://", "");
                URI url = new URI(value);
                this.databases = url.getPath() != null ? Collections.singleton(url.getPath().substring(1)) : null;
            } catch (URISyntaxException e) {
                log.error("Error parsing url: {}", e);
            }
        }

        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    }

    @Override
    public DbUser create(DbUser user) {
        if (databases != null && user.getDatabase() != null && !databases.contains(user.getDatabase())) {
            throw new IllegalArgumentException("invalid user: wrong database");
        }

        //get db
        String database = user.getDatabase();

        String role = user.getUsername();
        String password = user.getPassword();

        //keep only a single ROLE
        String inRole = user.getRoles() != null && !user.getRoles().isEmpty()
            ? user.getRoles().iterator().next()
            : null;
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

        if (inRole != null && !inRole.isEmpty()) {
            sql += IN_ROLE;
            params.add(inRole);
        }

        //need to raw execute query to create roles...
        String createSql = String.format(sql, params.toArray());
        log.debug("create role for {}", role);
        if (log.isTraceEnabled()) {
            log.trace("sql: {}", createSql);
        }
        jdbcTemplate.execute(createSql);

        if (database != null) {
            String grantSql = String.format(GRANT_SQL, database, role);
            log.debug("grant connect role for {} to {}", role, database);
            if (log.isTraceEnabled()) {
                log.trace("sql: {}", grantSql);
            }
            jdbcTemplate.execute(grantSql);
        }

        if (inRole != null) {
            String alterSql = String.format(ALTER_ROLE_SQL, role, inRole);
            log.debug("alter role {} to {}", role, inRole);
            if (log.isTraceEnabled()) {
                log.trace("sql: {}", alterSql);
            }
            jdbcTemplate.execute(alterSql);
        }

        return user;
    }

    @Override
    public void delete(DbUser user) {
        //safety check
        if (databases != null && user.getDatabase() != null && !databases.contains(user.getDatabase())) {
            throw new IllegalArgumentException("invalid user: wrong database");
        }

        String role = user.getUsername();
        String database = user.getDatabase();

        //keep only a single ROLE
        String inRole = user.getRoles() != null && !user.getRoles().isEmpty()
            ? user.getRoles().iterator().next()
            : null;

        //need to raw execute query to drop roles...
        if (database != null) {
            String revokeConnectSql = String.format(REVOKE_CONNECT_SQL, database, role);
            log.debug("revoke connect role for {} to {}", role, database);
            if (log.isTraceEnabled()) {
                log.trace("sql: {}", revokeConnectSql);
            }
            jdbcTemplate.execute(revokeConnectSql);
        }

        if (inRole != null) {
            String revokeRoleSql = String.format(REVOKE_ROLE_SQL, inRole, role);
            log.debug("revoke role {} to {}", inRole, role);
            if (log.isTraceEnabled()) {
                log.trace("sql: {}", revokeRoleSql);
            }
            jdbcTemplate.execute(revokeRoleSql);
        }

        String disableSql = String.format(DISABLE_SQL, role);
        log.debug("disable login to {}", role);
        if (log.isTraceEnabled()) {
            log.trace("sql: {}", disableSql);
        }
        jdbcTemplate.execute(disableSql);

        String dropSql = String.format(DROP_SQL, role);
        log.debug("drop role {}", role);
        if (log.isTraceEnabled()) {
            log.trace("sql: {}", dropSql);
        }
        jdbcTemplate.execute(dropSql);
    }

    private String quote(String value) {
        return "'" + value + "'";
    }
}
