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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
