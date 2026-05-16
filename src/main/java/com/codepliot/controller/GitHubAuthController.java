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

@RestController
@RequestMapping("/api/github")
public class GitHubAuthController {

    private final GitHubAuthService gitHubAuthService;

    public GitHubAuthController(GitHubAuthService gitHubAuthService) {
        this.gitHubAuthService = gitHubAuthService;
    }

    @GetMapping("/auth-url")
    public Result<GitHubAuthUrlVO> authUrl() {
        return Result.success(gitHubAuthService.buildConnectUrl());
    }

    @PostMapping("/callback")
    public Result<GitHubAccountVO> callback(@Valid @RequestBody GitHubConnectRequest request) {
        return Result.success("GitHub account connected", gitHubAuthService.connect(request));
    }

    @GetMapping("/account")
    public Result<GitHubAccountVO> account() {
        return Result.success(gitHubAuthService.currentAccount());
    }

    @DeleteMapping("/account")
    public Result<Void> disconnect() {
        gitHubAuthService.disconnect();
        return Result.success("GitHub account disconnected", null);
    }

    @GetMapping("/repositories")
    public Result<List<GitHubAuthorizedRepoVO>> repositories() {
        return Result.success(gitHubAuthService.listAuthorizedRepositories());
    }
}
