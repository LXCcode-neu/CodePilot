package com.codepliot.controller;

import com.codepliot.model.LlmApiKeyCreateRequest;
import com.codepliot.model.LlmApiKeyVO;
import com.codepliot.model.LlmConfigTestResult;
import com.codepliot.model.Result;
import com.codepliot.service.llm.LlmApiKeyConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM API Key 配置控制器
 * <p>
 * 提供大语言模型 API 密钥的管理接口，包括密钥的创建、查询、
 * 启用（应用）、连通性测试和删除等操作。
 * </p>
 */
@RestController
@RequestMapping("/api/llm/api-keys")
public class LlmApiKeyConfigController {

    private final LlmApiKeyConfigService llmApiKeyConfigService;

    public LlmApiKeyConfigController(LlmApiKeyConfigService llmApiKeyConfigService) {
        this.llmApiKeyConfigService = llmApiKeyConfigService;
    }

    /**
     * 获取当前用户的所有 LLM API 密钥列表
     *
     * @return API 密钥列表
     */
    @GetMapping
    public Result<List<LlmApiKeyVO>> list() {
        return Result.success(llmApiKeyConfigService.listCurrentUserKeys());
    }

    /**
     * 创建新的 LLM API 密钥
     *
     * @param request 密钥创建请求参数
     * @return 创建成功后的 API 密钥视图对象
     */
    @PostMapping
    public Result<LlmApiKeyVO> create(@Valid @RequestBody LlmApiKeyCreateRequest request) {
        return Result.success("API key created", llmApiKeyConfigService.create(request));
    }

    /**
     * 启用（应用）指定的 API 密钥
     *
     * @param id 密钥 ID
     * @return 启用后的 API 密钥视图对象
     */
    @PutMapping("/{id}/apply")
    public Result<LlmApiKeyVO> apply(@PathVariable Long id) {
        return Result.success("API key applied", llmApiKeyConfigService.apply(id));
    }

    /**
     * 测试指定 API 密钥的连通性
     *
     * @param id 密钥 ID
     * @return 测试结果
     */
    @PostMapping("/{id}/test")
    public Result<LlmConfigTestResult> test(@PathVariable Long id) {
        return Result.success(llmApiKeyConfigService.test(id));
    }

    /**
     * 删除指定的 API 密钥
     *
     * @param id 密钥 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        llmApiKeyConfigService.delete(id);
        return Result.success("API key deleted", null);
    }
}
