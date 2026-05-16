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

    @PostMapping("/alerts")
    public Result<SentryAlertTaskCreateResult> receiveAlert(
            @RequestHeader(value = WEBHOOK_TOKEN_HEADER, required = false) String webhookToken,
            @RequestParam(value = "token", required = false) String queryToken,
            @RequestBody JsonNode payload) {
        requireValidToken(webhookToken, queryToken);
        return Result.success("Sentry alert received", sentryAlertService.receive(payload));
    }

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
