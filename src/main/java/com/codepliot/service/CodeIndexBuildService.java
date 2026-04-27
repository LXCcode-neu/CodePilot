package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.CodeFile;
import com.codepliot.entity.CodeSymbol;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.CodeIndexBuildResult;
import com.codepliot.model.CodeSymbolType;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.ParsedCodeFile;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.repository.CodeFileMapper;
import com.codepliot.repository.CodeSymbolMapper;
import com.codepliot.repository.ProjectRepoMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 代码索引构建服务。
 *
 * <p>负责扫描仓库文件、调用 Tree-sitter 解析、提取符号，并把结果写入 code_file 与 code_symbol。
 */
@Service
public class CodeIndexBuildService {

    private static final Logger log = LoggerFactory.getLogger(CodeIndexBuildService.class);

    private static final String PARSE_STATUS_PARSED = "PARSED";

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
            ".git", "target", "node_modules", "dist", "build", ".idea", ".vscode", "out", "coverage"
    );

    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.$]+)\\s*;");
    private static final Pattern GO_PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*$");

    private final ProjectRepoMapper projectRepoMapper;
    private final LanguageDetector languageDetector;
    private final TreeSitterParserService treeSitterParserService;
    private final LanguageSymbolExtractorRegistry extractorRegistry;
    private final PlainTextFallbackExtractor plainTextFallbackExtractor;
    private final CodeFileMapper codeFileMapper;
    private final CodeSymbolMapper codeSymbolMapper;

    /**
     * 创建代码索引构建服务。
     */
    public CodeIndexBuildService(ProjectRepoMapper projectRepoMapper,
                                 LanguageDetector languageDetector,
                                 TreeSitterParserService treeSitterParserService,
                                 LanguageSymbolExtractorRegistry extractorRegistry,
                                 PlainTextFallbackExtractor plainTextFallbackExtractor,
                                 CodeFileMapper codeFileMapper,
                                 CodeSymbolMapper codeSymbolMapper) {
        this.projectRepoMapper = projectRepoMapper;
        this.languageDetector = languageDetector;
        this.treeSitterParserService = treeSitterParserService;
        this.extractorRegistry = extractorRegistry;
        this.plainTextFallbackExtractor = plainTextFallbackExtractor;
        this.codeFileMapper = codeFileMapper;
        this.codeSymbolMapper = codeSymbolMapper;
    }

    /**
     * 重建指定项目的数据库索引。
     *
     * <p>该方法会清空旧的 code_file 与 code_symbol，再按当前仓库内容重新入库。
     */
    @Transactional
    public CodeIndexBuildResult build(Long projectId) {
        ProjectRepo projectRepo = requireProjectRepo(projectId);
        Path repoRoot = resolveRepositoryRoot(projectRepo);

        // 先清空旧索引，保证数据库中的文件和符号快照与当前仓库一致。
        deleteExistingIndex(projectId);

        List<Path> files = scanRepositoryFiles(repoRoot);
        Map<LanguageType, Integer> languageStats = new EnumMap<>(LanguageType.class);
        int symbolCount = 0;
        int warningCount = 0;

        for (Path file : files) {
            FileBuildOutcome outcome = buildSingleFile(projectId, repoRoot, file, languageStats);
            symbolCount += outcome.symbolCount();
            warningCount += outcome.warningCount();
        }

        return new CodeIndexBuildResult(files.size(), symbolCount, warningCount, new LinkedHashMap<>(languageStats));
    }

    /**
     * 校验项目是否存在。
     */
    private ProjectRepo requireProjectRepo(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Project id must not be null");
        }

        ProjectRepo projectRepo = projectRepoMapper.selectById(projectId);
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    /**
     * 解析并校验仓库根目录。
     */
    private Path resolveRepositoryRoot(ProjectRepo projectRepo) {
        String localPath = projectRepo.getLocalPath();
        if (localPath == null || localPath.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Project repository local path is empty");
        }

        Path repoRoot = Path.of(localPath).normalize();
        if (!Files.exists(repoRoot) || !Files.isDirectory(repoRoot)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Project repository local path does not exist: " + localPath);
        }
        return repoRoot;
    }

    /**
     * 删除项目已有的文件和符号索引数据。
     */
    private void deleteExistingIndex(Long projectId) {
        codeSymbolMapper.delete(new LambdaQueryWrapper<CodeSymbol>().eq(CodeSymbol::getProjectId, projectId));
        codeFileMapper.delete(new LambdaQueryWrapper<CodeFile>().eq(CodeFile::getProjectId, projectId));
    }

    /**
     * 扫描仓库中的全部候选文件。
     *
     * <p>会跳过 Git、构建产物和前端依赖目录，减少无意义的索引开销。
     */
    private List<Path> scanRepositoryFiles(Path repoRoot) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
                /**
                 * 进入目录前过滤不需要扫描的目录。
                 */
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path fileName = dir.getFileName();
                    if (fileName != null && SKIPPED_DIRECTORIES.contains(fileName.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                /**
                 * 收集普通文件，后续统一进入解析流程。
                 */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Failed to scan repository files: " + buildErrorMessage(exception));
        }
        return files;
    }

    /**
     * 为单个文件构建索引记录。
     *
     * <p>正常情况下走 Tree-sitter + 语言专属提取器；失败时回退到纯文本提取，确保流程不中断。
     */
    private FileBuildOutcome buildSingleFile(Long projectId,
                                             Path repoRoot,
                                             Path filePath,
                                             Map<LanguageType, Integer> languageStats) {
        LanguageType languageType = languageDetector.detect(filePath);
        languageStats.merge(languageType, 1, Integer::sum);

        try {
            TreeSitterParseResult parseResult = parseFile(repoRoot, filePath, languageType);
            SymbolExtractionOutcome extractionOutcome = extractSymbols(parseResult, languageType);
            ParsedCodeFile parsedCodeFile = buildParsedCodeFile(projectId, extractionOutcome.parseResult(), extractionOutcome.symbols());

            CodeFile codeFile = toCodeFileEntity(parsedCodeFile);
            codeFileMapper.insert(codeFile);

            int savedSymbolCount = saveSymbols(projectId, codeFile.getId(), extractionOutcome.symbols());
            return new FileBuildOutcome(savedSymbolCount, extractionOutcome.warningCount());
        } catch (Exception exception) {
            log.warn("Failed to build code index for file {}", filePath, exception);

            // 文件解析失败时仍然保留最基础的文件记录和纯文本符号，避免整次建索引中断。
            TreeSitterParseResult fallbackResult = buildFileReadFailureResult(repoRoot, filePath, languageType, exception);
            ParsedCodeFile parsedCodeFile = plainTextFallbackExtractor.buildParsedCodeFile(projectId, fallbackResult);
            CodeFile codeFile = toCodeFileEntity(parsedCodeFile);
            codeFileMapper.insert(codeFile);

            int savedSymbolCount = saveSymbols(projectId, codeFile.getId(), plainTextFallbackExtractor.extract(fallbackResult));
            return new FileBuildOutcome(savedSymbolCount, 1);
        }
    }

    /**
     * 解析单个文件。
     */
    private TreeSitterParseResult parseFile(Path repoRoot, Path filePath, LanguageType languageType) {
        if (languageType == LanguageType.UNKNOWN) {
            return buildUnknownLanguageResult(repoRoot, filePath);
        }

        TreeSitterParseResult parseResult = treeSitterParserService.parse(filePath, languageType);
        return new TreeSitterParseResult(
                parseResult.language(),
                parseResult.filePath(),
                toRelativePath(repoRoot, filePath),
                parseResult.sourceCode(),
                parseResult.success(),
                parseResult.errorMessage(),
                parseResult.astObject()
        );
    }

    /**
     * 根据语言和解析结果提取代码符号。
     */
    private SymbolExtractionOutcome extractSymbols(TreeSitterParseResult parseResult, LanguageType languageType) {
        if (languageType == LanguageType.UNKNOWN) {
            return new SymbolExtractionOutcome(parseResult, plainTextFallbackExtractor.extract(parseResult), 0);
        }

        if (!parseResult.success()) {
            return new SymbolExtractionOutcome(parseResult, plainTextFallbackExtractor.extract(parseResult), 1);
        }

        LanguageSymbolExtractor extractor = extractorRegistry.get(parseResult.language());
        if (extractor == plainTextFallbackExtractor) {
            TreeSitterParseResult fallbackResult = markAsFallback(parseResult, "No language extractor registered");
            return new SymbolExtractionOutcome(fallbackResult, plainTextFallbackExtractor.extract(fallbackResult), 1);
        }

        try {
            return new SymbolExtractionOutcome(parseResult, extractor.extract(parseResult), 0);
        } catch (Exception exception) {
            log.warn("Failed to extract symbols for file {}", parseResult.relativePath(), exception);
            TreeSitterParseResult fallbackResult = markAsFallback(
                    parseResult,
                    "Symbol extraction failed: " + buildErrorMessage(exception)
            );
            return new SymbolExtractionOutcome(fallbackResult, plainTextFallbackExtractor.extract(fallbackResult), 1);
        }
    }

    /**
     * 构建文件级索引模型。
     */
    private ParsedCodeFile buildParsedCodeFile(Long projectId,
                                               TreeSitterParseResult parseResult,
                                               List<ParsedCodeSymbol> symbols) {
        if (!parseResult.success()) {
            return plainTextFallbackExtractor.buildParsedCodeFile(projectId, parseResult);
        }

        String sourceCode = parseResult.sourceCode() == null ? "" : parseResult.sourceCode();
        return new ParsedCodeFile(
                projectId,
                normalizePath(parseResult.relativePath(), parseResult.filePath()),
                parseResult.language(),
                extractPackageName(parseResult),
                extractModuleName(parseResult.relativePath(), parseResult.filePath()),
                extractPrimaryClassName(symbols),
                sha256(sourceCode),
                (long) sourceCode.getBytes(StandardCharsets.UTF_8).length,
                PARSE_STATUS_PARSED,
                null
        );
    }

    /**
     * 保存符号记录并返回写入数量。
     */
    private int saveSymbols(Long projectId, Long fileId, List<ParsedCodeSymbol> symbols) {
        int savedCount = 0;
        for (ParsedCodeSymbol parsedCodeSymbol : symbols) {
            CodeSymbol codeSymbol = toCodeSymbolEntity(projectId, fileId, parsedCodeSymbol);
            codeSymbolMapper.insert(codeSymbol);
            savedCount++;
        }
        return savedCount;
    }

    /**
     * 把文件索引模型转换为数据库实体。
     */
    private CodeFile toCodeFileEntity(ParsedCodeFile parsedCodeFile) {
        CodeFile codeFile = new CodeFile();
        codeFile.setProjectId(parsedCodeFile.projectId());
        codeFile.setFilePath(parsedCodeFile.filePath());
        codeFile.setLanguage(parsedCodeFile.language() == null ? LanguageType.UNKNOWN.name() : parsedCodeFile.language().name());
        codeFile.setPackageName(parsedCodeFile.packageName());
        codeFile.setModuleName(parsedCodeFile.moduleName());
        codeFile.setClassName(parsedCodeFile.className());
        codeFile.setContentHash(parsedCodeFile.contentHash());
        codeFile.setSize(parsedCodeFile.size());
        codeFile.setParseStatus(parsedCodeFile.parseStatus());
        codeFile.setParseError(parsedCodeFile.parseError());
        return codeFile;
    }

    /**
     * 把符号模型转换为数据库实体。
     */
    private CodeSymbol toCodeSymbolEntity(Long projectId, Long fileId, ParsedCodeSymbol parsedCodeSymbol) {
        CodeSymbol codeSymbol = new CodeSymbol();
        codeSymbol.setProjectId(projectId);
        codeSymbol.setFileId(fileId);
        codeSymbol.setLanguage(parsedCodeSymbol.language() == null ? LanguageType.UNKNOWN.name() : parsedCodeSymbol.language().name());
        codeSymbol.setFilePath(parsedCodeSymbol.filePath());
        codeSymbol.setSymbolType(parsedCodeSymbol.symbolType() == null ? CodeSymbolType.UNKNOWN.name() : parsedCodeSymbol.symbolType().name());
        codeSymbol.setSymbolName(parsedCodeSymbol.symbolName());
        codeSymbol.setParentSymbol(parsedCodeSymbol.parentSymbol());
        codeSymbol.setSignature(parsedCodeSymbol.signature());
        codeSymbol.setAnnotations(parsedCodeSymbol.annotations());
        codeSymbol.setRoutePath(parsedCodeSymbol.routePath());
        codeSymbol.setImportText(parsedCodeSymbol.importText());
        codeSymbol.setStartLine(parsedCodeSymbol.startLine());
        codeSymbol.setEndLine(parsedCodeSymbol.endLine());
        codeSymbol.setContent(parsedCodeSymbol.content());
        return codeSymbol;
    }

    /**
     * 为未识别语言构建基础解析结果。
     */
    private TreeSitterParseResult buildUnknownLanguageResult(Path repoRoot, Path filePath) {
        try {
            String sourceCode = Files.readString(filePath, StandardCharsets.UTF_8);
            return new TreeSitterParseResult(
                    LanguageType.UNKNOWN,
                    filePath.toString(),
                    toRelativePath(repoRoot, filePath),
                    sourceCode,
                    false,
                    null,
                    null
            );
        } catch (IOException exception) {
            return buildFileReadFailureResult(repoRoot, filePath, LanguageType.UNKNOWN, exception);
        }
    }

    /**
     * 为文件读取失败场景构建回退结果。
     */
    private TreeSitterParseResult buildFileReadFailureResult(Path repoRoot,
                                                             Path filePath,
                                                             LanguageType languageType,
                                                             Exception exception) {
        return new TreeSitterParseResult(
                languageType,
                filePath.toString(),
                toRelativePath(repoRoot, filePath),
                null,
                false,
                "Failed to read source file: " + buildErrorMessage(exception),
                null
        );
    }

    /**
     * 把当前解析结果标记为回退状态。
     */
    private TreeSitterParseResult markAsFallback(TreeSitterParseResult parseResult, String errorMessage) {
        return new TreeSitterParseResult(
                parseResult.language(),
                parseResult.filePath(),
                parseResult.relativePath(),
                parseResult.sourceCode(),
                false,
                errorMessage,
                parseResult.astObject()
        );
    }

    /**
     * 计算仓库内相对路径。
     */
    private String toRelativePath(Path repoRoot, Path filePath) {
        return normalizePath(repoRoot.relativize(filePath).toString(), filePath.toString());
    }

    /**
     * 统一路径分隔符格式。
     */
    private String normalizePath(String relativePath, String fallbackPath) {
        String value = (relativePath == null || relativePath.isBlank()) ? fallbackPath : relativePath;
        return value == null ? "" : value.replace('\\', '/');
    }

    /**
     * 从相对路径中提取顶层模块名。
     */
    private String extractModuleName(String relativePath, String filePath) {
        String normalized = normalizePath(relativePath, filePath);
        int index = normalized.indexOf('/');
        if (index <= 0) {
            return null;
        }
        return normalized.substring(0, index);
    }

    /**
     * 根据语言规则提取包名。
     */
    private String extractPackageName(TreeSitterParseResult parseResult) {
        if (parseResult.sourceCode() == null || parseResult.language() == null) {
            return null;
        }

        Pattern pattern = switch (parseResult.language()) {
            case JAVA -> JAVA_PACKAGE_PATTERN;
            case GO -> GO_PACKAGE_PATTERN;
            default -> null;
        };
        if (pattern == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(parseResult.sourceCode());
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 从符号列表中选出主类名。
     */
    private String extractPrimaryClassName(List<ParsedCodeSymbol> symbols) {
        if (symbols == null) {
            return null;
        }
        for (ParsedCodeSymbol symbol : symbols) {
            if (symbol == null || symbol.symbolType() == null || symbol.symbolName() == null) {
                continue;
            }
            if (symbol.symbolType() == CodeSymbolType.CLASS || symbol.symbolType() == CodeSymbolType.INTERFACE) {
                return symbol.symbolName();
            }
        }
        return null;
    }

    /**
     * 计算源码内容的 SHA-256 摘要。
     */
    private String sha256(String sourceCode) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(sourceCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    /**
     * 提取适合展示的错误信息。
     */
    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * 单文件建索引结果。
     */
    private record SymbolExtractionOutcome(TreeSitterParseResult parseResult,
                                           List<ParsedCodeSymbol> symbols,
                                           int warningCount) {
    }

    /**
     * 单文件入库统计结果。
     */
    private record FileBuildOutcome(int symbolCount, int warningCount) {
    }
}
