package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.NotificationActionToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知操作令牌数据访问接口，对应数据库表 notification_action_token。
 * <p>用于管理通知消息中的操作令牌（Token），用户通过点击通知中的链接携带令牌来执行对应的操作，
 * 如确认补丁应用、取消任务等。令牌具有过期机制，使用后状态会更新。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.NotificationActionToken
 */
@Mapper
public interface NotificationActionTokenMapper extends BaseMapper<NotificationActionToken> {
}
