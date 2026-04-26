package com.codepliot.git.service;

import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.git.config.WorkspaceProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * Git 工作区路径服务。
 * 统一负责项目工作目录、本地仓库目录的路径拼装与目录创建。
 */
@Service
public class GitWorkspaceService {

    private final WorkspaceProperties workspaceProperties;

    public GitWorkspaceService(WorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }

    /**
     * 获取工作区根目录的绝对路径。
     */
    public Path getWorkspaceRoot() {
        return Path.of(workspaceProperties.getRoot()).toAbsolutePath().normalize();
    }

    /**
     * 获取某个项目对应的工作目录。
     */
    public Path getProjectWorkspace(Long projectId) {
        validateProjectId(projectId);
        return getWorkspaceRoot().resolve(String.valueOf(projectId)).normalize();
    }

    /**
     * 获取某个项目的本地仓库目录，固定为 workspace/{projectId}/repo。
     */
    public Path getRepositoryPath(Long projectId) {
        return getProjectWorkspace(projectId).resolve("repo").normalize();
    }

    /**
     * 判断目标目录是否已经是一个 Git 仓库。
     */
    public boolean hasGitDirectory(Long projectId) {
        return Files.isDirectory(getRepositoryPath(projectId).resolve(".git"));
    }

    /**
     * 确保项目工作目录存在。
     */
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
