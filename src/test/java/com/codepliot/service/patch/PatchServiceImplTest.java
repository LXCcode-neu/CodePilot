package com.codepliot.service.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.User;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.LoginUser;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.policy.AgentExecutionPolicy;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.service.sse.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PatchServiceImplTest {

    @Mock
    private PatchRecordMapper patchRecordMapper;

    @Mock
    private AgentTaskMapper agentTaskMapper;

    @Mock
    private SseService sseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMarkPatchConfirmedAndCompleteTask() {
        LoginUser loginUser = buildLoginUser(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );

        AgentTask agentTask = new AgentTask();
        agentTask.setId(11L);
        agentTask.setUserId(7L);
        agentTask.setProjectId(3L);
        agentTask.setIssueTitle("issue");
        agentTask.setIssueDescription("description");
        agentTask.setStatus(AgentTaskStatus.WAITING_CONFIRM.name());

        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setId(21L);
        patchRecord.setTaskId(11L);
        patchRecord.setConfirmed(Boolean.FALSE);

        when(agentTaskMapper.selectOne(any())).thenReturn(agentTask);
        when(patchRecordMapper.selectOne(any())).thenReturn(patchRecord);
        when(agentTaskMapper.selectById(11L)).thenReturn(agentTask);

        PatchServiceImpl patchService = new PatchServiceImpl(
                patchRecordMapper,
                agentTaskMapper,
                new ObjectMapper(),
                new AgentExecutionPolicy(),
                sseService
        );

        AgentTaskVO result = patchService.confirmTaskPatch(11L);

        assertEquals(AgentTaskStatus.COMPLETED.name(), result.status());
        assertEquals(AgentTaskStatus.COMPLETED.name(), agentTask.getStatus());
        assertTrue(Boolean.TRUE.equals(patchRecord.getConfirmed()));
        assertNotNull(patchRecord.getConfirmedAt());

        verify(patchRecordMapper).updateById(patchRecord);
        verify(agentTaskMapper).updateById(agentTask);

        ArgumentCaptor<TaskEventMessage> eventCaptor = ArgumentCaptor.forClass(TaskEventMessage.class);
        verify(sseService).push(eventCaptor.capture());
        verify(sseService).complete(11L);

        TaskEventMessage eventMessage = eventCaptor.getValue();
        assertEquals(11L, eventMessage.taskId());
        assertEquals(AgentTaskStatus.COMPLETED.name(), eventMessage.taskStatus());
        assertEquals("COMPLETED", eventMessage.phase());
    }

    private LoginUser buildLoginUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("tester");
        user.setPassword("secret");
        user.setEmail("tester@example.com");
        return new LoginUser(user);
    }
}

