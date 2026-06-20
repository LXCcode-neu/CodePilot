package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.SentryProjectMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * Sentry 项目映射数据访问接口，对应数据库表 sentry_project_mapping。
 * <p>用于管理 CodePilot 项目与 Sentry 项目之间的映射关系，记录 Sentry 组织/项目标识、
 * 关联的用户及启用状态，支持配置是否开启自动运行（收到告警时自动触发修复流程）。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.SentryProjectMapping
 */
@Mapper
public interface SentryProjectMappingMapper extends BaseMapper<SentryProjectMapping> {
}
