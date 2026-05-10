package com.codepliot.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PatchTextNormalizerTest {

    @TempDir
    Path tempDir;

    private final PatchTextNormalizer normalizer = new PatchTextNormalizer();

    @Test
    void rewritesIncorrectHunkCounts() throws Exception {
        String patch = malformedPatch();

        String normalized = normalizer.normalize(patch);

        assertTrue(normalized.contains("@@ -1,5 +1,5 @@"));
    }

    @Test
    void normalizedPatchCanPassGitApplyCheck() throws Exception {
        Path file = tempDir.resolve("src/main/java/com/hmdp/service/impl/UserServiceImpl.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n",
                "}",
                "//3.generate code",
                "String code = RandomUtil.randomNumbers(5);",
                "//4.save code",
                "stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);",
                ""
        ));

        Path patchFile = tempDir.resolve("generated.patch");
        Files.writeString(patchFile, normalizer.normalize(malformedPatch()));

        Process process = new ProcessBuilder("git", "apply", "--check", patchFile.toString())
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);

        assertTrue(finished);
        assertEquals(0, process.exitValue());
    }

    private String malformedPatch() {
        return String.join("\n",
                "--- a/src/main/java/com/hmdp/service/impl/UserServiceImpl.java",
                "+++ b/src/main/java/com/hmdp/service/impl/UserServiceImpl.java",
                "@@ -1,7 +1,7 @@",
                " }",
                " //3.generate code",
                "-String code = RandomUtil.randomNumbers(5);",
                "+String code = RandomUtil.randomNumbers(6);",
                " //4.save code",
                " stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);",
                ""
        );
    }
}
