package com.codepliot.git.service;

import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.git.config.WorkspaceProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class GitWorkspaceService {

    private final WorkspaceProperties workspaceProperties;

    public GitWorkspaceService(WorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }

    public Path getWorkspaceRoot() {
        return Path.of(workspaceProperties.getRoot()).toAbsolutePath().normalize();
    }

    public Path getProjectWorkspace(Long projectId) {
        validateProjectId(projectId);
        return getWorkspaceRoot().resolve(String.valueOf(projectId)).normalize();
    }

    public Path getRepositoryPath(Long projectId) {
        return getProjectWorkspace(projectId).resolve("repo").normalize();
    }

    public boolean hasGitDirectory(Long projectId) {
        return Files.isDirectory(getRepositoryPath(projectId).resolve(".git"));
    }

    public void ensureProjectWorkspace(Long projectId) {
        createDirectories(getProjectWorkspace(projectId));
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to prepare workspace directory: " + path);
        }
    }

    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId must be greater than 0");
        }
    }
}
