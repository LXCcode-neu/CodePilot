package com.codepliot.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.AgentStep;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.ProjectLlmConfig;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.repository.AgentStepMapper;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.ProjectLlmConfigService;
import com.codepliot.service.sse.SseService;
import com.codepliot.utils.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentTaskService {

    private final AgentTaskMapper agentTaskMapper;
    private final AgentStepMapper agentStepMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final ProjectLlmConfigService projectLlmConfigService;
    private final TaskRunLockService taskRunLockService;
    private final SseService sseService;

    public AgentTaskService(AgentTaskMapper agentTaskMapper,
                            AgentStepMapper agentStepMapper,
                            PatchRecordMapper patchRecordMapper,
                            ProjectRepoMapper projectRepoMapper,
                            ProjectLlmConfigService projectLlmConfigService,
                            TaskRunLockService taskRunLockService,
                            SseService sseService) {
        this.agentTaskMapper = agentTaskMapper;
        this.agentStepMapper = agentStepMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.projectLlmConfigService = projectLlmConfigService;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
    }

    @Transactional
    public AgentTaskVO create(AgentTaskCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(currentUserId, request.projectId());
        ProjectLlmConfig llmConfig = projectLlmConfigService.requireProjectConfig(request.projectId());

        AgentTask agentTask = new AgentTask();
        agentTask.setUserId(currentUserId);
        agentTask.setProjectId(request.projectId());
        agentTask.setIssueTitle(request.issueTitle().trim());
        agentTask.setIssueDescription(request.issueDescription().trim());
        agentTask.setStatus(AgentTaskStatus.PENDING.name());
        agentTask.setResultSummary(null);
        agentTask.setErrorMessage(null);
        agentTask.setLlmProvider(llmConfig.getProvider());
        agentTask.setLlmModelName(llmConfig.getModelName());
        agentTask.setLlmDisplayName(llmConfig.getDisplayName());
        agentTask.setSourceType("MANUAL");
        agentTask.setSourceId(null);
        agentTaskMapper.insert(agentTask);
        return AgentTaskVO.from(agentTask);
    }

    @Transactional
    public AgentTaskVO createFromGitHubIssue(Long projectId, Long issueEventId, String issueTitle, String issueDescription) {
        return createFromGitHubIssue(SecurityUtils.getCurrentUserId(), projectId, issueEventId, issueTitle, issueDescription);
    }

    @Transactional
    public AgentTaskVO createFromGitHubIssue(Long userId,
                                             Long projectId,
                                             Long issueEventId,
                                             String issueTitle,
                                             String issueDescription) {
        requireOwnedProject(userId, projectId);
        ProjectLlmConfig llmConfig = projectLlmConfigService.requireProjectConfig(projectId, userId);

        AgentTask agentTask = new AgentTask();
        agentTask.setUserId(userId);
        agentTask.setProjectId(projectId);
        agentTask.setIssueTitle(issueTitle == null ? "" : issueTitle.trim());
        agentTask.setIssueDescription(issueDescription == null ? "" : issueDescription.trim());
        agentTask.setStatus(AgentTaskStatus.PENDING.name());
        agentTask.setResultSummary(null);
        agentTask.setErrorMessage(null);
        agentTask.setLlmProvider(llmConfig.getProvider());
        agentTask.setLlmModelName(llmConfig.getModelName());
        agentTask.setLlmDisplayName(llmConfig.getDisplayName());
        agentTask.setSourceType("GITHUB_ISSUE");
        agentTask.setSourceId(issueEventId);
        agentTaskMapper.insert(agentTask);
        return AgentTaskVO.from(agentTask);
    }

    public List<AgentTaskVO> listCurrentUserTasks() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return agentTaskMapper.selectList(new LambdaQueryWrapper<AgentTask>()
                        .eq(AgentTask::getUserId, currentUserId)
                        .orderByDesc(AgentTask::getCreatedAt))
                .stream()
                .map(AgentTaskVO::from)
                .toList();
    }

    public AgentTaskVO getDetail(Long id) {
        return AgentTaskVO.from(requireOwnedTask(id));
    }

    public AgentTask getOwnedTaskEntity(Long id) {
        return requireOwnedTask(id);
    }

    @Transactional
    public void delete(Long id) {
        AgentTask agentTask = requireOwnedTask(id);
        List<TaskLock> taskLocks = lockTasks(List.of(agentTask));
        try {
            deleteTasksByIds(List.of(agentTask.getId()));
        } finally {
            unlockTasks(taskLocks);
        }
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        List<AgentTask> tasks = agentTaskMapper.selectList(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getProjectId, projectId)
                .orderByAsc(AgentTask::getId));
        if (tasks.isEmpty()) {
            return;
        }

        List<TaskLock> taskLocks = lockTasks(tasks);
        try {
            deleteTasksByIds(tasks.stream().map(AgentTask::getId).toList());
        } finally {
            unlockTasks(taskLocks);
        }
    }

    @Transactional
    public void updateStatus(Long taskId, AgentTaskStatus status) {
        updateStatus(taskId, status, null, null);
    }

    @Transactional
    public void updateStatus(Long taskId, AgentTaskStatus status, String resultSummary, String errorMessage) {
        AgentTask agentTask = requireTask(taskId);
        agentTask.setStatus(status.name());
        agentTask.setResultSummary(resultSummary);
        agentTask.setErrorMessage(errorMessage);
        agentTaskMapper.updateById(agentTask);
    }

    private void requireOwnedProject(Long currentUserId, Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
    }

    private AgentTask requireOwnedTask(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AgentTask agentTask = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, id)
                .eq(AgentTask::getUserId, currentUserId)
                .last("limit 1"));
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    private AgentTask requireTask(Long id) {
        AgentTask agentTask = agentTaskMapper.selectById(id);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    private void deleteTasksByIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        List<AgentTask> currentTasks = agentTaskMapper.selectList(new LambdaQueryWrapper<AgentTask>()
                .in(AgentTask::getId, taskIds));
        if (currentTasks.size() != taskIds.size()) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        requireDeletableTasks(currentTasks);

        agentStepMapper.delete(new LambdaQueryWrapper<AgentStep>()
                .in(AgentStep::getTaskId, taskIds));
        patchRecordMapper.delete(new LambdaQueryWrapper<PatchRecord>()
                .in(PatchRecord::getTaskId, taskIds));
        agentTaskMapper.delete(new LambdaQueryWrapper<AgentTask>()
                .in(AgentTask::getId, taskIds));

        taskIds.forEach(sseService::complete);
    }

    private List<TaskLock> lockTasks(List<AgentTask> tasks) {
        List<TaskLock> taskLocks = new ArrayList<>();
        try {
            for (AgentTask task : tasks) {
                taskLocks.add(new TaskLock(task.getId(), taskRunLockService.lock(task.getId())));
            }
            return taskLocks;
        } catch (RuntimeException exception) {
            unlockTasks(taskLocks);
            throw exception;
        }
    }

    private void unlockTasks(List<TaskLock> taskLocks) {
        for (TaskLock taskLock : taskLocks) {
            taskRunLockService.unlock(taskLock.taskId(), taskLock.lockValue());
        }
    }

    private void requireDeletableTasks(List<AgentTask> tasks) {
        boolean hasRunningTask = tasks.stream().anyMatch(task -> isRunningStatus(task.getStatus()));
        if (hasRunningTask) {
            throw new BusinessException(ErrorCode.AGENT_TASK_RUNNING, "Running agent tasks cannot be deleted");
        }
    }

    private boolean isRunningStatus(String status) {
        return AgentTaskStatus.CLONING.name().equals(status)
                || AgentTaskStatus.RETRIEVING.name().equals(status)
                || AgentTaskStatus.ANALYZING.name().equals(status)
                || AgentTaskStatus.GENERATING_PATCH.name().equals(status)
                || AgentTaskStatus.VERIFYING.name().equals(status);
    }

    private record TaskLock(Long taskId, String lockValue) {
    }
}
