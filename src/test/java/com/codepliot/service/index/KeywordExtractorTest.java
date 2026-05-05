package com.codepliot.service.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordExtractorTest {

    @Test
    void shouldPreserveSingleDigitNumbersAndExpandChineseHints() {
        List<String> keywords = KeywordExtractor.extractKeywords("把验证码从5位改成6位");

        assertTrue(keywords.contains("5"));
        assertTrue(keywords.contains("6"));
        assertTrue(keywords.contains("code"));
        assertTrue(keywords.contains("verify"));
        assertTrue(keywords.contains("captcha"));
        assertTrue(keywords.contains("digits"));
    }

    @Test
    void shouldSplitCamelCaseIdentifiers() {
        assertEquals(List.of("send", "code", "random", "5"), KeywordExtractor.extractKeywords("sendCodeRandom5"));
    }
}
