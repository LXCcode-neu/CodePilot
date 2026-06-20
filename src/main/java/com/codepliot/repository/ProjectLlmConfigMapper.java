package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.ProjectLlmConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目级 LLM 配置数据访问接口，对应数据库表 project_llm_config。
 * <p>用于管理项目级别的大语言模型（LLM）配置，包括供应商、模型名称、显示名称、
 * Base URL、API Key（加密存储）及启用状态等信息，允许不同项目使用不同的 LLM 配置。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.ProjectLlmConfig
 */
@Mapper
public interface ProjectLlmConfigMapper extends BaseMapper<ProjectLlmConfig> {
}
