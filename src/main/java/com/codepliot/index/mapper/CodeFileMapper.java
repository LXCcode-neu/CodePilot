package com.codepliot.index.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.index.entity.CodeFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件级索引 Mapper。
 */
@Mapper
public interface CodeFileMapper extends BaseMapper<CodeFile> {
}
