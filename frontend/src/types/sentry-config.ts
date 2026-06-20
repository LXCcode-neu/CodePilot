/** Sentry 项目映射配置，关联 CodePilot 项目与 Sentry 项目 */
export interface SentryProjectMapping {
  /** 映射记录唯一标识 */
  id: string;
  /** 关联的 CodePilot 项目 ID */
  projectId: string;
  /** Sentry 组织标识（slug） */
  sentryOrganizationSlug: string;
  /** Sentry 项目标识（slug） */
  sentryProjectSlug: string;
  /** 是否启用该映射 */
  enabled: boolean;
  /** 是否启用告警触发时自动运行修复任务 */
  autoRunEnabled: boolean;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 保存 Sentry 项目映射请求参数 */
export interface SaveSentryProjectMappingRequest {
  /** Sentry 组织标识（slug） */
  sentryOrganizationSlug: string;
  /** Sentry 项目标识（slug） */
  sentryProjectSlug: string;
  /** 是否启用该映射 */
  enabled: boolean;
  /** 是否启用自动运行 */
  autoRunEnabled: boolean;
}
