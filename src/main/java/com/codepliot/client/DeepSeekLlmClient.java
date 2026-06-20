package com.codepliot.client;

import com.codepliot.config.LlmProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.LlmToolCall;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 大语言模型客户端实现。
 * <p>
 * 通过 HTTP 调用 DeepSeek API 实现文本生成和工具调用功能，
 * 支持标准的 Chat Completions 接口格式。
 * </p>
 */
@Component
public class DeepSeekLlmClient implements LlmClient {

    /** LLM 配置属性，包含模型名称、API密钥、基础URL等 */
    private final LlmProperties llmProperties;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端，用于发送 API 请求 */
    private final HttpClient httpClient;

    /**
     * 构造方法，注入配置属性和对象映射器。
     *
     * @param llmProperties  LLM 配置属性
     * @param objectMapper   JSON 对象映射器
     */
    public DeepSeekLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 返回当前客户端对应的 LLM 提供商名称。
     *
     * @return 提供商标识 "deepseek"
     */
    @Override
    public String provider() {
        return "deepseek";
    }

    /**
     * 调用 DeepSeek API 生成文本响应。
     *
     * @param config        运行时配置（模型名称、API密钥、基础URL等）
     * @param systemPrompt  系统提示词
     * @param userPrompt    用户提示词
     * @return 生成的文本内容
     */
    @Override
    public String generate(LlmRuntimeConfig config, String systemPrompt, String userPrompt) {
        LlmRuntimeConfig resolvedConfig = resolveConfig(config);
        validateConfiguration(resolvedConfig);
        try {
            HttpResponse<String> response = sendChatCompletion(
                    resolvedConfig,
                    buildRequestBody(resolvedConfig, systemPrompt, userPrompt)
            );
            return extractContent(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to call DeepSeek API: " + buildErrorMessage(exception)
            );
        }
    }

    /**
     * 调用 DeepSeek API 进行带工具调用的对话。
     *
     * @param config   运行时配置
     * @param messages 对话消息列表
     * @param tools    可用工具定义列表
     * @return 包含响应内容、完成原因和工具调用的响应对象
     */
    @Override
    public LlmToolChatResponse chatWithTools(LlmRuntimeConfig config,
                                             List<LlmMessage> messages,
                                             List<LlmToolDefinition> tools) {
        LlmRuntimeConfig resolvedConfig = resolveConfig(config);
        validateConfiguration(resolvedConfig);
        try {
            HttpResponse<String> response = sendChatCompletion(
                    resolvedConfig,
                    buildToolRequestBody(resolvedConfig, messages, tools)
            );
            return extractToolChatResponse(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to call DeepSeek API with tools: " + buildErrorMessage(exception)
            );
        }
    }

    private HttpResponse<String> sendChatCompletion(LlmRuntimeConfig config, String requestBody)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(config.baseUrl()) + "/chat/completions"))
                .header("Authorization", "Bearer " + config.apiKey().trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "DeepSeek request failed with status " + response.statusCode() + ": " + truncate(response.body())
            );
        }
        return response;
    }

    private String buildRequestBody(LlmRuntimeConfig config, String systemPrompt, String userPrompt) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.modelName().trim());
        body.put("temperature", 0.2d);
        body.put("stream", false);

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt == null ? "" : systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt == null ? "" : userPrompt);
        return objectMapper.writeValueAsString(body);
    }

    private String buildToolRequestBody(LlmRuntimeConfig config,
                                        List<LlmMessage> messages,
                                        List<LlmToolDefinition> tools) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.modelName().trim());
        body.put("temperature", 0.2d);
        body.put("stream", false);
        body.put("tool_choice", "auto");

        ArrayNode messageNodes = body.putArray("messages");
        for (LlmMessage message : messages == null ? List.<LlmMessage>of() : messages) {
            ObjectNode messageNode = messageNodes.addObject();
            messageNode.put("role", message.role());
            if (message.content() == null) {
                messageNode.putNull("content");
            } else {
                messageNode.put("content", message.content());
            }
            if ("tool".equals(message.role())) {
                messageNode.put("tool_call_id", message.toolCallId());
            }
            if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                ArrayNode toolCalls = messageNode.putArray("tool_calls");
                for (LlmToolCall toolCall : message.toolCalls()) {
                    toolCalls.add(toToolCallNode(toolCall));
                }
            }
        }

        ArrayNode toolNodes = body.putArray("tools");
        for (LlmToolDefinition tool : tools == null ? List.<LlmToolDefinition>of() : tools) {
            ObjectNode toolNode = toolNodes.addObject();
            toolNode.put("type", "function");
            ObjectNode functionNode = toolNode.putObject("function");
            functionNode.put("name", tool.name());
            functionNode.put("description", tool.description() == null ? "" : tool.description());
            functionNode.set("parameters", tool.parameters());
        }
        return objectMapper.writeValueAsString(body);
    }

    private ObjectNode toToolCallNode(LlmToolCall toolCall) {
        ObjectNode toolCallNode = objectMapper.createObjectNode();
        toolCallNode.put("id", toolCall.id());
        toolCallNode.put("type", "function");
        ObjectNode functionNode = toolCallNode.putObject("function");
        functionNode.put("name", toolCall.name());
        functionNode.put("arguments", toolCall.arguments() == null ? "{}" : toolCall.arguments());
        return toolCallNode;
    }

    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.isMissingNode() || contentNode.isNull() ? null : contentNode.asText();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek response did not contain message content");
        }
        return content.trim();
    }

    private LlmToolChatResponse extractToolChatResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        String content = message.path("content").isMissingNode() || message.path("content").isNull()
                ? ""
                : message.path("content").asText("");
        String finishReason = choice.path("finish_reason").asText("");
        List<LlmToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallNodes = message.path("tool_calls");
        if (toolCallNodes.isArray()) {
            for (JsonNode toolCallNode : toolCallNodes) {
                JsonNode functionNode = toolCallNode.path("function");
                String id = toolCallNode.path("id").asText("");
                String name = functionNode.path("name").asText("");
                String arguments = functionNode.path("arguments").asText("{}");
                if (!id.isBlank() && !name.isBlank()) {
                    toolCalls.add(new LlmToolCall(id, name, arguments));
                }
            }
        }
        return new LlmToolChatResponse(content, finishReason, toolCalls);
    }

    private void validateConfiguration(LlmRuntimeConfig config) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DeepSeek API key is required");
        }
        if (config.modelName() == null || config.modelName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DeepSeek model is required");
        }
        if (config.baseUrl() == null || config.baseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DeepSeek base URL is required");
        }
    }

    private LlmRuntimeConfig resolveConfig(LlmRuntimeConfig config) {
        if (config != null) {
            return config;
        }
        return new LlmRuntimeConfig(
                provider(),
                llmProperties.getModel(),
                llmProperties.getModel(),
                llmProperties.getBaseUrl(),
                llmProperties.getApiKey()
        );
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
