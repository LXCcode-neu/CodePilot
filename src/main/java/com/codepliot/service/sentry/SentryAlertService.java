package com.codepliot.service.sentry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.SentryApiClient;
import com.codepliot.config.SentryProperties;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.entity.SentryAlertEvent;
import com.codepliot.entity.SentryProjectMapping;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.SentryAlertContext;
import com.codepliot.model.SentryAlertTaskCreateResult;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.repository.SentryAlertEventMapper;
import com.codepliot.service.agent.AgentRunService;
import com.codepliot.service.task.AgentTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentryAlertService {

    private static final int MAX_TEXT_LENGTH = 65535;
    private final SentryProperties sentryProperties;
    private final SentryAlertEventMapper sentryAlertEventMapper;
    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;
    private final SentryApiClient sentryApiClient;
    private final SentryProjectMappingService sentryProjectMappingService;
    private final ObjectMapper objectMapper;

    public SentryAlertService(SentryProperties sentryProperties,
                              SentryAlertEventMapper sentryAlertEventMapper,
                              AgentTaskMapper agentTaskMapper,
                              ProjectRepoMapper projectRepoMapper,
                              AgentTaskService agentTaskService,
                              AgentRunService agentRunService,
                              SentryApiClient sentryApiClient,
                              SentryProjectMappingService sentryProjectMappingService,
                              ObjectMapper objectMapper) {
        this.sentryProperties = sentryProperties;
        this.sentryAlertEventMapper = sentryAlertEventMapper;
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
        this.sentryApiClient = sentryApiClient;
        this.sentryProjectMappingService = sentryProjectMappingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SentryAlertTaskCreateResult receive(JsonNode payload) {
        if (!sentryProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Sentry webhook integration is disabled");
        }

        SentryAlertContext context = extractContext(payload);
        ResolvedProject resolvedProject = resolveProject(context.organizationSlug(), context.projectSlug());
        ProjectRepo projectRepo = resolvedProject.projectRepo();
        String dedupeKey = buildDedupeKey(context);
        SentryAlertEvent existingActiveAlert = findExistingActiveAlert(dedupeKey);
        if (existingActiveAlert != null) {
            return new SentryAlertTaskCreateResult(
                    existingActiveAlert.getId(),
                    existingActiveAlert.getAgentTaskId(),
                    "DUPLICATED",
                    "Active Sentry alert task already exists"
            );
        }

        SentryAlertEvent alertEvent = saveReceivedAlert(context, dedupeKey);
        if (projectRepo == null) {
            markAlert(alertEvent, "IGNORED", null, "Sentry project is not mapped to a CodePilot project");
            return new SentryAlertTaskCreateResult(alertEvent.getId(), null, "IGNORED",
                    "Sentry project is not mapped to a CodePilot project");
        }

        String enrichedEvent = fetchEnrichedEvent(context);
        if (enrichedEvent != null) {
            alertEvent.setEnrichedContext(truncate(enrichedEvent));
            alertEvent.setStatus("ENRICHED");
            sentryAlertEventMapper.updateById(alertEvent);
        }

        AgentTaskVO task = agentTaskService.createFromSentryAlert(
                projectRepo.getUserId(),
                projectRepo.getId(),
                alertEvent.getId(),
                buildIssueTitle(context),
                buildIssueDescription(context, enrichedEvent)
        );
        markAlert(alertEvent, "TASK_CREATED", task.id(), null);

        if (resolvedProject.autoRunEnabled()) {
            agentRunService.run(task.id(), projectRepo.getUserId());
            markAlert(alertEvent, "RUNNING", task.id(), null);
        }

        return new SentryAlertTaskCreateResult(alertEvent.getId(), task.id(), alertEvent.getStatus(),
                resolvedProject.autoRunEnabled() ? "Sentry alert task created and submitted" : "Sentry alert task created");
    }

    private SentryAlertContext extractContext(JsonNode payload) {
        JsonNode data = payload.path("data");
        JsonNode event = firstPresent(data.path("event"), payload.path("event"));
        JsonNode issue = firstPresent(data.path("issue"), payload.path("issue"));
        JsonNode project = firstPresent(data.path("project"), payload.path("project"), event.path("project"));
        JsonNode rule = firstPresent(data.path("triggered_rule"), data.path("rule"), payload.path("rule"));

        String rawPayload = toJson(payload);
        String projectSlug = firstText(project, "slug", "name");
        String organizationSlug = firstNonBlank(
                text(payload.path("installation"), "organization_slug"),
                text(payload.path("organization"), "slug"),
                sentryProperties.getOrganizationSlug()
        );
        String issueId = firstNonBlank(text(issue, "id"), text(event, "groupID"), text(event, "group_id"));
        String eventId = firstNonBlank(text(event, "event_id"), text(event, "id"));
        String title = firstNonBlank(text(issue, "title"), text(event, "title"), text(event, "message"), "Sentry alert");

        return new SentryAlertContext(
                organizationSlug,
                projectSlug,
                issueId,
                eventId,
                firstNonBlank(text(rule, "id"), text(payload, "rule_id")),
                firstNonBlank(text(rule, "label"), text(rule, "name"), text(payload, "rule")),
                title,
                firstNonBlank(text(event, "level"), text(issue, "level")),
                firstNonBlank(text(event, "platform"), text(issue, "platform")),
                firstNonBlank(text(event, "culprit"), text(issue, "culprit")),
                firstNonBlank(text(event, "web_url"), text(issue, "permalink"), text(issue, "web_url")),
                firstNonBlank(text(event, "fingerprint"), text(issue, "fingerprint")),
                rawPayload,
                null
        );
    }

    private ResolvedProject resolveProject(String organizationSlug, String sentryProjectSlug) {
        if (isBlank(sentryProjectSlug)) {
            return new ResolvedProject(null, false);
        }
        SentryProjectMapping mapping = sentryProjectMappingService.findEnabledMapping(organizationSlug, sentryProjectSlug);
        if (mapping != null) {
            ProjectRepo projectRepo = projectRepoMapper.selectById(mapping.getProjectId());
            return new ResolvedProject(projectRepo,
                    sentryProperties.isAutoRunEnabled() && Boolean.TRUE.equals(mapping.getAutoRunEnabled()));
        }
        Long projectId = sentryProperties.getProjectMappings().get(sentryProjectSlug);
        if (projectId == null) {
            return new ResolvedProject(null, false);
        }
        return new ResolvedProject(projectRepoMapper.selectById(projectId), sentryProperties.isAutoRunEnabled());
    }

    private SentryAlertEvent findExistingActiveAlert(String dedupeKey) {
        if (isBlank(dedupeKey)) {
            return null;
        }
        return sentryAlertEventMapper.selectList(new LambdaQueryWrapper<SentryAlertEvent>()
                        .eq(SentryAlertEvent::getDedupeKey, dedupeKey)
                        .isNotNull(SentryAlertEvent::getAgentTaskId)
                        .orderByDesc(SentryAlertEvent::getCreatedAt))
                .stream()
                .filter(this::hasActiveTask)
                .findFirst()
                .orElse(null);
    }

    private SentryAlertEvent saveReceivedAlert(SentryAlertContext context, String dedupeKey) {
        SentryAlertEvent alertEvent = new SentryAlertEvent();
        alertEvent.setSentryOrganizationSlug(context.organizationSlug());
        alertEvent.setSentryProjectSlug(context.projectSlug());
        alertEvent.setSentryIssueId(context.issueId());
        alertEvent.setSentryEventId(context.eventId());
        alertEvent.setSentryAlertRuleId(context.alertRuleId());
        alertEvent.setSentryAlertRuleName(context.alertRuleName());
        alertEvent.setFingerprint(context.fingerprint());
        alertEvent.setTitle(truncate(context.title(), 512));
        alertEvent.setLevel(context.level());
        alertEvent.setPlatform(context.platform());
        alertEvent.setCulprit(truncate(context.culprit(), 512));
        alertEvent.setWebUrl(truncate(context.webUrl(), 1024));
        alertEvent.setStatus("RECEIVED");
        alertEvent.setDedupeKey(dedupeKey);
        alertEvent.setRawPayload(truncate(context.rawPayload()));
        sentryAlertEventMapper.insert(alertEvent);
        return alertEvent;
    }

    private String fetchEnrichedEvent(SentryAlertContext context) {
        try {
            return sentryApiClient.getProjectEvent(context.organizationSlug(), context.projectSlug(), context.eventId());
        } catch (BusinessException exception) {
            return null;
        }
    }

    private String buildIssueTitle(SentryAlertContext context) {
        String title = firstNonBlank(context.title(), "Sentry alert");
        return "[Sentry] " + title;
    }

    private String buildIssueDescription(SentryAlertContext context, String enrichedEvent) {
        StringBuilder builder = new StringBuilder();
        builder.append("Sentry alert triggered. Diagnose the root cause, implement a focused fix, and run automatic verification before confirmation.\n\n");
        appendLine(builder, "Organization", context.organizationSlug());
        appendLine(builder, "Sentry project", context.projectSlug());
        appendLine(builder, "Issue ID", context.issueId());
        appendLine(builder, "Event ID", context.eventId());
        appendLine(builder, "Alert rule", context.alertRuleName());
        appendLine(builder, "Level", context.level());
        appendLine(builder, "Platform", context.platform());
        appendLine(builder, "Culprit", context.culprit());
        appendLine(builder, "Sentry URL", context.webUrl());
        builder.append("\nRaw webhook payload:\n```json\n")
                .append(truncate(context.rawPayload(), 12000))
                .append("\n```\n");
        if (!isBlank(enrichedEvent)) {
            builder.append("\nSentry event detail:\n```json\n")
                    .append(truncate(enrichedEvent, 20000))
                    .append("\n```\n");
        }
        return builder.toString();
    }

    private void markAlert(SentryAlertEvent alertEvent, String status, Long taskId, String message) {
        alertEvent.setStatus(status);
        alertEvent.setAgentTaskId(taskId);
        if (!isBlank(message)) {
            alertEvent.setEnrichedContext(message);
        }
        sentryAlertEventMapper.updateById(alertEvent);
    }

    private String buildDedupeKey(SentryAlertContext context) {
        String stableId = firstNonBlank(context.issueId(), context.fingerprint(), context.eventId());
        return "sentry:" + nullToEmpty(context.organizationSlug()) + ":" + nullToEmpty(context.projectSlug()) + ":" + nullToEmpty(stableId);
    }

    private boolean hasActiveTask(SentryAlertEvent alert) {
        if (alert.getAgentTaskId() == null) {
            return false;
        }
        AgentTask task = agentTaskMapper.selectById(alert.getAgentTaskId());
        return task != null && isActiveTaskStatus(task.getStatus());
    }

    private boolean isActiveTaskStatus(String status) {
        return List.of(
                AgentTaskStatus.PENDING.name(),
                AgentTaskStatus.CLONING.name(),
                AgentTaskStatus.RETRIEVING.name(),
                AgentTaskStatus.ANALYZING.name(),
                AgentTaskStatus.GENERATING_PATCH.name(),
                AgentTaskStatus.VERIFYING.name(),
                AgentTaskStatus.REPAIRING_PATCH.name(),
                AgentTaskStatus.WAITING_CONFIRM.name(),
                AgentTaskStatus.CANCEL_REQUESTED.name()
        ).contains(status);
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (!isBlank(value)) {
            builder.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }

    private JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return objectMapper.nullNode();
    }

    private String firstText(JsonNode node, String... fields) {
        if (node != null && node.isTextual()) {
            return node.asText();
        }
        for (String field : fields) {
            String value = text(node, field);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isArray()) {
            return toJson(value);
        }
        return value.asText();
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception exception) {
            return String.valueOf(node);
        }
    }

    private String truncate(String value) {
        return truncate(value, MAX_TEXT_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResolvedProject(ProjectRepo projectRepo, boolean autoRunEnabled) {
    }
}
