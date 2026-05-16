package com.codepliot.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class CodePilotApiClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CliConfig config;

    public CodePilotApiClient(ObjectMapper objectMapper, CliConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode get(String path) {
        return send("GET", path, null);
    }

    public JsonNode delete(String path) {
        return send("DELETE", path, null);
    }

    public JsonNode post(String path) {
        return send("POST", path, "");
    }

    public JsonNode post(String path, Object body) {
        try {
            return send("POST", path, objectMapper.writeValueAsString(body));
        } catch (IOException exception) {
            throw new CliException(1, "序列化请求失败: " + exception.getMessage());
        }
    }

    public JsonNode login(String username, String password) {
        return post("/api/auth/login", Map.of("username", username, "password", password));
    }

    private JsonNode send(String method, String path, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json");
        if (config.accessToken() != null && !config.accessToken().isBlank()) {
            builder.header("Authorization", "Bearer " + config.accessToken().trim());
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = parseResponse(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CliException(mapHttpExitCode(response.statusCode()), "后端请求失败: " + response.statusCode() + " " + message(json));
            }
            if (json.has("code") && json.path("code").asInt(-1) != 0) {
                throw new CliException(mapBusinessExitCode(json.path("code").asInt()), "后端拒绝操作: " + message(json));
            }
            return json;
        } catch (IOException exception) {
            throw new CliException(5, "无法连接 CodePilot 后端: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CliException(1, "请求被中断");
        }
    }

    private URI uri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(config.effectiveServerUrl() + normalizedPath);
    }

    private JsonNode parseResponse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private String message(JsonNode json) {
        String message = json.path("message").asText("");
        return message.isBlank() ? "无错误详情" : message;
    }

    private int mapHttpExitCode(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return 4;
        }
        if (statusCode >= 500) {
            return 5;
        }
        return 3;
    }

    private int mapBusinessExitCode(int code) {
        if (code == 401 || code == 403 || code == 1004) {
            return 4;
        }
        return 3;
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
