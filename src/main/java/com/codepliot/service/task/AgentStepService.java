package com.codepliot.service.task;

import com.codepliot.entity.AgentStepType;
import com.codepliot.model.AgentStepVO;
import java.util.List;
/**
 * AgentStepService 服务类，负责封装业务流程和领域能力。
 */
public interface AgentStepService {
Long startStep(Long taskId, AgentStepType stepType, String stepName, String input);
void successStep(Long stepId, String output);
void failStep(Long stepId, String errorMessage);
List<AgentStepVO> listTaskSteps(Long taskId);
}
