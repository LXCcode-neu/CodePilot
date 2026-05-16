package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.SentryAlertEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SentryAlertEventMapper extends BaseMapper<SentryAlertEvent> {
}
