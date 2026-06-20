package com.codepliot.controller;

import com.codepliot.model.GitHubAccountVO;
import com.codepliot.model.GitHubAuthUrlVO;
import com.codepliot.model.GitHubAuthorizedRepoVO;
import com.codepliot.model.GitHubConnectRequest;
import com.codepliot.model.Result;
import com.codepliot.service.auth.GitHubAuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub 授权控制器
 * <p>
 * 提供 GitHub OAuth 授权流程相关接口，包括获取授权链接、处理回调、
 * 查看已连接账户、断开连接以及列出已授权仓库等操作。
 * </p>
 */
@RestController
@RequestMapping("/api/github")
public class GitHubAuthController {

    private final GitHubAuthService gitHubAuthService;

    public GitHubAuthController(GitHubAuthService gitHubAuthService) {
        this.gitHubAuthService = gitHubAuthService;
    }

    /**
     * 获取 GitHub OAuth 授权链接
     *
     * @return 包含授权 URL 的视图对象，用户可通过该链接进行 GitHub 授权
     */
    @GetMapping("/auth-url")
    public Result<GitHubAuthUrlVO> authUrl() {
        return Result.success(gitHubAuthService.buildConnectUrl());
    }

    /**
     * 处理 GitHub OAuth 授权回调，完成账户绑定
     *
     * @param request 包含授权码等回调参数的请求对象
     * @return 连接成功后的 GitHub 账户信息
     */
    @PostMapping("/callback")
    public Result<GitHubAccountVO> callback(@Valid @RequestBody GitHubConnectRequest request) {
        return Result.success("GitHub account connected", gitHubAuthService.connect(request));
    }

    /**
     * 获取当前用户已连接的 GitHub 账户信息
     *
     * @return GitHub 账户视图对象
     */
    @GetMapping("/account")
    public Result<GitHubAccountVO> account() {
        return Result.success(gitHubAuthService.currentAccount());
    }

    /**
     * 断开当前用户与 GitHub 账户的连接
     *
     * @return 操作结果
     */
    @DeleteMapping("/account")
    public Result<Void> disconnect() {
        gitHubAuthService.disconnect();
        return Result.success("GitHub account disconnected", null);
    }

    /**
     * 获取当前用户已授权的 GitHub 仓库列表
     *
     * @return 已授权仓库列表
     */
    @GetMapping("/repositories")
    public Result<List<GitHubAuthorizedRepoVO>> repositories() {
        return Result.success(gitHubAuthService.listAuthorizedRepositories());
    }
}
