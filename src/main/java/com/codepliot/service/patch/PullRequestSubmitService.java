package com.codepliot.service.patch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.GitHubPullRequestClient;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubPullRequestCreateResult;
import com.codepliot.model.GitHubRepositoryRef;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PullRequestPreview;
import com.codepliot.model.PullRequestSubmitResult;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.auth.GitHubAuthService;
import com.codepliot.service.git.GitService;
import com.codepliot.utils.SecurityUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PullRequestSubmitService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final GitService gitService;
    private final GitHubPullRequestClient gitHubPullRequestClient;
    private final GitHubAuthService gitHubAuthService;

    public PullRequestSubmitService(AgentTaskMapper agentTaskMapper,
                                    ProjectRepoMapper projectRepoMapper,
                                    PatchRecordMapper patchRecordMapper,
                                    GitService gitService,
                                    GitHubPullRequestClient gitHubPullRequestClient,
                                    GitHubAuthService gitHubAuthService) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.gitService = gitService;
        this.gitHubPullRequestClient = gitHubPullRequestClient;
        this.gitHubAuthService = gitHubAuthService;
    }

    @Transactional
    public PullRequestSubmitResult submit(Long taskId) {
        return submit(taskId, SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public PullRequestSubmitResult submit(Long taskId, Long userId) {
        AgentTask task = requireOwnedTask(taskId, userId);
        ProjectRepo projectRepo = requireOwnedProject(task.getProjectId(), task.getUserId());
        PatchRecord patchRecord = requirePatchRecord(taskId);
        validateSubmittable(task.getUserId(), patchRecord);
        String accessToken = gitHubAuthService.requireAccessTokenForUser(
                task.getUserId(),
                "Connect GitHub before submitting a pull request"
        );

        String repositoryPath = gitService.syncRepository(projectRepo.getId());
        projectRepo = projectRepoMapper.selectById(projectRepo.getId());

        PullRequestPreview preview = PatchRecordVO.from(patchRecord).pullRequest();
        GitHubRepoRef repoRef = parseRepoRef(projectRepo.getRepoUrl());
        String tokenUser = gitHubPullRequestClient.getAuthenticatedUsername(accessToken);
        GitHubRepositoryRef pushRepository = resolvePushRepository(accessToken, repoRef, tokenUser);
        String baseBranch = resolveBaseBranch(projectRepo, repositoryPath);
        String branchName = preview.branchName();
        String pullRequestHead = tokenUser.equalsIgnoreCase(repoRef.owner())
                ? branchName
                : tokenUser + ":" + branchName;

        pushPatchBranch(
                repositoryPath,
                baseBranch,
                branchName,
                pushRepository.cloneUrl(),
                patchRecord.getPatch(),
                preview.commitMessage(),
                accessToken
        );
        GitHubPullRequestCreateResult pullRequest = gitHubPullRequestClient.createPullRequest(
                accessToken,
                repoRef.owner(),
                repoRef.repo(),
                preview.title(),
                preview.body(),
                pullRequestHead,
                baseBranch
        );

        LocalDateTime submittedAt = LocalDateTime.now();
        patchRecord.setPrSubmitted(Boolean.TRUE);
        patchRecord.setPrSubmittedAt(submittedAt);
        patchRecord.setPrUrl(pullRequest.htmlUrl());
        patchRecord.setPrNumber(pullRequest.number());
        patchRecord.setPrBranch(branchName);
        patchRecordMapper.updateById(patchRecord);

        return new PullRequestSubmitResult(taskId, pullRequest.number(), pullRequest.htmlUrl(), branchName, submittedAt);
    }

    private GitHubRepositoryRef resolvePushRepository(String accessToken, GitHubRepoRef upstream, String tokenUser) {
        if (tokenUser.equalsIgnoreCase(upstream.owner())) {
            return gitHubPullRequestClient.getRepository(accessToken, upstream.owner(), upstream.repo());
        }
        return gitHubPullRequestClient.ensureFork(accessToken, upstream.owner(), upstream.repo(), tokenUser);
    }

    private void validateSubmittable(Long userId, PatchRecord patchRecord) {
        if (!Boolean.TRUE.equals(patchRecord.getConfirmed())) {
            throw new BusinessException(ErrorCode.PATCH_PR_NOT_ALLOWED, "Patch must be confirmed before submitting a pull request");
        }
        if (Boolean.TRUE.equals(patchRecord.getPrSubmitted())) {
            throw new BusinessException(ErrorCode.PATCH_PR_NOT_ALLOWED, "Pull request has already been submitted");
        }
        if (patchRecord.getPatch() == null || patchRecord.getPatch().isBlank()) {
            throw new BusinessException(ErrorCode.PATCH_PR_NOT_ALLOWED, "Patch content is empty");
        }
        String token = gitHubAuthService.resolveAccessTokenForUser(userId);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Connect GitHub before submitting a pull request");
        }
    }

    private void pushPatchBranch(String repositoryPath,
                                 String baseBranch,
                                 String branchName,
                                 String pushRemoteUrl,
                                 String patch,
                                 String commitMessage,
                                 String accessToken) {
        try (Git git = Git.open(Path.of(repositoryPath).toFile())) {
            Repository repository = git.getRepository();
            checkoutBaseBranch(git, repository, baseBranch);
            if (repository.findRef(branchName) != null) {
                git.branchDelete().setBranchNames(branchName).setForce(true).call();
            }
            git.checkout().setCreateBranch(true).setName(branchName).call();
            git.apply()
                    .setPatch(new ByteArrayInputStream(patch.getBytes(StandardCharsets.UTF_8)))
                    .call();
            if (git.status().call().isClean()) {
                throw new BusinessException(ErrorCode.PATCH_PR_NOT_ALLOWED, "Patch did not change any files");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();
            git.push()
                    .setRemote(pushRemoteUrl)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), "x-oauth-basic"))
                    .add(branchName)
                    .call();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to submit pull request branch: " + errorMessage(exception));
        }
    }

    private void checkoutBaseBranch(Git git, Repository repository, String baseBranch) throws Exception {
        if (repository.findRef(baseBranch) == null) {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(baseBranch)
                    .setStartPoint("origin/" + baseBranch)
                    .call();
        } else {
            git.checkout().setName(baseBranch).call();
        }
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + baseBranch).call();
    }

    private String resolveBaseBranch(ProjectRepo projectRepo, String repositoryPath) {
        if (projectRepo.getDefaultBranch() != null && !projectRepo.getDefaultBranch().isBlank()) {
            return projectRepo.getDefaultBranch().trim();
        }
        try (Git git = Git.open(Path.of(repositoryPath).toFile())) {
            String branch = git.getRepository().getBranch();
            return branch == null || branch.isBlank() ? "main" : branch;
        } catch (Exception exception) {
            return "main";
        }
    }

    private AgentTask requireOwnedTask(Long taskId) {
        return requireOwnedTask(taskId, SecurityUtils.getCurrentUserId());
    }

    private AgentTask requireOwnedTask(Long taskId, Long currentUserId) {
        AgentTask agentTask = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, currentUserId)
                .last("limit 1"));
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    private ProjectRepo requireOwnedProject(Long projectId, Long userId) {
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, userId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private PatchRecord requirePatchRecord(Long taskId) {
        PatchRecord patchRecord = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId)
                .last("limit 1"));
        if (patchRecord == null) {
            throw new BusinessException(ErrorCode.PATCH_RECORD_NOT_FOUND);
        }
        return patchRecord;
    }

    private GitHubRepoRef parseRepoRef(String repoUrl) {
        String normalized = repoUrl == null ? "" : repoUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        String prefix = "https://github.com/";
        if (!normalized.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        String[] parts = normalized.substring(prefix.length()).split("/");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return new GitHubRepoRef(parts[0], parts[1]);
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private record GitHubRepoRef(String owner, String repo) {
    }
}
