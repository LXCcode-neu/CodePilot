package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.UserRepoWatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户仓库订阅数据访问接口，对应数据库表 user_repo_watch。
 * <p>用于管理用户订阅/关注的 GitHub 仓库信息，包括仓库所有者、仓库名称、仓库 URL、
 * 默认分支、订阅启用状态、订阅模式及最后检查时间等。当仓库有新的 Issue 时，系统会根据订阅关系进行通知。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.UserRepoWatch
 */
@Mapper
public interface UserRepoWatchMapper extends BaseMapper<UserRepoWatch> {
}
