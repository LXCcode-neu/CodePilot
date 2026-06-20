package com.codepliot.service.llm;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.LlmAvailableModelVO;
import com.codepliot.model.LlmProviderVO;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * LLM 提供商服务。
 * <p>
 * 管理系统支持的所有 LLM 提供商及其模型配置，包括：
 * <ul>
 *   <li>维护支持的 LLM 提供商列表（DeepSeek、OpenAI、Kimi、MiniMax、GLM、MiMo 等）</li>
 *   <li>提供各提供商的可用模型查询</li>
 *   <li>根据提供商和模型名称解析具体的模型配置</li>
 * </ul>
 * <p>
 * 所有提供商和模型信息以静态配置方式存储在内存中。
 */
@Service
public class LlmProviderService {

    private static final List<LlmProviderVO> PROVIDERS = List.of(
            provider("deepseek", "DeepSeek", "https://api.deepseek.com", true, List.of(
                    model("deepseek", "deepseek-chat", "DeepSeek Chat", "https://api.deepseek.com", true),
                    model("deepseek", "deepseek-reasoner", "DeepSeek Reasoner", "https://api.deepseek.com", true),
                    model("deepseek", "deepseek-v4-flash", "DeepSeek V4 Flash", "https://api.deepseek.com", true)
            )),
            provider("openai", "OpenAI", "https://api.openai.com/v1", true, List.of(
                    model("openai", "gpt-4.1", "GPT-4.1", "https://api.openai.com/v1", true),
                    model("openai", "gpt-4.1-mini", "GPT-4.1 Mini", "https://api.openai.com/v1", true),
                    model("openai", "gpt-4o-mini", "GPT-4o Mini", "https://api.openai.com/v1", true)
            )),
            provider("kimi", "Kimi", "https://api.moonshot.cn/v1", true, List.of(
                    model("kimi", "moonshot-v1-8k", "Moonshot v1 8K", "https://api.moonshot.cn/v1", true),
                    model("kimi", "moonshot-v1-32k", "Moonshot v1 32K", "https://api.moonshot.cn/v1", true),
                    model("kimi", "moonshot-v1-128k", "Moonshot v1 128K", "https://api.moonshot.cn/v1", true)
            )),
            provider("minimax", "MiniMax", "https://api.minimax.chat/v1", true, List.of(
                    model("minimax", "MiniMax-Text-01", "MiniMax Text 01", "https://api.minimax.chat/v1", true)
            )),
            provider("glm", "GLM", "https://open.bigmodel.cn/api/paas/v4", true, List.of(
                    model("glm", "glm-4-plus", "GLM-4 Plus", "https://open.bigmodel.cn/api/paas/v4", true),
                    model("glm", "glm-4-flash", "GLM-4 Flash", "https://open.bigmodel.cn/api/paas/v4", true)
            )),
            provider("mimo", "MiMo", "", true, List.of(
                    model("mimo", "mimo", "MiMo", "", true)
            )),
            provider("mock", "Mock", "http://mock.local", true, List.of(
                    model("mock", "mock", "Mock Model", "http://mock.local", true)
            ))
    );

    /**
     * 获取所有支持的 LLM 提供商列表。
     *
     * @return 提供商列表
     */
    public List<LlmProviderVO> listProviders() {
        return PROVIDERS;
    }

    /**
     * 获取所有提供商下的所有可用模型列表。
     *
     * @return 可用模型列表
     */
    public List<LlmAvailableModelVO> listAvailableModels() {
        return PROVIDERS.stream()
                .flatMap(provider -> provider.defaultModels().stream())
                .toList();
    }

    /**
     * 根据提供商标识获取提供商配置，若不存在则抛出异常。
     *
     * @param provider 提供商标识（如 deepseek、openai）
     * @return 提供商配置
     */
    public LlmProviderVO requireProvider(String provider) {
        String normalized = normalize(provider);
        return PROVIDERS.stream()
                .filter(item -> item.provider().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported LLM provider"));
    }

    /**
     * 解析并返回指定提供商下的模型配置。
     * <p>
     * 若模型名称在默认模型列表中不存在，则使用传入的参数动态构建模型配置。
     *
     * @param provider    提供商标识
     * @param modelName   模型名称
     * @param displayName 显示名称
     * @param baseUrl     API 基础 URL
     * @return 解析后的模型配置
     */
    public LlmAvailableModelVO resolveModel(String provider, String modelName, String displayName, String baseUrl) {
        LlmProviderVO providerMetadata = requireProvider(provider);
        String normalizedModelName = modelName == null ? "" : modelName.trim();
        if (normalizedModelName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Model name is required");
        }
        return providerMetadata.defaultModels().stream()
                .filter(model -> model.modelName().equals(normalizedModelName))
                .findFirst()
                .orElseGet(() -> new LlmAvailableModelVO(
                        providerMetadata.provider(),
                        normalizedModelName,
                        displayName == null || displayName.isBlank() ? normalizedModelName : displayName.trim(),
                        baseUrl == null || baseUrl.isBlank() ? providerMetadata.defaultBaseUrl() : baseUrl.trim(),
                        providerMetadata.supportsTools()
                ));
    }

    private static LlmProviderVO provider(String provider,
                                          String displayName,
                                          String defaultBaseUrl,
                                          boolean supportsTools,
                                          List<LlmAvailableModelVO> defaultModels) {
        return new LlmProviderVO(provider, displayName, defaultBaseUrl, defaultModels, supportsTools);
    }

    private static LlmAvailableModelVO model(String provider,
                                             String modelName,
                                             String displayName,
                                             String defaultBaseUrl,
                                             boolean supportsTools) {
        return new LlmAvailableModelVO(provider, modelName, displayName, defaultBaseUrl, supportsTools);
    }

    private String normalize(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }
}
