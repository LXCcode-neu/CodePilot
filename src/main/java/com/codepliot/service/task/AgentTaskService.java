package com.codepliot.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.utils.SecurityUtils;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.model.AgentTaskVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * AgentTaskService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class AgentTaskService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
/**
 * 创建 AgentTaskService 实例。
 */
public AgentTaskService(AgentTaskMapper agentTaskMapper, ProjectRepoMapper projectRepoMapper) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
    }
    /**
     * 执行 create 相关逻辑。
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
 * 列出Current User Tasks相关逻辑。
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
 * 获取Detail相关逻辑。
 */
public AgentTaskVO getDetail(Long id) {
        return AgentTaskVO.from(requireOwnedTask(id));
    }
/**
 * 获取Owned Task Entity相关逻辑。
 */
public AgentTask getOwnedTaskEntity(Long id) {
        return requireOwnedTask(id);
    }
    /**
     * 更新Status相关逻辑。
     */
@Transactional
    public void updateStatus(Long taskId, AgentTaskStatus status) {
        updateStatus(taskId, status, null, null);
    }
    /**
     * 更新Status相关逻辑。
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
 * 检查并返回Owned Project相关逻辑。
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
 * 检查并返回Owned Task相关逻辑。
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
 * 检查并返回Task相关逻辑。
 */
private AgentTask requireTask(Long id) {
        AgentTask agentTask = agentTaskMapper.selectById(id);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }
}

