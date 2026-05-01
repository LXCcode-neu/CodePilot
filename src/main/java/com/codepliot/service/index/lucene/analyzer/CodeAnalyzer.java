package com.codepliot.service.index.lucene.analyzer;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * 面向代码的 Lucene 分析器，在分词时拆分驼峰命名和下划线。
 *
 * <p>例如 {@code getUserById} 会被拆分为 {@code get}、{@code user}、{@code by}、{@code id}
 * 四个小写 token，使搜索 "user" 时能匹配到该符号。
 */
public final class CodeAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new CodeTokenizer();
        TokenFilter filter = new LowerCaseFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
    }

    /**
     * 代码感知的分词器，按非字母数字字符拆分，同时在驼峰边界处断词。
     */
    static final class CodeTokenizer extends Tokenizer {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

        private int charCount;
        private int pendingTokenStart = -1;

        @Override
        public boolean incrementToken() throws IOException {
            clearAttributes();

            int c;
            boolean inToken = false;
            int uppercaseCount = 0;
            boolean prevIsDigit = false;
            termAtt.setEmpty();

            while ((c = readChar()) != -1) {
                char ch = (char) c;
                boolean isTokenChar = Character.isLetterOrDigit(ch);

                if (!isTokenChar) {
                    if (inToken && termAtt.length() > 0) {
                        return true;
                    }
                    inToken = false;
                    uppercaseCount = 0;
                    prevIsDigit = false;
                    continue;
                }

                if (!inToken) {
                    inToken = true;
                    pendingTokenStart = charCount - 1;
                    uppercaseCount = 0;
                    prevIsDigit = false;
                    termAtt.setEmpty();
                }

                boolean isUpper = Character.isUpperCase(ch);
                boolean isDigit = Character.isDigit(ch);

                if (isUpper) {
                    if (prevIsDigit || (uppercaseCount == 0 && termAtt.length() > 0)) {
                        // 数字→大写 或 小写→大写 边界，先输出已积累的 token
                        emitToken();
                        termAtt.setEmpty();
                        pendingTokenStart = charCount - 1;
                    }
                    uppercaseCount++;
                } else {
                    if (uppercaseCount > 1) {
                        // 连续大写后跟小写（如 "HTMLP" 后的 "arser"），把最后一个大写留给当前
                        char last = termAtt.charAt(termAtt.length() - 1);
                        emitTokenExceptLast();
                        termAtt.setLength(0);
                        termAtt.append(last);
                        pendingTokenStart = charCount - 2;
                    }
                    uppercaseCount = 0;
                }

                if (isDigit && !prevIsDigit && termAtt.length() > 0) {
                    emitToken();
                    termAtt.setEmpty();
                    pendingTokenStart = charCount - 1;
                }

                termAtt.append(ch);
                prevIsDigit = isDigit;
            }

            if (termAtt.length() > 0) {
                return true;
            }
            return false;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            charCount = 0;
            pendingTokenStart = -1;
        }

        @Override
        public void end() throws IOException {
            super.end();
            offsetAtt.setOffset(charCount, charCount);
        }

        private int readChar() throws IOException {
            int c = input.read();
            if (c != -1) {
                charCount++;
            }
            return c;
        }

        private void emitToken() {
            offsetAtt.setOffset(pendingTokenStart, pendingTokenStart + termAtt.length());
        }

        private void emitTokenExceptLast() {
            int len = termAtt.length();
            if (len <= 1) {
                return;
            }
            // 临时缩短长度来 emit，保留最后一个字符在外部处理
            termAtt.setLength(len - 1);
            emitToken();
        }
    }

    /**
     * 将 token 转为小写的过滤器。
     */
    static final class LowerCaseFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        LowerCaseFilter(TokenStream input) {
            super(input);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (!input.incrementToken()) {
                return false;
            }
            char[] buffer = termAtt.buffer();
            int length = termAtt.length();
            for (int i = 0; i < length; i++) {
                buffer[i] = Character.toLowerCase(buffer[i]);
            }
            return true;
        }
    }
}
