
package com.smartservice.management.repository;

import com.smartservice.management.model.RequestAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequestAssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestAssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RequestAssignment> assignmentRowMapper =
            (rs, rowNum) -> {

                RequestAssignment assignment = new RequestAssignment();

                assignment.setAssignmentId(rs.getInt("assignment_id"));
                assignment.setRequestId(rs.getInt("request_id"));
                assignment.setStaffId(rs.getInt("staff_id"));

                if (rs.getTimestamp("assigned_at") != null) {
                    assignment.setAssignedAt(
                            rs.getTimestamp("assigned_at").toLocalDateTime()
                    );
                }

                return assignment;
            };

    public int assignRequest(int requestId, int staffId) {

        String sql = """
                INSERT INTO request_assignments
                (request_id, staff_id)
                VALUES (?, ?)
                """;

        return jdbcTemplate.update(sql, requestId, staffId);
    }
    public boolean existsByRequestIdAndStaffId(int requestId, int staffId) {

        String sql = """
            SELECT COUNT(*)
            FROM request_assignments
            WHERE request_id = ?
            AND staff_id = ?
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                requestId,
                staffId
        );

        return count != null && count > 0;
    }
    public List<RequestAssignment> findByRequestId(int requestId) {

        String sql = """
                SELECT *
                FROM request_assignments
                WHERE request_id = ?
                ORDER BY assigned_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                assignmentRowMapper,
                requestId
        );
    }

    public List<RequestAssignment> findByStaffId(int staffId) {

        String sql = """
                SELECT *
                FROM request_assignments
                WHERE staff_id = ?
                ORDER BY assigned_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                assignmentRowMapper,
                staffId
        );
    }
}
