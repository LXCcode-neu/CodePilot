package com.codepliot.config;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IssueAutoFixSchemaInitializer implements ApplicationRunner {

    private static final Map<String, String> AGENT_TASK_COLUMNS = Map.of(
            "source_type", "ALTER TABLE agent_task ADD COLUMN source_type VARCHAR(32) DEFAULT 'MANUAL'",
            "source_id", "ALTER TABLE agent_task ADD COLUMN source_id BIGINT DEFAULT NULL"
    );

    private final JdbcTemplate jdbcTemplate;

    public IssueAutoFixSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_repo_watch (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  project_repo_id BIGINT DEFAULT NULL,
                  owner VARCHAR(100) NOT NULL,
                  repo_name VARCHAR(100) NOT NULL,
                  repo_url VARCHAR(500) NOT NULL,
                  default_branch VARCHAR(100) DEFAULT 'main',
                  watch_enabled TINYINT(1) NOT NULL DEFAULT 1,
                  watch_mode VARCHAR(32) NOT NULL DEFAULT 'POLLING',
                  last_checked_at DATETIME DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_repo_watch_user_repo (user_id, owner, repo_name),
                  KEY idx_user_repo_watch_enabled (watch_enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS github_issue_event (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  repo_watch_id BIGINT NOT NULL,
                  project_repo_id BIGINT DEFAULT NULL,
                  github_issue_id BIGINT NOT NULL,
                  issue_number INT NOT NULL,
                  issue_title VARCHAR(500) NOT NULL,
                  issue_body LONGTEXT DEFAULT NULL,
                  issue_url VARCHAR(500) DEFAULT NULL,
                  issue_state VARCHAR(32) NOT NULL,
                  sender_login VARCHAR(100) DEFAULT NULL,
                  event_action VARCHAR(32) DEFAULT 'opened',
                  status VARCHAR(32) NOT NULL DEFAULT 'NEW',
                  agent_task_id BIGINT DEFAULT NULL,
                  notified_at DATETIME DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_github_issue_event_repo_issue (repo_watch_id, issue_number),
                  KEY idx_github_issue_event_user_status (user_id, status),
                  KEY idx_github_issue_event_task_id (agent_task_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_channel (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  channel_type VARCHAR(32) NOT NULL,
                  channel_name VARCHAR(100) DEFAULT NULL,
                  webhook_url_encrypted TEXT NOT NULL,
                  enabled TINYINT(1) NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_notification_channel_user_enabled (user_id, enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_record (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  channel_id BIGINT NOT NULL,
                  event_type VARCHAR(64) NOT NULL,
                  title VARCHAR(255) DEFAULT NULL,
                  content LONGTEXT DEFAULT NULL,
                  status VARCHAR(32) NOT NULL,
                  error_message LONGTEXT DEFAULT NULL,
                  sent_at DATETIME DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_notification_record_user_created (user_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_action_token (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  issue_event_id BIGINT DEFAULT NULL,
                  task_id BIGINT DEFAULT NULL,
                  patch_id BIGINT DEFAULT NULL,
                  action_type VARCHAR(32) NOT NULL,
                  token_hash VARCHAR(128) NOT NULL,
                  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                  expires_at DATETIME NOT NULL,
                  used_at DATETIME DEFAULT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_notification_action_token_hash (token_hash),
                  KEY idx_notification_action_token_issue (issue_event_id),
                  KEY idx_notification_action_token_user_status (user_id, status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS bot_action_code (
                  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
                  user_id BIGINT NOT NULL,
                  issue_event_id BIGINT NOT NULL,
                  task_id BIGINT DEFAULT NULL,
                  patch_id BIGINT DEFAULT NULL,
                  channel_type VARCHAR(32) NOT NULL,
                  chat_id VARCHAR(128) DEFAULT NULL,
                  action_code VARCHAR(32) NOT NULL,
                  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                  last_message_id VARCHAR(128) DEFAULT NULL,
                  expires_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_bot_action_code (action_code),
                  KEY idx_bot_action_code_issue (issue_event_id),
                  KEY idx_bot_action_code_task (task_id),
                  KEY idx_bot_action_code_chat (channel_type, chat_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        if (tableExists("agent_task")) {
            for (Map.Entry<String, String> column : AGENT_TASK_COLUMNS.entrySet()) {
                if (!columnExists("agent_task", column.getKey())) {
                    jdbcTemplate.execute(column.getValue());
                }
            }
        }
    }

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
