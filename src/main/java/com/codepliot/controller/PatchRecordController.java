package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.service.patch.PatchService;
import com.codepliot.model.PatchRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * PatchRecordController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/patch")
public class PatchRecordController {

    private final PatchService patchService;
/**
 * 创建 PatchRecordController 实例。
 */
public PatchRecordController(PatchService patchService) {
        this.patchService = patchService;
    }
    /**
     * 执行 detail 相关逻辑。
     */
@GetMapping
    public Result<PatchRecordVO> detail(@PathVariable Long taskId) {
        return Result.success(patchService.getTaskPatch(taskId));
    }
}

