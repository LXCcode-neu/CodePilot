package com.codepliot.search.grep;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codepliot.search.dto.GrepMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class RipgrepResultParserTest {

    private final RipgrepResultParser parser = new RipgrepResultParser();

    @Test
    void shouldParseNormalRipgrepOutput() {
        List<GrepMatch> matches = parser.parse("src/main/java/UserController.java:12:5:@RestController", "@RestController", 10);

        assertEquals(1, matches.size());
        assertEquals("src/main/java/UserController.java", matches.get(0).getFilePath());
        assertEquals(12, matches.get(0).getLineNumber());
        assertEquals(5, matches.get(0).getColumn());
        assertEquals("@RestController", matches.get(0).getLineText());
    }

    @Test
    void shouldParseWindowsPath() {
        GrepMatch match = parser.parseLine("C:\\repo\\src\\UserController.java:20:9:public class UserController {", "UserController");

        assertEquals("C:/repo/src/UserController.java", match.getFilePath());
        assertEquals(20, match.getLineNumber());
        assertEquals(9, match.getColumn());
    }

    @Test
    void shouldKeepColonsInsideLineText() {
        GrepMatch match = parser.parseLine("src/App.java:7:18:String value = \"http://localhost:8080\";", "localhost");

        assertEquals("src/App.java", match.getFilePath());
        assertEquals(7, match.getLineNumber());
        assertEquals(18, match.getColumn());
        assertEquals("String value = \"http://localhost:8080\";", match.getLineText());
    }
}
