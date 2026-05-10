package com.codepliot.config;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PatchRecordSchemaInitializer implements ApplicationRunner {

    private static final Map<String, String> PR_COLUMNS = Map.of(
            "pr_submitted", "ALTER TABLE patch_record ADD COLUMN pr_submitted TINYINT(1) NOT NULL DEFAULT 0",
            "pr_submitted_at", "ALTER TABLE patch_record ADD COLUMN pr_submitted_at DATETIME DEFAULT NULL",
            "pr_url", "ALTER TABLE patch_record ADD COLUMN pr_url VARCHAR(512) DEFAULT NULL",
            "pr_number", "ALTER TABLE patch_record ADD COLUMN pr_number INT DEFAULT NULL",
            "pr_branch", "ALTER TABLE patch_record ADD COLUMN pr_branch VARCHAR(255) DEFAULT NULL"
    );

    private final JdbcTemplate jdbcTemplate;

    public PatchRecordSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, String> column : PR_COLUMNS.entrySet()) {
            if (!columnExists(column.getKey())) {
                jdbcTemplate.execute(column.getValue());
            }
        }
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'patch_record'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }
}
