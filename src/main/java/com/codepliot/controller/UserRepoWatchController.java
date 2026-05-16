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

@RestController
@RequestMapping("/api/repo-watches")
public class UserRepoWatchController {

    private final UserRepoWatchService userRepoWatchService;

    public UserRepoWatchController(UserRepoWatchService userRepoWatchService) {
        this.userRepoWatchService = userRepoWatchService;
    }

    @PostMapping
    public Result<UserRepoWatchVO> create(@Valid @RequestBody UserRepoWatchCreateRequest request) {
        return Result.success("Repository watch created", userRepoWatchService.create(request));
    }

    @GetMapping
    public Result<List<UserRepoWatchVO>> list() {
        return Result.success(userRepoWatchService.listCurrentUserWatches());
    }

    @PutMapping("/{id}/enabled")
    public Result<UserRepoWatchVO> updateEnabled(@PathVariable Long id,
                                                 @Valid @RequestBody EnabledUpdateRequest request) {
        return Result.success("Repository watch updated", userRepoWatchService.updateEnabled(id, request));
    }
}
