package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Projects", description = "Project APIs")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project", description = "새로운 프로젝트를 생성합니다. 생성자는 자동으로 Owner가 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectCreateResponse response = projectService.createProject(userDetails.getUserId(), request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List projects", description = "내가 참여 중인 프로젝트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> listProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProjectListResponse response = projectService.listProjects(userDetails.getUserId(), page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get project detail", description = "프로젝트 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectDetailResponse response = projectService.getProjectDetail(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Update project", description = "프로젝트 메타데이터를 수정합니다. Owner만 가능합니다.")
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> updateProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectDetailResponse response = projectService.updateProject(
                userDetails.getUserId(), projectId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Delete project", description = "프로젝트를 삭제합니다. Owner만 가능합니다.")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        projectService.deleteProject(userDetails.getUserId(), projectId);
        return ApiResponse.success();
    }
}

