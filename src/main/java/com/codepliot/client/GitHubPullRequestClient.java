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

@Component
public class GitHubPullRequestClient {

    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitHubPullRequestClient(GitHubProperties properties,
                                   ObjectMapper objectMapper,
                                   RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    public GitHubPullRequestCreateResult createPullRequest(String owner,
                                                           String repo,
                                                           String title,
                                                           String body,
                                                           String head,
                                                           String base) {
        ResponseEntity<String> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls")
                        .build(owner, repo))
                .headers(this::applyHeaders)
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

    public String getAuthenticatedUsername() {
        ResponseEntity<String> response = restClient.get()
                .uri("/user")
                .headers(this::applyHeaders)
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

    public GitHubRepositoryRef ensureFork(String owner, String repo, String forkOwner) {
        GitHubRepositoryRef existingFork = findRepository(forkOwner, repo);
        if (existingFork != null) {
            return existingFork;
        }

        restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/forks")
                        .build(owner, repo))
                .headers(this::applyHeaders)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toBodilessEntity();

        return waitForFork(forkOwner, repo);
    }

    public GitHubRepositoryRef getRepository(String owner, String repo) {
        GitHubRepositoryRef repository = findRepository(owner, repo);
        if (repository == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub repository not found");
        }
        return repository;
    }

    private GitHubRepositoryRef waitForFork(String forkOwner, String repo) {
        for (int attempt = 0; attempt < 10; attempt++) {
            GitHubRepositoryRef fork = findRepository(forkOwner, repo);
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

    private GitHubRepositoryRef findRepository(String owner, String repo) {
        ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}")
                        .build(owner, repo))
                .headers(this::applyHeaders)
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

    private void applyHeaders(HttpHeaders headers) {
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "GITHUB_TOKEN is required to submit pull requests");
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
