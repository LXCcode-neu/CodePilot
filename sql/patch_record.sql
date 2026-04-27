CREATE TABLE IF NOT EXISTS patch_record (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  task_id BIGINT NOT NULL,
  analysis TEXT DEFAULT NULL,
  solution TEXT DEFAULT NULL,
  patch LONGTEXT DEFAULT NULL,
  risk TEXT DEFAULT NULL,
  safety_check_result TEXT DEFAULT NULL,
  raw_output LONGTEXT DEFAULT NULL,
  confirmed TINYINT(1) NOT NULL DEFAULT 0,
  confirmed_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_patch_record_task_id (task_id),
  KEY idx_patch_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE patch_record ADD COLUMN IF NOT EXISTS safety_check_result TEXT DEFAULT NULL;
ALTER TABLE patch_record ADD COLUMN IF NOT EXISTS confirmed TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE patch_record ADD COLUMN IF NOT EXISTS confirmed_at DATETIME DEFAULT NULL;
