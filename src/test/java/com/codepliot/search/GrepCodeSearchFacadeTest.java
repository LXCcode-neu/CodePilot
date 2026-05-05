package com.codepliot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.grep.RipgrepCommandBuilder;
import com.codepliot.search.grep.RipgrepResultParser;
import com.codepliot.search.read.CodeReadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrepCodeSearchFacadeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnSearchResultsForIssue() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                package com.example;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                    public void confirmPatch() {
                    }
                }
                """);

        List<CodeSearchResult> results = facade().search(request("confirmPatch fails in UserController", 10, false));

        assertFalse(results.isEmpty());
        assertEquals("src/main/java/com/example/UserController.java", results.get(0).getFilePath());
        assertTrue(results.get(0).getContentWithLineNumbers().contains("confirmPatch"));
    }

    @Test
    void shouldUseSpringAnnotationFallbackWhenExactKeywordMisses() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                package com.example;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                }
                """);

        List<CodeSearchResult> results = facade().search(request("NoExactBusinessKeywordHere", 10, false));

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getContentWithLineNumbers().contains("@RestController"));
    }

    @Test
    void shouldDeduplicateAndRespectMaxResults() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                @RestController
                public class UserController {
                    String login = "login";
                    String anotherLogin = "login";
                }
                """);

        List<CodeSearchResult> results = facade().search(request("login UserController", 10, false));

        assertEquals(1, results.size());
    }

    @Test
    void shouldPreferVerificationCodeBusinessCodeOverControllerFallback() throws IOException {
        write("src/main/java/com/example/controller/UserController.java", """
                package com.example.controller;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                    public String login() {
                        return "ok";
                    }
                }
                """);
        write("src/main/java/com/example/service/UserService.java", """
                package com.example.service;

                public class UserService {
                    public String sendCode(String phone) {
                        String code = RandomUtil.randomNumbers(5);
                        return code;
                    }
                }
                """);
        write("src/main/java/com/example/util/RandomUtil.java", """
                package com.example.util;

                public class RandomUtil {
                    public static String randomNumbers(int length) {
                        return "0".repeat(length);
                    }
                }
                """);

        List<CodeSearchResult> results = facade().search(request("修复验证码不为6位的问题，生成的验证码当前为5位，我希望修复为6位", 10, false));

        assertFalse(results.isEmpty());
        assertEquals("src/main/java/com/example/service/UserService.java", results.get(0).getFilePath());
        assertTrue(results.get(0).getContentWithLineNumbers().contains("randomNumbers(5)"));
        assertTrue(results.stream().noneMatch(result -> result.getFilePath().contains("controller/UserController.java")));
    }

    @Test
    void shouldIgnoreSingleQueryFailureAndContinueSearching() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                @RestController
                public class UserController {
                }
                """);

        List<CodeSearchResult> results = facade().search(request("UserController [", 10, true));

        assertFalse(results.isEmpty());
    }

    private GrepCodeSearchFacade facade() {
        CodeSearchProperties properties = new CodeSearchProperties();
        properties.setUseRipgrep(false);
        GrepSearchService grepSearchService = new GrepSearchService(
                properties,
                new RipgrepCommandBuilder(),
                new RipgrepResultParser()
        );
        return new GrepCodeSearchFacade(
                new SearchQueryPlanner(),
                grepSearchService,
                new CodeReadService(),
                properties
        );
    }

    private SearchRequest request(String issueText, int maxResults, boolean regexEnabled) {
        SearchRequest request = new SearchRequest();
        request.setRepoPath(tempDir.toString());
        request.setIssueText(issueText);
        request.setQuery(issueText);
        request.setMaxResults(maxResults);
        request.setContextBeforeLines(2);
        request.setContextAfterLines(2);
        request.setGlobPatterns(List.of("**/*.java"));
        request.setRegexEnabled(regexEnabled);
        return request;
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
