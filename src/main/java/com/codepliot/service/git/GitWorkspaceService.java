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

/**
 * Git 工作区管理服务。
 * <p>
 * 管理项目仓库在本地文件系统中的工作区目录结构。每个项目拥有独立的工作区，
 * 工作区下包含 {@code repo} 子目录用于存放克隆的 Git 仓库。
 * </p>
 * <p>
 * 目录结构：{workspaceRoot}/{projectId}/repo/
 * </p>
 * <p>
 * 主要功能包括：获取工作区路径、确保目录存在、检查 .git 目录是否存在、
 * 以及递归删除仓库工作区。
 * </p>
 */
@Service
public class GitWorkspaceService {

    private final WorkspaceProperties workspaceProperties;

    public GitWorkspaceService(WorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }

    /**
     * 获取工作区根目录的绝对路径。
     *
     * @return 工作区根目录路径
     */
    public Path getWorkspaceRoot() {
        return Path.of(workspaceProperties.getRoot()).toAbsolutePath().normalize();
    }

    /**
     * 获取指定项目的工作区目录路径。
     *
     * @param projectId 项目 ID（必须大于 0）
     * @return 项目工作区目录路径
     * @throws BusinessException 如果 projectId 无效
     */
    public Path getProjectWorkspace(Long projectId) {
        validateProjectId(projectId);
        return getWorkspaceRoot().resolve(String.valueOf(projectId)).normalize();
    }

    /**
     * 获取指定项目的 Git 仓库目录路径。
     *
     * @param projectId 项目 ID
     * @return 仓库目录路径（{projectWorkspace}/repo）
     */
    public Path getRepositoryPath(Long projectId) {
        return getProjectWorkspace(projectId).resolve("repo").normalize();
    }

    /**
     * 检查指定项目的仓库目录下是否存在 .git 目录。
     *
     * @param projectId 项目 ID
     * @return 如果 .git 目录存在则返回 true
     */
    public boolean hasGitDirectory(Long projectId) {
        return Files.isDirectory(getRepositoryPath(projectId).resolve(".git"));
    }

    /**
     * 确保项目工作区目录存在，如果不存在则创建。
     *
     * @param projectId 项目 ID
     */
    public void ensureProjectWorkspace(Long projectId) {
        createDirectories(getProjectWorkspace(projectId));
    }

    /**
     * 递归删除指定项目的仓库工作区目录。
     * <p>
     * 按逆序遍历删除所有文件和目录，确保安全性检查（路径必须在项目工作区内）。
     * </p>
     *
     * @param projectId 项目 ID
     */
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
