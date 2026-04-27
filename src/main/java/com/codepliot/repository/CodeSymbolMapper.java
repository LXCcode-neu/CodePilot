package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.CodeSymbol;
import org.apache.ibatis.annotations.Mapper;
/**
 * CodeSymbolMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface CodeSymbolMapper extends BaseMapper<CodeSymbol> {
}

