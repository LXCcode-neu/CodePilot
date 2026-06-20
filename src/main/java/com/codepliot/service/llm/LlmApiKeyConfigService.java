package com.codepliot.service.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.LlmApiKeyConfig;
import com.codepliot.entity.ProjectLlmConfig;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.LlmApiKeyCreateRequest;
import com.codepliot.model.LlmApiKeyVO;
import com.codepliot.model.LlmConfigTestResult;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.repository.LlmApiKeyConfigMapper;
import com.codepliot.service.llm.LlmService;
import com.codepliot.utils.ApiKeyCryptoUtils;
import com.codepliot.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLM API 密钥配置服务。
 * <p>
 * 管理用户级别的 LLM API 密钥配置，包括：
 * <ul>
 *   <li>创建、删除 API 密钥配置</li>
 *   <li>切换当前活跃的 API 密钥</li>
 *   <li>测试 API 密钥连接是否可用</li>
 *   <li>获取当前活跃密钥作为项目级 LLM 配置</li>
 * </ul>
 * <p>
 * API 密钥使用加密存储，展示时进行脱敏处理。
 */
@Service
public class LlmApiKeyConfigService {

    private final LlmApiKeyConfigMapper llmApiKeyConfigMapper;
    private final ApiKeyCryptoUtils apiKeyCryptoUtils;
    private final LlmService llmService;
    private final LlmProviderService llmProviderService;

    public LlmApiKeyConfigService(LlmApiKeyConfigMapper llmApiKeyConfigMapper,
                                  ApiKeyCryptoUtils apiKeyCryptoUtils,
                                  LlmService llmService,
                                  LlmProviderService llmProviderService) {
        this.llmApiKeyConfigMapper = llmApiKeyConfigMapper;
        this.apiKeyCryptoUtils = apiKeyCryptoUtils;
        this.llmService = llmService;
        this.llmProviderService = llmProviderService;
    }

    /**
     * 查询当前用户的所有 LLM API 密钥配置列表，按活跃状态和创建时间排序。
     *
     * @return API 密钥配置列表（密钥已脱敏）
     */
    public List<LlmApiKeyVO> listCurrentUserKeys() {
        Long userId = SecurityUtils.getCurrentUserId();
        return llmApiKeyConfigMapper.selectList(new LambdaQueryWrapper<LlmApiKeyConfig>()
                        .eq(LlmApiKeyConfig::getUserId, userId)
                        .orderByDesc(LlmApiKeyConfig::getActive)
                        .orderByDesc(LlmApiKeyConfig::getCreatedAt))
                .stream()
                .map(config -> LlmApiKeyVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted())))
                .toList();
    }

    /**
     * 创建新的 LLM API 密钥配置。
     * <p>
     * 若当前用户尚无活跃密钥，新创建的密钥将自动设为活跃状态。
     *
     * @param request 创建请求，包含提供商、模型名称、API 密钥等信息
     * @return 创建的密钥配置视图对象（密钥已脱敏）
     */
    @Transactional
    public LlmApiKeyVO create(LlmApiKeyCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean shouldActivate = activeKey(userId) == null;
        var model = llmProviderService.resolveModel(
                request.provider(),
                request.modelName(),
                request.displayName(),
                request.baseUrl()
        );

        LlmApiKeyConfig config = new LlmApiKeyConfig();
        config.setUserId(userId);
        config.setKeyName(request.name().trim());
        config.setProvider(model.provider());
        config.setModelName(model.modelName());
        config.setDisplayName(request.displayName().trim());
        config.setBaseUrl(normalizeBaseUrl(request.baseUrl()));
        config.setApiKeyEncrypted(apiKeyCryptoUtils.encrypt(request.apiKey().trim()));
        config.setActive(shouldActivate);
        config.setLastUsedAt(null);
        llmApiKeyConfigMapper.insert(config);
        return LlmApiKeyVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted()));
    }

    /**
     * 将指定的 API 密钥设为当前用户的活跃密钥。
     * <p>
     * 会先将该用户的所有密钥设为非活跃状态，再激活目标密钥。
     *
     * @param id API 密钥配置 ID
     * @return 更新后的密钥配置视图对象
     */
    @Transactional
    public LlmApiKeyVO apply(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        LlmApiKeyConfig config = requireOwnedKey(id, userId);
        llmApiKeyConfigMapper.update(null, new LambdaUpdateWrapper<LlmApiKeyConfig>()
                .eq(LlmApiKeyConfig::getUserId, userId)
                .set(LlmApiKeyConfig::getActive, false));
        config.setActive(true);
        llmApiKeyConfigMapper.updateById(config);
        return LlmApiKeyVO.from(config, apiKeyCryptoUtils.mask(config.getApiKeyEncrypted()));
    }

    /**
     * 测试指定 API 密钥的连接可用性。
     * <p>
     * 发送简单的测试请求到 LLM 提供商，验证 API 密钥是否有效。
     *
     * @param id API 密钥配置 ID
     * @return 测试结果，包含成功标志和消息
     */
    public LlmConfigTestResult test(Long id) {
        LlmApiKeyConfig config = requireOwnedKey(id, SecurityUtils.getCurrentUserId());
        try {
            llmService.generate(toRuntimeConfig(config), "You are a connection test.", "Reply with OK.");
            return new LlmConfigTestResult(true, "Connection succeeded");
        } catch (RuntimeException exception) {
            return new LlmConfigTestResult(false, exception.getMessage());
        }
    }

    /**
     * 删除指定的 API 密钥配置。
     * <p>
     * 若删除的是当前活跃密钥，系统会自动将最新创建的密钥设为活跃状态。
     *
     * @param id API 密钥配置 ID
     */
    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        LlmApiKeyConfig config = requireOwnedKey(id, userId);
        boolean deletedActiveKey = Boolean.TRUE.equals(config.getActive());
        llmApiKeyConfigMapper.deleteById(config.getId());
        if (deletedActiveKey) {
            activateLatestKey(userId);
        }
    }

    /**
     * 获取当前用户活跃的 API 密钥，并转换为项目级 LLM 配置。
     *
     * @return 项目级 LLM 配置
     * @throws BusinessException 若无活跃密钥则抛出异常
     */
    public ProjectLlmConfig requireActiveAsProjectConfig() {
        return requireActiveAsProjectConfig(SecurityUtils.getCurrentUserId());
    }

    public ProjectLlmConfig requireActiveAsProjectConfig(Long userId) {
        LlmApiKeyConfig active = activeKey(userId);
        if (active == null || !Boolean.TRUE.equals(active.getActive())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please configure and apply an API key first");
        }
        active.setLastUsedAt(LocalDateTime.now());
        llmApiKeyConfigMapper.updateById(active);

        ProjectLlmConfig config = new ProjectLlmConfig();
        config.setProjectRepoId(0L);
        config.setProvider(active.getProvider());
        config.setModelName(active.getModelName());
        config.setDisplayName(active.getDisplayName());
        config.setBaseUrl(active.getBaseUrl());
        config.setApiKeyEncrypted(active.getApiKeyEncrypted());
        config.setEnabled(true);
        return config;
    }

    private LlmRuntimeConfig toRuntimeConfig(LlmApiKeyConfig config) {
        return new LlmRuntimeConfig(
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                apiKeyCryptoUtils.decrypt(config.getApiKeyEncrypted())
        );
    }

    private LlmApiKeyConfig activeKey(Long userId) {
        return llmApiKeyConfigMapper.selectOne(new LambdaQueryWrapper<LlmApiKeyConfig>()
                .eq(LlmApiKeyConfig::getUserId, userId)
                .eq(LlmApiKeyConfig::getActive, true)
                .last("limit 1"));
    }

    private void activateLatestKey(Long userId) {
        LlmApiKeyConfig latest = llmApiKeyConfigMapper.selectOne(new LambdaQueryWrapper<LlmApiKeyConfig>()
                .eq(LlmApiKeyConfig::getUserId, userId)
                .orderByDesc(LlmApiKeyConfig::getCreatedAt)
                .last("limit 1"));
        if (latest != null) {
            latest.setActive(true);
            llmApiKeyConfigMapper.updateById(latest);
        }
    }

    private LlmApiKeyConfig requireOwnedKey(Long id, Long userId) {
        LlmApiKeyConfig config = llmApiKeyConfigMapper.selectOne(new LambdaQueryWrapper<LlmApiKeyConfig>()
                .eq(LlmApiKeyConfig::getId, id)
                .eq(LlmApiKeyConfig::getUserId, userId)
                .last("limit 1"));
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API key not found");
        }
        return config;
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
