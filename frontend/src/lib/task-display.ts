import type { AgentStepType } from "@/types/step";

const STEP_TYPE_LABELS: Record<AgentStepType, string> = {
  CLONE_REPOSITORY: "克隆仓库",
  SEARCH_RELEVANT_CODE: "检索相关代码",
  ANALYZE_ISSUE: "分析 Issue",
  GENERATE_PATCH: "生成 Patch",
  VERIFY_PATCH: "验证 Patch",
  COMPLETE_RUN: "流程完成",
};

function normalizeStepType(stepType?: string | null) {
  return (stepType ?? "")
    .replace(/[\u0000-\u001F\u007F-\u009F\u200B-\u200D\uFEFF]/g, "")
    .replace(/\s+/g, "")
    .trim()
    .toUpperCase();
}

export function isVisibleStepType(stepType?: string | null) {
  return Boolean(normalizeStepType(stepType));
}

export function getStepTypeLabel(stepType?: string | null) {
  const normalizedStepType = normalizeStepType(stepType);

  if (!normalizedStepType) {
    return "未知步骤";
  }

  return STEP_TYPE_LABELS[normalizedStepType as AgentStepType] ?? normalizedStepType;
}

export function getStepDisplayName(stepType?: string | null, stepName?: string | null) {
  const normalizedStepType = normalizeStepType(stepType);

  if (normalizedStepType in STEP_TYPE_LABELS) {
    return STEP_TYPE_LABELS[normalizedStepType as AgentStepType];
  }

  return stepName?.trim() || getStepTypeLabel(stepType);
}
