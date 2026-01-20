package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Projects", description = "Project APIs")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateProjectRequest request) {
        ProjectCreateResponse response = projectService.createProject(userDetails.getUserId(), request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List projects")
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> listProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProjectListResponse response = projectService.listProjects(userDetails.getUserId(), page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get project detail")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectDetailResponse response = projectService.getProjectDetail(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }
}
