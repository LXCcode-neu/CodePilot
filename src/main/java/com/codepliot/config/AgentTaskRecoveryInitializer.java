package com.codepliot.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.repository.AgentTaskMapper;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskRecoveryInitializer implements ApplicationRunner {

    private static final List<String> RUNNING_STATUSES = List.of(
            AgentTaskStatus.CLONING.name(),
            AgentTaskStatus.RETRIEVING.name(),
            AgentTaskStatus.ANALYZING.name(),
            AgentTaskStatus.GENERATING_PATCH.name(),
            AgentTaskStatus.VERIFYING.name(),
            AgentTaskStatus.REPAIRING_PATCH.name()
    );

    private final AgentTaskMapper agentTaskMapper;

    public AgentTaskRecoveryInitializer(AgentTaskMapper agentTaskMapper) {
        this.agentTaskMapper = agentTaskMapper;
    }

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
