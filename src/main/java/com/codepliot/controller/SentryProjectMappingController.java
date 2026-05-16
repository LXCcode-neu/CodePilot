package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.model.SentryProjectMappingSaveRequest;
import com.codepliot.model.SentryProjectMappingVO;
import com.codepliot.service.SentryProjectMappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/sentry-config")
public class SentryProjectMappingController {

    private final SentryProjectMappingService sentryProjectMappingService;

    public SentryProjectMappingController(SentryProjectMappingService sentryProjectMappingService) {
        this.sentryProjectMappingService = sentryProjectMappingService;
    }

    @GetMapping
    public Result<SentryProjectMappingVO> get(@PathVariable Long projectId) {
        return Result.success(sentryProjectMappingService.getProjectMapping(projectId));
    }

    @PutMapping
    public Result<SentryProjectMappingVO> save(@PathVariable Long projectId,
                                               @Valid @RequestBody SentryProjectMappingSaveRequest request) {
        return Result.success("Sentry project mapping saved",
                sentryProjectMappingService.saveProjectMapping(projectId, request));
    }

    @DeleteMapping
    public Result<Void> delete(@PathVariable Long projectId) {
        sentryProjectMappingService.deleteProjectMapping(projectId);
        return Result.success("Sentry project mapping deleted", null);
    }
}
