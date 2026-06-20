package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.LlmApiKeyConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * LLM API 密钥配置数据访问接口，对应数据库表 llm_api_key_config。
 * <p>用于管理用户自定义的大语言模型（LLM）API 密钥配置，包括不同供应商（如 OpenAI、Anthropic 等）
 * 的 API Key、模型名称、Base URL 等信息。密钥以加密形式存储。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.LlmApiKeyConfig
 */
@Mapper
public interface LlmApiKeyConfigMapper extends BaseMapper<LlmApiKeyConfig> {
}
