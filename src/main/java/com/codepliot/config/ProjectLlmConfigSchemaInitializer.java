package com.codepliot.config;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 项目 LLM 配置数据库表结构初始化器。
 * <p>
 * 应用启动时自动执行，负责创建项目级 LLM 配置表和用户级 LLM API Key 配置表，
 * 并为 agent_task 表补充 LLM 提供商及模型相关字段。
 * </p>
 */
@Component
public class ProjectLlmConfigSchemaInitializer implements ApplicationRunner {

    /** 需要为 agent_task 表追加的 LLM 相关列定义 */
    private static final Map<String, String> AGENT_TASK_COLUMNS = Map.of(
            "llm_provider", "ALTER TABLE agent_task ADD COLUMN llm_provider VARCHAR(32) DEFAULT NULL",
            "llm_model_name", "ALTER TABLE agent_task ADD COLUMN llm_model_name VARCHAR(128) DEFAULT NULL",
            "llm_display_name", "ALTER TABLE agent_task ADD COLUMN llm_display_name VARCHAR(128) DEFAULT NULL"
    );

    /** JDBC 模板，用于执行 DDL 语句 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造方法，注入 JdbcTemplate。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public ProjectLlmConfigSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动时执行表结构初始化。
     * <p>
     * 创建 project_llm_config 和 llm_api_key_config 表，
     * 并为 agent_task 表按需追加 LLM 相关字段。
     * </p>
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS project_llm_config (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  project_repo_id BIGINT NOT NULL,
                  provider VARCHAR(32) NOT NULL,
                  model_name VARCHAR(128) NOT NULL,
                  display_name VARCHAR(128) NOT NULL,
                  base_url VARCHAR(255) NOT NULL,
                  api_key_encrypted TEXT NOT NULL,
                  enabled TINYINT(1) NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_project_llm_config_project_repo_id (project_repo_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS llm_api_key_config (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  key_name VARCHAR(128) NOT NULL,
                  provider VARCHAR(32) NOT NULL,
                  model_name VARCHAR(128) NOT NULL,
                  display_name VARCHAR(128) NOT NULL,
                  base_url VARCHAR(255) NOT NULL,
                  api_key_encrypted TEXT NOT NULL,
                  active TINYINT(1) NOT NULL DEFAULT 0,
                  last_used_at DATETIME DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_llm_api_key_user_id (user_id),
                  KEY idx_llm_api_key_user_active (user_id, active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        if (!tableExists("agent_task")) {
            return;
        }
        for (Map.Entry<String, String> column : AGENT_TASK_COLUMNS.entrySet()) {
            if (!columnExists("agent_task", column.getKey())) {
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
