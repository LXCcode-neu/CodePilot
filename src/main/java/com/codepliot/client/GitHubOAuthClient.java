package com.codepliot.client;

import com.codepliot.config.GitHubProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubAuthorizedRepoVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub OAuth 客户端。
 * <p>
 * 封装 GitHub OAuth 认证流程，支持授权码换取访问令牌、
 * 获取已认证用户信息以及查询用户授权的仓库列表。
 * </p>
 */
@Component
public class GitHubOAuthClient {

    /** GitHub 配置属性，包含 OAuth 客户端ID、密钥、重定向URI等 */
    private final GitHubProperties gitHubProperties;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** GitHub 基础 REST 客户端（用于 OAuth 相关请求） */
    private final RestClient gitHubBaseClient;

    /** GitHub API REST 客户端（用于仓库和用户相关请求） */
    private final RestClient gitHubApiClient;

    /**
     * 构造方法，注入配置属性并构建两个 REST 客户端。
     *
     * @param gitHubProperties  GitHub 配置属性
     * @param objectMapper      JSON 对象映射器
     * @param restClientBuilder REST 客户端构建器
     */
    public GitHubOAuthClient(GitHubProperties gitHubProperties,
                             ObjectMapper objectMapper,
                             RestClient.Builder restClientBuilder) {
        this.gitHubProperties = gitHubProperties;
        this.objectMapper = objectMapper;
        this.gitHubBaseClient = restClientBuilder.baseUrl(gitHubProperties.getBaseUrl()).build();
        this.gitHubApiClient = restClientBuilder.baseUrl(gitHubProperties.getApiBaseUrl()).build();
    }

    /**
     * 使用授权码换取 GitHub 访问令牌。
     *
     * @param code OAuth 授权码
     * @return 包含访问令牌和授权范围的响应对象
     */
    public GitHubOAuthTokenResponse exchangeCode(String code) {
        ResponseEntity<String> response = gitHubBaseClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "client_id", required(gitHubProperties.getClientId(), "GitHub OAuth client ID is not configured"),
                        "client_secret", required(gitHubProperties.getClientSecret(), "GitHub OAuth client secret is not configured"),
                        "code", code,
                        "redirect_uri", required(gitHubProperties.getOauthRedirectUri(), "GitHub OAuth redirect URI is not configured")
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "GitHub OAuth token exchange failed: " + errorResponse.getStatusCode().value());
                })
                .toEntity(String.class);
        JsonNode root = parseJson(response.getBody(), "Failed to parse GitHub OAuth token response");
        String accessToken = text(root, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub OAuth token exchange returned no access token");
        }
        return new GitHubOAuthTokenResponse(accessToken, text(root, "scope"));
    }

    /**
     * 获取已认证用户的 GitHub 信息。
     *
     * @param accessToken 访问令牌
     * @return 包含用户ID、登录名、头像等信息的响应对象
     */
    public GitHubOAuthUserResponse getAuthenticatedUser(String accessToken) {
        JsonNode root = apiGet(accessToken, "/user");
        Long githubUserId = longValue(root, "id");
        String login = text(root, "login");
        if (githubUserId == null || login == null || login.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to resolve GitHub user profile");
        }
        return new GitHubOAuthUserResponse(
                githubUserId,
                login,
                text(root, "name"),
                text(root, "avatar_url")
        );
    }

    /**
     * 列出用户授权的所有仓库（自动分页）。
     *
     * @param accessToken 访问令牌
     * @return 仓库列表
     */
    public List<GitHubAuthorizedRepoVO> listRepositories(String accessToken) {
        List<GitHubAuthorizedRepoVO> repositories = new ArrayList<>();
        int page = 1;
        while (true) {
            int currentPage = page;
            ResponseEntity<String> response = gitHubApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/repos")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", 100)
                            .queryParam("page", currentPage)
                            .build())
                    .headers(headers -> applyApiHeaders(headers, accessToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, errorResponse) -> throwGitHubApiException(errorResponse.getStatusCode()))
                    .toEntity(String.class);
            JsonNode root = parseJson(response.getBody(), "Failed to parse GitHub repositories response");
            if (!root.isArray()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unexpected GitHub repositories response");
            }
            int count = 0;
            for (JsonNode repository : root) {
                repositories.add(toRepositoryVO(repository));
                count++;
            }
            if (count < 100) {
                break;
            }
            page++;
        }
        return repositories;
    }

    /**
     * 获取指定仓库的详细信息。
     *
     * @param accessToken 访问令牌
     * @param owner       仓库所有者
     * @param repo        仓库名称
     * @return 仓库详情
     */
    public GitHubAuthorizedRepoVO getRepository(String accessToken, String owner, String repo) {
        ResponseEntity<String> response = gitHubApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}")
                        .build(owner, repo))
                .headers(headers -> applyApiHeaders(headers, accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> throwGitHubApiException(errorResponse.getStatusCode()))
                .toEntity(String.class);
        return toRepositoryVO(parseJson(response.getBody(), "Failed to parse GitHub repository response"));
    }

    private JsonNode apiGet(String accessToken, String path) {
        ResponseEntity<String> response = gitHubApiClient.get()
                .uri(path)
                .headers(headers -> applyApiHeaders(headers, accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> throwGitHubApiException(errorResponse.getStatusCode()))
                .toEntity(String.class);
        return parseJson(response.getBody(), "Failed to parse GitHub API response");
    }

    private void applyApiHeaders(HttpHeaders headers, String accessToken) {
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.setBearerAuth(accessToken.trim());
    }

    private GitHubAuthorizedRepoVO toRepositoryVO(JsonNode repository) {
        JsonNode ownerNode = repository.path("owner");
        return new GitHubAuthorizedRepoVO(
                longValue(repository, "id"),
                text(ownerNode, "login"),
                text(repository, "name"),
                text(repository, "full_name"),
                repository.path("private").asBoolean(false),
                text(repository, "default_branch"),
                text(repository, "html_url"),
                text(repository, "clone_url")
        );
    }

    private void throwGitHubApiException(HttpStatusCode statusCode) {
        int status = statusCode.value();
        if (status == 401) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "GitHub authorization has expired or is invalid");
        }
        if (status == 403) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "GitHub API rate limited or access denied");
        }
        if (status == 404) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub resource not found");
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub API request failed: " + status);
    }

    private JsonNode parseJson(String body, String errorMessage) {
        try {
            return objectMapper.readTree(body == null ? "" : body);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, errorMessage);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asLong() : null;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    public record GitHubOAuthTokenResponse(String accessToken, String scope) {
    }

    public record GitHubOAuthUserResponse(Long githubUserId, String login, String name, String avatarUrl) {
    }
}
