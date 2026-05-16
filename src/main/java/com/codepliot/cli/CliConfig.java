package com.codepliot.cli;

public record CliConfig(
        String serverUrl,
        String accessToken,
        Long defaultProjectId,
        String outputMode
) {

    public static CliConfig empty() {
        return new CliConfig("http://localhost:8080", null, null, "text");
    }

    public CliConfig withServerUrl(String value) {
        return new CliConfig(trimTrailingSlash(value), accessToken, defaultProjectId, outputMode);
    }

    public CliConfig withAccessToken(String value) {
        return new CliConfig(serverUrl, normalizeToken(value), defaultProjectId, outputMode);
    }

    public CliConfig withoutAccessToken() {
        return new CliConfig(serverUrl, null, defaultProjectId, outputMode);
    }

    public String effectiveServerUrl() {
        return serverUrl == null || serverUrl.isBlank() ? "http://localhost:8080" : trimTrailingSlash(serverUrl);
    }

    public boolean jsonMode(boolean commandJsonFlag) {
        return commandJsonFlag || "json".equalsIgnoreCase(outputMode);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String token = value.trim();
        return token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? token.substring("Bearer ".length()).trim()
                : token;
    }
}
