export type AgentTaskStatus =
  | "PENDING"
  | "CLONING"
  | "RETRIEVING"
  | "ANALYZING"
  | "GENERATING_PATCH"
  | "VERIFYING"
  | "REPAIRING_PATCH"
  | "REVIEWING_PATCH"
  | "CANCEL_REQUESTED"
  | "CANCELLED"
  | "VERIFY_FAILED"
  | "WAITING_CONFIRM"
  | "COMPLETED"
  | "FAILED";

export interface AgentTask {
  id: string;
  userId: string;
  projectId: string;
  issueTitle: string;
  issueDescription: string;
  status: AgentTaskStatus | string;
  resultSummary?: string | null;
  errorMessage?: string | null;
  llmProvider?: string | null;
  llmModelName?: string | null;
  llmDisplayName?: string | null;
  sourceType?: "MANUAL" | "GITHUB_ISSUE" | "SENTRY_ALERT" | string | null;
  sourceId?: string | number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  projectId: string;
  issueTitle: string;
  issueDescription: string;
}
