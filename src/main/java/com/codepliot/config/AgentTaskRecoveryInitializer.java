package com.codepliot.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.repository.AgentTaskMapper;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 代理任务恢复初始化器。
 * <p>
 * 应用启动时自动执行，将上次运行中因应用重启而中断的代理任务标记为失败或已取消，
 * 避免任务一直处于中间状态无法被用户感知。
 * </p>
 */
@Component
public class AgentTaskRecoveryInitializer implements ApplicationRunner {

    /** 应用重启前可能处于运行中的状态列表 */
    private static final List<String> RUNNING_STATUSES = List.of(
            AgentTaskStatus.CLONING.name(),
            AgentTaskStatus.RETRIEVING.name(),
            AgentTaskStatus.ANALYZING.name(),
            AgentTaskStatus.GENERATING_PATCH.name(),
            AgentTaskStatus.VERIFYING.name(),
            AgentTaskStatus.REPAIRING_PATCH.name()
    );

    /** 代理任务数据访问层 */
    private final AgentTaskMapper agentTaskMapper;

    /**
     * 构造方法，注入代理任务 Mapper。
     *
     * @param agentTaskMapper 代理任务数据访问层
     */
    public AgentTaskRecoveryInitializer(AgentTaskMapper agentTaskMapper) {
        this.agentTaskMapper = agentTaskMapper;
    }

    /**
     * 应用启动后执行任务恢复逻辑。
     * <p>
     * 将处于运行中状态的任务标记为 FAILED，将请求取消的任务标记为 CANCELLED。
     * </p>
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        agentTaskMapper.update(null, new LambdaUpdateWrapper<AgentTask>()
                .in(AgentTask::getStatus, RUNNING_STATUSES)
                .set(AgentTask::getStatus, AgentTaskStatus.FAILED.name())
                .set(AgentTask::getErrorMessage, "Application restarted while task was running. Please rerun the task."));
        agentTaskMapper.update(null, new LambdaUpdateWrapper<AgentTask>()
                .eq(AgentTask::getStatus, AgentTaskStatus.CANCEL_REQUESTED.name())
                .set(AgentTask::getStatus, AgentTaskStatus.CANCELLED.name())
                .set(AgentTask::getResultSummary, "Task cancelled during application restart")
                .set(AgentTask::getErrorMessage, null));
    }
}
