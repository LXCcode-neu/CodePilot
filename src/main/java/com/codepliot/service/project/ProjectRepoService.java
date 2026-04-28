package com.codepliot.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.utils.SecurityUtils;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.ProjectCreateRequest;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.entity.ProjectRepoStatus;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.model.ProjectRepoVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * ProjectRepoService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class ProjectRepoService {

    private final ProjectRepoMapper projectRepoMapper;
/**
 * 创建 ProjectRepoService 实例。
 */
public ProjectRepoService(ProjectRepoMapper projectRepoMapper) {
        this.projectRepoMapper = projectRepoMapper;
    }
    /**
     * 执行 create 相关逻辑。
     */
@Transactional
    public ProjectRepoVO create(ProjectCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String normalizedRepoUrl = normalizeRepoUrl(request.repoUrl());
        String repoName = parseRepoName(normalizedRepoUrl);

        ProjectRepo projectRepo = new ProjectRepo();
        projectRepo.setUserId(currentUserId);
        projectRepo.setRepoUrl(normalizedRepoUrl);
        projectRepo.setRepoName(repoName);
        projectRepo.setLocalPath(null);
        projectRepo.setDefaultBranch(null);
        projectRepo.setStatus(ProjectRepoStatus.CREATED.name());
        projectRepoMapper.insert(projectRepo);
        return ProjectRepoVO.from(projectRepo);
    }
/**
 * 列出Current User Repos相关逻辑。
 */
public List<ProjectRepoVO> listCurrentUserRepos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return projectRepoMapper.selectList(new LambdaQueryWrapper<ProjectRepo>()
                        .eq(ProjectRepo::getUserId, currentUserId)
                        .orderByDesc(ProjectRepo::getCreatedAt))
                .stream()
                .map(ProjectRepoVO::from)
                .toList();
    }
/**
 * 获取Detail相关逻辑。
 */
public ProjectRepoVO getDetail(Long id) {
        return ProjectRepoVO.from(requireOwnedRepo(id));
    }
    /**
     * 执行 delete 相关逻辑。
     */
@Transactional
    public void delete(Long id) {
        ProjectRepo projectRepo = requireOwnedRepo(id);
        projectRepoMapper.deleteById(projectRepo.getId());
    }
/**
 * 检查并返回Owned Repo相关逻辑。
 */
private ProjectRepo requireOwnedRepo(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, id)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }
/**
 * 规范化Repo Url相关逻辑。
 */
private String normalizeRepoUrl(String repoUrl) {
        String normalized = repoUrl == null ? "" : repoUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.matches("^https://github\\.com/[^/]+/[^/]+(\\.git)?$")) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return normalized;
    }
/**
 * 解析Repo Name相关逻辑。
 */
private String parseRepoName(String repoUrl) {
        String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1);
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        if (repoName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return repoName;
    }
}

