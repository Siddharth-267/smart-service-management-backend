
package com.smartservice.management.repository;

import com.smartservice.management.model.RequestStatusHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequestStatusHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestStatusHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RequestStatusHistory> historyRowMapper =
            (rs, rowNum) -> {

                RequestStatusHistory history = new RequestStatusHistory();

                history.setHistoryId(rs.getInt("history_id"));
                history.setRequestId(rs.getInt("request_id"));
                history.setOldStatus(rs.getString("old_status"));
                history.setNewStatus(rs.getString("new_status"));

                int changedByValue = rs.getInt("changed_by");
                Integer changedBy = rs.wasNull() ? null : changedByValue;
                history.setChangedBy(changedBy);

                history.setRemarks(rs.getString("remarks"));

                if (rs.getTimestamp("changed_at") != null) {
                    history.setChangedAt(
                            rs.getTimestamp("changed_at").toLocalDateTime()
                    );
                }

                return history;
            };

    public int save(
            int requestId,
            String oldStatus,
            String newStatus,
            Integer changedBy,
            String remarks) {

        String sql = """
                INSERT INTO request_status_history
                (request_id, old_status, new_status, changed_by, remarks)
                VALUES (?, ?, ?, ?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                requestId,
                oldStatus,
                newStatus,
                changedBy,
                remarks
        );
    }

    public List<RequestStatusHistory> findByRequestId(int requestId) {

        String sql = """
                SELECT *
                FROM request_status_history
                WHERE request_id = ?
                ORDER BY changed_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                historyRowMapper,
                requestId
        );
    }
}
