package com.codepliot.service;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.config.WorkspaceProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
/**
 * GitWorkspaceService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class GitWorkspaceService {

    private final WorkspaceProperties workspaceProperties;
/**
 * 创建 GitWorkspaceService 实例。
 */
public GitWorkspaceService(WorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }
/**
 * 获取Workspace Root相关逻辑。
 */
public Path getWorkspaceRoot() {
        return Path.of(workspaceProperties.getRoot()).toAbsolutePath().normalize();
    }
/**
 * 获取Project Workspace相关逻辑。
 */
public Path getProjectWorkspace(Long projectId) {
        validateProjectId(projectId);
        return getWorkspaceRoot().resolve(String.valueOf(projectId)).normalize();
    }
/**
 * 获取Repository Path相关逻辑。
 */
public Path getRepositoryPath(Long projectId) {
        return getProjectWorkspace(projectId).resolve("repo").normalize();
    }
/**
 * 获取Lucene Index Path相关逻辑。
 */
public Path getLuceneIndexPath(Long projectId) {
        return getProjectWorkspace(projectId).resolve("lucene-index").normalize();
    }
/**
 * 执行 hasGitDirectory 相关逻辑。
 */
public boolean hasGitDirectory(Long projectId) {
        return Files.isDirectory(getRepositoryPath(projectId).resolve(".git"));
    }
/**
 * 执行 ensureProjectWorkspace 相关逻辑。
 */
public void ensureProjectWorkspace(Long projectId) {
        createDirectories(getProjectWorkspace(projectId));
    }
/**
 * 创建Directories相关逻辑。
 */
private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to prepare workspace directory: " + path);
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
}

