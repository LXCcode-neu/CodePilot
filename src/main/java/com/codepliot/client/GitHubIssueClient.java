package com.codepliot.client;

import com.codepliot.config.GitHubProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubIssuePageVO;
import com.codepliot.model.GitHubIssueVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubIssueClient {

    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitHubIssueClient(GitHubProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    public GitHubIssuePageVO listIssues(String owner, String repo, String state, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        List<GitHubIssueVO> issues = fetchAllRepositoryIssues(owner, repo, state);
        int totalCount = issues.size();
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / safePageSize);
        int effectivePage = totalPages == 0 ? 1 : Math.min(safePage, totalPages);
        int fromIndex = totalCount == 0 ? 0 : (effectivePage - 1) * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, totalCount);

        return new GitHubIssuePageVO(
                issues.subList(fromIndex, toIndex),
                effectivePage,
                safePageSize,
                totalCount,
                totalPages,
                effectivePage > 1,
                totalPages > 0 && effectivePage < totalPages
        );
    }

    private List<GitHubIssueVO> fetchAllRepositoryIssues(String owner, String repo, String state) {
        List<GitHubIssueVO> issues = new ArrayList<>();
        int githubPage = 1;
        boolean hasNext;
        do {
            int currentPage = githubPage;
            ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .queryParam("state", normalizeState(state))
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .queryParam("page", currentPage)
                        .queryParam("per_page", 100)
                        .build(owner, repo))
                .headers(this::applyHeaders)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);

            issues.addAll(parseIssueList(response.getBody()));
            hasNext = hasNextPage(response);
            githubPage++;
        } while (hasNext);
        return issues;
    }

    public GitHubIssueVO getIssue(String owner, String repo, int issueNumber) {
        ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}")
                        .build(owner, repo, issueNumber))
                .headers(this::applyHeaders)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);

        JsonNode issue = parseJson(response.getBody());
        if (issue.hasNonNull("pull_request")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub pull requests cannot be imported as issues");
        }
        return toIssueVO(issue);
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
        String token = properties.getToken();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token.trim());
        }
    }

    private String normalizeState(String state) {
        if ("closed".equalsIgnoreCase(state) || "all".equalsIgnoreCase(state)) {
            return state.toLowerCase();
        }
        return "open";
    }

    private List<GitHubIssueVO> parseIssueList(String body) {
        JsonNode root = parseJson(body);
        if (!root.isArray()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unexpected GitHub issues response");
        }

        List<GitHubIssueVO> issues = new ArrayList<>();
        for (JsonNode issue : root) {
            if (!issue.hasNonNull("pull_request")) {
                issues.add(toIssueVO(issue));
            }
        }
        return issues;
    }

    private GitHubIssueVO toIssueVO(JsonNode issue) {
        return new GitHubIssueVO(
                longValue(issue, "id"),
                intValue(issue, "number"),
                textValue(issue, "title"),
                textValue(issue, "body"),
                textValue(issue, "state"),
                textValue(issue, "html_url"),
                issue.path("user").isMissingNode() ? null : textValue(issue.path("user"), "login"),
                labels(issue.path("labels")),
                intValue(issue, "comments"),
                offsetDateTimeValue(issue, "created_at"),
                offsetDateTimeValue(issue, "updated_at")
        );
    }

    private List<String> labels(JsonNode labelsNode) {
        if (!labelsNode.isArray()) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (JsonNode labelNode : labelsNode) {
            String name = textValue(labelNode, "name");
            if (name != null && !name.isBlank()) {
                labels.add(name);
            }
        }
        return labels;
    }

    private boolean hasNextPage(ResponseEntity<String> response) {
        List<String> linkHeaders = response.getHeaders().get(HttpHeaders.LINK);
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return false;
        }
        return linkHeaders.stream().anyMatch(link -> link.contains("rel=\"next\""));
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body == null ? "" : body);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to parse GitHub response");
        }
    }

    private Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asLong() : null;
    }

    private Integer intValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asInt() : null;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private OffsetDateTime offsetDateTimeValue(JsonNode node, String fieldName) {
        String value = textValue(node, fieldName);
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "GitHub repository or issue not found");
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub API request failed: " + status);
    }
}
