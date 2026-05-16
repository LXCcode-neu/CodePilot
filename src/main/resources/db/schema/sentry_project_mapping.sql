CREATE TABLE IF NOT EXISTS sentry_project_mapping (
  id BIGINT NOT NULL COMMENT 'MyBatis-Plus ASSIGN_ID',
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  sentry_organization_slug VARCHAR(128) NOT NULL,
  sentry_project_slug VARCHAR(128) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  auto_run_enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sentry_project_mapping_project (project_id),
  KEY idx_sentry_project_mapping_sentry_project (sentry_organization_slug, sentry_project_slug),
  KEY idx_sentry_project_mapping_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
