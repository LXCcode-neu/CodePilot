package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.GitHubIssueEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * GitHub Issue 事件数据访问接口，对应数据库表 github_issue_event。
 * <p>用于记录从 GitHub Webhook 接收到的 Issue 相关事件（如 opened、edited、closed 等），
 * 支持对事件的查询和管理，以便触发后续的自动化处理流程。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.GitHubIssueEvent
 */
@Mapper
public interface GitHubIssueEventMapper extends BaseMapper<GitHubIssueEvent> {
}
