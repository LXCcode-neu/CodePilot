package com.codepliot.service.git;

import com.codepliot.config.WorkspaceProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
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

    public void deleteRepositoryWorkspace(Long projectId) {
        Path repositoryPath = getRepositoryPath(projectId);
        Path projectWorkspace = getProjectWorkspace(projectId);
        if (!repositoryPath.startsWith(projectWorkspace) || !Files.exists(repositoryPath)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(repositoryPath)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to clean repository workspace: " + repositoryPath);
        }
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to prepare workspace directory: " + path);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to delete workspace path: " + path);
        }
    }

    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "projectId must be greater than 0");
        }
    }
}
