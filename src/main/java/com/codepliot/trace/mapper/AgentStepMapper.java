package com.codepliot.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.trace.entity.AgentStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent Step 基础数据库访问接口。
 */
@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStep> {
}
