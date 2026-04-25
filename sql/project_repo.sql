USE codepilot;

CREATE TABLE project_repo (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  user_id BIGINT NOT NULL,
  repo_url VARCHAR(255) NOT NULL,
  repo_name VARCHAR(128) NOT NULL,
  local_path VARCHAR(255) DEFAULT NULL,
  default_branch VARCHAR(64) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_project_repo_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
