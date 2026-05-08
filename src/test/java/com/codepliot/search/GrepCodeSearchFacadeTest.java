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
    void shouldRunOneShotGrepAndReadSnippet() throws IOException {
        write("src/main/java/com/example/UserController.java", """
                package com.example;

                public class UserController {
                    public void confirmPatch() {
                    }
                }
                """);

        List<CodeSearchResult> results = facade().search(request("confirmPatch"));

        assertFalse(results.isEmpty());
        assertEquals("src/main/java/com/example/UserController.java", results.get(0).getFilePath());
        assertTrue(results.get(0).getContentWithLineNumbers().contains("confirmPatch"));
    }

    private GrepCodeSearchFacade facade() {
        CodeSearchProperties properties = new CodeSearchProperties();
        properties.setUseRipgrep(false);
        GrepSearchService grepSearchService = new GrepSearchService(
                properties,
                new RipgrepCommandBuilder(),
                new RipgrepResultParser()
        );
        return new GrepCodeSearchFacade(grepSearchService, new CodeReadService(), properties);
    }

    private SearchRequest request(String query) {
        SearchRequest request = new SearchRequest();
        request.setRepoPath(tempDir.toString());
        request.setQuery(query);
        request.setMaxResults(10);
        request.setContextBeforeLines(2);
        request.setContextAfterLines(2);
        request.setGlobPatterns(List.of("**/*.java"));
        return request;
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
