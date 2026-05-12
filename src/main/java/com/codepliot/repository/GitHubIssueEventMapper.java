package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.GitHubIssueEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitHubIssueEventMapper extends BaseMapper<GitHubIssueEvent> {
}
