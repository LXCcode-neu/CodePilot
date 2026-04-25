package com.codepliot.trace.service;

import com.codepliot.trace.entity.AgentStepType;
import com.codepliot.trace.vo.AgentStepVO;
import java.util.List;

/**
 * Agent Step 服务接口。
 */
public interface AgentStepService {

    /**
     * 开始记录步骤，并返回步骤 id。
     */
    Long startStep(Long taskId, AgentStepType stepType, String stepName, String input);

    /**
     * 将步骤标记为成功，并写入输出内容。
     */
    void successStep(Long stepId, String output);

    /**
     * 将步骤标记为失败，并写入错误信息。
     */
    void failStep(Long stepId, String errorMessage);

    /**
     * 查询某个任务下的全部步骤。
     */
    List<AgentStepVO> listTaskSteps(Long taskId);
}
