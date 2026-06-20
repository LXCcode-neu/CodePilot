package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.model.SentryProjectMappingSaveRequest;
import com.codepliot.model.SentryProjectMappingVO;
import com.codepliot.service.sentry.SentryProjectMappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentry 项目映射配置控制器
 * <p>
 * 管理项目与 Sentry 项目之间的映射关系，支持查看、保存和删除映射配置，
 * 用于将 Sentry 错误告警关联到对应的 CodePilot 项目。
 * </p>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/sentry-config")
public class SentryProjectMappingController {

    private final SentryProjectMappingService sentryProjectMappingService;

    public SentryProjectMappingController(SentryProjectMappingService sentryProjectMappingService) {
        this.sentryProjectMappingService = sentryProjectMappingService;
    }

    /**
     * 获取指定项目的 Sentry 项目映射配置
     *
     * @param projectId 项目 ID
     * @return Sentry 项目映射配置视图对象
     */
    @GetMapping
    public Result<SentryProjectMappingVO> get(@PathVariable Long projectId) {
        return Result.success(sentryProjectMappingService.getProjectMapping(projectId));
    }

    /**
     * 保存指定项目的 Sentry 项目映射配置
     *
     * @param projectId 项目 ID
     * @param request   映射配置保存请求参数
     * @return 保存后的 Sentry 项目映射配置视图对象
     */
    @PutMapping
    public Result<SentryProjectMappingVO> save(@PathVariable Long projectId,
                                               @Valid @RequestBody SentryProjectMappingSaveRequest request) {
        return Result.success("Sentry project mapping saved",
                sentryProjectMappingService.saveProjectMapping(projectId, request));
    }

    /**
     * 删除指定项目的 Sentry 项目映射配置
     *
     * @param projectId 项目 ID
     * @return 操作结果
     */
    @DeleteMapping
    public Result<Void> delete(@PathVariable Long projectId) {
        sentryProjectMappingService.deleteProjectMapping(projectId);
        return Result.success("Sentry project mapping deleted", null);
    }
}
