package com.codepliot.controller;

import com.codepliot.service.bot.FeishuBotEventService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot/events/feishu")
public class FeishuBotEventController {

    private final FeishuBotEventService feishuBotEventService;

    public FeishuBotEventController(FeishuBotEventService feishuBotEventService) {
        this.feishuBotEventService = feishuBotEventService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode receive(@RequestBody String body,
                            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
                            @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
                            @RequestHeader(value = "X-Lark-Signature", required = false) String signature) {
        return feishuBotEventService.handleEvent(body, timestamp, nonce, signature);
    }
}
