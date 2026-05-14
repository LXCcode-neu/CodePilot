CREATE TABLE IF NOT EXISTS patch_verification_record (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  task_id BIGINT NOT NULL,
  patch_record_id BIGINT DEFAULT NULL,
  attempt_no INT NOT NULL DEFAULT 1,
  command_name VARCHAR(128) NOT NULL,
  command_text TEXT DEFAULT NULL,
  working_directory VARCHAR(1024) DEFAULT NULL,
  exit_code INT DEFAULT NULL,
  passed TINYINT(1) NOT NULL DEFAULT 0,
  timed_out TINYINT(1) NOT NULL DEFAULT 0,
  output_summary MEDIUMTEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_patch_verification_record_task_attempt (task_id, attempt_no),
  KEY idx_patch_verification_record_patch_id (patch_record_id),
  KEY idx_patch_verification_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
