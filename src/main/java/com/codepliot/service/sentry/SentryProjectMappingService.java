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

@Service
public class SentryProjectMappingService {

    private final SentryProjectMappingMapper sentryProjectMappingMapper;
    private final ProjectRepoMapper projectRepoMapper;

    public SentryProjectMappingService(SentryProjectMappingMapper sentryProjectMappingMapper,
                                       ProjectRepoMapper projectRepoMapper) {
        this.sentryProjectMappingMapper = sentryProjectMappingMapper;
        this.projectRepoMapper = projectRepoMapper;
    }

    public SentryProjectMappingVO getProjectMapping(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireOwnedProject(projectId, currentUserId);
        return SentryProjectMappingVO.from(findByProjectId(projectId));
    }

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
