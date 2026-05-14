package com.codepliot.model;

public record BotCommand(
        BotCommandType type,
        String actionCode,
        String rawText
) {
}
