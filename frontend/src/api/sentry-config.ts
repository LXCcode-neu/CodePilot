import { request } from "@/api/request";
import type { SaveSentryProjectMappingRequest, SentryProjectMapping } from "@/types/sentry-config";

/**
 * 获取指定项目的 Sentry 配置
 * @param projectId - 项目 ID
 * @returns 返回 Sentry 项目映射配置，未配置时返回 null
 */
export function getProjectSentryConfig(projectId: string) {
  return request.get<SentryProjectMapping | null>(`/api/projects/${projectId}/sentry-config`);
}

/**
 * 保存指定项目的 Sentry 配置
 * @param projectId - 项目 ID
 * @param data - Sentry 项目映射的保存请求参数
 * @returns 返回保存后的 Sentry 项目映射配置
 */
export function saveProjectSentryConfig(projectId: string, data: SaveSentryProjectMappingRequest) {
  return request.put<SentryProjectMapping>(`/api/projects/${projectId}/sentry-config`, data);
}

/**
 * 删除指定项目的 Sentry 配置
 * @param projectId - 项目 ID
 * @returns 无返回值
 */
export function deleteProjectSentryConfig(projectId: string) {
  return request.delete<void>(`/api/projects/${projectId}/sentry-config`);
}
