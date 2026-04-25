package com.codepliot.project.controller;

import com.codepliot.common.result.Result;
import com.codepliot.project.dto.ProjectCreateRequest;
import com.codepliot.project.service.ProjectRepoService;
import com.codepliot.project.vo.ProjectRepoVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectRepoController {

    private final ProjectRepoService projectRepoService;

    public ProjectRepoController(ProjectRepoService projectRepoService) {
        this.projectRepoService = projectRepoService;
    }

    @PostMapping
    public Result<ProjectRepoVO> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success("Project repository created", projectRepoService.create(request));
    }

    @GetMapping
    public Result<List<ProjectRepoVO>> list() {
        return Result.success(projectRepoService.listCurrentUserRepos());
    }

    @GetMapping("/{id}")
    public Result<ProjectRepoVO> detail(@PathVariable Long id) {
        return Result.success(projectRepoService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectRepoService.delete(id);
        return Result.success("Project repository deleted", null);
    }
}
