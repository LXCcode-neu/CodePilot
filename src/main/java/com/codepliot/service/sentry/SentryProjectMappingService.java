package com.codepliot.service.sentry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.entity.SentryProjectMapping;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.SentryProjectMappingSaveRequest;
import com.codepliot.model.SentryProjectMappingVO;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.repository.SentryProjectMappingMapper;
import com.codepliot.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sentry 项目映射服务。
 * <p>
 * 管理 Sentry 项目与 CodePilot 项目仓库之间的映射关系，支持：
 * <ul>
 *   <li>查询指定项目的 Sentry 映射配置</li>
 *   <li>创建或更新 Sentry 项目映射（组织 slug、项目 slug、启用状态等）</li>
 *   <li>删除项目映射</li>
 *   <li>根据 Sentry 组织和项目 slug 查找已启用的映射</li>
 * </ul>
 * <p>
 * 映射关系用于将 Sentry 告警路由到正确的 CodePilot 项目进行自动修复。
 */
@Service
public class SentryProjectMappingService {

    private final SentryProjectMappingMapper sentryProjectMappingMapper;
    private final ProjectRepoMapper projectRepoMapper;

    public SentryProjectMappingService(SentryProjectMappingMapper sentryProjectMappingMapper,
                                       ProjectRepoMapper projectRepoMapper) {
        this.sentryProjectMappingMapper = sentryProjectMappingMapper;
        this.projectRepoMapper = projectRepoMapper;
    }

    /**
     * 获取指定项目的 Sentry 映射配置。
     *
     * @param projectId CodePilot 项目仓库 ID
     * @return 映射配置视图对象，若未配置则返回 null
     */
    public SentryProjectMappingVO getProjectMapping(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(projectId, currentUserId);
        return SentryProjectMappingVO.from(findByProjectId(projectId));
    }

    /**
     * 保存项目的 Sentry 映射配置（新增或更新）。
     *
     * @param projectId CodePilot 项目仓库 ID
     * @param request   映射配置保存请求
     * @return 保存后的映射配置视图对象
     */
    @Transactional
    public SentryProjectMappingVO saveProjectMapping(Long projectId, SentryProjectMappingSaveRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(projectId, currentUserId);
        SentryProjectMapping mapping = findByProjectId(projectId);
        if (mapping == null) {
            mapping = new SentryProjectMapping();
            mapping.setProjectId(projectId);
            mapping.setUserId(currentUserId);
        }
        mapping.setSentryOrganizationSlug(normalize(request.sentryOrganizationSlug()));
        mapping.setSentryProjectSlug(normalize(request.sentryProjectSlug()));
        mapping.setEnabled(request.enabled() == null || request.enabled());
        mapping.setAutoRunEnabled(request.autoRunEnabled() == null || request.autoRunEnabled());
        if (mapping.getId() == null) {
            sentryProjectMappingMapper.insert(mapping);
        } else {
            sentryProjectMappingMapper.updateById(mapping);
        }
        return SentryProjectMappingVO.from(mapping);
    }

    @Transactional
    public void deleteProjectMapping(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(projectId, currentUserId);
        deleteByProjectId(projectId);
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        sentryProjectMappingMapper.delete(new LambdaQueryWrapper<SentryProjectMapping>()
                .eq(SentryProjectMapping::getProjectId, projectId));
    }

    /**
     * 根据 Sentry 组织和项目 slug 查找已启用的映射配置。
     *
     * @param organizationSlug Sentry 组织标识
     * @param projectSlug      Sentry 项目标识
     * @return 匹配的映射实体，未找到则返回 null
     */
    public SentryProjectMapping findEnabledMapping(String organizationSlug, String projectSlug) {
        if (isBlank(projectSlug)) {
            return null;
        }
        LambdaQueryWrapper<SentryProjectMapping> wrapper = new LambdaQueryWrapper<SentryProjectMapping>()
                .eq(SentryProjectMapping::getSentryProjectSlug, normalize(projectSlug))
                .eq(SentryProjectMapping::getEnabled, true)
                .orderByDesc(SentryProjectMapping::getUpdatedAt)
                .last("limit 1");
        if (!isBlank(organizationSlug)) {
            wrapper.eq(SentryProjectMapping::getSentryOrganizationSlug, normalize(organizationSlug));
        }
        return sentryProjectMappingMapper.selectOne(wrapper);
    }

    private SentryProjectMapping findByProjectId(Long projectId) {
        return sentryProjectMappingMapper.selectOne(new LambdaQueryWrapper<SentryProjectMapping>()
                .eq(SentryProjectMapping::getProjectId, projectId)
                .last("limit 1"));
    }

    private void requireOwnedProject(Long projectId, Long currentUserId) {
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
