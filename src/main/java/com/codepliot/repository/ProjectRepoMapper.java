package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.ProjectRepo;
import org.apache.ibatis.annotations.Mapper;
/**
 * ProjectRepoMapper 仓储接口，负责数据库读写。
 */
@Mapper
public interface ProjectRepoMapper extends BaseMapper<ProjectRepo> {
}

