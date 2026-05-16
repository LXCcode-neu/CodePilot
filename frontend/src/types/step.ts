export type AgentStepStatus = "RUNNING" | "SUCCESS" | "FAILED";

export type AgentStepType =
  | "CLONE_REPOSITORY"
  | "SEARCH_RELEVANT_CODE"
  | "ANALYZE_ISSUE"
  | "GENERATE_PATCH"
  | "VERIFY_PATCH"
  | "REPAIR_PATCH"
  | "REVIEW_PATCH"
  | "COMPLETE_RUN";

export interface AgentStep {
  id: string;
  taskId: string;
  stepType: AgentStepType | string;
  stepName: string;
  input?: string | null;
  output?: string | null;
  status: AgentStepStatus | string;
  errorMessage?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  createdAt: string;
  updatedAt: string;
}
