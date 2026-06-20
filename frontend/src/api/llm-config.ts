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

/**
 * 获取所有可用的 LLM 模型列表
 * @returns 返回可用模型列表
 */
export function getAvailableLlmModels() {
  return request.get<LlmAvailableModel[]>("/api/llm/models");
}

/**
 * 获取所有 LLM 服务商列表
 * @returns 返回 LLM 服务商列表
 */
export function getLlmProviders() {
  return request.get<LlmProvider[]>("/api/llm/providers");
}

/**
 * 获取全局 LLM 配置
 * @returns 返回全局 LLM 配置，未配置时返回 null
 */
export function getGlobalLlmConfig() {
  return request.get<ProjectLlmConfig | null>("/api/llm/config");
}

/**
 * 保存全局 LLM 配置
 * @param data - LLM 配置请求参数
 * @returns 返回保存后的全局 LLM 配置
 */
export function saveGlobalLlmConfig(data: SaveProjectLlmConfigRequest) {
  return request.put<ProjectLlmConfig>("/api/llm/config", data);
}

/**
 * 创建全局 LLM API Key
 * @param data - 包含 API Key 信息的配置请求参数
 * @returns 返回更新后的全局 LLM 配置
 */
export function createGlobalLlmApiKey(data: SaveProjectLlmConfigRequest) {
  return request.post<ProjectLlmConfig>("/api/llm/config/api-key", data);
}

/**
 * 测试全局 LLM 配置是否可用
 * @returns 返回配置测试结果
 */
export function testGlobalLlmConfig() {
  return request.post<LlmConfigTestResult>("/api/llm/config/test");
}

/**
 * 获取所有 LLM API Key 列表
 * @returns 返回 API Key 列表
 */
export function listLlmApiKeys() {
  return request.get<LlmApiKey[]>("/api/llm/api-keys");
}

/**
 * 创建新的 LLM API Key
 * @param data - 创建 API Key 的请求参数
 * @returns 返回新创建的 API Key 信息
 */
export function createLlmApiKey(data: CreateLlmApiKeyRequest) {
  return request.post<LlmApiKey>("/api/llm/api-keys", data);
}

/**
 * 应用指定的 LLM API Key（设为当前使用）
 * @param id - API Key ID
 * @returns 返回更新后的 API Key 信息
 */
export function applyLlmApiKey(id: string) {
  return request.put<LlmApiKey>(`/api/llm/api-keys/${id}/apply`);
}

/**
 * 删除指定的 LLM API Key
 * @param id - API Key ID
 * @returns 无返回值
 */
export function deleteLlmApiKey(id: string) {
  return request.delete<void>(`/api/llm/api-keys/${id}`);
}

/**
 * 测试指定的 LLM API Key 是否可用
 * @param id - API Key ID
 * @returns 返回测试结果
 */
export function testLlmApiKey(id: string) {
  return request.post<LlmConfigTestResult>(`/api/llm/api-keys/${id}/test`);
}

/**
 * 获取指定项目的 LLM 配置
 * @param projectId - 项目 ID
 * @returns 返回项目 LLM 配置，未配置时返回 null
 */
export function getProjectLlmConfig(projectId: string) {
  return request.get<ProjectLlmConfig | null>(`/api/projects/${projectId}/llm-config`);
}

/**
 * 保存指定项目的 LLM 配置
 * @param projectId - 项目 ID
 * @param data - LLM 配置请求参数
 * @returns 返回保存后的项目 LLM 配置
 */
export function saveProjectLlmConfig(projectId: string, data: SaveProjectLlmConfigRequest) {
  return request.put<ProjectLlmConfig>(`/api/projects/${projectId}/llm-config`, data);
}

/**
 * 测试指定项目的 LLM 配置是否可用
 * @param projectId - 项目 ID
 * @returns 返回配置测试结果
 */
export function testProjectLlmConfig(projectId: string) {
  return request.post<LlmConfigTestResult>(`/api/projects/${projectId}/llm-config/test`);
}
