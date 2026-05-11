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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
