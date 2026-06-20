package com.codepliot.config;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * GitHub 认证数据库表结构初始化器。
 * <p>
 * 应用启动时自动执行，负责创建用户 GitHub 账号关联表
 * 并为 {@code project_repo} 表补充 GitHub 仓库相关字段。
 * </p>
 */
@Component
public class GitHubAuthSchemaInitializer implements ApplicationRunner {

    /** 需要为 project_repo 表追加的 GitHub 相关列定义 */
    private static final Map<String, String> PROJECT_REPO_COLUMNS = Map.of(
            "github_owner", "ALTER TABLE project_repo ADD COLUMN github_owner VARCHAR(128) DEFAULT NULL",
            "github_repo_name", "ALTER TABLE project_repo ADD COLUMN github_repo_name VARCHAR(128) DEFAULT NULL",
            "github_repo_id", "ALTER TABLE project_repo ADD COLUMN github_repo_id BIGINT DEFAULT NULL",
            "github_private", "ALTER TABLE project_repo ADD COLUMN github_private TINYINT(1) NOT NULL DEFAULT 0"
    );

    /** JDBC 模板，用于执行 DDL 语句 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造方法，注入 JdbcTemplate。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public GitHubAuthSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动时执行表结构初始化。
     * <p>
     * 创建 user_github_account 表，并为 project_repo 表按需追加 GitHub 相关列。
     * </p>
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_github_account (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  github_user_id BIGINT NOT NULL,
                  github_login VARCHAR(128) NOT NULL,
                  github_name VARCHAR(255) DEFAULT NULL,
                  github_avatar_url VARCHAR(512) DEFAULT NULL,
                  access_token_encrypted TEXT NOT NULL,
                  scope VARCHAR(255) DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_github_account_user_id (user_id),
                  UNIQUE KEY uk_user_github_account_github_user_id (github_user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        if (!tableExists("project_repo")) {
            return;
        }
        for (Map.Entry<String, String> column : PROJECT_REPO_COLUMNS.entrySet()) {
            if (!columnExists("project_repo", column.getKey())) {
                jdbcTemplate.execute(column.getValue());
            }
        }
    }

    /**
     * 判断指定表是否存在于当前数据库中。
     *
     * @param tableName 表名
     * @return 如果表存在返回 true，否则返回 false
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    /**
     * 判断指定表中是否存在指定列。
     *
     * @param tableName  表名
     * @param columnName 列名
     * @return 如果列存在返回 true，否则返回 false
     */
    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
