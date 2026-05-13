import { request } from "@/api/request";
import type {
  LlmAvailableModel,
  LlmApiKey,
  LlmConfigTestResult,
  LlmProvider,
  CreateLlmApiKeyRequest,
  ProjectLlmConfig,
  SaveProjectLlmConfigRequest,
} from "@/types/llm-config";

export function getAvailableLlmModels() {
  return request.get<LlmAvailableModel[]>("/api/llm/models");
}

export function getLlmProviders() {
  return request.get<LlmProvider[]>("/api/llm/providers");
}

export function getGlobalLlmConfig() {
  return request.get<ProjectLlmConfig | null>("/api/llm/config");
}

export function saveGlobalLlmConfig(data: SaveProjectLlmConfigRequest) {
  return request.put<ProjectLlmConfig>("/api/llm/config", data);
}

export function createGlobalLlmApiKey(data: SaveProjectLlmConfigRequest) {
  return request.post<ProjectLlmConfig>("/api/llm/config/api-key", data);
}

export function testGlobalLlmConfig() {
  return request.post<LlmConfigTestResult>("/api/llm/config/test");
}

export function listLlmApiKeys() {
  return request.get<LlmApiKey[]>("/api/llm/api-keys");
}

export function createLlmApiKey(data: CreateLlmApiKeyRequest) {
  return request.post<LlmApiKey>("/api/llm/api-keys", data);
}

export function applyLlmApiKey(id: string) {
  return request.put<LlmApiKey>(`/api/llm/api-keys/${id}/apply`);
}

export function deleteLlmApiKey(id: string) {
  return request.delete<void>(`/api/llm/api-keys/${id}`);
}

export function testLlmApiKey(id: string) {
  return request.post<LlmConfigTestResult>(`/api/llm/api-keys/${id}/test`);
}

export function getProjectLlmConfig(projectId: string) {
  return request.get<ProjectLlmConfig | null>(`/api/projects/${projectId}/llm-config`);
}

export function saveProjectLlmConfig(projectId: string, data: SaveProjectLlmConfigRequest) {
  return request.put<ProjectLlmConfig>(`/api/projects/${projectId}/llm-config`, data);
}

export function testProjectLlmConfig(projectId: string) {
  return request.post<LlmConfigTestResult>(`/api/projects/${projectId}/llm-config/test`);
}
