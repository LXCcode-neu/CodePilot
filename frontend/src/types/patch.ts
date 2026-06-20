/** 补丁记录，包含 AI 生成的代码补丁及其元数据 */
export interface PatchRecord {
  /** 补丁记录唯一标识 */
  id?: string | number | null;
  /** 关联的任务 ID */
  taskId?: string | number | null;
  /** 问题分析结果 */
  analysis?: string | null;
  /** 解决方案描述 */
  solution?: string | null;
  /** 生成的补丁内容（unified diff 格式） */
  patch?: string | null;
  /** 风险评估 */
  risk?: string | null;
  /** 安全检查结果 */
  safetyCheckResult?: string | null;
  /** LLM 原始输出 */
  rawOutput?: string | null;
  /** 是否已由用户确认 */
  confirmed?: boolean | null;
  /** 用户确认时间 */
  confirmedAt?: string | null;
  /** 是否已提交 PR */
  prSubmitted?: boolean | null;
  /** PR 提交时间 */
  prSubmittedAt?: string | null;
  /** PR 网页地址 */
  prUrl?: string | null;
  /** PR 编号 */
  prNumber?: number | null;
  /** PR 分支名称 */
  prBranch?: string | null;
  /** 创建时间 */
  createdAt?: string | null;
  /** 更新时间 */
  updatedAt?: string | null;
  /** 补丁涉及的文件变更列表 */
  fileChanges?: PatchFileChange[];
  /** PR 预览信息 */
  pullRequest?: PullRequestPreview | null;
  /** 原始响应数据 */
  raw?: unknown;
}

/** PR 提交结果 */
export interface PullRequestSubmitResult {
  /** 关联的任务 ID */
  taskId: string | number;
  /** PR 编号 */
  number?: number | null;
  /** PR 网页地址 */
  url?: string | null;
  /** PR 分支名称 */
  branch: string;
  /** 提交时间 */
  submittedAt: string;
}

/** 补丁评审记录，由 LLM 对补丁进行自动化评审 */
export interface PatchReviewRecord {
  /** 评审记录唯一标识 */
  id?: string | number | null;
  /** 关联的任务 ID */
  taskId?: string | number | null;
  /** 关联的补丁记录 ID */
  patchRecordId?: string | number | null;
  /** 评审使用的模型提供商 */
  reviewerProvider?: string | null;
  /** 评审使用的模型名称 */
  reviewerModelName?: string | null;
  /** 评审是否通过 */
  passed?: boolean | null;
  /** 评审评分 */
  score?: number | null;
  /** 风险等级 */
  riskLevel?: string | null;
  /** 评审摘要 */
  summary?: string | null;
  /** 发现的问题 */
  findings?: string | null;
  /** 改进建议 */
  recommendations?: string | null;
  /** LLM 原始响应 */
  rawResponse?: string | null;
  /** 创建时间 */
  createdAt?: string | null;
  /** 更新时间 */
  updatedAt?: string | null;
}

/** 补丁 diff 行类型：新增、删除或上下文 */
export type PatchLineType = "added" | "removed" | "context";

/** 补丁 diff 中的单行变更 */
export interface PatchDiffLine {
  /** 行类型（added/removed/context） */
  type: PatchLineType | string;
  /** 原文件行号 */
  oldLineNumber?: number | null;
  /** 新文件行号 */
  newLineNumber?: number | null;
  /** 行内容 */
  content: string;
}

/** 补丁 diff 的代码块（hunk） */
export interface PatchDiffHunk {
  /** hunk 头部信息 */
  header: string;
  /** 原文件起始行号 */
  oldStart?: number | null;
  /** 原文件涉及行数 */
  oldLineCount?: number | null;
  /** 新文件起始行号 */
  newStart?: number | null;
  /** 新文件涉及行数 */
  newLineCount?: number | null;
  /** 该 hunk 包含的所有变更行 */
  lines: PatchDiffLine[];
}

/** 补丁中单个文件的变更信息 */
export interface PatchFileChange {
  /** 变更前的文件路径 */
  oldPath?: string | null;
  /** 变更后的文件路径 */
  newPath?: string | null;
  /** 当前文件路径 */
  filePath: string;
  /** 新增行数 */
  addedLines: number;
  /** 删除行数 */
  removedLines: number;
  /** 文件包含的 diff hunk 列表 */
  hunks: PatchDiffHunk[];
}

/** Pull Request 预览信息 */
export interface PullRequestPreview {
  /** PR 标题 */
  title: string;
  /** PR 分支名称 */
  branchName: string;
  /** 提交信息 */
  commitMessage: string;
  /** PR 描述正文 */
  body: string;
  /** 变更文件数量 */
  changedFiles: number;
  /** 新增行数 */
  addedLines: number;
  /** 删除行数 */
  removedLines: number;
  /** 涉及的文件路径列表 */
  touchedFiles: string[];
  /** 是否已准备好提交 */
  ready: boolean;
  /** 当前状态 */
  status: string;
}
