package com.codepliot.search;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.CodeSnippet;
import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.grep.GrepSearchService.GrepSearchResponse;
import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.LlmToolCall;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import com.codepliot.service.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Claude Code 风格的代码检索循环。
 *
 * <p>LLM 决定下一步执行 grep、glob 还是 read 操作。Java 侧只负责校验参数、
 * 执行工具，并把观察结果回传给 LLM。
 */
@Service
public class AgenticCodeSearchService {

    private static final int DEFAULT_TOOL_MAX_RESULTS = 20;
    private static final int OBSERVATION_ITEM_LIMIT = 12;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final GrepTool grepTool;
    private final GlobTool globTool;
    private final ReadTool readTool;
    private final CodeSearchProperties properties;

    public AgenticCodeSearchService(LlmService llmService,
                                    ObjectMapper objectMapper,
                                    GrepTool grepTool,
                                    GlobTool globTool,
                                    ReadTool readTool,
                                    CodeSearchProperties properties) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.grepTool = grepTool;
        this.globTool = globTool;
        this.readTool = readTool;
        this.properties = properties;
    }

    public List<CodeSearchResult> search(String repoPath, String issueText) {
        return search(repoPath, issueText, null);
    }

    public List<CodeSearchResult> search(String repoPath, String issueText, LlmRuntimeConfig llmRuntimeConfig) {
        if (repoPath == null || repoPath.isBlank() || issueText == null || issueText.isBlank()) {
            return List.of();
        }

        int maxResults = Math.max(1, properties.getMaxResults());
        Map<String, CodeSearchResult> collected = new LinkedHashMap<>();
        String projectOverview = buildProjectOverview(repoPath);
        Instant deadline = Instant.now().plus(resolveMaxDuration());
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(buildSystemPrompt(projectOverview)));
        messages.add(LlmMessage.user(buildUserPrompt(issueText)));

        while (collected.size() < maxResults && Instant.now().isBefore(deadline)) {
            LlmToolChatResponse response;
            try {
                response = llmService.chatWithTools(llmRuntimeConfig, messages, toolDefinitions());
            } catch (RuntimeException exception) {
                break;
            }
            if (response == null || !response.hasToolCalls()) {
                break;
            }

            messages.add(LlmMessage.assistant(response.content(), response.toolCalls()));
            for (LlmToolCall toolCall : response.toolCalls()) {
                String observation = executeToolCall(repoPath, toolCall, collected, maxResults);
                messages.add(LlmMessage.tool(toolCall.id(), observation));
            }
        }

        if (collected.isEmpty()) {
            runFallbackGrep(repoPath, issueText, collected, maxResults);
        }

        return collected.values().stream()
                .limit(maxResults)
                .toList();
    }

    private String executeToolCall(String repoPath,
                                   LlmToolCall toolCall,
                                   Map<String, CodeSearchResult> collected,
                                   int maxResults) {
        JsonNode arguments = parseArguments(toolCall.arguments());
        return switch (toolCall.name()) {
            case "grep" -> executeGrep(repoPath, arguments, collected, maxResults);
            case "glob" -> executeGlob(repoPath, arguments);
            case "read" -> executeRead(repoPath, arguments, collected);
            default -> toJson(Map.of("success", false, "message", "unknown tool '" + toolCall.name() + "'"));
        };
    }

    private String executeGrep(String repoPath,
                               JsonNode arguments,
                               Map<String, CodeSearchResult> collected,
                               int maxResults) {
        String query = arguments.path("query").asText("").trim();
        if (query.isBlank()) {
            return toJson(Map.of("success", false, "message", "grep skipped because query is blank"));
        }

        GrepSearchResponse response = grepTool.execute(
                repoPath,
                query,
                parseStringArray(arguments.path("globPatterns")),
                arguments.path("regexEnabled").asBoolean(false),
                Math.min(resolveToolMaxResults(arguments.path("maxResults").isInt() ? arguments.path("maxResults").asInt() : null), maxResults)
        );
        if (!response.success()) {
            return toJson(Map.of("success", false, "message", "grep failed: " + response.message()));
        }

        for (GrepMatch match : response.matches()) {
            if (collected.size() >= maxResults) {
                break;
            }
            addMatchSnippet(repoPath, match, collected);
        }
        return toJson(Map.of(
                "success", true,
                "tool", "grep",
                "query", query,
                "matchCount", response.matches().size(),
                "matches", summarizeMatches(response.matches())
        ));
    }

    private String executeGlob(String repoPath, JsonNode arguments) {
        List<String> files;
        List<String> patterns = parseStringArray(arguments.path("patterns"));
        try {
            files = globTool.execute(repoPath, patterns);
        } catch (RuntimeException exception) {
            return toJson(Map.of("success", false, "message", "glob failed: " + buildErrorMessage(exception)));
        }
        return toJson(Map.of(
                "success", true,
                "tool", "glob",
                "patterns", patterns,
                "fileCount", files.size(),
                "files", files.stream().limit(OBSERVATION_ITEM_LIMIT).toList()
        ));
    }

    private String executeRead(String repoPath,
                               JsonNode arguments,
                               Map<String, CodeSearchResult> collected) {
        String filePath = arguments.path("filePath").asText("").trim();
        if (filePath.isBlank()) {
            return toJson(Map.of("success", false, "message", "read skipped because filePath is blank"));
        }
        try {
            Integer startLine = arguments.path("startLine").isInt() ? arguments.path("startLine").asInt() : null;
            Integer endLine = arguments.path("endLine").isInt() ? arguments.path("endLine").asInt() : null;
            CodeSnippet snippet = readTool.execute(repoPath, filePath, startLine, endLine);
            addSnippet(snippet, "read " + filePath, collected);
            return toJson(Map.of(
                    "success", true,
                    "tool", "read",
                    "filePath", snippet.getFilePath(),
                    "startLine", snippet.getStartLine(),
                    "endLine", snippet.getEndLine(),
                    "content", abbreviate(snippet.getContentWithLineNumbers(), 3000)
            ));
        } catch (RuntimeException exception) {
            return toJson(Map.of("success", false, "message", "read failed: " + buildErrorMessage(exception)));
        }
    }

    private void runFallbackGrep(String repoPath,
                                 String issueText,
                                 Map<String, CodeSearchResult> collected,
                                 int maxResults) {
        GrepSearchResponse response = grepTool.execute(repoPath, issueText, List.of(), false, maxResults);
        if (!response.success()) {
            return;
        }
        for (GrepMatch match : response.matches()) {
            if (collected.size() >= maxResults) {
                break;
            }
            addMatchSnippet(repoPath, match, collected);
        }
    }

    private void addMatchSnippet(String repoPath, GrepMatch match, Map<String, CodeSearchResult> collected) {
        if (match == null || match.getFilePath() == null || match.getFilePath().isBlank()) {
            return;
        }
        try {
            CodeSnippet snippet = readTool.executeAround(
                    repoPath,
                    match.getFilePath(),
                    match.getLineNumber(),
                    properties.getContextBeforeLines(),
                    properties.getContextAfterLines()
            );
            addSnippet(snippet, "grep matched '" + nullToEmpty(match.getQuery()) + "' at line " + nullToEmpty(match.getLineNumber()), collected);
        } catch (RuntimeException ignored) {
            // 过期或不可读的 grep 匹配不应中断检索循环。
        }
    }

    private void addSnippet(CodeSnippet snippet, String reason, Map<String, CodeSearchResult> collected) {
        if (snippet == null || snippet.getFilePath() == null || snippet.getFilePath().isBlank()) {
            return;
        }
        collected.putIfAbsent(snippetKey(snippet), toResult(snippet, reason));
    }

    private String snippetKey(CodeSnippet snippet) {
        return snippet.getFilePath() + ":" + snippet.getStartLine() + ":" + snippet.getEndLine();
    }

    private CodeSearchResult toResult(CodeSnippet snippet, String reason) {
        CodeSearchResult result = new CodeSearchResult();
        result.setFilePath(snippet.getFilePath());
        result.setStartLine(snippet.getStartLine());
        result.setEndLine(snippet.getEndLine());
        result.setScore(0.0d);
        result.setReason(reason);
        result.setContentWithLineNumbers(snippet.getContentWithLineNumbers());
        return result;
    }

    private JsonNode parseArguments(String arguments) {
        try {
            return objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String buildSystemPrompt(String projectOverview) {
        return """
                You are CodePilot's code search agent. Use the available tools to inspect the repository.

                Search strategy:
                - Start broad, then narrow based on observations.
                - Use grep first for likely identifiers, API paths, errors, method names, or domain words.
                - Use glob when a keyword is too broad or a file type/name pattern is more useful.
                - Use read after finding a suspicious file to inspect context.
                - Do not assume the answer in the first round.
                - Continue using tools until you have enough evidence to identify the relevant files.
                - Stop calling tools when further search is unlikely to improve the result.
                - Prefer fewer, high-signal tool calls, but do not stop early if important context is missing.

                Project context:
                %s
                """.formatted(projectOverview);
    }

    private String buildUserPrompt(String issueText) {
        return """
                Issue:
                %s

                Choose the next grep, glob, or read tool call.
                """.formatted(issueText);
    }

    private List<LlmToolDefinition> toolDefinitions() {
        return List.of(grepToolDefinition(), globToolDefinition(), readToolDefinition());
    }

    private LlmToolDefinition grepToolDefinition() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode propertiesNode = parameters.putObject("properties");
        propertiesNode.putObject("query").put("type", "string").put("description", "Search keyword or regex.");
        propertiesNode.putObject("regexEnabled").put("type", "boolean").put("description", "Whether query is a regular expression.");
        propertiesNode.putObject("maxResults").put("type", "integer").put("description", "Maximum grep matches to return.");
        ObjectNode globPatterns = propertiesNode.putObject("globPatterns");
        globPatterns.put("type", "array");
        globPatterns.put("description", "Optional include globs such as **/*.java.");
        globPatterns.putObject("items").put("type", "string");
        parameters.putArray("required").add("query");
        return new LlmToolDefinition("grep", "Search repository text matches.", parameters);
    }

    private LlmToolDefinition globToolDefinition() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode propertiesNode = parameters.putObject("properties");
        ObjectNode patterns = propertiesNode.putObject("patterns");
        patterns.put("type", "array");
        patterns.put("description", "Glob patterns such as **/*Service*.java.");
        patterns.putObject("items").put("type", "string");
        parameters.putArray("required").add("patterns");
        return new LlmToolDefinition("glob", "Find repository files by path/name patterns.", parameters);
    }

    private LlmToolDefinition readToolDefinition() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode propertiesNode = parameters.putObject("properties");
        propertiesNode.putObject("filePath").put("type", "string").put("description", "Repository-relative file path.");
        propertiesNode.putObject("startLine").put("type", "integer").put("description", "Optional start line.");
        propertiesNode.putObject("endLine").put("type", "integer").put("description", "Optional end line.");
        parameters.putArray("required").add("filePath");
        return new LlmToolDefinition("read", "Read a repository file with line numbers.", parameters);
    }

    private String buildProjectOverview(String repoPath) {
        Path root = Path.of(repoPath).toAbsolutePath().normalize();
        return "Technology stack:\n"
                + buildTechStack(root)
                + "\nDirectory tree, depth 2:\n"
                + buildDirectoryTree(root, 2);
    }

    private String buildTechStack(Path root) {
        List<String> items = new ArrayList<>();
        items.addAll(readPomOverview(root.resolve("pom.xml")));
        items.addAll(readPackageJsonOverview(root.resolve("package.json")));
        if (items.isEmpty()) {
            return "- Unknown";
        }
        return String.join("\n", items);
    }

    private List<String> readPomOverview(Path pomPath) {
        if (!Files.isRegularFile(pomPath)) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(pomPath.toFile());
            List<String> values = new ArrayList<>();
            String artifactId = firstText(document, "artifactId");
            if (!artifactId.isBlank()) {
                values.add("- Maven artifact: " + artifactId);
            }
            var dependencies = document.getElementsByTagNameNS("*", "dependency");
            int limit = Math.min(dependencies.getLength(), 12);
            for (int index = 0; index < limit; index++) {
                var dependency = dependencies.item(index);
                String groupId = childText(dependency, "groupId");
                String dependencyArtifactId = childText(dependency, "artifactId");
                if (!dependencyArtifactId.isBlank()) {
                    values.add("- Maven dependency: " + (groupId.isBlank() ? dependencyArtifactId : groupId + ":" + dependencyArtifactId));
                }
            }
            return values;
        } catch (Exception exception) {
            return List.of("- Maven project detected");
        }
    }

    private List<String> readPackageJsonOverview(Path packageJsonPath) {
        if (!Files.isRegularFile(packageJsonPath)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(packageJsonPath.toFile());
            List<String> values = new ArrayList<>();
            String name = root.path("name").asText("");
            if (!name.isBlank()) {
                values.add("- npm package: " + name);
            }
            addPackageDependencies(values, root.path("dependencies"));
            addPackageDependencies(values, root.path("devDependencies"));
            return values;
        } catch (IOException exception) {
            return List.of("- Node package detected");
        }
    }

    private void addPackageDependencies(List<String> values, JsonNode dependencies) {
        if (!dependencies.isObject()) {
            return;
        }
        dependencies.fieldNames().forEachRemaining(name -> {
            if (values.size() < 16) {
                values.add("- npm dependency: " + name);
            }
        });
    }

    private String buildDirectoryTree(Path root, int maxDepth) {
        if (!Files.isDirectory(root)) {
            return "- Repository path not found";
        }
        List<String> lines = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root, maxDepth)) {
            stream.filter(path -> !root.equals(path))
                    .filter(path -> !isExcludedPath(root, path))
                    .limit(80)
                    .forEach(path -> {
                        int depth = root.relativize(path).getNameCount();
                        lines.add("  ".repeat(Math.max(0, depth - 1)) + "- " + root.relativize(path).toString().replace('\\', '/'));
                    });
        } catch (IOException exception) {
            return "- Failed to read directory tree";
        }
        return lines.isEmpty() ? "- Empty repository" : String.join("\n", lines);
    }

    private boolean isExcludedPath(Path root, Path path) {
        String normalized = "/" + root.relativize(path).toString().replace('\\', '/').toLowerCase();
        return normalized.contains("/.git")
                || normalized.contains("/target")
                || normalized.contains("/build")
                || normalized.contains("/dist")
                || normalized.contains("/node_modules")
                || normalized.contains("/.idea")
                || normalized.contains("/.vscode");
    }

    private String firstText(Document document, String tagName) {
        var nodes = document.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(tagName);
        }
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String childText(org.w3c.dom.Node node, String tagName) {
        for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (tagName.equals(child.getLocalName()) || tagName.equals(child.getNodeName())) {
                return child.getTextContent() == null ? "" : child.getTextContent().trim();
            }
        }
        return "";
    }

    private List<String> parseStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (value != null && !value.isBlank()) {
                values.add(value.trim().replace('\\', '/'));
            }
        }
        return List.copyOf(values);
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        return objectStart >= 0 && objectEnd > objectStart ? trimmed.substring(objectStart, objectEnd + 1) : "";
    }

    private String summarizeMatches(List<GrepMatch> matches) {
        try {
            ArrayNode values = objectMapper.createArrayNode();
            matches.stream()
                .limit(OBSERVATION_ITEM_LIMIT)
                    .forEach(match -> {
                        ObjectNode node = values.addObject();
                        node.put("filePath", match.getFilePath());
                        node.put("lineNumber", match.getLineNumber());
                        node.put("column", match.getColumn());
                        node.put("lineText", abbreviate(match.getLineText()));
                    });
            return objectMapper.writeValueAsString(values);
        } catch (IOException exception) {
            return "[]";
        }
    }

    private String abbreviate(String value) {
        return abbreviate(value, 140);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private int resolveToolMaxResults(Integer requestedMaxResults) {
        return requestedMaxResults == null || requestedMaxResults <= 0 ? DEFAULT_TOOL_MAX_RESULTS : requestedMaxResults;
    }

    private Duration resolveMaxDuration() {
        Duration maxDuration = properties.getMaxDuration();
        if (maxDuration == null || maxDuration.isZero() || maxDuration.isNegative()) {
            return Duration.ofMinutes(2);
        }
        return maxDuration;
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            return "{\"success\":false,\"message\":\"failed to serialize tool observation\"}";
        }
    }
}
