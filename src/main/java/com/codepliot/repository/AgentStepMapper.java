package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.AgentStep;
import org.apache.ibatis.annotations.Mapper;
/**
 * AgentStepMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStep> {
}

