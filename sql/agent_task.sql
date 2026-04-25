USE codepilot;

CREATE TABLE agent_task (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  issue_title VARCHAR(255) NOT NULL,
  issue_description TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  result_summary TEXT DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_task_user_id (user_id),
  KEY idx_agent_task_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
