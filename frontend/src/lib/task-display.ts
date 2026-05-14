import type { AgentStepType } from "@/types/step";

const STEP_TYPE_LABELS: Record<AgentStepType, string> = {
  CLONE_REPOSITORY: "Clone repository",
  SEARCH_RELEVANT_CODE: "Search code",
  ANALYZE_ISSUE: "Analyze issue",
  GENERATE_PATCH: "Generate patch",
  VERIFY_PATCH: "Verify patch",
  REPAIR_PATCH: "Repair patch",
  COMPLETE_RUN: "Complete run",
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
    return "Unknown step";
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
