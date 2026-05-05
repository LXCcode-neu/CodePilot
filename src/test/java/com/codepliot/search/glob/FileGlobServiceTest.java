package com.codepliot.search.glob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.search.config.CodeSearchProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileGlobServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFindControllerAndApplicationYaml() throws IOException {
        write("src/main/java/com/example/UserController.java", "class UserController {}\n");
        write("application.yml", "server:\n  port: 8080\n");

        FileGlobService service = new FileGlobService(new CodeSearchProperties());

        assertEquals(
                List.of("src/main/java/com/example/UserController.java"),
                service.findFiles(tempDir.toString(), "**/*Controller.java")
        );
        assertEquals(List.of("application.yml"), service.findFiles(tempDir.toString(), "application.yml"));
    }

    @Test
    void shouldExcludeGeneratedAndDependencyDirectories() throws IOException {
        write("src/main/java/com/example/UserController.java", "class UserController {}\n");
        write("target/generated/GeneratedController.java", "class GeneratedController {}\n");
        write("node_modules/pkg/NodeController.java", "class NodeController {}\n");
        write(".git/hooks/GitController.java", "class GitController {}\n");

        FileGlobService service = new FileGlobService(new CodeSearchProperties());

        List<String> files = service.findFiles(tempDir.toString(), "**/*Controller.java");

        assertEquals(List.of("src/main/java/com/example/UserController.java"), files);
    }

    @Test
    void shouldNotMatchTraversalPatternsOutsideRepository() throws IOException {
        write("src/main/java/com/example/UserController.java", "class UserController {}\n");
        Path outsideFile = tempDir.getParent().resolve("OutsideController.java");
        Files.writeString(outsideFile, "class OutsideController {}\n");

        FileGlobService service = new FileGlobService(new CodeSearchProperties());

        assertTrue(service.findFiles(tempDir.toString(), "../**/*Controller.java").isEmpty());
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent() == null ? tempDir : file.getParent());
        Files.writeString(file, content);
    }
}
