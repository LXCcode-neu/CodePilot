import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "--";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function parseJsonString<T>(value?: string | null) {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export function stringifyPretty(value: unknown) {
  if (value == null) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  return JSON.stringify(value, null, 2);
}

const DISPLAY_KEY_LABELS: Record<string, string> = {
  id: "ID",
  taskId: "任务 ID",
  projectId: "项目 ID",
  patchRecordId: "Patch 记录 ID",
  stepType: "步骤类型",
  stepName: "步骤名称",
  status: "状态",
  taskStatus: "任务状态",
  phase: "阶段",
  success: "是否成功",
  message: "消息",
  errorMessage: "错误信息",
  data: "数据",
  input: "输入",
  output: "输出",
  analysis: "问题分析",
  solution: "修复方案",
  risk: "风险",
  patch: "Patch 内容",
  rawOutput: "原始输出",
  safetyCheckResult: "安全检查结果",
  command: "命令",
  commandName: "命令名称",
  exitCode: "退出码",
  stdout: "标准输出",
  stderr: "错误输出",
  startedAt: "开始时间",
  finishedAt: "结束时间",
  startTime: "开始时间",
  endTime: "结束时间",
  durationMs: "耗时毫秒",
  passed: "是否通过",
  score: "评分",
  riskLevel: "风险等级",
  summary: "摘要",
  findings: "发现的问题",
  recommendations: "改进建议",
  reviewerProvider: "审查模型厂商",
  reviewerModelName: "审查模型",
  fileChanges: "文件变更",
  filePath: "文件路径",
  oldPath: "旧路径",
  newPath: "新路径",
  addedLines: "新增行数",
  removedLines: "删除行数",
  pullRequest: "Pull Request",
  title: "标题",
  body: "正文",
  branchName: "分支名",
  commitMessage: "提交信息",
  changedFiles: "变更文件数",
  touchedFiles: "涉及文件",
  ready: "是否就绪",
};

const DISPLAY_VALUE_LABELS: Record<string, string> = {
  SUCCESS: "成功",
  FAILED: "失败",
  RUNNING: "运行中",
  PENDING: "待执行",
  CLONING: "克隆中",
  RETRIEVING: "检索中",
  ANALYZING: "分析中",
  GENERATING_PATCH: "生成 Patch 中",
  VERIFYING: "验证中",
  REPAIRING_PATCH: "修复 Patch 中",
  REVIEWING_PATCH: "AI 审查中",
  WAITING_CONFIRM: "待确认",
  VERIFY_FAILED: "验证失败",
  COMPLETED: "已完成",
  CANCEL_REQUESTED: "取消中",
  CANCELLED: "已取消",
  CLONE_REPOSITORY: "克隆仓库",
  SEARCH_RELEVANT_CODE: "检索相关代码",
  ANALYZE_ISSUE: "分析问题",
  GENERATE_PATCH: "生成 Patch",
  VERIFY_PATCH: "验证 Patch",
  REPAIR_PATCH: "修复 Patch",
  REVIEW_PATCH: "AI 代码审查",
  COMPLETE_RUN: "完成执行",
  STARTED: "已开始",
};

export function localizeDisplayValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(localizeDisplayValue);
  }

  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [DISPLAY_KEY_LABELS[key] ?? key, localizeDisplayValue(item)])
    );
  }

  if (typeof value === "string") {
    return DISPLAY_VALUE_LABELS[value] ?? value;
  }

  return value;
}

export function stringifyDisplay(value: unknown) {
  return stringifyPretty(localizeDisplayValue(value));
}

export function isRunningTask(status?: string | null) {
  return [
    "CLONING",
    "RETRIEVING",
    "ANALYZING",
    "GENERATING_PATCH",
    "VERIFYING",
    "REPAIRING_PATCH",
    "REVIEWING_PATCH",
    "CANCEL_REQUESTED",
  ].includes(status ?? "");
}

export function isFinishedTask(status?: string | null) {
  return ["COMPLETED", "FAILED", "VERIFY_FAILED", "CANCELLED"].includes(status ?? "");
}
