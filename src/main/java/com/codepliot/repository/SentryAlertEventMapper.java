package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.SentryAlertEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * Sentry 告警事件数据访问接口，对应数据库表 sentry_alert_event。
 * <p>用于记录从 Sentry 接收到的告警事件信息，包括 Sentry 组织/项目标识、Issue ID、
 * 告警规则、错误标题、严重级别、平台、触发源、Web URL、处理状态及关联的代理任务等。
 * 支持去重机制，通过 dedupe_key 避免重复处理。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.SentryAlertEvent
 */
@Mapper
public interface SentryAlertEventMapper extends BaseMapper<SentryAlertEvent> {
}
