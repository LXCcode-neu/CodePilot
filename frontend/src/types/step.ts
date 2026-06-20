/** Agent 步骤执行状态 */
export type AgentStepStatus = "RUNNING" | "SUCCESS" | "FAILED";

/** Agent 步骤类型，定义自动化修复流程的各个阶段 */
export type AgentStepType =
  | "CLONE_REPOSITORY"     // 克隆仓库
  | "SEARCH_RELEVANT_CODE" // 搜索相关代码
  | "ANALYZE_ISSUE"        // 分析问题
  | "GENERATE_PATCH"       // 生成补丁
  | "VERIFY_PATCH"         // 验证补丁
  | "REPAIR_PATCH"         // 修复补丁
  | "REVIEW_PATCH"         // 评审补丁
  | "COMPLETE_RUN";        // 完成运行

/** Agent 任务步骤详情 */
export interface AgentStep {
  /** 步骤唯一标识 */
  id: string;
  /** 关联的任务 ID */
  taskId: string;
  /** 步骤类型 */
  stepType: AgentStepType | string;
  /** 步骤显示名称 */
  stepName: string;
  /** 步骤输入内容 */
  input?: string | null;
  /** 步骤输出内容 */
  output?: string | null;
  /** 步骤执行状态 */
  status: AgentStepStatus | string;
  /** 错误信息（执行失败时） */
  errorMessage?: string | null;
  /** 步骤开始时间 */
  startTime?: string | null;
  /** 步骤结束时间 */
  endTime?: string | null;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}
