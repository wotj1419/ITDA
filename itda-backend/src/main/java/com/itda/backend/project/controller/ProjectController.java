package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.job.controller.dto.JobAcceptedResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.media.MediaFile;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectExportResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.project.service.ProjectService;
import com.itda.backend.project.service.ProjectMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
    private final ProjectMediaService projectMediaService;
    private final MediaFileService mediaFileService;

    @Operation(summary = "Create project", description = "Create a new project and assign the requester as owner.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", ref = "#/components/responses/ProjectCreateSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectCreateResponse response = projectService.createProject(userDetails.getUserId(), request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List projects", description = "List projects the requester participates in.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/ProjectListSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> listProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProjectListResponse response = projectService.listProjects(userDetails.getUserId(), page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get project detail", description = "Get project details and metadata.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/ProjectDetailSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/ProjectNotFound")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectDetailResponse response = projectService.getProjectDetail(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Update project", description = "Update project metadata (owner only).")
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> updateProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectDetailResponse response = projectService.updateProject(
                userDetails.getUserId(), projectId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Delete project", description = "Delete a project (owner only).")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        projectService.deleteProject(userDetails.getUserId(), projectId);
        return ApiResponse.success();
    }

    @Operation(summary = "Get project timeline", description = "Get confirmed video timeline for a project.")
    @GetMapping("/{projectId}/timeline")
    public ResponseEntity<ApiResponse<ProjectTimelineResponse>> getTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectTimelineResponse response = projectMediaService.getTimeline(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Request project merge", description = "Create merge job for a project.")
    @PostMapping("/{projectId}/merge")
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> mergeProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        Job job = projectMediaService.requestMerge(userDetails.getUserId(), projectId);
        return ApiResponse.accepted(JobAcceptedResponse.from(job));
    }

    @Operation(summary = "Get project export", description = "Get export URL when merge is completed.")
    @GetMapping("/{projectId}/export")
    public ResponseEntity<ApiResponse<ProjectExportResponse>> getExport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectExportResponse response = projectMediaService.getExport(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Download project export", description = "Download merged project file.")
    @GetMapping("/{projectId}/export/file")
    public ResponseEntity<Resource> downloadExport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        MediaFile mediaFile = mediaFileService.loadProjectExport(userDetails.getUserId(), projectId);
        return ResponseEntity.ok()
                .contentType(mediaFile.mediaType())
                .contentLength(mediaFile.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + mediaFile.filename() + "\"")
                .body(mediaFile.resource());
    }
}

