package com.codepliot.index.lucene;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.git.service.GitWorkspaceService;
import com.codepliot.index.entity.CodeSymbol;
import com.codepliot.index.mapper.CodeSymbolMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lucene 本地索引构建服务。
 * 从 code_symbol 表读取指定项目的多语言符号数据，并统一重建为本地 Lucene 索引。
 */
@Service
public class LuceneCodeIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneCodeIndexService.class);

    private final CodeSymbolMapper codeSymbolMapper;
    private final GitWorkspaceService gitWorkspaceService;

    public LuceneCodeIndexService(CodeSymbolMapper codeSymbolMapper, GitWorkspaceService gitWorkspaceService) {
        this.codeSymbolMapper = codeSymbolMapper;
        this.gitWorkspaceService = gitWorkspaceService;
    }

    /**
     * 重建某个项目的 Lucene 本地索引。
     * 索引目录固定为 workspace/{projectId}/lucene-index，重建前会清空旧索引。
     */
    public int rebuildProjectIndex(Long projectId) {
        validateProjectId(projectId);
        List<CodeSymbol> symbols = loadProjectSymbols(projectId);
        Path indexPath = prepareIndexDirectory(projectId);

        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        int indexDocCount = 0;
        try (Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, config)) {
            for (CodeSymbol symbol : symbols) {
                writer.addDocument(toDocument(symbol));
                indexDocCount++;
            }
            writer.commit();
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to rebuild Lucene index for project " + projectId + ": " + buildErrorMessage(exception)
            );
        }

        log.info("Lucene index rebuilt for project {}, documents={}, path={}", projectId, indexDocCount, indexPath);
        return indexDocCount;
    }

    private List<CodeSymbol> loadProjectSymbols(Long projectId) {
        return codeSymbolMapper.selectList(new LambdaQueryWrapper<CodeSymbol>()
                .eq(CodeSymbol::getProjectId, projectId)
                .orderByAsc(CodeSymbol::getFileId)
                .orderByAsc(CodeSymbol::getStartLine)
                .orderByAsc(CodeSymbol::getId));
    }

    private Path prepareIndexDirectory(Long projectId) {
        gitWorkspaceService.ensureProjectWorkspace(projectId);
        Path indexPath = gitWorkspaceService.getLuceneIndexPath(projectId);
        deleteDirectoryIfExists(indexPath);
        try {
            Files.createDirectories(indexPath);
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to create Lucene index directory: " + indexPath
            );
        }
        return indexPath;
    }

    private void deleteDirectoryIfExists(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to clear old Lucene index directory: " + directory
            );
        }
    }

    private Document toDocument(CodeSymbol symbol) {
        Document document = new Document();
        addKeywordField(document, "projectId", asString(symbol.getProjectId()));
        addKeywordField(document, "filePath", symbol.getFilePath());
        addSearchOnlyTextField(document, "filePath", symbol.getFilePath());
        addKeywordField(document, "language", symbol.getLanguage());
        addKeywordField(document, "symbolType", symbol.getSymbolType());
        addTextField(document, "symbolName", symbol.getSymbolName());
        addTextField(document, "parentSymbol", symbol.getParentSymbol());
        addTextField(document, "signature", symbol.getSignature());
        addTextField(document, "annotations", symbol.getAnnotations());
        addTextField(document, "routePath", symbol.getRoutePath());
        addTextField(document, "importText", symbol.getImportText());
        addTextField(document, "content", symbol.getContent());
        addKeywordField(document, "startLine", asString(symbol.getStartLine()));
        addKeywordField(document, "endLine", asString(symbol.getEndLine()));
        return document;
    }

    private void addKeywordField(Document document, String fieldName, String value) {
        document.add(new StringField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.YES));
    }

    private void addTextField(Document document, String fieldName, String value) {
        document.add(new TextField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.YES));
    }

    private void addSearchOnlyTextField(Document document, String fieldName, String value) {
        document.add(new TextField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.NO));
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId must be greater than 0");
        }
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
