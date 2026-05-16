package com.codepliot.model;

public record SentryAlertContext(
        String organizationSlug,
        String projectSlug,
        String issueId,
        String eventId,
        String alertRuleId,
        String alertRuleName,
        String title,
        String level,
        String platform,
        String culprit,
        String webUrl,
        String fingerprint,
        String rawPayload,
        String enrichedEvent
) {
}
