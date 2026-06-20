/** LLM 可用模型信息 */
export interface LlmAvailableModel {
  /** 模型提供商（如 openai、anthropic） */
  provider: string;
  /** 模型标识名称 */
  modelName: string;
  /** 模型显示名称 */
  displayName: string;
  /** 默认 API 基础地址 */
  defaultBaseUrl: string;
  /** 是否支持工具调用（Function Calling） */
  supportsTools?: boolean;
}

/** LLM 提供商信息 */
export interface LlmProvider {
  /** 提供商标识 */
  provider: string;
  /** 提供商显示名称 */
  displayName: string;
  /** 默认 API 基础地址 */
  defaultBaseUrl: string;
  /** 该提供商下的默认模型列表 */
  defaultModels: LlmAvailableModel[];
  /** 是否支持工具调用 */
  supportsTools: boolean;
}

/** 项目的 LLM 配置信息 */
export interface ProjectLlmConfig {
  /** 关联的项目仓库 ID */
  projectRepoId: string;
  /** 模型提供商 */
  provider: string;
  /** 模型标识名称 */
  modelName: string;
  /** 模型显示名称 */
  displayName: string;
  /** API 基础地址 */
  baseUrl: string;
  /** 是否已配置 API Key */
  hasApiKey: boolean;
  /** API Key 脱敏显示 */
  apiKeyMask?: string | null;
  /** 是否启用该配置 */
  enabled: boolean;
}

/** 保存项目 LLM 配置请求参数 */
export interface SaveProjectLlmConfigRequest {
  /** 模型提供商 */
  provider: string;
  /** 模型标识名称 */
  modelName: string;
  /** 模型显示名称 */
  displayName: string;
  /** API 基础地址 */
  baseUrl: string;
  /** API Key（可选，为空则不更新） */
  apiKey?: string;
}

/** LLM 配置连通性测试结果 */
export interface LlmConfigTestResult {
  /** 测试是否成功 */
  success: boolean;
  /** 测试结果消息 */
  message: string;
}

/** LLM API Key 记录 */
export interface LlmApiKey {
  /** 记录唯一标识 */
  id: string;
  /** 自定义名称 */
  name: string;
  /** 模型提供商 */
  provider: string;
  /** 模型标识名称 */
  modelName: string;
  /** 模型显示名称 */
  displayName: string;
  /** API 基础地址 */
  baseUrl: string;
  /** API Key 脱敏显示 */
  apiKeyMask: string;
  /** 是否为当前激活的 Key */
  active: boolean;
  /** 创建时间 */
  createdAt: string;
  /** 最近使用时间 */
  lastUsedAt?: string | null;
}

/** 创建 LLM API Key 请求参数 */
export interface CreateLlmApiKeyRequest {
  /** 自定义名称 */
  name: string;
  /** 模型提供商 */
  provider: string;
  /** 模型标识名称 */
  modelName: string;
  /** 模型显示名称 */
  displayName: string;
  /** API 基础地址 */
  baseUrl: string;
  /** API Key 明文 */
  apiKey: string;
}
