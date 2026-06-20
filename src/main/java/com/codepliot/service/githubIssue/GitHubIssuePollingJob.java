package com.codepliot.service.githubIssue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.GitHubIssueClient;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.model.GitHubIssueVO;
import com.codepliot.repository.UserRepoWatchMapper;
import com.codepliot.service.auth.GitHubAuthService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * GitHub Issue 轮询定时任务。
 * <p>
 * 按照配置的时间间隔定期轮询所有已启用的用户仓库监听，拉取最新的 Open 状态 Issue，
 * 并将新发现的 Issue 交给 {@link GitHubIssueEventService} 处理。
 * <p>
 * 轮询间隔可通过配置项 {@code codepilot.issue-polling.fixed-delay-ms} 调整，默认为 5 分钟。
 */
@Component
public class GitHubIssuePollingJob {

    private static final Logger log = LoggerFactory.getLogger(GitHubIssuePollingJob.class);

    private final UserRepoWatchMapper userRepoWatchMapper;
    private final GitHubIssueClient gitHubIssueClient;
    private final GitHubAuthService gitHubAuthService;
    private final GitHubIssueEventService gitHubIssueEventService;

    public GitHubIssuePollingJob(UserRepoWatchMapper userRepoWatchMapper,
                                 GitHubIssueClient gitHubIssueClient,
                                 GitHubAuthService gitHubAuthService,
                                 GitHubIssueEventService gitHubIssueEventService) {
        this.userRepoWatchMapper = userRepoWatchMapper;
        this.gitHubIssueClient = gitHubIssueClient;
        this.gitHubAuthService = gitHubAuthService;
        this.gitHubIssueEventService = gitHubIssueEventService;
    }

    /**
     * 定时轮询所有已启用的仓库监听，拉取最新的 Open Issue 并处理新发现的 Issue。
     */
    @Scheduled(fixedDelayString = "${codepilot.issue-polling.fixed-delay-ms:300000}")
    public void pollOpenIssues() {
        List<UserRepoWatch> watches = userRepoWatchMapper.selectList(new LambdaQueryWrapper<UserRepoWatch>()
                .eq(UserRepoWatch::getWatchEnabled, true)
                .orderByAsc(UserRepoWatch::getUpdatedAt));
        for (UserRepoWatch watch : watches) {
            pollWatch(watch);
        }
    }

    private void pollWatch(UserRepoWatch watch) {
        try {
            String token = gitHubAuthService.resolveAccessTokenForUser(watch.getUserId());
            List<GitHubIssueVO> issues = gitHubIssueClient
                    .listIssues(token, watch.getOwner(), watch.getRepoName(), "open", 1, 50)
                    .items();
            for (GitHubIssueVO issue : issues) {
                gitHubIssueEventService.saveNewIssueIfAbsent(watch, issue);
            }
            watch.setLastCheckedAt(LocalDateTime.now());
            userRepoWatchMapper.updateById(watch);
        } catch (RuntimeException exception) {
            log.warn("Failed to poll GitHub issues for {}/{}: {}",
                    watch.getOwner(),
                    watch.getRepoName(),
                    exception.getMessage());
        }
    }
}
