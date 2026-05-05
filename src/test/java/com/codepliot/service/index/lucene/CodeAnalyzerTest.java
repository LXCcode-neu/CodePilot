package com.codepliot.service.index.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.service.index.lucene.analyzer.CodeAnalyzer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.jupiter.api.Test;

class CodeAnalyzerTest {

    @Test
    void shouldSplitCamelCaseSnakeCaseAndNumbersWithMonotonicOffsets() throws IOException {
        List<TokenData> tokens = analyze("content", "getUserById snake_case99 parseHTTP2Response");

        assertEquals(
                List.of("get", "user", "by", "id", "snake", "case", "99", "parse", "http", "2", "response"),
                tokens.stream().map(TokenData::term).toList()
        );

        int lastStartOffset = -1;
        for (TokenData token : tokens) {
            assertTrue(token.startOffset() >= 0);
            assertTrue(token.endOffset() >= token.startOffset());
            assertTrue(token.startOffset() >= lastStartOffset);
            lastStartOffset = token.startOffset();
        }
    }

    private List<TokenData> analyze(String fieldName, String value) throws IOException {
        List<TokenData> tokens = new ArrayList<>();
        try (CodeAnalyzer analyzer = new CodeAnalyzer();
             TokenStream tokenStream = analyzer.tokenStream(fieldName, value)) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(new TokenData(
                        termAttribute.toString(),
                        offsetAttribute.startOffset(),
                        offsetAttribute.endOffset()
                ));
            }
            tokenStream.end();
        }
        return tokens;
    }

    private record TokenData(String term, int startOffset, int endOffset) {
    }
}
