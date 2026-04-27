package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.model.ProjectCreateRequest;
import com.codepliot.service.ProjectRepoService;
import com.codepliot.model.ProjectRepoVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * ProjectRepoController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectRepoController {

    private final ProjectRepoService projectRepoService;
/**
 * 创建 ProjectRepoController 实例。
 */
public ProjectRepoController(ProjectRepoService projectRepoService) {
        this.projectRepoService = projectRepoService;
    }
    /**
     * 执行 create 相关逻辑。
     */
@PostMapping
    public Result<ProjectRepoVO> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success("Project repository created", projectRepoService.create(request));
    }
    /**
     * 执行 list 相关逻辑。
     */
@GetMapping
    public Result<List<ProjectRepoVO>> list() {
        return Result.success(projectRepoService.listCurrentUserRepos());
    }
    /**
     * 执行 detail 相关逻辑。
     */
@GetMapping("/{id}")
    public Result<ProjectRepoVO> detail(@PathVariable Long id) {
        return Result.success(projectRepoService.getDetail(id));
    }
    /**
     * 执行 delete 相关逻辑。
     */
@DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectRepoService.delete(id);
        return Result.success("Project repository deleted", null);
    }
}

