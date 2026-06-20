package com.codepliot.client;

import com.codepliot.config.SentryProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sentry API 客户端。
 * <p>
 * 封装 Sentry REST API，支持查询项目事件详情。
 * 用于获取 Sentry 错误告警的详细信息以进行自动分析和修复。
 * </p>
 */
@Component
public class SentryApiClient {

    /** Sentry 配置属性，包含 API 基础 URL 和认证令牌 */
    private final SentryProperties properties;

    /** REST 客户端，用于发送 HTTP 请求 */
    private final RestClient restClient;

    /**
     * 构造方法，注入配置属性并构建 REST 客户端。
     *
     * @param properties        Sentry 配置属性
     * @param restClientBuilder REST 客户端构建器
     */
    public SentryApiClient(SentryProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    /**
     * 获取指定 Sentry 项目的事件详情。
     *
     * @param organizationSlug 组织标识
     * @param projectSlug      项目标识
     * @param eventId          事件ID
     * @return 事件详情的 JSON 字符串，参数无效时返回 null
     */
    public String getProjectEvent(String organizationSlug, String projectSlug, String eventId) {
        if (isBlank(properties.getAuthToken()) || isBlank(organizationSlug) || isBlank(projectSlug) || isBlank(eventId)) {
            return null;
        }
        return restClient.get()
                .uri("/projects/{organizationSlug}/{projectSlug}/events/{eventId}/",
                        organizationSlug, projectSlug, eventId)
                .headers(this::applyHeaders)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Sentry event fetch failed: " + response.getStatusCode().value());
                })
                .body(String.class);
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.setBearerAuth(properties.getAuthToken().trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
