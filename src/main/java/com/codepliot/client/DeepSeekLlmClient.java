package com.codepliot.client;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
/**
 * DeepSeekLlmClient 客户端实现，负责封装外部系统调用。
 */
@Component
@ConditionalOnProperty(prefix = "codepilot.llm", name = "provider", havingValue = "deepseek")
public class DeepSeekLlmClient implements LlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
/**
 * 创建 DeepSeekLlmClient 实例。
 */
public DeepSeekLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }
    /**
     * 执行 generate 相关逻辑。
     */
@Override
    public String generate(String systemPrompt, String userPrompt) {
        validateConfiguration();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions"))
                    .header("Authorization", "Bearer " + llmProperties.getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(systemPrompt, userPrompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "DeepSeek request failed with status " + response.statusCode() + ": " + truncate(response.body())
                );
            }
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
 * 构建Request Body相关逻辑。
 */
private String buildRequestBody(String systemPrompt, String userPrompt) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", llmProperties.getModel().trim());
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
/**
 * 提取Content相关逻辑。
 */
private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.isMissingNode() || contentNode.isNull() ? null : contentNode.asText();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek response did not contain message content");
        }
        return content.trim();
    }
/**
 * 校验Configuration相关逻辑。
 */
private void validateConfiguration() {
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "使用 deepseek provider 时必须配?codepilot.llm.api-key");
        }
        if (llmProperties.getModel() == null || llmProperties.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "使用 deepseek provider 时必须配?codepilot.llm.model");
        }
        if (llmProperties.getBaseUrl() == null || llmProperties.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "使用 deepseek provider 时必须配?codepilot.llm.base-url");
        }
    }
/**
 * 规范化Base Url相关逻辑。
 */
private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
/**
 * 执行 truncate 相关逻辑。
 */
private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
/**
 * 构建Error Message相关逻辑。
 */
private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}

