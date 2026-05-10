export type AgentTaskStatus =
  | "PENDING"
  | "CLONING"
  | "RETRIEVING"
  | "ANALYZING"
  | "GENERATING_PATCH"
  | "VERIFYING"
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
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  projectId: string;
  issueTitle: string;
  issueDescription: string;
}
