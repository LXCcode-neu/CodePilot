package com.codepliot.client;

import com.codepliot.config.SentryProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SentryApiClient {

    private final SentryProperties properties;
    private final RestClient restClient;

    public SentryApiClient(SentryProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

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
