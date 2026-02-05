package com.itda.backend.project.service;

import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.controller.dto.response.ProjectSummaryResponse;
import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.project.repository.dto.ProjectPreviewCandidate;
import com.itda.backend.project.repository.dto.ProjectSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_PAGE_SIZE = 100;
    private static final String PREVIEW_TYPE_PROJECT_MERGE = "PROJECT_MERGE";
    private static final String PREVIEW_TYPE_SCENE_MERGE = "SCENE_MERGE";
    private static final String PREVIEW_TYPE_CLIP = "CLIP";
    private static final PreviewPayload EMPTY_PREVIEW = new PreviewPayload(null, null, null);

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final AssetUrlResolver assetUrlResolver;

    @Transactional
    public ProjectCreateResponse createProject(Long userId, CreateProjectRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Project project = buildProject(userId, request, now);

        projectMapper.insertProject(project);
        addOwnerMember(project.getId(), userId);

        return toCreateResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectListResponse listProjects(Long userId, int page, int size) {
        validatePaging(page, size);
        int total = projectMapper.countByUserId(userId);
        if (total == 0) {
            return emptyProjectList(page, size, total);
        }
        int offset = safeOffset(page, size);
        if (isOutOfRange(offset, total)) {
            return emptyProjectList(page, size, total);
        }

        List<ProjectSummaryResponse> items = fetchProjectSummaries(userId, size, offset);

        return new ProjectListResponse(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long userId, Long projectId) {
        Project project = requireProject(projectId);
        String role = requireMemberRole(projectId, userId);
        Integer memberCount = countMembers(projectId);
        return ProjectDetailResponse.from(project, role, memberCount);
    }

    @Transactional
    public ProjectDetailResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        requireProject(projectId);
        String role = requireOwnerRole(projectId, userId);

        applyUpdate(projectId, request);

        Project updatedProject = requireProject(projectId);
        Integer memberCount = countMembers(projectId);
        return ProjectDetailResponse.from(updatedProject, role, memberCount);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        requireProject(projectId);
        requireOwnerRole(projectId, userId);

        int deleted = projectMapper.deleteProject(projectId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private Project buildProject(Long userId, CreateProjectRequest request, LocalDateTime now) {
        return Project.builder()
                .ownerId(userId)
                .title(request.title())
                .description(request.description())
                .genre(request.genre())
                .status(STATUS_ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void addOwnerMember(Long projectId, Long userId) {
        projectMemberMapper.insertMember(projectId, userId, ROLE_OWNER);
    }

    private Integer countMembers(Long projectId) {
        return projectMemberMapper.countByProjectId(projectId);
    }

    private ProjectCreateResponse toCreateResponse(Project project) {
        return new ProjectCreateResponse(
                project.getId(),
                project.getTitle(),
                ROLE_OWNER,
                project.getCreatedAt()
        );
    }

    private void applyUpdate(Long projectId, UpdateProjectRequest request) {
        projectMapper.updateProject(
                projectId,
                request.title(),
                request.description(),
                request.genre()
        );
    }

    private List<ProjectSummaryResponse> fetchProjectSummaries(Long userId, int size, int offset) {
        return projectMapper.findAllByUserId(userId, size, offset).stream()
                .map(this::toProjectSummaryResponse)
                .toList();
    }

    private ProjectSummaryResponse toProjectSummaryResponse(ProjectSummary summary) {
        PreviewPayload preview = resolvePreview(summary.getProjectId());
        return ProjectSummaryResponse.from(
                summary,
                preview.type(),
                preview.thumbnailUrl(),
                preview.videoUrl()
        );
    }

    private PreviewPayload resolvePreview(Long projectId) {
        List<ResolvedPreviewCandidate> candidates = new ArrayList<>();
        resolvePreviewCandidate(projectMapper.findProjectMergePreview(projectId), PREVIEW_TYPE_PROJECT_MERGE)
                .ifPresent(candidates::add);
        resolvePreviewCandidate(projectMapper.findSceneMergePreview(projectId), PREVIEW_TYPE_SCENE_MERGE)
                .ifPresent(candidates::add);
        resolvePreviewCandidate(projectMapper.findClipPreview(projectId), PREVIEW_TYPE_CLIP)
                .ifPresent(candidates::add);

        if (candidates.isEmpty()) {
            return EMPTY_PREVIEW;
        }

        ResolvedPreviewCandidate primary = candidates.stream()
                .filter(candidate -> !isBlank(candidate.videoUrl()))
                .findFirst()
                .orElse(candidates.get(0));

        String thumbnailUrl = firstNonBlank(primary.thumbnailUrl(), candidates);
        return new PreviewPayload(primary.type(), thumbnailUrl, primary.videoUrl());
    }

    private Optional<ResolvedPreviewCandidate> resolvePreviewCandidate(
            Optional<ProjectPreviewCandidate> candidate,
            String type
    ) {
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        ProjectPreviewCandidate value = candidate.get();
        String thumbnailUrl = assetUrlResolver.resolvePublicUrl(
                value.getThumbnailAssetId(),
                value.getThumbnailFallbackUrl()
        );
        String videoUrl = assetUrlResolver.resolvePublicUrl(
                value.getVideoAssetId(),
                value.getVideoFallbackUrl()
        );

        if (thumbnailUrl == null && videoUrl == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedPreviewCandidate(type, thumbnailUrl, videoUrl));
    }

    private String firstNonBlank(String primaryThumbnail, List<ResolvedPreviewCandidate> candidates) {
        if (!isBlank(primaryThumbnail)) {
            return primaryThumbnail;
        }
        return candidates.stream()
                .map(ResolvedPreviewCandidate::thumbnailUrl)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ProjectListResponse emptyProjectList(int page, int size, int total) {
        return new ProjectListResponse(List.of(), page, size, total);
    }

    private boolean isOutOfRange(int offset, int total) {
        return offset >= total;
    }

    private Project requireProject(Long projectId) {
        return projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private String requireMemberRole(Long projectId, Long userId) {
        return projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private String requireOwnerRole(Long projectId, Long userId) {
        String role = requireMemberRole(projectId, userId);
        if (!ROLE_OWNER.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return role;
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private int safeOffset(int page, int size) {
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return (int) offset;
    }

    private record PreviewPayload(String type, String thumbnailUrl, String videoUrl) {
    }

    private record ResolvedPreviewCandidate(String type, String thumbnailUrl, String videoUrl) {
    }
}
