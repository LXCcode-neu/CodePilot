export type NotificationChannelType = "FEISHU" | "WE_COM";

export interface UserRepoWatch {
  id: string;
  projectRepoId?: string | null;
  owner: string;
  repoName: string;
  repoUrl: string;
  defaultBranch?: string | null;
  watchEnabled: boolean;
  watchMode?: string | null;
  lastCheckedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRepoWatchRequest {
  owner: string;
  repoName: string;
  repoUrl: string;
  defaultBranch?: string;
}

export interface NotificationChannel {
  id: string;
  channelType: NotificationChannelType;
  channelName?: string | null;
  webhookMasked: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNotificationChannelRequest {
  channelType: NotificationChannelType;
  channelName?: string;
  webhookUrl: string;
}

export interface NotificationSendResult {
  success: boolean;
  message: string;
}

export type GitHubIssueEventStatus =
  | "NEW"
  | "NOTIFIED"
  | "IGNORED"
  | "RUNNING"
  | "PATCH_READY"
  | "FAILED"
  | "PR_CREATED";

export interface GitHubIssueEvent {
  id: string;
  repoWatchId: string;
  projectRepoId?: string | null;
  issueNumber: number;
  issueTitle: string;
  issueBody?: string | null;
  issueUrl?: string | null;
  issueState: string;
  senderLogin?: string | null;
  status: GitHubIssueEventStatus;
  agentTaskId?: string | null;
  notifiedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GitHubIssueEventRunResult {
  taskId: string;
  status: string;
}
