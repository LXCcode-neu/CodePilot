package com.codepliot.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.GitHubOAuthClient;
import com.codepliot.config.GitHubProperties;
import com.codepliot.entity.UserGitHubAccount;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubAccountVO;
import com.codepliot.model.GitHubAuthUrlVO;
import com.codepliot.model.GitHubAuthorizedRepoVO;
import com.codepliot.model.GitHubConnectRequest;
import com.codepliot.repository.UserGitHubAccountMapper;
import com.codepliot.utils.ApiKeyCryptoUtils;
import com.codepliot.utils.JwtUtils;
import com.codepliot.utils.SecurityUtils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubAuthService {

    private static final long STATE_EXPIRE_SECONDS = 600L;

    private final UserGitHubAccountMapper userGitHubAccountMapper;
    private final GitHubOAuthClient gitHubOAuthClient;
    private final ApiKeyCryptoUtils apiKeyCryptoUtils;
    private final JwtUtils jwtUtils;
    private final GitHubProperties gitHubProperties;

    public GitHubAuthService(UserGitHubAccountMapper userGitHubAccountMapper,
                             GitHubOAuthClient gitHubOAuthClient,
                             ApiKeyCryptoUtils apiKeyCryptoUtils,
                             JwtUtils jwtUtils,
                             GitHubProperties gitHubProperties) {
        this.userGitHubAccountMapper = userGitHubAccountMapper;
        this.gitHubOAuthClient = gitHubOAuthClient;
        this.apiKeyCryptoUtils = apiKeyCryptoUtils;
        this.jwtUtils = jwtUtils;
        this.gitHubProperties = gitHubProperties;
    }

    public GitHubAuthUrlVO buildConnectUrl() {
        Long userId = SecurityUtils.getCurrentUserId();
        String clientId = required(gitHubProperties.getClientId(), "GitHub OAuth client ID is not configured");
        String redirectUri = required(gitHubProperties.getOauthRedirectUri(), "GitHub OAuth redirect URI is not configured");
        String scope = gitHubProperties.getOauthScope() == null || gitHubProperties.getOauthScope().isBlank()
                ? "repo read:user"
                : gitHubProperties.getOauthScope().trim();
        String state = jwtUtils.generateGitHubOauthStateToken(userId, STATE_EXPIRE_SECONDS);
        String url = gitHubProperties.getBaseUrl()
                + "/login/oauth/authorize?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(scope)
                + "&state=" + encode(state);
        return new GitHubAuthUrlVO(url);
    }

    @Transactional
    public GitHubAccountVO connect(GitHubConnectRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            jwtUtils.validateGitHubOauthStateToken(request.state(), userId);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub authorization state is invalid or expired");
        }

        GitHubOAuthClient.GitHubOAuthTokenResponse tokenResponse = gitHubOAuthClient.exchangeCode(request.code().trim());
        GitHubOAuthClient.GitHubOAuthUserResponse userResponse = gitHubOAuthClient.getAuthenticatedUser(tokenResponse.accessToken());

        UserGitHubAccount account = findByUserId(userId);
        if (account == null) {
            account = new UserGitHubAccount();
            account.setUserId(userId);
        }
        account.setGithubUserId(userResponse.githubUserId());
        account.setGithubLogin(userResponse.login());
        account.setGithubName(userResponse.name());
        account.setGithubAvatarUrl(userResponse.avatarUrl());
        account.setAccessTokenEncrypted(apiKeyCryptoUtils.encrypt(tokenResponse.accessToken()));
        account.setScope(tokenResponse.scope());

        if (account.getId() == null) {
            userGitHubAccountMapper.insert(account);
        } else {
            userGitHubAccountMapper.updateById(account);
        }
        return GitHubAccountVO.from(account);
    }

    public GitHubAccountVO currentAccount() {
        UserGitHubAccount account = findByUserId(SecurityUtils.getCurrentUserId());
        return account == null ? GitHubAccountVO.disconnected() : GitHubAccountVO.from(account);
    }

    @Transactional
    public void disconnect() {
        Long userId = SecurityUtils.getCurrentUserId();
        userGitHubAccountMapper.delete(new LambdaQueryWrapper<UserGitHubAccount>()
                .eq(UserGitHubAccount::getUserId, userId));
    }

    public List<GitHubAuthorizedRepoVO> listAuthorizedRepositories() {
        return gitHubOAuthClient.listRepositories(requireAccessTokenForCurrentUser("Please connect GitHub first"));
    }

    public GitHubAuthorizedRepoVO requireAuthorizedRepository(String owner, String repo) {
        GitHubAuthorizedRepoVO repository = gitHubOAuthClient.getRepository(
                requireAccessTokenForCurrentUser("Please connect GitHub first"),
                owner,
                repo
        );
        if (repository.owner() == null || repository.name() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub repository is not accessible");
        }
        return repository;
    }

    public String resolveAccessTokenForCurrentUser() {
        return resolveAccessTokenForUser(SecurityUtils.getCurrentUserId());
    }

    public String resolveAccessTokenForUser(Long userId) {
        UserGitHubAccount account = findByUserId(userId);
        if (account != null && account.getAccessTokenEncrypted() != null && !account.getAccessTokenEncrypted().isBlank()) {
            return apiKeyCryptoUtils.decrypt(account.getAccessTokenEncrypted());
        }
        String fallback = gitHubProperties.getToken();
        return fallback == null ? "" : fallback.trim();
    }

    public String requireAccessTokenForCurrentUser(String message) {
        String accessToken = resolveAccessTokenForCurrentUser();
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, message);
        }
        return accessToken;
    }

    public String requireAccessTokenForUser(Long userId, String message) {
        String accessToken = resolveAccessTokenForUser(userId);
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, message);
        }
        return accessToken;
    }

    private UserGitHubAccount findByUserId(Long userId) {
        return userGitHubAccountMapper.selectOne(new LambdaQueryWrapper<UserGitHubAccount>()
                .eq(UserGitHubAccount::getUserId, userId)
                .last("limit 1"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
