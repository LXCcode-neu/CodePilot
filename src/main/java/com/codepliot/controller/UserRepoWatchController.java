package com.codepliot.controller;

import com.codepliot.model.EnabledUpdateRequest;
import com.codepliot.model.Result;
import com.codepliot.model.UserRepoWatchCreateRequest;
import com.codepliot.model.UserRepoWatchVO;
import com.codepliot.service.githubIssue.UserRepoWatchService;
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
 * 仓库监听控制器
 * <p>
 * 提供用户对 GitHub 仓库的监听管理接口，支持创建监听、查看监听列表
 * 以及启用/禁用监听状态，用于自动追踪仓库的 Issue 变化。
 * </p>
 */
@RestController
@RequestMapping("/api/repo-watches")
public class UserRepoWatchController {

    private final UserRepoWatchService userRepoWatchService;

    public UserRepoWatchController(UserRepoWatchService userRepoWatchService) {
        this.userRepoWatchService = userRepoWatchService;
    }

    /**
     * 创建仓库监听
     *
     * @param request 监听创建请求参数，包含目标仓库信息
     * @return 创建成功后的仓库监听视图对象
     */
    @PostMapping
    public Result<UserRepoWatchVO> create(@Valid @RequestBody UserRepoWatchCreateRequest request) {
        return Result.success("Repository watch created", userRepoWatchService.create(request));
    }

    /**
     * 获取当前用户的所有仓库监听列表
     *
     * @return 仓库监听列表
     */
    @GetMapping
    public Result<List<UserRepoWatchVO>> list() {
        return Result.success(userRepoWatchService.listCurrentUserWatches());
    }

    /**
     * 更新仓库监听的启用/禁用状态
     *
     * @param id      监听记录 ID
     * @param request 包含启用状态的更新请求参数
     * @return 更新后的仓库监听视图对象
     */
    @PutMapping("/{id}/enabled")
    public Result<UserRepoWatchVO> updateEnabled(@PathVariable Long id,
                                                 @Valid @RequestBody EnabledUpdateRequest request) {
        return Result.success("Repository watch updated", userRepoWatchService.updateEnabled(id, request));
    }
}
