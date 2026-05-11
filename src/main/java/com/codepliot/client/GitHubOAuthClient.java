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

@Component
public class GitHubOAuthClient {

    private final GitHubProperties gitHubProperties;
    private final ObjectMapper objectMapper;
    private final RestClient gitHubBaseClient;
    private final RestClient gitHubApiClient;

    public GitHubOAuthClient(GitHubProperties gitHubProperties,
                             ObjectMapper objectMapper,
                             RestClient.Builder restClientBuilder) {
        this.gitHubProperties = gitHubProperties;
        this.objectMapper = objectMapper;
        this.gitHubBaseClient = restClientBuilder.baseUrl(gitHubProperties.getBaseUrl()).build();
        this.gitHubApiClient = restClientBuilder.baseUrl(gitHubProperties.getApiBaseUrl()).build();
    }

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
