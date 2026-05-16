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
