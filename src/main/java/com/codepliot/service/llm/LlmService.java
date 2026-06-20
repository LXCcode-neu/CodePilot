package com.codepliot.service.llm;

import com.codepliot.client.LlmClient;
import com.codepliot.config.LlmProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LLM（大语言模型）调用服务。
 * <p>
 * 统一封装 LLM 模型的调用逻辑，支持：
 * <ul>
 *   <li>根据提供商自动路由到对应的 LLM 客户端实现</li>
 *   <li>使用默认配置或自定义运行时配置调用模型</li>
 *   <li>纯文本生成（generate）和工具调用对话（chatWithTools）两种模式</li>
 * </ul>
 * <p>
 * 支持通过构造函数注入自定义 LLM 客户端或生成函数，便于测试和扩展。
 */
@Service
public class LlmService {

    private final LlmProperties llmProperties;
    private final Map<String, LlmClient> clients;

    @Autowired
    public LlmService(LlmProperties llmProperties, List<LlmClient> clients) {
        this.llmProperties = llmProperties;
        this.clients = clients.stream()
                .flatMap(client -> client.supportedProviders().stream()
                        .map(provider -> Map.entry(normalizeProvider(provider), client)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public LlmService(LlmClient llmClient) {
        this.llmProperties = new LlmProperties();
        this.llmProperties.setProvider(llmClient.provider());
        this.llmProperties.setModel("mock");
        this.llmProperties.setBaseUrl("http://mock.local");
        this.llmProperties.setApiKey("mock");
        this.clients = Map.of(normalizeProvider(llmClient.provider()), llmClient);
    }

    public LlmService(BiFunction<String, String, String> generateFunction) {
        this(new LlmClient() {
            @Override
            public String generate(String systemPrompt, String userPrompt) {
                return generateFunction.apply(systemPrompt, userPrompt);
            }
        });
    }

    /**
     * 使用默认配置生成文本响应。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型生成的文本
     */
    public String generate(String systemPrompt, String userPrompt) {
        return generate(defaultConfig(), systemPrompt, userPrompt);
    }

    /**
     * 使用指定的运行时配置生成文本响应。
     *
     * @param config       LLM 运行时配置（包含提供商、模型、API 密钥等）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型生成的文本
     */
    public String generate(LlmRuntimeConfig config, String systemPrompt, String userPrompt) {
        LlmRuntimeConfig resolvedConfig = config == null ? defaultConfig() : config;
        return clientFor(resolvedConfig).generate(resolvedConfig, systemPrompt, userPrompt);
    }

    /**
     * 使用默认配置进行带工具调用的对话。
     *
     * @param messages 对话消息列表
     * @param tools    可用工具定义列表
     * @return 包含模型响应和工具调用结果的响应对象
     */
    /**
     * 使用默认配置进行带工具调用的对话。
     *
     * @param messages 对话消息列表
     * @param tools    可用工具定义列表
     * @return 包含模型响应和工具调用结果的响应对象
     */
    public LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return chatWithTools(defaultConfig(), messages, tools);
    }

    public LlmToolChatResponse chatWithTools(LlmRuntimeConfig config,
                                             List<LlmMessage> messages,
                                             List<LlmToolDefinition> tools) {
        LlmRuntimeConfig resolvedConfig = config == null ? defaultConfig() : config;
        return clientFor(resolvedConfig).chatWithTools(resolvedConfig, messages, tools);
    }

    private LlmClient clientFor(LlmRuntimeConfig config) {
        String provider = normalizeProvider(config == null ? null : config.provider());
        LlmClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported LLM provider: " + provider);
        }
        return client;
    }

    private LlmRuntimeConfig defaultConfig() {
        return new LlmRuntimeConfig(
                llmProperties.getProvider(),
                llmProperties.getModel(),
                llmProperties.getModel(),
                llmProperties.getBaseUrl(),
                llmProperties.getApiKey()
        );
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "mock" : provider.trim().toLowerCase();
    }
}
