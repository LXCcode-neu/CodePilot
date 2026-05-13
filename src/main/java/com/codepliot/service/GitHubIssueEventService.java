package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.GitHubIssueEvent;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubIssueEventRunResult;
import com.codepliot.model.GitHubIssueEventVO;
import com.codepliot.model.GitHubIssueVO;
import com.codepliot.repository.GitHubIssueEventMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.repository.UserRepoWatchMapper;
import com.codepliot.service.agent.AgentRunService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubIssueEventService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_NOTIFIED = "NOTIFIED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PATCH_READY = "PATCH_READY";
    private static final String STATUS_FAILED = "FAILED";

    private final GitHubIssueEventMapper gitHubIssueEventMapper;
    private final UserRepoWatchMapper userRepoWatchMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final NotificationService notificationService;
    private final NotificationTemplateFactory notificationTemplateFactory;
    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;

    public GitHubIssueEventService(GitHubIssueEventMapper gitHubIssueEventMapper,
                                   UserRepoWatchMapper userRepoWatchMapper,
                                   PatchRecordMapper patchRecordMapper,
                                   NotificationService notificationService,
                                   NotificationTemplateFactory notificationTemplateFactory,
                                   AgentTaskService agentTaskService,
                                   AgentRunService agentRunService) {
        this.gitHubIssueEventMapper = gitHubIssueEventMapper;
        this.userRepoWatchMapper = userRepoWatchMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.notificationService = notificationService;
        this.notificationTemplateFactory = notificationTemplateFactory;
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
    }

    @Transactional
    public void saveNewIssueIfAbsent(UserRepoWatch watch, GitHubIssueVO issue) {
        if (watch == null || issue == null || issue.number() == null) {
            return;
        }
        GitHubIssueEvent existing = gitHubIssueEventMapper.selectOne(new LambdaQueryWrapper<GitHubIssueEvent>()
                .eq(GitHubIssueEvent::getRepoWatchId, watch.getId())
                .eq(GitHubIssueEvent::getIssueNumber, issue.number())
                .last("limit 1"));
        if (existing != null) {
            return;
        }

        GitHubIssueEvent event = new GitHubIssueEvent();
        event.setUserId(watch.getUserId());
        event.setRepoWatchId(watch.getId());
        event.setProjectRepoId(watch.getProjectRepoId());
        event.setGithubIssueId(issue.id());
        event.setIssueNumber(issue.number());
        event.setIssueTitle(defaultText(issue.title(), "Untitled GitHub Issue"));
        event.setIssueBody(issue.body());
        event.setIssueUrl(issue.htmlUrl());
        event.setIssueState(defaultText(issue.state(), "open"));
        event.setSenderLogin(issue.authorLogin());
        event.setEventAction("opened");
        event.setStatus(STATUS_NEW);
        event.setAgentTaskId(null);
        event.setNotifiedAt(null);
        gitHubIssueEventMapper.insert(event);

        boolean notified = notificationService.sendToUser(
                watch.getUserId(),
                notificationTemplateFactory.newIssue(watch, event)
        );
        if (notified) {
            event.setStatus(STATUS_NOTIFIED);
            event.setNotifiedAt(LocalDateTime.now());
            gitHubIssueEventMapper.updateById(event);
        }
    }

    public List<GitHubIssueEventVO> listCurrentUserEvents(String status, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        LambdaQueryWrapper<GitHubIssueEvent> wrapper = new LambdaQueryWrapper<GitHubIssueEvent>()
                .eq(GitHubIssueEvent::getUserId, SecurityUtils.getCurrentUserId())
                .orderByDesc(GitHubIssueEvent::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(GitHubIssueEvent::getStatus, status.trim().toUpperCase());
        }
        return gitHubIssueEventMapper.selectList(wrapper)
                .stream()
                .skip((long) (safePage - 1) * safePageSize)
                .limit(safePageSize)
                .map(GitHubIssueEventVO::from)
                .toList();
    }

    @Transactional
    public GitHubIssueEventVO ignore(Long id) {
        GitHubIssueEvent event = requireOwnedEvent(id);
        ignoreEvent(event);
        return GitHubIssueEventVO.from(event);
    }

    @Transactional
    public GitHubIssueEventVO ignoreFromNotification(Long id, Long userId) {
        GitHubIssueEvent event = requireUserEvent(id, userId);
        ignoreEvent(event);
        return GitHubIssueEventVO.from(event);
    }

    private void ignoreEvent(GitHubIssueEvent event) {
        if (event.getAgentTaskId() != null || STATUS_RUNNING.equals(event.getStatus())
                || STATUS_PATCH_READY.equals(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub issue event cannot be ignored in current status");
        }
        event.setStatus(STATUS_IGNORED);
        gitHubIssueEventMapper.updateById(event);
    }

    public GitHubIssueEventRunResult run(Long id) {
        GitHubIssueEvent event = requireOwnedEvent(id);
        return runEvent(event, SecurityUtils.getCurrentUserId());
    }

    public GitHubIssueEventRunResult runFromNotification(Long id, Long userId) {
        GitHubIssueEvent event = requireUserEvent(id, userId);
        return runEvent(event, userId);
    }

    private GitHubIssueEventRunResult runEvent(GitHubIssueEvent event, Long userId) {
        if (event.getAgentTaskId() != null) {
            return new GitHubIssueEventRunResult(event.getAgentTaskId(), event.getStatus());
        }
        if (!STATUS_NEW.equals(event.getStatus()) && !STATUS_NOTIFIED.equals(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub issue event cannot be run in current status");
        }

        UserRepoWatch watch = requireWatch(event.getRepoWatchId());
        AgentTaskVO task = agentTaskService.createFromGitHubIssue(
                userId,
                event.getProjectRepoId(),
                event.getId(),
                event.getIssueTitle(),
                buildIssueDescription(event)
        );
        event.setAgentTaskId(task.id());
        event.setStatus(STATUS_RUNNING);
        gitHubIssueEventMapper.updateById(event);
        notificationService.sendToUser(event.getUserId(), notificationTemplateFactory.repairStarted(watch, event, task.id()));

        try {
            agentRunService.run(task.id(), userId);
        } catch (RuntimeException exception) {
            markFailed(task.id(), exception.getMessage());
            throw exception;
        }
        return new GitHubIssueEventRunResult(task.id(), STATUS_RUNNING);
    }

    @Transactional
    public void markPatchReady(Long taskId, Long patchId) {
        GitHubIssueEvent event = findByTaskId(taskId);
        if (event == null) {
            return;
        }
        event.setStatus(STATUS_PATCH_READY);
        gitHubIssueEventMapper.updateById(event);
        UserRepoWatch watch = requireWatch(event.getRepoWatchId());
        PatchRecord patchRecord = patchRecordMapper.selectById(patchId);
        notificationService.sendToUser(
                event.getUserId(),
                patchRecord == null
                        ? notificationTemplateFactory.patchReady(watch, event, taskId, patchId)
                        : notificationTemplateFactory.patchReady(watch, event, patchRecord)
        );
    }

    @Transactional
    public void markFailed(Long taskId, String reason) {
        GitHubIssueEvent event = findByTaskId(taskId);
        if (event == null) {
            return;
        }
        event.setStatus(STATUS_FAILED);
        gitHubIssueEventMapper.updateById(event);
        UserRepoWatch watch = requireWatch(event.getRepoWatchId());
        notificationService.sendToUser(
                event.getUserId(),
                notificationTemplateFactory.repairFailed(watch, event, taskId, reason)
        );
    }

    private GitHubIssueEvent requireOwnedEvent(Long id) {
        return requireUserEvent(id, SecurityUtils.getCurrentUserId());
    }

    private GitHubIssueEvent requireUserEvent(Long id, Long userId) {
        GitHubIssueEvent event = gitHubIssueEventMapper.selectOne(new LambdaQueryWrapper<GitHubIssueEvent>()
                .eq(GitHubIssueEvent::getId, id)
                .eq(GitHubIssueEvent::getUserId, userId)
                .last("limit 1"));
        if (event == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub issue event not found");
        }
        return event;
    }

    private UserRepoWatch requireWatch(Long id) {
        UserRepoWatch watch = userRepoWatchMapper.selectById(id);
        if (watch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Repository watch not found");
        }
        return watch;
    }

    private GitHubIssueEvent findByTaskId(Long taskId) {
        return gitHubIssueEventMapper.selectOne(new LambdaQueryWrapper<GitHubIssueEvent>()
                .eq(GitHubIssueEvent::getAgentTaskId, taskId)
                .last("limit 1"));
    }

    private String buildIssueDescription(GitHubIssueEvent event) {
        StringBuilder description = new StringBuilder();
        if (event.getIssueBody() != null && !event.getIssueBody().isBlank()) {
            description.append(event.getIssueBody().trim()).append("\n\n");
        }
        description.append("GitHub Issue: ").append(event.getIssueUrl());
        description.append("\nIssue Number: #").append(event.getIssueNumber());
        return description.toString();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
