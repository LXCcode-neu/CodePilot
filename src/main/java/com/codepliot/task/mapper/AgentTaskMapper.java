package com.codepliot.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.task.entity.AgentTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Agent 任务基础数据库访问接口。
 */
public interface AgentTaskMapper extends BaseMapper<AgentTask> {
}
