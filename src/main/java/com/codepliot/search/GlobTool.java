package com.codepliot.search;

import com.codepliot.search.glob.FileGlobService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Finds repository files by glob patterns.
 */
@Component
public class GlobTool {

    private final FileGlobService fileGlobService;

    public GlobTool(FileGlobService fileGlobService) {
        this.fileGlobService = fileGlobService;
    }

    public List<String> execute(String repoPath, List<String> patterns) {
        return fileGlobService.findFiles(repoPath, patterns == null ? List.of() : patterns);
    }
}
