package com.codepliot.service.githubIssue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.entity.ProjectRepoStatus;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.EnabledUpdateRequest;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.UserRepoWatchCreateRequest;
import com.codepliot.model.UserRepoWatchVO;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.repository.UserRepoWatchMapper;
import com.codepliot.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRepoWatchService {

    private final UserRepoWatchMapper userRepoWatchMapper;
    private final ProjectRepoMapper projectRepoMapper;

    public UserRepoWatchService(UserRepoWatchMapper userRepoWatchMapper,
                                ProjectRepoMapper projectRepoMapper) {
        this.userRepoWatchMapper = userRepoWatchMapper;
        this.projectRepoMapper = projectRepoMapper;
    }

    @Transactional
    public UserRepoWatchVO create(UserRepoWatchCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String owner = requireText(request.owner(), "owner").trim();
        String repoName = requireText(request.repoName(), "repoName").trim();
        String repoUrl = normalizeRepoUrl(request.repoUrl());

        UserRepoWatch existing = userRepoWatchMapper.selectOne(new LambdaQueryWrapper<UserRepoWatch>()
                .eq(UserRepoWatch::getUserId, userId)
                .eq(UserRepoWatch::getOwner, owner)
                .eq(UserRepoWatch::getRepoName, repoName)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Repository watch already exists");
        }

        ProjectRepo projectRepo = findOrCreateProjectRepo(userId, owner, repoName, repoUrl, request.defaultBranch());

        UserRepoWatch watch = new UserRepoWatch();
        watch.setUserId(userId);
        watch.setProjectRepoId(projectRepo.getId());
        watch.setOwner(owner);
        watch.setRepoName(repoName);
        watch.setRepoUrl(repoUrl);
        watch.setDefaultBranch(defaultBranch(request.defaultBranch()));
        watch.setWatchEnabled(true);
        watch.setWatchMode("POLLING");
        watch.setLastCheckedAt(null);
        userRepoWatchMapper.insert(watch);
        return UserRepoWatchVO.from(watch);
    }

    public List<UserRepoWatchVO> listCurrentUserWatches() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepoWatchMapper.selectList(new LambdaQueryWrapper<UserRepoWatch>()
                        .eq(UserRepoWatch::getUserId, userId)
                        .orderByDesc(UserRepoWatch::getCreatedAt))
                .stream()
                .map(UserRepoWatchVO::from)
                .toList();
    }

    @Transactional
    public UserRepoWatchVO updateEnabled(Long id, EnabledUpdateRequest request) {
        UserRepoWatch watch = requireOwnedWatch(id);
        watch.setWatchEnabled(request.enabled());
        userRepoWatchMapper.updateById(watch);
        return UserRepoWatchVO.from(watch);
    }

    private UserRepoWatch requireOwnedWatch(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserRepoWatch watch = userRepoWatchMapper.selectOne(new LambdaQueryWrapper<UserRepoWatch>()
                .eq(UserRepoWatch::getId, id)
                .eq(UserRepoWatch::getUserId, userId)
                .last("limit 1"));
        if (watch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Repository watch not found");
        }
        return watch;
    }

    private ProjectRepo findOrCreateProjectRepo(Long userId,
                                                String owner,
                                                String repoName,
                                                String repoUrl,
                                                String defaultBranch) {
        ProjectRepo existing = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getUserId, userId)
                .eq(ProjectRepo::getRepoUrl, repoUrl)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        ProjectRepo projectRepo = new ProjectRepo();
        projectRepo.setUserId(userId);
        projectRepo.setRepoUrl(repoUrl);
        projectRepo.setRepoName(repoName);
        projectRepo.setGithubOwner(owner);
        projectRepo.setGithubRepoName(repoName);
        projectRepo.setGithubRepoId(null);
        projectRepo.setGithubPrivate(false);
        projectRepo.setLocalPath(null);
        projectRepo.setDefaultBranch(defaultBranch(defaultBranch));
        projectRepo.setStatus(ProjectRepoStatus.CREATED.name());
        projectRepoMapper.insert(projectRepo);
        return projectRepo;
    }

    private String normalizeRepoUrl(String repoUrl) {
        String normalized = requireText(repoUrl, "repoUrl").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.matches("^https://github\\.com/[^/]+/[^/]+(\\.git)?$")) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return normalized;
    }

    private String defaultBranch(String value) {
        return value == null || value.isBlank() ? "main" : value.trim();
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " is required");
        }
        return value;
    }
}
