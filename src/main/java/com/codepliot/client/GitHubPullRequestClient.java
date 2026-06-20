package com.codepliot.client;

import com.codepliot.config.GitHubProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubPullRequestCreateResult;
import com.codepliot.model.GitHubRepositoryRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub Pull Request 客户端。
 * <p>
 * 封装 GitHub REST API，支持创建 Pull Request、Fork 仓库、
 * 获取仓库信息以及查询已认证用户名等操作。
 * </p>
 */
@Component
public class GitHubPullRequestClient {

    /** GitHub API 版本号 */
    private static final String GITHUB_API_VERSION = "2022-11-28";

    /** GitHub 配置属性，包含 API 基础 URL 和访问令牌 */
    private final GitHubProperties properties;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** REST 客户端，用于发送 HTTP 请求 */
    private final RestClient restClient;

    /**
     * 构造方法，注入配置属性和 REST 客户端构建器。
     *
     * @param properties        GitHub 配置属性
     * @param objectMapper      JSON 对象映射器
     * @param restClientBuilder REST 客户端构建器
     */
    public GitHubPullRequestClient(GitHubProperties properties,
                                   ObjectMapper objectMapper,
                                   RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    /**
     * 创建 Pull Request（使用默认令牌）。
     *
     * @param owner 仓库所有者
     * @param repo  仓库名称
     * @param title PR 标题
     * @param body  PR 描述内容
     * @param head  源分支（格式：owner:branch）
     * @param base  目标分支
     * @return 创建结果，包含 PR 编号和链接
     */
    public GitHubPullRequestCreateResult createPullRequest(String owner,
                                                           String repo,
                                                           String title,
                                                           String body,
                                                           String head,
                                                           String base) {
        return createPullRequest(null, owner, repo, title, body, head, base);
    }

    /**
     * 创建 Pull Request（使用自定义令牌）。
     *
     * @param accessToken 访问令牌（为空时使用默认令牌）
     * @param owner       仓库所有者
     * @param repo        仓库名称
     * @param title       PR 标题
     * @param body        PR 描述内容
     * @param head        源分支（格式：owner:branch）
     * @param base        目标分支
     * @return 创建结果，包含 PR 编号和链接
     */
    public GitHubPullRequestCreateResult createPullRequest(String accessToken,
                                                           String owner,
                                                           String repo,
                                                           String title,
                                                           String body,
                                                           String head,
                                                           String base) {
        ResponseEntity<String> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls")
                        .build(owner, repo))
                .headers(headers -> applyHeaders(headers, accessToken))
                .body(Map.of(
                        "title", title,
                        "body", body,
                        "head", head,
                        "base", base
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);

        JsonNode root = parseJson(response.getBody());
        return new GitHubPullRequestCreateResult(
                root.path("number").isNumber() ? root.path("number").asInt() : null,
                textValue(root, "html_url")
        );
    }

    /**
     * 获取已认证用户的 GitHub 用户名（使用默认令牌）。
     *
     * @return GitHub 用户名
     */
    public String getAuthenticatedUsername() {
        return getAuthenticatedUsername(null);
    }

    /**
     * 获取已认证用户的 GitHub 用户名（使用自定义令牌）。
     *
     * @param accessToken 访问令牌（为空时使用默认令牌）
     * @return GitHub 用户名
     */
    public String getAuthenticatedUsername(String accessToken) {
        ResponseEntity<String> response = restClient.get()
                .uri("/user")
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);
        JsonNode root = parseJson(response.getBody());
        String login = textValue(root, "login");
        if (login == null || login.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to resolve GitHub token user");
        }
        return login;
    }

    /**
     * 确保指定用户拥有目标仓库的 Fork（使用默认令牌）。
     * <p>
     * 如果 Fork 已存在则直接返回，否则创建新的 Fork 并等待其就绪。
     * </p>
     *
     * @param owner     原仓库所有者
     * @param repo      仓库名称
     * @param forkOwner Fork 目标用户
     * @return Fork 仓库引用
     */
    public GitHubRepositoryRef ensureFork(String owner, String repo, String forkOwner) {
        return ensureFork(null, owner, repo, forkOwner);
    }

    /**
     * 确保指定用户拥有目标仓库的 Fork（使用自定义令牌）。
     *
     * @param accessToken 访问令牌（为空时使用默认令牌）
     * @param owner       原仓库所有者
     * @param repo        仓库名称
     * @param forkOwner   Fork 目标用户
     * @return Fork 仓库引用
     */
    public GitHubRepositoryRef ensureFork(String accessToken, String owner, String repo, String forkOwner) {
        GitHubRepositoryRef existingFork = findRepository(accessToken, forkOwner, repo);
        if (existingFork != null) {
            return existingFork;
        }

        restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/forks")
                        .build(owner, repo))
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toBodilessEntity();

        return waitForFork(accessToken, forkOwner, repo);
    }

    /**
     * 获取仓库信息（使用默认令牌）。
     *
     * @param owner 仓库所有者
     * @param repo  仓库名称
     * @return 仓库引用信息
     */
    public GitHubRepositoryRef getRepository(String owner, String repo) {
        return getRepository(null, owner, repo);
    }

    /**
     * 获取仓库信息（使用自定义令牌）。
     *
     * @param accessToken 访问令牌（为空时使用默认令牌）
     * @param owner       仓库所有者
     * @param repo        仓库名称
     * @return 仓库引用信息
     */
    public GitHubRepositoryRef getRepository(String accessToken, String owner, String repo) {
        GitHubRepositoryRef repository = findRepository(accessToken, owner, repo);
        if (repository == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub repository not found");
        }
        return repository;
    }

    private GitHubRepositoryRef waitForFork(String accessToken, String forkOwner, String repo) {
        for (int attempt = 0; attempt < 10; attempt++) {
            GitHubRepositoryRef fork = findRepository(accessToken, forkOwner, repo);
            if (fork != null) {
                return fork;
            }
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Interrupted while waiting for GitHub fork");
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GitHub fork was created but is not ready yet");
    }

    private GitHubRepositoryRef findRepository(String accessToken, String owner, String repo) {
        ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}")
                        .build(owner, repo))
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, errorResponse) -> {
                })
                .onStatus(status -> status.isError() && status.value() != 404, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);
        if (response.getStatusCode().value() == 404 || response.getBody() == null || response.getBody().isBlank()) {
            return null;
        }
        JsonNode root = parseJson(response.getBody());
        JsonNode ownerNode = root.path("owner");
        return new GitHubRepositoryRef(
                textValue(ownerNode, "login"),
                textValue(root, "name"),
                textValue(root, "clone_url")
        );
    }

    private void applyHeaders(HttpHeaders headers, String accessToken) {
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
        String token = accessToken == null || accessToken.isBlank() ? properties.getToken() : accessToken;
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "GitHub authorization is required");
        }
        headers.setBearerAuth(token.trim());
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body == null ? "" : body);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to parse GitHub pull request response");
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private void throwGitHubException(HttpStatusCode statusCode) {
        int status = statusCode.value();
        if (status == 401) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "GitHub token is invalid");
        }
        if (status == 403) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "GitHub permission denied or rate limit exceeded");
        }
        if (status == 404) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub repository not found");
        }
        if (status == 422) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub pull request already exists or request is invalid");
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub pull request request failed: " + status);
    }
}
