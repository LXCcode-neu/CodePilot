USE codepilot;

CREATE TABLE code_symbol (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  project_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  language VARCHAR(32) NOT NULL,
  file_path VARCHAR(1024) NOT NULL,
  symbol_type VARCHAR(32) NOT NULL,
  symbol_name VARCHAR(255) NOT NULL,
  parent_symbol VARCHAR(255) DEFAULT NULL,
  signature TEXT DEFAULT NULL,
  annotations TEXT DEFAULT NULL,
  route_path VARCHAR(255) DEFAULT NULL,
  import_text TEXT DEFAULT NULL,
  start_line INT DEFAULT NULL,
  end_line INT DEFAULT NULL,
  content TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_code_symbol_project_id (project_id),
  KEY idx_code_symbol_file_id (file_id),
  KEY idx_code_symbol_language (language),
  KEY idx_code_symbol_symbol_type (symbol_type),
  KEY idx_code_symbol_symbol_name (symbol_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
