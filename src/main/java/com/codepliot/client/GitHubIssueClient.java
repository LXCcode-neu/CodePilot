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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubIssueClient {

    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final Pattern LAST_PAGE_PATTERN = Pattern.compile("[?&]page=(\\d+)[^>]*>;\\s*rel=\"last\"");

    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitHubIssueClient(GitHubProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    public GitHubIssuePageVO listIssues(String owner, String repo, String state, int page, int pageSize) {
        return listIssues(null, owner, repo, state, page, pageSize);
    }

    public GitHubIssuePageVO listIssues(String accessToken, String owner, String repo, String state, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        ResponseEntity<String> response = fetchRepositoryIssuesPage(accessToken, owner, repo, state, safePage, safePageSize);
        List<GitHubIssueVO> issues = parseIssueList(response.getBody());
        int totalPages = resolveTotalPages(response, safePage, issues);
        int totalCount = estimateTotalCount(totalPages, safePageSize, safePage, issues.size());

        return new GitHubIssuePageVO(
                issues,
                safePage,
                safePageSize,
                totalCount,
                totalPages,
                safePage > 1,
                totalPages > 0 && safePage < totalPages
        );
    }

    private ResponseEntity<String> fetchRepositoryIssuesPage(String accessToken, String owner, String repo, String state, int page, int pageSize) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .queryParam("state", normalizeState(state))
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .queryParam("page", page)
                        .queryParam("per_page", pageSize)
                        .build(owner, repo))
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throwGitHubException(errorResponse.getStatusCode());
                })
                .toEntity(String.class);
    }

    public GitHubIssueVO getIssue(String owner, String repo, int issueNumber) {
        return getIssue(null, owner, repo, issueNumber);
    }

    public GitHubIssueVO getIssue(String accessToken, String owner, String repo, int issueNumber) {
        ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}")
                        .build(owner, repo, issueNumber))
                .headers(headers -> applyHeaders(headers, accessToken))
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

    private void applyHeaders(HttpHeaders headers, String accessToken) {
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
        String token = accessToken == null || accessToken.isBlank() ? properties.getToken() : accessToken;
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

    private int resolveTotalPages(ResponseEntity<String> response, int currentPage, List<GitHubIssueVO> issues) {
        List<String> linkHeaders = response.getHeaders().get(HttpHeaders.LINK);
        if (linkHeaders != null) {
            for (String linkHeader : linkHeaders) {
                Matcher matcher = LAST_PAGE_PATTERN.matcher(linkHeader);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }
        if (hasNextPage(response)) {
            return currentPage + 1;
        }
        return issues.isEmpty() && currentPage == 1 ? 0 : currentPage;
    }

    private int estimateTotalCount(int totalPages, int pageSize, int currentPage, int currentItemCount) {
        if (totalPages == 0) {
            return 0;
        }
        if (currentPage >= totalPages) {
            return Math.max(0, (currentPage - 1) * pageSize + currentItemCount);
        }
        return totalPages * pageSize;
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
