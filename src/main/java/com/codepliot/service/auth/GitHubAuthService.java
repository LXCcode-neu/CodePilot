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

/**
 * GitHub OAuth 认证服务。
 * <p>
 * 处理用户的 GitHub 账号关联和授权流程，包括：
 * <ul>
 *   <li>构建 GitHub OAuth 授权 URL</li>
 *   <li>处理 OAuth 回调，交换授权码获取 Access Token</li>
 *   <li>管理用户 GitHub 账号的绑定和解绑</li>
 *   <li>查询已授权的 GitHub 仓库</li>
 *   <li>解析和提供用户的 GitHub Access Token（支持用户个人 Token 和系统级 fallback）</li>
 * </ul>
 * Access Token 使用加密方式存储在数据库中。
 * </p>
 */
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

    /**
     * 构建 GitHub OAuth 授权连接 URL。
     * <p>
     * 生成包含 client_id、redirect_uri、scope 和防 CSRF state 参数的授权 URL，
     * state 参数使用 JWT 生成，有效期为 10 分钟。
     * </p>
     *
     * @return 包含授权 URL 的视图对象
     */
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

    /**
     * 处理 GitHub OAuth 回调，完成账号关联。
     * <p>
     * 验证 state 参数防止 CSRF 攻击，使用授权码交换 Access Token，
     * 获取 GitHub 用户信息并保存到数据库。如果用户已有关联账号则更新。
     * </p>
     *
     * @param request 包含授权码和 state 参数的请求
     * @return 关联的 GitHub 账号视图对象
     */
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

    /**
     * 获取当前用户的 GitHub 账号信息。
     *
     * @return GitHub 账号视图对象，如果未关联则返回断开连接状态的对象
     */
    public GitHubAccountVO currentAccount() {
        UserGitHubAccount account = findByUserId(SecurityUtils.getCurrentUserId());
        return account == null ? GitHubAccountVO.disconnected() : GitHubAccountVO.from(account);
    }

    /**
     * 解除当前用户的 GitHub 账号关联。
     */
    @Transactional
    public void disconnect() {
        Long userId = SecurityUtils.getCurrentUserId();
        userGitHubAccountMapper.delete(new LambdaQueryWrapper<UserGitHubAccount>()
                .eq(UserGitHubAccount::getUserId, userId));
    }

    /**
     * 列出当前用户已授权的 GitHub 仓库。
     *
     * @return 已授权仓库列表
     */
    public List<GitHubAuthorizedRepoVO> listAuthorizedRepositories() {
        return gitHubOAuthClient.listRepositories(requireAccessTokenForCurrentUser("Please connect GitHub first"));
    }

    /**
     * 获取并验证当前用户对指定 GitHub 仓库的访问权限。
     *
     * @param owner 仓库所有者
     * @param repo  仓库名称
     * @return 仓库信息视图对象
     * @throws BusinessException 如果仓库不可访问
     */
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
