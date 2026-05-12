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

    public String generate(String systemPrompt, String userPrompt) {
        return generate(defaultConfig(), systemPrompt, userPrompt);
    }

    public String generate(LlmRuntimeConfig config, String systemPrompt, String userPrompt) {
        LlmRuntimeConfig resolvedConfig = config == null ? defaultConfig() : config;
        return clientFor(resolvedConfig).generate(resolvedConfig, systemPrompt, userPrompt);
    }

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
