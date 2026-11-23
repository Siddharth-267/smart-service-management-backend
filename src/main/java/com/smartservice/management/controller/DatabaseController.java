package com.smartservice.management.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DatabaseController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/database/test")
    public Map<String, Object> testDatabase() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        List<String> tables = jdbcTemplate.queryForList(
                "SHOW TABLES",
                String.class
        );

        return Map.of(
                "database", "smart_service_db",
                "connection", result != null && result == 1
                        ? "SUCCESS"
                        : "FAILED",
                "tables", tables
        );
    }
}
