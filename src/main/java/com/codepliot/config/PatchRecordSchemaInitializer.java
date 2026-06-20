package com.codepliot.config;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 补丁记录数据库表结构初始化器。
 * <p>
 * 应用启动时自动执行，为 patch_record 表补充 GitHub Pull Request 相关字段，
 * 包括 PR 提交状态、提交时间、PR 链接、PR 编号及分支名等。
 * </p>
 */
@Component
public class PatchRecordSchemaInitializer implements ApplicationRunner {

    /** 需要为 patch_record 表追加的 PR 相关列定义 */
    private static final Map<String, String> PR_COLUMNS = Map.of(
            "pr_submitted", "ALTER TABLE patch_record ADD COLUMN pr_submitted TINYINT(1) NOT NULL DEFAULT 0",
            "pr_submitted_at", "ALTER TABLE patch_record ADD COLUMN pr_submitted_at DATETIME DEFAULT NULL",
            "pr_url", "ALTER TABLE patch_record ADD COLUMN pr_url VARCHAR(512) DEFAULT NULL",
            "pr_number", "ALTER TABLE patch_record ADD COLUMN pr_number INT DEFAULT NULL",
            "pr_branch", "ALTER TABLE patch_record ADD COLUMN pr_branch VARCHAR(255) DEFAULT NULL"
    );

    /** JDBC 模板，用于执行 DDL 语句 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造方法，注入 JdbcTemplate。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public PatchRecordSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动时执行表结构初始化，按需为 patch_record 表追加 PR 相关列。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, String> column : PR_COLUMNS.entrySet()) {
            if (!columnExists(column.getKey())) {
                jdbcTemplate.execute(column.getValue());
            }
        }
    }

    /**
     * 判断 patch_record 表中是否存在指定列。
     *
     * @param columnName 列名
     * @return 如果列存在返回 true，否则返回 false
     */
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
