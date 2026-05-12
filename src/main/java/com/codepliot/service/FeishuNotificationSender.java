package com.codepliot.service;

import com.codepliot.model.NotificationChannelType;
import com.codepliot.model.NotificationMessage;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FeishuNotificationSender implements NotificationSender {

    private final RestClient restClient;

    public FeishuNotificationSender(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.FEISHU;
    }

    @Override
    public void send(String webhookUrl, NotificationMessage message) {
        restClient.post()
                .uri(webhookUrl)
                .body(Map.of(
                        "msg_type", "text",
                        "content", Map.of("text", buildText(message))
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String buildText(NotificationMessage message) {
        return message.title() + "\n\n" + message.content()
                + (message.linkUrl() == null || message.linkUrl().isBlank() ? "" : "\n\n" + message.linkUrl());
    }
}
