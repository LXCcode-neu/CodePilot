package com.codepliot.controller;

import com.codepliot.service.bot.FeishuBotEventService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞书机器人事件控制器
 * <p>
 * 接收飞书开放平台推送的事件回调，包括消息事件、交互事件等，
 * 并通过签名校验确保请求来源的合法性。
 * </p>
 */
@RestController
@RequestMapping("/api/bot/events/feishu")
public class FeishuBotEventController {

    private final FeishuBotEventService feishuBotEventService;

    public FeishuBotEventController(FeishuBotEventService feishuBotEventService) {
        this.feishuBotEventService = feishuBotEventService;
    }

    /**
     * 接收飞书平台推送的事件回调
     *
     * @param body      请求体 JSON 字符串
     * @param timestamp 请求时间戳（用于签名校验）
     * @param nonce     随机字符串（用于签名校验）
     * @param signature 签名值（用于验证请求合法性）
     * @return 处理结果 JSON 节点
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode receive(@RequestBody String body,
                            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
                            @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
                            @RequestHeader(value = "X-Lark-Signature", required = false) String signature) {
        return feishuBotEventService.handleEvent(body, timestamp, nonce, signature);
    }
}
