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

@RestController
@RequestMapping("/api")
public class ProjectLlmConfigController {

    private final ProjectLlmConfigService projectLlmConfigService;

    public ProjectLlmConfigController(ProjectLlmConfigService projectLlmConfigService) {
        this.projectLlmConfigService = projectLlmConfigService;
    }

    @GetMapping("/llm/models")
    public Result<List<LlmAvailableModelVO>> listModels() {
        return Result.success(projectLlmConfigService.listAvailableModels());
    }

    @GetMapping("/llm/providers")
    public Result<List<LlmProviderVO>> listProviders() {
        return Result.success(projectLlmConfigService.listProviders());
    }

    @GetMapping("/llm/config")
    public Result<ProjectLlmConfigVO> getGlobalConfig() {
        return Result.success(projectLlmConfigService.getGlobalConfig());
    }

    @PutMapping("/llm/config")
    public Result<ProjectLlmConfigVO> saveGlobalConfig(@Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Global LLM config saved", projectLlmConfigService.saveGlobalConfig(request));
    }

    @PostMapping("/llm/config/api-key")
    public Result<ProjectLlmConfigVO> createGlobalApiKey(@Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Global LLM API key saved", projectLlmConfigService.saveGlobalConfig(request));
    }

    @PostMapping("/llm/config/test")
    public Result<LlmConfigTestResult> testGlobalConfig() {
        return Result.success(projectLlmConfigService.testGlobalConfig());
    }

    @GetMapping("/projects/{projectRepoId}/llm-config")
    public Result<ProjectLlmConfigVO> getProjectConfig(@PathVariable Long projectRepoId) {
        return Result.success(projectLlmConfigService.getProjectConfig(projectRepoId));
    }

    @PutMapping("/projects/{projectRepoId}/llm-config")
    public Result<ProjectLlmConfigVO> saveProjectConfig(@PathVariable Long projectRepoId,
                                                        @Valid @RequestBody ProjectLlmConfigSaveRequest request) {
        return Result.success("Project LLM config saved", projectLlmConfigService.saveProjectConfig(projectRepoId, request));
    }

    @PostMapping("/projects/{projectRepoId}/llm-config/test")
    public Result<LlmConfigTestResult> testProjectConfig(@PathVariable Long projectRepoId) {
        return Result.success(projectLlmConfigService.testProjectConfig(projectRepoId));
    }
}
