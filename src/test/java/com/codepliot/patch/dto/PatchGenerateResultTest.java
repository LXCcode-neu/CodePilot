package com.codepliot.patch.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PatchGenerateResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseValidJsonObject() {
        String rawOutput = """
                {
                  "analysis": "a",
                  "solution": "b",
                  "patch": "",
                  "risk": "c"
                }
                """;

        PatchGenerateResult result = PatchGenerateResult.fromRawOutput(objectMapper, rawOutput);

        assertEquals("a", result.analysis());
        assertEquals("b", result.solution());
        assertEquals("", result.patch());
        assertEquals("c", result.risk());
    }

    @Test
    void shouldRejectMissingKeys() {
        String rawOutput = """
                {
                  "analysis": "a",
                  "solution": "b",
                  "patch": ""
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> PatchGenerateResult.fromRawOutput(objectMapper, rawOutput));
    }

    @Test
    void shouldRejectExtraKeys() {
        String rawOutput = """
                {
                  "analysis": "a",
                  "solution": "b",
                  "patch": "",
                  "risk": "c",
                  "extra": "d"
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> PatchGenerateResult.fromRawOutput(objectMapper, rawOutput));
    }

    @Test
    void shouldRejectNonStringFieldValues() {
        String rawOutput = """
                {
                  "analysis": "a",
                  "solution": "b",
                  "patch": "",
                  "risk": 123
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> PatchGenerateResult.fromRawOutput(objectMapper, rawOutput));
    }
}
