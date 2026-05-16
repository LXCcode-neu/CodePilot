import { request } from "@/api/request";
import type { SaveSentryProjectMappingRequest, SentryProjectMapping } from "@/types/sentry-config";

export function getProjectSentryConfig(projectId: string) {
  return request.get<SentryProjectMapping | null>(`/api/projects/${projectId}/sentry-config`);
}

export function saveProjectSentryConfig(projectId: string, data: SaveSentryProjectMappingRequest) {
  return request.put<SentryProjectMapping>(`/api/projects/${projectId}/sentry-config`, data);
}

export function deleteProjectSentryConfig(projectId: string) {
  return request.delete<void>(`/api/projects/${projectId}/sentry-config`);
}
