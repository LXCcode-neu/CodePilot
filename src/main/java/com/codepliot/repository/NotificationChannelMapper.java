package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.NotificationChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知渠道数据访问接口，对应数据库表 notification_channel。
 * <p>用于管理用户配置的通知渠道（如 Telegram、Discord、邮件等），包括渠道类型、名称、
 * Webhook URL（加密存储）及启用状态等信息。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.NotificationChannel
 */
@Mapper
public interface NotificationChannelMapper extends BaseMapper<NotificationChannel> {
}
