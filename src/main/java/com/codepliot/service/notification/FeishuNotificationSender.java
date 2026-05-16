package com.codepliot.service.notification;

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
        StringBuilder text = new StringBuilder();
        text.append(message.title()).append("\n\n").append(message.content());
        if (message.linkUrl() != null && !message.linkUrl().isBlank()) {
            text.append("\n\n").append(message.linkUrl());
        }
        if (message.actions() != null && !message.actions().isEmpty()) {
            text.append("\n\n操作：");
            message.actions().forEach(action -> text
                    .append("\n[")
                    .append(action.label())
                    .append("] ")
                    .append(action.url()));
        }
        return text.toString();
    }
}
