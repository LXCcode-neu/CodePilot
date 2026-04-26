package com.codepliot.patch.controller;

import com.codepliot.common.result.Result;
import com.codepliot.patch.service.PatchService;
import com.codepliot.patch.vo.PatchRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/patch")
public class PatchRecordController {

    private final PatchService patchService;

    public PatchRecordController(PatchService patchService) {
        this.patchService = patchService;
    }

    @GetMapping
    public Result<PatchRecordVO> detail(@PathVariable Long taskId) {
        return Result.success(patchService.getTaskPatch(taskId));
    }
}
