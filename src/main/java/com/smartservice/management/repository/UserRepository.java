
package com.smartservice.management.repository;

import com.smartservice.management.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEmail(String email) {

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, email
        );

        return count != null && count > 0;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {

        User user = new User();

        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("active"));

        Timestamp timestamp = rs.getTimestamp("created_at");

        if (timestamp != null) {
            user.setCreatedAt(timestamp.toLocalDateTime());
        }

        return user;
    };

    public List<User> findAll() {

        String sql = """
        SELECT user_id, name, email, password, role, created_at, active
        FROM users
        ORDER BY user_id
        """;
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public User findById(int userId) {

        String sql = """
        SELECT user_id, name, email, password, role, created_at, active
        FROM users
        WHERE user_id = ?
        """;

        List<User> users = jdbcTemplate.query(
                sql, userRowMapper, userId
        );

        return users.isEmpty() ? null : users.get(0);
    }

    public int save(User user) {

        String sql = """
                INSERT INTO users (name, email, password, role)
                VALUES (?, ?, ?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
    }

    // Update existing user details
    public int update(User user) {

        String sql = """
                UPDATE users
                SET name = ?, email = ?
                WHERE user_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                user.getName(),
                user.getEmail(),
                user.getUserId()
        );
    }
    public int deactivateUser(int userId) {

        String sql = """
            UPDATE users
            SET active = false
            WHERE user_id = ?
            """;

        return jdbcTemplate.update(sql, userId);
    }
    public User findByEmail(String email) {
        String sql = """
            SELECT *
            FROM users
            WHERE email = ?
            """;

        List<User> users = jdbcTemplate.query(sql, userRowMapper, email);

        return users.isEmpty() ? null : users.get(0);
    }
}
