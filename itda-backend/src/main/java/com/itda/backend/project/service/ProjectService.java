package com.itda.backend.project.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

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
        return ProjectDetailResponse.from(project, role);
    }

    @Transactional
    public ProjectDetailResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        requireProject(projectId);
        String role = requireOwnerRole(projectId, userId);

        applyUpdate(projectId, request);

        Project updatedProject = requireProject(projectId);
        return ProjectDetailResponse.from(updatedProject, role);
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
                .map(ProjectSummaryResponse::from)
                .toList();
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
}

