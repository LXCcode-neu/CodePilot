package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.BotActionCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机器人操作码数据访问接口，对应数据库表 bot_action_code。
 * <p>用于管理通知消息中的交互操作码（Action Code），用户通过回复操作码来触发对应的任务动作，
 * 如确认补丁、取消任务等。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.BotActionCode
 */
@Mapper
public interface BotActionCodeMapper extends BaseMapper<BotActionCode> {
}
