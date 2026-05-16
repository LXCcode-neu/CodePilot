package com.codepliot.service.notification;

import com.codepliot.model.NotificationChannelType;
import com.codepliot.model.NotificationMessage;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeComNotificationSender implements NotificationSender {

    private final RestClient restClient;

    public WeComNotificationSender(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.WE_COM;
    }

    @Override
    public void send(String webhookUrl, NotificationMessage message) {
        restClient.post()
                .uri(webhookUrl)
                .body(Map.of(
                        "msgtype", "text",
                        "text", Map.of("content", buildText(message))
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
