package com.codepliot.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.User;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.LoginUser;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.service.sse.SseService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AgentTaskCancellationServiceTest {

    @Mock
    private AgentTaskMapper agentTaskMapper;

    @Mock
    private SseService sseService;

    @Mock
    private AgentTaskCancellationRegistry cancellationRegistry;

    @BeforeAll
    static void initMybatisPlusMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AgentTask.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestCancelMovesRunningTaskToCancelRequestedAndPushesEvent() {
        setCurrentUser(7L);
        AgentTask runningTask = task(11L, 7L, AgentTaskStatus.GENERATING_PATCH.name());
        AgentTask cancelRequestedTask = task(11L, 7L, AgentTaskStatus.CANCEL_REQUESTED.name());

        when(agentTaskMapper.selectOne(any())).thenReturn(runningTask);
        when(agentTaskMapper.update(any(), any())).thenReturn(1);
        when(agentTaskMapper.selectById(11L)).thenReturn(cancelRequestedTask);

        AgentTaskCancellationService service = new AgentTaskCancellationService(agentTaskMapper, sseService, cancellationRegistry);
        AgentTaskVO result = service.requestCancel(11L);

        assertEquals(AgentTaskStatus.CANCEL_REQUESTED.name(), result.status());
        verify(agentTaskMapper).update(any(), any());

        ArgumentCaptor<TaskEventMessage> eventCaptor = ArgumentCaptor.forClass(TaskEventMessage.class);
        verify(sseService).push(eventCaptor.capture());
        TaskEventMessage event = eventCaptor.getValue();
        assertEquals(11L, event.taskId());
        assertEquals(AgentTaskStatus.CANCEL_REQUESTED.name(), event.taskStatus());
        assertEquals("RUNNING", event.phase());
        verify(cancellationRegistry).interrupt(11L);
    }

    @Test
    void markCancelledCompletesTaskEventStream() {
        AgentTask task = task(12L, 7L, AgentTaskStatus.CANCEL_REQUESTED.name());
        when(agentTaskMapper.selectById(12L)).thenReturn(task);

        AgentTaskCancellationService service = new AgentTaskCancellationService(agentTaskMapper, sseService, cancellationRegistry);
        service.markCancelled(12L, "stopped");

        assertEquals(AgentTaskStatus.CANCELLED.name(), task.getStatus());
        assertEquals("stopped", task.getResultSummary());
        verify(agentTaskMapper).updateById(task);
        verify(sseService).complete(12L);

        ArgumentCaptor<TaskEventMessage> eventCaptor = ArgumentCaptor.forClass(TaskEventMessage.class);
        verify(sseService).push(eventCaptor.capture());
        assertEquals(AgentTaskStatus.CANCELLED.name(), eventCaptor.getValue().taskStatus());
        assertEquals("COMPLETED", eventCaptor.getValue().phase());
    }

    private void setCurrentUser(Long userId) {
        LoginUser loginUser = new LoginUser(user(userId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }

    private User user(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("tester");
        user.setPassword("secret");
        user.setEmail("tester@example.com");
        return user;
    }

    private AgentTask task(Long taskId, Long userId, String status) {
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setUserId(userId);
        task.setProjectId(3L);
        task.setIssueTitle("issue");
        task.setIssueDescription("description");
        task.setStatus(status);
        return task;
    }
}
