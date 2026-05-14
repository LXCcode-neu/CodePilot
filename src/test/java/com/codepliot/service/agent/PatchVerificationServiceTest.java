package com.codepliot.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.codepliot.config.PatchVerificationProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PatchVerificationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void detectVerificationCommandsForSupportedRepositoryTypes() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project />");
        Files.writeString(tempDir.resolve("go.mod"), "module example.com/demo\n");
        Files.writeString(tempDir.resolve("pyproject.toml"), "[project]\nname = \"demo\"\n");
        Files.writeString(tempDir.resolve("package.json"), """
                {
                  "scripts": {
                    "build": "vite build"
                  }
                }
                """);
        Files.writeString(tempDir.resolve("package-lock.json"), "{}");

        PatchVerificationService service = new PatchVerificationService(new ObjectMapper(), new PatchVerificationProperties(), null, null);
        List<PatchVerificationService.VerificationCommand> commands = service.detectVerificationCommands(tempDir);

        assertEquals(5, commands.size());
        assertTrue(commands.stream().anyMatch(command -> "maven test".equals(command.name())));
        assertTrue(commands.stream().anyMatch(command -> "go test".equals(command.name())));
        assertTrue(commands.stream().anyMatch(command -> "python compileall".equals(command.name())));
        assertTrue(commands.stream().anyMatch(command -> "npm ci".equals(command.name())));
        assertTrue(commands.stream().anyMatch(command -> "npm build".equals(command.name())));
    }

    @Test
    void detectVerificationCommandsSkipsPackageJsonWithoutBuildScript() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {
                  "scripts": {
                    "test": "vitest"
                  }
                }
                """);

        PatchVerificationService service = new PatchVerificationService(new ObjectMapper(), new PatchVerificationProperties(), null, null);
        List<PatchVerificationService.VerificationCommand> commands = service.detectVerificationCommands(tempDir);

        assertTrue(commands.isEmpty());
    }
}
