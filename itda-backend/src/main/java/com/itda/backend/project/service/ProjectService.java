package com.itda.backend.project.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.controller.dto.response.ProjectSummaryResponse;
import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.project.repository.dto.ProjectSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    @Transactional
    public ProjectCreateResponse createProject(Long userId, CreateProjectRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Project project = Project.builder()
                .ownerId(userId)
                .title(request.title())
                .description(request.description())
                .genre(request.genre())
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        projectMapper.insertProject(project);
        projectMemberMapper.insertMember(project.getId(), userId, ROLE_OWNER);

        return new ProjectCreateResponse(
                project.getId(),
                project.getTitle(),
                ROLE_OWNER,
                project.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ProjectListResponse listProjects(Long userId, int page, int size) {
        List<ProjectSummary> summaries = projectMapper.findAllByUserId(userId);
        int total = summaries.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<ProjectSummaryResponse> items = summaries.subList(fromIndex, toIndex).stream()
                .map(ProjectSummaryResponse::from)
                .toList();

        return new ProjectListResponse(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long userId, Long projectId) {
        Project project = projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        String role = projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        return ProjectDetailResponse.from(project, role);
    }
}
