package com.codepliot.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.auth.security.SecurityUtils;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.project.entity.ProjectRepo;
import com.codepliot.project.mapper.ProjectRepoMapper;
import com.codepliot.task.dto.AgentTaskCreateRequest;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.task.mapper.AgentTaskMapper;
import com.codepliot.task.vo.AgentTaskVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 任务服务。
 * 负责任务创建、任务查询以及执行过程中的状态更新。
 */
@Service
public class AgentTaskService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;

    public AgentTaskService(AgentTaskMapper agentTaskMapper, ProjectRepoMapper projectRepoMapper) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
    }

    /**
     * 创建任务前先校验项目是否属于当前登录用户。
     */
    @Transactional
    public AgentTaskVO create(AgentTaskCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(currentUserId, request.projectId());

        AgentTask agentTask = new AgentTask();
        agentTask.setUserId(currentUserId);
        agentTask.setProjectId(request.projectId());
        agentTask.setIssueTitle(request.issueTitle().trim());
        agentTask.setIssueDescription(request.issueDescription().trim());
        agentTask.setStatus(AgentTaskStatus.PENDING.name());
        agentTask.setResultSummary(null);
        agentTask.setErrorMessage(null);
        agentTaskMapper.insert(agentTask);
        return AgentTaskVO.from(agentTask);
    }

    /**
     * 查询当前用户自己的任务列表。
     */
    public List<AgentTaskVO> listCurrentUserTasks() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return agentTaskMapper.selectList(new LambdaQueryWrapper<AgentTask>()
                        .eq(AgentTask::getUserId, currentUserId)
                        .orderByDesc(AgentTask::getCreatedAt))
                .stream()
                .map(AgentTaskVO::from)
                .toList();
    }

    /**
     * 查询任务详情，并校验任务归属。
     */
    public AgentTaskVO getDetail(Long id) {
        return AgentTaskVO.from(requireOwnedTask(id));
    }

    /**
     * 供执行器内部调用，只更新任务状态。
     */
    @Transactional
    public void updateStatus(Long taskId, AgentTaskStatus status) {
        updateStatus(taskId, status, null, null);
    }

    /**
     * 供执行器内部调用，同时更新状态、结果摘要和错误信息。
     */
    @Transactional
    public void updateStatus(Long taskId, AgentTaskStatus status, String resultSummary, String errorMessage) {
        AgentTask agentTask = requireTask(taskId);
        agentTask.setStatus(status.name());
        agentTask.setResultSummary(resultSummary);
        agentTask.setErrorMessage(errorMessage);
        agentTaskMapper.updateById(agentTask);
    }

    /**
     * 校验项目是否属于当前用户，避免基于他人仓库创建任务。
     */
    private void requireOwnedProject(Long currentUserId, Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
    }

    /**
     * 查询当前用户自己的任务，不属于当前用户则视为不存在。
     */
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

    /**
     * 按任务 ID 查询任务，供内部状态更新使用。
     */
    private AgentTask requireTask(Long id) {
        AgentTask agentTask = agentTaskMapper.selectById(id);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }
}
