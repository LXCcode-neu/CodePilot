package com.codepliot.service;

import com.codepliot.model.NotificationChannelType;
import com.codepliot.model.NotificationMessage;

public interface NotificationSender {

    NotificationChannelType type();

    void send(String webhookUrl, NotificationMessage message);
}
