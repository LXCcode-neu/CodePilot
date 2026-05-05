package com.codepliot.service.index.lucene.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * 面向代码的 Lucene Analyzer，用于拆分驼峰、下划线和数字混合标识符。
 *
 * <p>例如 {@code getUserById} 会被拆分为 {@code get}、{@code user}、{@code by}、{@code id}，
 * 同时复用 Lucene 内置 offset 计算，避免自定义分词器导致的位置信息异常。
 */
public final class CodeAnalyzer extends Analyzer {

    private static final int WORD_DELIMITER_FLAGS =
            WordDelimiterGraphFilter.GENERATE_WORD_PARTS
                    | WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS
                    | WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE
                    | WordDelimiterGraphFilter.SPLIT_ON_NUMERICS
                    | WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE;

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new WordDelimiterGraphFilter(tokenizer, WORD_DELIMITER_FLAGS, null);
        tokenStream = new LowerCaseFilter(tokenStream);
        return new TokenStreamComponents(tokenizer, tokenStream);
    }
}
