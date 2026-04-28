package com.codepliot.service.index.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codepliot.config.WorkspaceProperties;
import com.codepliot.service.git.GitWorkspaceService;
import com.codepliot.entity.CodeSymbol;
import com.codepliot.repository.CodeSymbolMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LuceneCodeIndexServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRebuildLuceneIndexUnderProjectWorkspace() throws IOException {
        CodeSymbolMapper codeSymbolMapper = mock(CodeSymbolMapper.class);
        when(codeSymbolMapper.selectList(any())).thenReturn(List.of(
                createSymbol(42L, "src/main/java/App.java", "JAVA", "CLASS", "App", 1, 20),
                createSymbol(42L, "src/main/java/App.java", "JAVA", "METHOD", "run", 5, 12)
        ));

        WorkspaceProperties workspaceProperties = new WorkspaceProperties();
        workspaceProperties.setRoot(tempDir.toString());
        GitWorkspaceService gitWorkspaceService = new GitWorkspaceService(workspaceProperties);

        Path staleFile = gitWorkspaceService.getLuceneIndexPath(42L).resolve("stale.txt");
        Files.createDirectories(staleFile.getParent());
        Files.writeString(staleFile, "obsolete");

        LuceneCodeIndexService service = new LuceneCodeIndexService(codeSymbolMapper, gitWorkspaceService);
        int docCount = service.rebuildProjectIndex(42L);

        Path indexPath = gitWorkspaceService.getLuceneIndexPath(42L);
        assertEquals(2, docCount);
        assertTrue(Files.isDirectory(indexPath));
        assertFalse(Files.exists(staleFile));

        try (Directory directory = FSDirectory.open(indexPath);
             DirectoryReader reader = DirectoryReader.open(directory)) {
            assertEquals(2, reader.numDocs());
            assertEquals("42", reader.storedFields().document(0).get("projectId"));
            assertEquals("JAVA", reader.storedFields().document(0).get("language"));
            assertEquals("App", reader.storedFields().document(0).get("symbolName"));
            assertEquals("1", reader.storedFields().document(0).get("startLine"));
        }
    }

    private CodeSymbol createSymbol(Long projectId,
                                    String filePath,
                                    String language,
                                    String symbolType,
                                    String symbolName,
                                    Integer startLine,
                                    Integer endLine) {
        CodeSymbol symbol = new CodeSymbol();
        symbol.setProjectId(projectId);
        symbol.setFilePath(filePath);
        symbol.setLanguage(language);
        symbol.setSymbolType(symbolType);
        symbol.setSymbolName(symbolName);
        symbol.setParentSymbol("App");
        symbol.setSignature(symbolName + "()");
        symbol.setAnnotations("@Demo");
        symbol.setRoutePath("/demo");
        symbol.setImportText("import demo");
        symbol.setContent("public void " + symbolName + "() {}");
        symbol.setStartLine(startLine);
        symbol.setEndLine(endLine);
        return symbol;
    }
}


