USE codepilot;

CREATE TABLE agent_step (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  task_id BIGINT NOT NULL,
  step_type VARCHAR(64) NOT NULL,
  step_name VARCHAR(255) NOT NULL,
  input TEXT DEFAULT NULL,
  output TEXT DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  error_message TEXT DEFAULT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_step_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
