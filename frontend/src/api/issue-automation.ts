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

/**
 * 获取用户的所有仓库监听配置
 * @returns 返回仓库监听配置列表
 */
export function getRepoWatches() {
  return request.get<UserRepoWatch[]>("/api/repo-watches");
}

/**
 * 创建仓库监听配置
 * @param data - 创建仓库监听的请求参数
 * @returns 返回新创建的仓库监听配置
 */
export function createRepoWatch(data: CreateRepoWatchRequest) {
  return request.post<UserRepoWatch>("/api/repo-watches", data);
}

/**
 * 更新仓库监听的启用/禁用状态
 * @param id - 仓库监听配置 ID
 * @param enabled - 是否启用
 * @returns 返回更新后的仓库监听配置
 */
export function updateRepoWatchEnabled(id: string, enabled: boolean) {
  return request.put<UserRepoWatch>(`/api/repo-watches/${id}/enabled`, { enabled });
}

/**
 * 获取所有通知渠道
 * @returns 返回通知渠道列表
 */
export function getNotificationChannels() {
  return request.get<NotificationChannel[]>("/api/notification/channels");
}

/**
 * 创建通知渠道
 * @param data - 创建通知渠道的请求参数
 * @returns 返回新创建的通知渠道
 */
export function createNotificationChannel(data: CreateNotificationChannelRequest) {
  return request.post<NotificationChannel>("/api/notification/channels", data);
}

/**
 * 测试通知渠道是否可用
 * @param id - 通知渠道 ID
 * @returns 返回测试发送结果
 */
export function testNotificationChannel(id: string) {
  return request.post<NotificationSendResult>(`/api/notification/channels/${id}/test`);
}

/**
 * 删除通知渠道
 * @param id - 通知渠道 ID
 * @returns 无返回值
 */
export function deleteNotificationChannel(id: string) {
  return request.delete<void>(`/api/notification/channels/${id}`);
}

/**
 * 获取 GitHub Issue 事件列表（分页）
 * @param params - 查询参数
 * @param params.status - 事件状态筛选，传 "ALL" 或不传表示全部
 * @param params.page - 页码
 * @param params.pageSize - 每页数量
 * @returns 返回 GitHub Issue 事件列表
 */
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

/**
 * 忽略指定的 GitHub Issue 事件
 * @param id - 事件 ID
 * @returns 返回更新后的事件信息
 */
export function ignoreGitHubIssueEvent(id: string) {
  return request.post<GitHubIssueEvent>(`/api/issues/events/${id}/ignore`);
}

/**
 * 手动执行指定的 GitHub Issue 事件处理
 * @param id - 事件 ID
 * @returns 返回事件执行结果
 */
export function runGitHubIssueEvent(id: string) {
  return request.post<GitHubIssueEventRunResult>(`/api/issues/events/${id}/run`);
}
