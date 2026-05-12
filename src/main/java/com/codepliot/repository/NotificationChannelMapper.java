package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.NotificationChannel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationChannelMapper extends BaseMapper<NotificationChannel> {
}
