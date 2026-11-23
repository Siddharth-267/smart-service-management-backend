package com.smartservice.management.repository;

import com.smartservice.management.model.ServiceRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ServiceRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public ServiceRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Map database records to ServiceRequest objects
    private final RowMapper<ServiceRequest> requestRowMapper =
            (rs, rowNum) -> {

                ServiceRequest request = new ServiceRequest();

                request.setRequestId(rs.getInt("request_id"));
                request.setUserId(rs.getInt("user_id"));
                request.setCategoryId(rs.getInt("category_id"));
                request.setTitle(rs.getString("title"));
                request.setDescription(rs.getString("description"));
                request.setLocation(rs.getString("location"));
                request.setPriority(rs.getString("priority"));
                request.setStatus(rs.getString("status"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");

                if (createdAt != null) {
                    request.setCreatedAt(createdAt.toLocalDateTime());
                }

                if (updatedAt != null) {
                    request.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                return request;
            };

    // Get all service requests
    public List<ServiceRequest> findAll() {

        String sql = """
                SELECT *
                FROM service_requests
                ORDER BY request_id DESC
                """;

        return jdbcTemplate.query(sql, requestRowMapper);
    }

    // Get service request by ID
    public ServiceRequest findById(int requestId) {

        String sql = """
                SELECT *
                FROM service_requests
                WHERE request_id = ?
                """;

        List<ServiceRequest> requests =
                jdbcTemplate.query(sql, requestRowMapper, requestId);

        return requests.isEmpty() ? null : requests.get(0);
    }

    // Create a new service request and return generated ID
    public int createRequest(ServiceRequest request) {

        String sql = """
                INSERT INTO service_requests
                (user_id, category_id, title, description, location, priority, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement statement =
                    connection.prepareStatement(
                            sql,
                            Statement.RETURN_GENERATED_KEYS
                    );

            statement.setInt(1, request.getUserId());
            statement.setInt(2, request.getCategoryId());
            statement.setString(3, request.getTitle());
            statement.setString(4, request.getDescription());
            statement.setString(5, request.getLocation());
            statement.setString(6, request.getPriority());
            statement.setString(7, request.getStatus());

            return statement;

        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException(
                    "Failed to generate service request ID"
            );
        }

        return keyHolder.getKey().intValue();
    }

    // Update request status
    public int updateStatus(int requestId, String newStatus) {

        String sql = """
                UPDATE service_requests
                SET status = ?
                WHERE request_id = ?
                """;

        return jdbcTemplate.update(sql, newStatus, requestId);
    }

    // Find overdue service requests
    public List<ServiceRequest> findOverdueRequests() {

        String sql = """
                SELECT *
                FROM service_requests
                WHERE status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED')
                AND TIMESTAMPDIFF(MINUTE, updated_at, CURRENT_TIMESTAMP) >=
                    CASE
                        WHEN priority = 'HIGH' THEN 120
                        WHEN priority = 'MEDIUM' THEN 1440
                        WHEN priority = 'LOW' THEN 2880
                    END
                """;

        return jdbcTemplate.query(sql, requestRowMapper);
    }
    // Count repeated issues in the same category and location
    public int countRepeatedIssues(int categoryId, String location) {

        String sql = """
                SELECT COUNT(*)
                FROM service_requests
                WHERE category_id = ?
                AND LOWER(TRIM(location)) = LOWER(TRIM(?))
                AND created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                categoryId,
                location
        );
    }
}
