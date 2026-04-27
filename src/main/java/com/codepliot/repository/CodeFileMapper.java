package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.CodeFile;
import org.apache.ibatis.annotations.Mapper;
/**
 * CodeFileMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface CodeFileMapper extends BaseMapper<CodeFile> {
}

