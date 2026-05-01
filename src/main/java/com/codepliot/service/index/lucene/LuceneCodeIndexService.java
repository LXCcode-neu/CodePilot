package com.codepliot.service.index.lucene;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.CodeSymbol;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.repository.CodeSymbolMapper;
import com.codepliot.service.git.GitWorkspaceService;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import com.codepliot.service.index.lucene.analyzer.CodeAnalyzer;
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
 * Lucene 索引构建服务。
 *
 * <p>负责把项目下的 code_symbol 统一写入本地 Lucene 索引目录，供后续检索流程使用。
 */
@Service
public class LuceneCodeIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneCodeIndexService.class);

    private final CodeSymbolMapper codeSymbolMapper;
    private final GitWorkspaceService gitWorkspaceService;

    /**
     * 创建 Lucene 索引构建服务。
     */
    public LuceneCodeIndexService(CodeSymbolMapper codeSymbolMapper, GitWorkspaceService gitWorkspaceService) {
        this.codeSymbolMapper = codeSymbolMapper;
        this.gitWorkspaceService = gitWorkspaceService;
    }

    /**
     * 重建指定项目的 Lucene 索引。
     *
     * <p>每次重建都会先清空旧目录，再基于数据库里的最新 code_symbol 全量写入。
     */
    public int rebuildProjectIndex(Long projectId) {
        validateProjectId(projectId);
        List<CodeSymbol> symbols = loadProjectSymbols(projectId);
        Path indexPath = prepareIndexDirectory(projectId);

        IndexWriterConfig config = new IndexWriterConfig(new CodeAnalyzer());
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

    /**
     * 按稳定顺序加载项目下的全部符号。
     */
    private List<CodeSymbol> loadProjectSymbols(Long projectId) {
        return codeSymbolMapper.selectList(new LambdaQueryWrapper<CodeSymbol>()
                .eq(CodeSymbol::getProjectId, projectId)
                .orderByAsc(CodeSymbol::getFileId)
                .orderByAsc(CodeSymbol::getStartLine)
                .orderByAsc(CodeSymbol::getId));
    }

    /**
     * 准备索引目录。
     *
     * <p>目录不存在时自动创建，存在时会先删除旧索引文件。
     */
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

    /**
     * 删除已有索引目录及其中的全部文件。
     */
    private void deleteDirectoryIfExists(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                /**
                 * 先删除目录中的文件。
                 */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                /**
                 * 文件删除完成后再回收空目录。
                 */
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

    /**
     * 把单个 code_symbol 转换为 Lucene Document。
     */
    private Document toDocument(CodeSymbol symbol) {
        Document document = new Document();
        addKeywordField(document, "projectId", asString(symbol.getProjectId()));
        addKeywordField(document, "filePath", symbol.getFilePath());

        // filePath 保留一个仅用于分词检索的字段，避免和精确存储字段产生类型冲突。
        addSearchOnlyTextField(document, "filePathText", symbol.getFilePath());

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

    /**
     * 添加精确匹配字段，同时把原值保存在索引中。
     */
    private void addKeywordField(Document document, String fieldName, String value) {
        document.add(new StringField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.YES));
    }

    /**
     * 添加可分词检索且需要回显的文本字段。
     */
    private void addTextField(Document document, String fieldName, String value) {
        document.add(new TextField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.YES));
    }

    /**
     * 添加仅用于检索、不回写原值的文本字段。
     */
    private void addSearchOnlyTextField(Document document, String fieldName, String value) {
        document.add(new TextField(fieldName, normalize(value), org.apache.lucene.document.Field.Store.NO));
    }

    /**
     * 统一处理空字符串，避免 Lucene 字段写入空指针。
     */
    private String normalize(String value) {
        return value == null ? "" : value;
    }

    /**
     * 把任意对象安全转换成字符串。
     */
    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 校验项目 ID 是否合法。
     */
    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId must be greater than 0");
        }
    }

    /**
     * 提取适合展示的异常信息。
     */
    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
