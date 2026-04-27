package com.codepliot.service;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.service.GitWorkspaceService;
import com.codepliot.model.RetrievedCodeChunk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * LuceneCodeSearchService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class LuceneCodeSearchService {

    private static final Logger log = LoggerFactory.getLogger(LuceneCodeSearchService.class);

    private static final String[] SEARCH_FIELDS = {
            "symbolName", "parentSymbol", "signature", "annotations",
            "routePath", "importText", "content", "filePathText"
    };

    private static final Map<String, Float> FIELD_BOOSTS = Map.of(
            "symbolName", 3.0f,
            "filePathText", 2.2f,
            "routePath", 2.0f,
            "parentSymbol", 1.7f,
            "signature", 1.4f,
            "annotations", 1.2f,
            "importText", 1.1f,
            "content", 1.0f
    );

    private final GitWorkspaceService gitWorkspaceService;
/**
 * 创建 LuceneCodeSearchService 实例。
 */
public LuceneCodeSearchService(GitWorkspaceService gitWorkspaceService) {
        this.gitWorkspaceService = gitWorkspaceService;
    }
/**
 * 执行 search 相关逻辑。
 */
public List<RetrievedCodeChunk> search(Long projectId, String query, int topK) {
        validateProjectId(projectId);
        if (topK <= 0 || query == null || query.isBlank()) {
            return List.of();
        }

        Path indexPath = gitWorkspaceService.getLuceneIndexPath(projectId);
        if (!Files.isDirectory(indexPath)) {
            log.info("Lucene index directory does not exist for project {}, path={}", projectId, indexPath);
            return List.of();
        }

        try (Directory directory = FSDirectory.open(indexPath)) {
            if (!DirectoryReader.indexExists(directory)) {
                return List.of();
            }

            Query luceneQuery = buildQuery(query);
            int fetchSize = Math.max(topK * 5, topK);
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                ScoreDoc[] scoreDocs = searcher.search(luceneQuery, fetchSize).scoreDocs;
                List<RetrievedCodeChunk> chunks = new ArrayList<>(scoreDocs.length);
                for (ScoreDoc scoreDoc : scoreDocs) {
                    Document document = searcher.storedFields().document(scoreDoc.doc);
                    chunks.add(toRetrievedCodeChunk(document, scoreDoc.score));
                }
                return chunks;
            }
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to search Lucene code index for project " + projectId + ": " + buildErrorMessage(exception)
            );
        }
    }
/**
 * 构建Query相关逻辑。
 */
private Query buildQuery(String queryText) throws Exception {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(SEARCH_FIELDS, new StandardAnalyzer(), FIELD_BOOSTS);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        parser.setAllowLeadingWildcard(false);
        return parser.parse(buildEscapedKeywordQuery(queryText));
    }
/**
 * 构建Escaped Keyword Query相关逻辑。
 */
private String buildEscapedKeywordQuery(String queryText) {
        List<String> keywords = extractKeywords(queryText);
        if (keywords.isEmpty()) {
            return QueryParser.escape(queryText.trim());
        }
        return keywords.stream()
                .map(QueryParser::escape)
                .reduce((left, right) -> left + " " + right)
                .orElse(QueryParser.escape(queryText.trim()));
    }
/**
 * 提取Keywords相关逻辑。
 */
private List<String> extractKeywords(String queryText) {
        String normalized = queryText == null ? "" : queryText.toLowerCase();
        String[] tokens = normalized.split("[^a-z0-9_./-]+");
        Map<String, Boolean> ordered = new LinkedHashMap<>();
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.length() <= 1) {
                continue;
            }
            ordered.put(token, Boolean.TRUE);
        }
        return new ArrayList<>(ordered.keySet());
    }
/**
 * 转换为Retrieved Code Chunk相关逻辑。
 */
private RetrievedCodeChunk toRetrievedCodeChunk(Document document, float score) {
        double rawScore = score;
        return new RetrievedCodeChunk(
                document.get("filePath"),
                document.get("language"),
                document.get("symbolType"),
                document.get("symbolName"),
                document.get("parentSymbol"),
                document.get("routePath"),
                parseInteger(document.get("startLine")),
                parseInteger(document.get("endLine")),
                document.get("content"),
                rawScore,
                rawScore
        );
    }
/**
 * 解析Integer相关逻辑。
 */
private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
/**
 * 校验Project Id相关逻辑。
 */
private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId must be greater than 0");
        }
    }
/**
 * 构建Error Message相关逻辑。
 */
private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}

