import { request } from "@/api/request";
import type {
  CreateNotificationChannelRequest,
  CreateRepoWatchRequest,
  GitHubIssueEvent,
  GitHubIssueEventRunResult,
  GitHubIssueEventStatus,
  NotificationChannel,
  NotificationSendResult,
  UserRepoWatch,
} from "@/types/issue-automation";

export function getRepoWatches() {
  return request.get<UserRepoWatch[]>("/api/repo-watches");
}

export function createRepoWatch(data: CreateRepoWatchRequest) {
  return request.post<UserRepoWatch>("/api/repo-watches", data);
}

export function updateRepoWatchEnabled(id: string, enabled: boolean) {
  return request.put<UserRepoWatch>(`/api/repo-watches/${id}/enabled`, { enabled });
}

export function getNotificationChannels() {
  return request.get<NotificationChannel[]>("/api/notification/channels");
}

export function createNotificationChannel(data: CreateNotificationChannelRequest) {
  return request.post<NotificationChannel>("/api/notification/channels", data);
}

export function testNotificationChannel(id: string) {
  return request.post<NotificationSendResult>(`/api/notification/channels/${id}/test`);
}

export function deleteNotificationChannel(id: string) {
  return request.delete<void>(`/api/notification/channels/${id}`);
}

export function getGitHubIssueEvents(params: {
  status?: GitHubIssueEventStatus | "ALL";
  page?: number;
  pageSize?: number;
}) {
  const status = params.status && params.status !== "ALL" ? params.status : undefined;
  return request.get<GitHubIssueEvent[]>("/api/issues/events", {
    params: { status, page: params.page, pageSize: params.pageSize },
  });
}

export function ignoreGitHubIssueEvent(id: string) {
  return request.post<GitHubIssueEvent>(`/api/issues/events/${id}/ignore`);
}

export function runGitHubIssueEvent(id: string) {
  return request.post<GitHubIssueEventRunResult>(`/api/issues/events/${id}/run`);
}
