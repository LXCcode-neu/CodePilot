import type { AgentStepType } from "@/types/step";

const STEP_TYPE_LABELS: Record<AgentStepType, string> = {
  CLONE_REPOSITORY: "\u514b\u9686\u4ed3\u5e93",
  BUILD_CODE_INDEX: "\u6784\u5efa\u4ee3\u7801\u7d22\u5f15",
  SEARCH_RELEVANT_CODE: "\u68c0\u7d22\u76f8\u5173\u4ee3\u7801",
  ANALYZE_ISSUE: "\u5206\u6790 Issue",
  GENERATE_PATCH: "\u751f\u6210 Patch",
  COMPLETE_RUN: "\u6d41\u7a0b\u5b8c\u6210",
};

/**
 * 规范化后端返回的步骤类型。
 *
 * 后端或历史数据里可能混入空白、换行、零宽字符等不可见字符，
 * 如果不先清理，前端会误判为未知类型，然后回退显示数据库里的旧乱码 stepName。
 */
function normalizeStepType(stepType?: string | null) {
  return (stepType ?? "")
    .replace(/[\u0000-\u001F\u007F-\u009F\u200B-\u200D\uFEFF]/g, "")
    .replace(/\s+/g, "")
    .trim()
    .toUpperCase();
}

export function getStepTypeLabel(stepType?: string | null) {
  const normalizedStepType = normalizeStepType(stepType);

  if (!normalizedStepType) {
    return "\u672a\u77e5\u6b65\u9aa4";
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
