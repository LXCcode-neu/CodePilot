package com.codepliot.controller;

import com.codepliot.config.SentryProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.Result;
import com.codepliot.model.SentryAlertTaskCreateResult;
import com.codepliot.service.sentry.SentryAlertService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentry 告警控制器
 * <p>
 * 接收 Sentry 平台推送的 Webhook 告警通知，通过 Token 校验确保请求来源的合法性，
 * 并将告警信息转换为 Agent 任务进行自动处理。
 * </p>
 */
@RestController
@RequestMapping("/api/sentry")
public class SentryAlertController {

    private static final String WEBHOOK_TOKEN_HEADER = "X-CodePilot-Sentry-Token";

    private final SentryProperties sentryProperties;
    private final SentryAlertService sentryAlertService;

    public SentryAlertController(SentryProperties sentryProperties, SentryAlertService sentryAlertService) {
        this.sentryProperties = sentryProperties;
        this.sentryAlertService = sentryAlertService;
    }

    /**
     * 接收 Sentry 告警 Webhook 通知
     * <p>
     * 支持通过请求头（X-CodePilot-Sentry-Token）或查询参数（token）两种方式传递认证 Token。
     * </p>
     *
     * @param webhookToken 请求头中的 Webhook Token
     * @param queryToken   查询参数中的 Token
     * @param payload      Sentry 告警 JSON 数据
     * @return 告警任务创建结果
     */
    @PostMapping("/alerts")
    public Result<SentryAlertTaskCreateResult> receiveAlert(
            @RequestHeader(value = WEBHOOK_TOKEN_HEADER, required = false) String webhookToken,
            @RequestParam(value = "token", required = false) String queryToken,
            @RequestBody JsonNode payload) {
        requireValidToken(webhookToken, queryToken);
        return Result.success("Sentry alert received", sentryAlertService.receive(payload));
    }

    /**
     * 校验 Webhook Token 的有效性
     *
     * @param webhookToken 请求头中的 Token
     * @param queryToken   查询参数中的 Token
     * @throws BusinessException Token 无效或未配置时抛出异常
     */
    private void requireValidToken(String webhookToken, String queryToken) {
        String expectedToken = sentryProperties.getWebhookToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Sentry webhook token is not configured");
        }
        if (!expectedToken.equals(webhookToken) && !expectedToken.equals(queryToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Invalid Sentry webhook token");
        }
    }
}
