package com.codepliot.controller;

import com.codepliot.model.LlmAvailableModelVO;
import com.codepliot.model.LlmConfigTestResult;
import com.codepliot.model.LlmProviderVO;
import com.codepliot.model.ProjectLlmConfigSaveRequest;
import com.codepliot.model.ProjectLlmConfigVO;
import com.codepliot.model.Result;
import com.codepliot.service.llm.ProjectLlmConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目 LLM 配置控制器
 * <p>
 * 提供大语言模型（LLM）的全局配置和项目级别配置管理接口，
 * 包括查看可用模型和供应商列表、获取/保存配置、测试配置连通性等操作。
 * </p>
 */
@RestController
@RequestMapping("/api")
public class ProjectLlmConfigController {

    private final ProjectLlmConfigService projectLlmConfigService;

    public ProjectLlmConfigController(ProjectLlmConfigService projectLlmConfigService) {
        this.projectLlmConfigService = projectLlmConfigService;
    }

    /**
     * 获取可用的 LLM 模型列表
     *
     * @return 可用模型列表
     */
    @GetMapping("/llm/models")
    public Result<List<LlmAvailableModelVO>> listModels() {
        return Result.success(projectLlmConfigService.listAvailableModels());
    }

    /**
     * 获取可用的 LLM 供应商列表
     *
     * @return 供应商列表
     */
    @GetMapping("/llm/providers")
    public Result<List<LlmProviderVO>> listProviders() {
        return Result.success(projectLlmConfigService.listProviders());
    }

    /**
     * 获取全局 LLM 配置
     *
     * @return 全局 LLM 配置视图对象
     */
    @GetMapping("/llm/config")
    public Result<ProjectLlmConfigVO> getGlobalConfig() {
        return Result.success(projectLlmConfigService.getGlobalConfig());
    }

    /**
     * 保存全局 LLM 配置
     *
     * @param request 配置保存请求参数
     * @return 保存后的全局 LLM 配置视图对象
     */
    @PutMapping("/llm/config")
    public Result<ProjectLlmConfigVO> saveGlobalConfig(@Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Global LLM config saved", projectLlmConfigService.saveGlobalConfig(request));
    }

    /**
     * 创建/保存全局 LLM API 密钥配置
     *
     * @param request 包含 API 密钥信息的配置保存请求参数
     * @return 保存后的全局 LLM 配置视图对象
     */
    @PostMapping("/llm/config/api-key")
    public Result<ProjectLlmConfigVO> createGlobalApiKey(@Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Global LLM API key saved", projectLlmConfigService.saveGlobalConfig(request));
    }

    /**
     * 测试全局 LLM 配置的连通性
     *
     * @return 测试结果
     */
    @PostMapping("/llm/config/test")
    public Result<LlmConfigTestResult> testGlobalConfig() {
        return Result.success(projectLlmConfigService.testGlobalConfig());
    }

    /**
     * 获取指定项目的 LLM 配置
     *
     * @param projectRepoId 项目仓库 ID
     * @return 项目 LLM 配置视图对象
     */
    @GetMapping("/projects/{projectRepoId}/llm-config")
    public Result<ProjectLlmConfigVO> getProjectConfig(@PathVariable Long projectRepoId) {
        return Result.success(projectLlmConfigService.getProjectConfig(projectRepoId));
    }

    /**
     * 保存指定项目的 LLM 配置
     *
     * @param projectRepoId 项目仓库 ID
     * @param request       配置保存请求参数
     * @return 保存后的项目 LLM 配置视图对象
     */
    @PutMapping("/projects/{projectRepoId}/llm-config")
    public Result<ProjectLlmConfigVO> saveProjectConfig(@PathVariable Long projectRepoId,
                                                        @Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Project LLM config saved", projectLlmConfigService.saveProjectConfig(projectRepoId, request));
    }

    /**
     * 测试指定项目 LLM 配置的连通性
     *
     * @param projectRepoId 项目仓库 ID
     * @return 测试结果
     */
    @PostMapping("/projects/{projectRepoId}/llm-config/test")
    public Result<LlmConfigTestResult> testProjectConfig(@PathVariable Long projectRepoId) {
        return Result.success(projectLlmConfigService.testProjectConfig(projectRepoId));
    }
}
