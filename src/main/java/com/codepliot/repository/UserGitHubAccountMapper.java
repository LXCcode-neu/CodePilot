package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.UserGitHubAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 GitHub 账户数据访问接口，对应数据库表 user_github_account。
 * <p>用于管理用户绑定的 GitHub 账户信息，包括 GitHub 用户 ID、登录名、昵称、头像 URL、
 * 访问令牌（加密存储）及授权范围（scope）等。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.UserGitHubAccount
 */
@Mapper
public interface UserGitHubAccountMapper extends BaseMapper<UserGitHubAccount> {
}
