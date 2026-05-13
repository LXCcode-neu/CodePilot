package com.codepliot.controller;

import com.codepliot.model.LlmApiKeyCreateRequest;
import com.codepliot.model.LlmApiKeyVO;
import com.codepliot.model.LlmConfigTestResult;
import com.codepliot.model.Result;
import com.codepliot.service.LlmApiKeyConfigService;
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

@RestController
@RequestMapping("/api/llm/api-keys")
public class LlmApiKeyConfigController {

    private final LlmApiKeyConfigService llmApiKeyConfigService;

    public LlmApiKeyConfigController(LlmApiKeyConfigService llmApiKeyConfigService) {
        this.llmApiKeyConfigService = llmApiKeyConfigService;
    }

    @GetMapping
    public Result<List<LlmApiKeyVO>> list() {
        return Result.success(llmApiKeyConfigService.listCurrentUserKeys());
    }

    @PostMapping
    public Result<LlmApiKeyVO> create(@Valid @RequestBody LlmApiKeyCreateRequest request) {
        return Result.success("API key created", llmApiKeyConfigService.create(request));
    }

    @PutMapping("/{id}/apply")
    public Result<LlmApiKeyVO> apply(@PathVariable Long id) {
        return Result.success("API key applied", llmApiKeyConfigService.apply(id));
    }

    @PostMapping("/{id}/test")
    public Result<LlmConfigTestResult> test(@PathVariable Long id) {
        return Result.success(llmApiKeyConfigService.test(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        llmApiKeyConfigService.delete(id);
        return Result.success("API key deleted", null);
    }
}
