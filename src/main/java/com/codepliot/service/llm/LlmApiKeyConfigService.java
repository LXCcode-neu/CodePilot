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

    public LlmConfigTestResult test(Long id) {
        LlmApiKeyConfig config = requireOwnedKey(id, SecurityUtils.getCurrentUserId());
        try {
            llmService.generate(toRuntimeConfig(config), "You are a connection test.", "Reply with OK.");
            return new LlmConfigTestResult(true, "Connection succeeded");
        } catch (RuntimeException exception) {
            return new LlmConfigTestResult(false, exception.getMessage());
        }
    }

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
