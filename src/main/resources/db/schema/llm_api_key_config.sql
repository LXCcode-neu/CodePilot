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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
