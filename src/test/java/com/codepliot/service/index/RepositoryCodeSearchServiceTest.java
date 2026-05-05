package com.codepliot.service.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.model.RetrievedCodeChunk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryCodeSearchServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFindVerificationCodeGenerationFromChineseIssueText() throws IOException {
        Path sourceFile = tempDir.resolve("src/main/java/com/hmdp/service/impl/UserServiceImpl.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, """
                package com.hmdp.service.impl;

                public class UserServiceImpl {
                    public Result sendCode(String phone, HttpSession session) {
                        if (RegexUtils.isPhoneInvalid(phone)) {
                            return Result.fail("手机号格式错误");
                        }
                        String code = RandomUtil.randomNumbers(5);
                        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
                        log.debug("发送短信验证码成功，验证码：{}", code);
                        return Result.ok();
                    }
                }
                """);

        RepositoryCodeSearchService service = new RepositoryCodeSearchService(new FileLanguageMapper());
        List<RetrievedCodeChunk> chunks = service.search(tempDir.toString(), "把验证码从5位改成6位", 10);

        assertFalse(chunks.isEmpty());
        RetrievedCodeChunk first = chunks.get(0);
        assertEquals("src/main/java/com/hmdp/service/impl/UserServiceImpl.java", first.filePath());
        assertTrue(first.content().contains("RandomUtil.randomNumbers(5)"));
        assertTrue(first.startLine() != null && first.startLine() <= 7);
        assertTrue(first.endLine() != null && first.endLine() >= 7);
    }
}
