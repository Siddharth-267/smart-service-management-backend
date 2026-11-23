
package com.smartservice.management.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RequestCategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsById(int categoryId) {

        String sql = """
                SELECT COUNT(*)
                FROM request_categories
                WHERE category_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                categoryId
        );

        return count != null && count > 0;
    }
}
