/** Agent 任务状态，表示自动化修复任务的生命周期 */
export type AgentTaskStatus =
  | "PENDING"            // 等待中
  | "CLONING"            // 正在克隆仓库
  | "RETRIEVING"         // 正在检索代码
  | "ANALYZING"          // 正在分析问题
  | "GENERATING_PATCH"   // 正在生成补丁
  | "VERIFYING"          // 正在验证补丁
  | "REPAIRING_PATCH"    // 正在修复补丁
  | "REVIEWING_PATCH"    // 正在评审补丁
  | "CANCEL_REQUESTED"   // 已请求取消
  | "CANCELLED"          // 已取消
  | "VERIFY_FAILED"      // 验证失败
  | "WAITING_CONFIRM"    // 等待用户确认
  | "COMPLETED"          // 已完成
  | "FAILED";            // 失败

/** Agent 自动化修复任务 */
export interface AgentTask {
  /** 任务唯一标识 */
  id: string;
  /** 所属用户 ID */
  userId: string;
  /** 关联的项目 ID */
  projectId: string;
  /** 问题标题 */
  issueTitle: string;
  /** 问题详细描述 */
  issueDescription: string;
  /** 任务当前状态 */
  status: AgentTaskStatus | string;
  /** 任务结果摘要 */
  resultSummary?: string | null;
  /** 错误信息（任务失败时） */
  errorMessage?: string | null;
  /** 使用的 LLM 提供商 */
  llmProvider?: string | null;
  /** 使用的 LLM 模型名称 */
  llmModelName?: string | null;
  /** 使用的 LLM 模型显示名称 */
  llmDisplayName?: string | null;
  /** 任务来源类型：手动创建、GitHub Issue 或 Sentry 告警 */
  sourceType?: "MANUAL" | "GITHUB_ISSUE" | "SENTRY_ALERT" | string | null;
  /** 来源的关联 ID（如 Issue 编号或 Sentry 事件 ID） */
  sourceId?: string | number | null;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 创建 Agent 任务请求参数 */
export interface CreateTaskRequest {
  /** 关联的项目 ID */
  projectId: string;
  /** 问题标题 */
  issueTitle: string;
  /** 问题详细描述 */
  issueDescription: string;
}
