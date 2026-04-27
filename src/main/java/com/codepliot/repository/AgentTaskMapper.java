package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.AgentTask;
import org.apache.ibatis.annotations.Mapper;
/**
 * AgentTaskMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface AgentTaskMapper extends BaseMapper<AgentTask> {
}

