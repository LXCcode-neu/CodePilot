package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.PatchRecord;
import org.apache.ibatis.annotations.Mapper;
/**
 * PatchRecordMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface PatchRecordMapper extends BaseMapper<PatchRecord> {
}

