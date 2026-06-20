package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知记录数据访问接口，对应数据库表 notification_record。
 * <p>用于记录系统发送的每一条通知消息的详细信息，包括通知渠道、事件类型、标题、内容、
 * 发送状态、错误信息及发送时间等，便于通知的追踪和问题排查。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.NotificationRecord
 */
@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {
}
