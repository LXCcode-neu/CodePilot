package com.codepliot.service.git;

import com.codepliot.config.GitOperationProperties;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.auth.GitHubAuthService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    private final ProjectRepoMapper projectRepoMapper;
    private final GitWorkspaceService gitWorkspaceService;
    private final GitHubAuthService gitHubAuthService;
    private final GitOperationProperties gitOperationProperties;

    public GitService(ProjectRepoMapper projectRepoMapper,
                      GitWorkspaceService gitWorkspaceService,
                      GitHubAuthService gitHubAuthService,
                      GitOperationProperties gitOperationProperties) {
        this.projectRepoMapper = projectRepoMapper;
        this.gitWorkspaceService = gitWorkspaceService;
        this.gitHubAuthService = gitHubAuthService;
        this.gitOperationProperties = gitOperationProperties;
    }

    public String cloneRepository(Long projectId) {
        ProjectRepo projectRepo = requireProjectRepo(projectId);
        Path repositoryPath = gitWorkspaceService.getRepositoryPath(projectId);

        if (gitWorkspaceService.hasGitDirectory(projectId)) {
            syncRepository(projectRepo, repositoryPath);
            return updateLocalPath(projectRepo, repositoryPath);
        }

        if (Files.exists(repositoryPath)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Repository directory already exists but is not a Git repository: " + repositoryPath);
        }

        gitWorkspaceService.ensureProjectWorkspace(projectId);
        try {
            var cloneCommand = Git.cloneRepository()
                    .setURI(projectRepo.getRepoUrl())
                    .setDirectory(repositoryPath.toFile())
                    .setTimeout(Math.max(gitOperationProperties.getCloneTimeoutSeconds(), 1));
            String accessToken = gitHubAuthService.resolveAccessTokenForUser(projectRepo.getUserId());
            if (accessToken != null && !accessToken.isBlank()) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), "x-oauth-basic"));
            }
            try (Git ignored = cloneCommand.call()) {
                return updateLocalPath(projectRepo, repositoryPath);
            }
        } catch (GitAPIException | RuntimeException ex) {
            gitWorkspaceService.deleteRepositoryWorkspace(projectId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to clone GitHub repository: " + buildErrorMessage(ex));
        }
    }

    public String syncRepository(Long projectId) {
        ProjectRepo projectRepo = requireProjectRepo(projectId);
        Path repositoryPath = gitWorkspaceService.getRepositoryPath(projectId);
        if (!gitWorkspaceService.hasGitDirectory(projectId)) {
            return cloneRepository(projectId);
        }
        syncRepository(projectRepo, repositoryPath);
        return updateLocalPath(projectRepo, repositoryPath);
    }

    private void syncRepository(ProjectRepo projectRepo, Path repositoryPath) {
        try (Git git = Git.open(repositoryPath.toFile())) {
            fetchOrigin(git, projectRepo.getUserId());
            String baseBranch = resolveBaseBranch(projectRepo, git.getRepository());
            checkoutBaseBranch(git, git.getRepository(), baseBranch);
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef(remoteTrackingRef(baseBranch)).call();
            git.clean().setCleanDirectories(true).setIgnore(false).call();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "同步 GitHub 仓库失败: " + buildErrorMessage(ex));
        }
    }

    private void fetchOrigin(Git git, Long userId) throws GitAPIException {
        var fetchCommand = git.fetch().setRemote("origin");
        String accessToken = gitHubAuthService.resolveAccessTokenForUser(userId);
        if (accessToken != null && !accessToken.isBlank()) {
            fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), "x-oauth-basic"));
        }
        fetchCommand.call();
    }

    private void checkoutBaseBranch(Git git, Repository repository, String baseBranch) throws Exception {
        String remoteRef = remoteTrackingRef(baseBranch);
        if (repository.findRef(baseBranch) == null) {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(baseBranch)
                    .setStartPoint(remoteRef)
                    .call();
        } else {
            git.checkout().setName(baseBranch).call();
        }
    }

    private String resolveBaseBranch(ProjectRepo projectRepo, Repository repository) {
        if (projectRepo.getDefaultBranch() != null && !projectRepo.getDefaultBranch().isBlank()) {
            return normalizeBranchName(projectRepo.getDefaultBranch());
        }
        String originHeadBranch = resolveOriginHeadBranch(repository);
        if (originHeadBranch != null) {
            return originHeadBranch;
        }
        if (hasRemoteBranch(repository, "main")) {
            return "main";
        }
        if (hasRemoteBranch(repository, "master")) {
            return "master";
        }
        return "main";
    }

    private String resolveOriginHeadBranch(Repository repository) {
        try {
            Ref originHead = repository.exactRef("refs/remotes/origin/HEAD");
            if (originHead == null) {
                return null;
            }
            Ref target = originHead.isSymbolic() ? originHead.getTarget() : originHead;
            return normalizeBranchName(target.getName());
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean hasRemoteBranch(Repository repository, String branchName) {
        try {
            return repository.exactRef(remoteTrackingRef(branchName)) != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private String remoteTrackingRef(String branchName) {
        return "refs/remotes/origin/" + normalizeBranchName(branchName);
    }

    private String normalizeBranchName(String branchName) {
        String normalized = branchName == null ? "" : branchName.trim();
        String remotePrefix = "refs/remotes/origin/";
        String localPrefix = "refs/heads/";
        if (normalized.startsWith(remotePrefix)) {
            return normalized.substring(remotePrefix.length());
        }
        if (normalized.startsWith("origin/")) {
            return normalized.substring("origin/".length());
        }
        if (normalized.startsWith(localPrefix)) {
            return normalized.substring(localPrefix.length());
        }
        return normalized.isBlank() ? "main" : normalized;
    }

    private ProjectRepo requireProjectRepo(Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectById(projectId);
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private String updateLocalPath(ProjectRepo projectRepo, Path repositoryPath) {
        String localPath = repositoryPath.toString();
        projectRepo.setLocalPath(localPath);
        int updated = projectRepoMapper.updateById(projectRepo);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to update project repository localPath");
        }
        return localPath;
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}
