package it.smartcommunitylab.dbsts.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
public class UserRepository {

    private static final String INSERT_SQL =
        "INSERT INTO users (id, created_at, web_issuer, web_user, db_database, db_user, db_roles, valid_until) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_EXPIRED = "SELECT * FROM users WHERE valid_until < ?";
    private static final String DELETE_SQL = "DELETE FROM users WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private RowMapper<User> rowMapper;

    public UserRepository(JdbcTemplate template) {
        Assert.notNull(template, "jdbc is required to store users");
        this.jdbcTemplate = template;

        this.rowMapper = new UserRowMapper();
    }

    public void store(User user) {
        Timestamp now = new Timestamp(Date.from(Instant.now()).getTime());
        String dbRoles = user.getDbRoles() != null ? StringUtils.arrayToCommaDelimitedString(user.getDbRoles()) : null;

        jdbcTemplate.update(
            INSERT_SQL,
            new Object[] {
                user.getId(),
                now,
                user.getWebIssuer(),
                user.getWebUser(),
                user.getDbDatabase(),
                user.getDbUser(),
                dbRoles,
                user.getDbValidUntil(),
            },
            new int[] {
                Types.VARCHAR,
                Types.TIMESTAMP,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.TIMESTAMP,
            }
        );
    }

    public void remove(String id) {
        if (id == null) {
            throw new IllegalArgumentException("invalid id");
        }

        jdbcTemplate.update(DELETE_SQL, id);
    }

    public List<User> findExpired() {
        Timestamp now = new Timestamp(Date.from(Instant.now()).getTime());

        return jdbcTemplate.query(SELECT_EXPIRED, new Object[] { now }, new int[] { Types.TIMESTAMP }, rowMapper);
    }

    private class UserRowMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("id");
            if (id == null) {
                return null;
            }

            String roles = rs.getString("db_roles");

            return User.builder()
                .id(id)
                .createdAt(rs.getTimestamp("created_at"))
                //web identity
                .webIssuer(rs.getString("web_issuer"))
                .webUser(rs.getString("web_user"))
                //db
                .dbDatabase(rs.getString("db_database"))
                .dbUser(rs.getString("db_user"))
                .dbRoles(roles != null ? StringUtils.commaDelimitedListToStringArray(roles) : null)
                .dbValidUntil(rs.getTimestamp("valid_until"))
                .build();
        }
    }
}
