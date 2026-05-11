package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.UserGitHubAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserGitHubAccountMapper extends BaseMapper<UserGitHubAccount> {
}
