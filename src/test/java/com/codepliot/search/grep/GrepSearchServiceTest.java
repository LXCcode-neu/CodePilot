package com.codepliot.search.grep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.search.grep.GrepSearchService.GrepSearchResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrepSearchServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSearchJavaKeywordAndSpringAnnotationWithFallback() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                    public String login() {
                        return "login";
                    }
                }
                """);

        GrepSearchService service = fallbackService();

        List<GrepMatch> keywordMatches = service.search(request("UserController", 10));
        List<GrepMatch> annotationMatches = service.search(request("@RestController", 10));

        assertFalse(keywordMatches.isEmpty());
        assertEquals("src/main/java/com/example/UserController.java", keywordMatches.get(0).getFilePath());
        assertTrue(keywordMatches.get(0).getLineNumber() > 0);
        assertTrue(keywordMatches.get(0).getColumn() > 0);
        assertTrue(keywordMatches.get(0).getLineText().contains("UserController"));
        assertFalse(annotationMatches.isEmpty());
    }

    @Test
    void shouldLimitMaxResults() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                class UserController {
                    String one = "login";
                    String two = "login";
                    String three = "login";
                }
                """);

        List<GrepMatch> matches = fallbackService().search(request("login", 2));

        assertEquals(2, matches.size());
    }

    @Test
    void shouldReturnClearMessageWhenRipgrepIsMissingAndUseFallback() throws IOException {
        write("src/main/java/com/example/UserController.java", "class UserController {}\n");
        CodeSearchProperties properties = new CodeSearchProperties();
        properties.setUseRipgrep(true);
        properties.setFallbackEnabled(true);
        properties.setRgPath("rg-command-that-does-not-exist-for-codepilot-tests");
        GrepSearchService service = new GrepSearchService(properties, new RipgrepCommandBuilder(), new RipgrepResultParser());

        GrepSearchResponse response = service.searchWithStatus(request("UserController", 10));

        assertTrue(response.success());
        assertTrue(response.message().contains("ripgrep is not available"));
        assertFalse(response.matches().isEmpty());
    }

    private GrepSearchService fallbackService() {
        CodeSearchProperties properties = new CodeSearchProperties();
        properties.setUseRipgrep(false);
        return new GrepSearchService(properties, new RipgrepCommandBuilder(), new RipgrepResultParser());
    }

    private SearchRequest request(String query, int maxResults) {
        SearchRequest request = new SearchRequest();
        request.setRepoPath(tempDir.toString());
        request.setQuery(query);
        request.setMaxResults(maxResults);
        request.setGlobPatterns(List.of("**/*.java"));
        return request;
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
