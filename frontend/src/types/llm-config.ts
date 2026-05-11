export interface LlmAvailableModel {
  provider: string;
  modelName: string;
  displayName: string;
  defaultBaseUrl: string;
}

export interface ProjectLlmConfig {
  projectRepoId: string;
  provider: string;
  modelName: string;
  displayName: string;
  baseUrl: string;
  hasApiKey: boolean;
  apiKeyMask?: string | null;
  enabled: boolean;
}

export interface SaveProjectLlmConfigRequest {
  provider: string;
  modelName: string;
  displayName: string;
  baseUrl: string;
  apiKey?: string;
}

export interface LlmConfigTestResult {
  success: boolean;
  message: string;
}

export interface LlmApiKey {
  id: string;
  name: string;
  provider: string;
  modelName: string;
  displayName: string;
  baseUrl: string;
  apiKeyMask: string;
  active: boolean;
  createdAt: string;
  lastUsedAt?: string | null;
}

export interface CreateLlmApiKeyRequest {
  name: string;
  provider: string;
  modelName: string;
  displayName: string;
  baseUrl: string;
  apiKey: string;
}
