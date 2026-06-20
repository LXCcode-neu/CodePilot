package com.codepliot.service.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.ProjectLlmConfig;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.LlmAvailableModelVO;
import com.codepliot.model.LlmConfigTestResult;
import com.codepliot.model.LlmProviderVO;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.ProjectLlmConfigSaveRequest;
import com.codepliot.model.ProjectLlmConfigVO;
import com.codepliot.repository.ProjectLlmConfigMapper;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.llm.LlmService;
import com.codepliot.utils.ApiKeyCryptoUtils;
import com.codepliot.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目级 LLM 配置服务。
 * <p>
 * 管理项目级别的 LLM 模型配置，支持：
 * <ul>
 *   <li>为每个项目仓库单独配置 LLM 提供商、模型和 API 密钥</li>
 *   <li>维护全局默认 LLM 配置（projectRepoId = 0）</li>
 *   <li>查询可用的 LLM 提供商和模型列表</li>
 *   <li>测试项目/全局 LLM 配置的连接可用性</li>
 * </ul>
 * <p>
 * 当项目未单独配置时，会自动回退到全局配置或用户活跃 API 密钥配置。
 */
@Service
public class ProjectLlmConfigService {

    private static final long GLOBAL_PROJECT_REPO_ID = 0L;

    private final ProjectLlmConfigMapper projectLlmConfigMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final ApiKeyCryptoUtils apiKeyCryptoUtils;
    private final LlmService llmService;
    private final LlmApiKeyConfigService llmApiKeyConfigService;
    private final LlmProviderService llmProviderService;

    public ProjectLlmConfigService(ProjectLlmConfigMapper projectLlmConfigMapper,
                                   ProjectRepoMapper projectRepoMapper,
                                   ApiKeyCryptoUtils apiKeyCryptoUtils,
                                   LlmService llmService,
                                   LlmApiKeyConfigService llmApiKeyConfigService,
                                   LlmProviderService llmProviderService) {
        this.projectLlmConfigMapper = projectLlmConfigMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.apiKeyCryptoUtils = apiKeyCryptoUtils;
        this.llmService = llmService;
        this.llmApiKeyConfigService = llmApiKeyConfigService;
        this.llmProviderService = llmProviderService;
    }

    public List<LlmProviderVO> listProviders() {
        return llmProviderService.listProviders();
    }

    public List<LlmAvailableModelVO> listAvailableModels() {
        return llmProviderService.listAvailableModels();
    }

    /**
     * 获取指定项目的 LLM 配置。
     *
     * @param projectRepoId 项目仓库 ID
     * @return 项目 LLM 配置视图对象，若未配置则返回 null
     */
    public ProjectLlmConfigVO getProjectConfig(Long projectRepoId) {
        requireOwnedProject(projectRepoId);
        ProjectLlmConfig config = findByProjectId(projectRepoId);
        if (config == null) {
            return null;
        }
        return ProjectLlmConfigVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted()));
    }

    /**
     * 获取全局默认 LLM 配置。
     *
     * @return 全局 LLM 配置视图对象，若未配置则返回 null
     */
    public ProjectLlmConfigVO getGlobalConfig() {
        ProjectLlmConfig config = findByProjectId(GLOBAL_PROJECT_REPO_ID);
        if (config == null) {
            return null;
        }
        return ProjectLlmConfigVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted()));
    }

    /**
     * 保存指定项目的 LLM 配置（新增或更新）。
     *
     * @param projectRepoId 项目仓库 ID
     * @param request       配置保存请求
     * @return 保存后的配置视图对象
     */
    @Transactional
    public ProjectLlmConfigVO saveProjectConfig(Long projectRepoId, ProjectLlmConfigSaveRequest request) {
        requireOwnedProject(projectRepoId);
        return saveConfig(projectRepoId, request);
    }

    /**
     * 保存全局默认 LLM 配置。
     *
     * @param request 配置保存请求
     * @return 保存后的配置视图对象
     */
    @Transactional
    public ProjectLlmConfigVO saveGlobalConfig(ProjectLlmConfigSaveRequest request) {
        return saveConfig(GLOBAL_PROJECT_REPO_ID, request);
    }

    private ProjectLlmConfigVO saveConfig(Long projectRepoId, ProjectLlmConfigSaveRequest request) {
        LlmAvailableModelVO model = requireSupportedModel(request.provider(), request.modelName());
        ProjectLlmConfig config = findByProjectId(projectRepoId);
        boolean creating = config == null;
        if (creating) {
            config = new ProjectLlmConfig();
            config.setProjectRepoId(projectRepoId);
        }

        String apiKey = request.apiKey() == null ? "" : request.apiKey().trim();
        if (apiKey.isBlank() && creating) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "API key is required");
        }
        if (!apiKey.isBlank()) {
            config.setApiKeyEncrypted(apiKeyCryptoUtils.encrypt(apiKey));
        }

        config.setProvider(model.provider());
        config.setModelName(model.modelName());
        config.setDisplayName(resolveDisplayName(request.displayName(), model));
        config.setBaseUrl(normalizeBaseUrl(request.baseUrl()));
        config.setEnabled(true);

        if (creating) {
            projectLlmConfigMapper.insert(config);
        } else {
            projectLlmConfigMapper.updateById(config);
        }
        return ProjectLlmConfigVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted()));
    }

    /**
     * 测试指定项目的 LLM 配置连接可用性。
     *
     * @param projectRepoId 项目仓库 ID
     * @return 测试结果
     */
    public LlmConfigTestResult testProjectConfig(Long projectRepoId) {
        requireOwnedProject(projectRepoId);
        ProjectLlmConfig config = requireProjectConfig(projectRepoId);
        return testConfig(config);
    }

    public LlmConfigTestResult testGlobalConfig() {
        ProjectLlmConfig config = requireGlobalConfig();
        return testConfig(config);
    }

    private LlmConfigTestResult testConfig(ProjectLlmConfig config) {
        try {
            llmService.generate(toRuntimeConfig(config), "You are a connection test.", "Reply with OK.");
            return new LlmConfigTestResult(true, "Connection succeeded");
        } catch (RuntimeException exception) {
            return new LlmConfigTestResult(false, exception.getMessage());
        }
    }

    /**
     * 获取指定项目的必需 LLM 配置。
     * <p>
     * 若项目未单独配置，回退到用户活跃 API 密钥配置。若均无配置则抛出异常。
     *
     * @param projectRepoId 项目仓库 ID
     * @return 项目 LLM 配置实体
     */
    public ProjectLlmConfig requireProjectConfig(Long projectRepoId) {
        ProjectLlmConfig config = findByProjectId(projectRepoId);
        if (config == null) {
            config = llmApiKeyConfigService.requireActiveAsProjectConfig();
        }
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please configure the global LLM model and API key first");
        }
        if (config.getApiKeyEncrypted() == null || config.getApiKeyEncrypted().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Project API key is not configured");
        }
        return config;
    }

    public ProjectLlmConfig requireProjectConfig(Long projectRepoId, Long userId) {
        ProjectLlmConfig config = findByProjectId(projectRepoId);
        if (config == null) {
            config = llmApiKeyConfigService.requireActiveAsProjectConfig(userId);
        }
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please configure the global LLM model and API key first");
        }
        if (config.getApiKeyEncrypted() == null || config.getApiKeyEncrypted().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Project API key is not configured");
        }
        return config;
    }

    /**
     * 获取全局必需的 LLM 配置，若未配置则抛出异常。
     *
     * @return 全局 LLM 配置实体
     */
    public ProjectLlmConfig requireGlobalConfig() {
        ProjectLlmConfig config = findByProjectId(GLOBAL_PROJECT_REPO_ID);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please configure the global LLM model and API key first");
        }
        if (config.getApiKeyEncrypted() == null || config.getApiKeyEncrypted().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Global API key is not configured");
        }
        return config;
    }

    /**
     * 将项目 LLM 配置实体转换为运行时配置（包含解密后的 API 密钥）。
     *
     * @param config 项目 LLM 配置实体
     * @return LLM 运行时配置
     */
    public LlmRuntimeConfig toRuntimeConfig(ProjectLlmConfig config) {
        return new LlmRuntimeConfig(
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                apiKeyCryptoUtils.decrypt(config.getApiKeyEncrypted())
        );
    }

    @Transactional
    public void deleteByProjectId(Long projectRepoId) {
        projectLlmConfigMapper.delete(new LambdaQueryWrapper<ProjectLlmConfig>()
                .eq(ProjectLlmConfig::getProjectRepoId, projectRepoId));
    }

    private ProjectLlmConfig findByProjectId(Long projectRepoId) {
        return projectLlmConfigMapper.selectOne(new LambdaQueryWrapper<ProjectLlmConfig>()
                .eq(ProjectLlmConfig::getProjectRepoId, projectRepoId)
                .last("limit 1"));
    }

    private ProjectRepo requireOwnedProject(Long projectRepoId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectRepoId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private LlmAvailableModelVO requireSupportedModel(String provider, String modelName) {
        return llmProviderService.resolveModel(provider, modelName, modelName, null);
    }

    private String resolveDisplayName(String displayName, LlmAvailableModelVO model) {
        if (displayName == null || displayName.isBlank()) {
            return model.displayName();
        }
        return displayName.trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Base URL is required");
        }
        return normalized;
    }
}
