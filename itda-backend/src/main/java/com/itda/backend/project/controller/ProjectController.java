package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.job.controller.dto.JobAcceptedResponse;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;
import com.itda.backend.media.MediaFile;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.timeline.controller.dto.request.ReorderProjectTimelineRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectExportResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.project.service.ProjectService;
import com.itda.backend.project.service.ProjectMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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

@Tag(name = "프로젝트", description = "프로젝트 생성/조회/수정/삭제 및 내보내기 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

        private final ProjectService projectService;
        private final ProjectMediaService projectMediaService;
        private final MediaFileService mediaFileService;

        @Operation(summary = "프로젝트 생성", description = "새 프로젝트를 생성하고 요청자를 프로젝트 소유자로 지정합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공", content = @Content(schema = @Schema(implementation = ProjectCreateResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping
        public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid @RequestBody CreateProjectRequest request) {
                ProjectCreateResponse response = projectService.createProject(userDetails.getUserId(), request);
                return ApiResponse.created(response);
        }

        @Operation(summary = "프로젝트 목록 조회", description = "요청자가 참여 중인 프로젝트 목록을 페이징하여 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ProjectListResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터가 올바르지 않음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @GetMapping
        public ResponseEntity<ApiResponse<ProjectListResponse>> listProjects(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "페이지(0부터)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
                ProjectListResponse response = projectService.listProjects(userDetails.getUserId(), page, size);
                return ApiResponse.success(response);
        }

        @Operation(summary = "프로젝트 상세 조회", description = "프로젝트의 상세 정보 및 메타데이터를 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ProjectDetailResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
        })
        @GetMapping("/{projectId}")
        public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                ProjectDetailResponse response = projectService.getProjectDetail(userDetails.getUserId(), projectId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "프로젝트 수정", description = "프로젝트 메타데이터를 수정합니다. (권한 필요)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = ProjectDetailResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
        })
        @PutMapping("/{projectId}")
        public ResponseEntity<ApiResponse<ProjectDetailResponse>> updateProject(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Valid @RequestBody UpdateProjectRequest request) {
                ProjectDetailResponse response = projectService.updateProject(
                                userDetails.getUserId(), projectId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다. (권한 필요)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
        })
        @DeleteMapping("/{projectId}")
        public ResponseEntity<ApiResponse<Void>> deleteProject(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                projectService.deleteProject(userDetails.getUserId(), projectId);
                return ApiResponse.success();
        }

        @Operation(summary = "프로젝트 타임라인 조회", description = "확정된 비디오 노드 기반의 프로젝트 타임라인을 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ProjectTimelineResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
        })
        @GetMapping("/{projectId}/timeline")
        public ResponseEntity<ApiResponse<ProjectTimelineResponse>> getTimeline(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                ProjectTimelineResponse response = projectMediaService.getTimeline(userDetails.getUserId(), projectId);
                return ApiResponse.success(response);
        }

        @PutMapping("/{projectId}/timeline/order")
        public ResponseEntity<ApiResponse<Void>> reorderProjectTimeline(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "Project ID") @PathVariable Long projectId,
                        @Valid @RequestBody ReorderProjectTimelineRequest request) {
                projectMediaService.reorderTimeline(userDetails.getUserId(), projectId, request.orderedVideoNodeIds());
                return ApiResponse.success();
        }

        @Operation(summary = "프로젝트 병합 요청", description = "프로젝트 병합 작업(Job)을 생성하고 큐에 등록합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "요청 수락", content = @Content(schema = @Schema(implementation = JobAcceptedResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
        })
        @PostMapping("/{projectId}/merge")
        public ResponseEntity<ApiResponse<JobAcceptedResponse>> mergeProject(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                MergeResponse result = projectMediaService.requestMerge(userDetails.getUserId(), projectId);
                if (result.cached()) {
                        // 캐시 히트: 동일 signature의 active 결과가 이미 존재
                        return ApiResponse.success(JobAcceptedResponse.cacheHit());
                }
                return ApiResponse.accepted(new JobAcceptedResponse(result.jobId(), result.status()));
        }

        @Operation(summary = "프로젝트 내보내기 URL 조회", description = "병합이 완료된 프로젝트의 내보내기 URL을 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ProjectExportResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 내보내기 파일을 찾을 수 없음")
        })
        @GetMapping("/{projectId}/export")
        public ResponseEntity<ApiResponse<ProjectExportResponse>> getExport(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                ProjectExportResponse response = projectMediaService.getExport(userDetails.getUserId(), projectId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "프로젝트 내보내기 파일 다운로드", description = "병합된 프로젝트 파일을 다운로드합니다. (실제 Content-Type은 서버에서 설정됩니다.)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "다운로드 성공", content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 파일을 찾을 수 없음")
        })
        @GetMapping("/{projectId}/export/file")
        public ResponseEntity<Resource> downloadExport(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                MediaFile mediaFile = mediaFileService.loadProjectExport(userDetails.getUserId(), projectId);
                ContentDisposition contentDisposition = ContentDisposition.attachment()
                                .filename(mediaFile.filename())
                                .build();
                return ResponseEntity.ok()
                                .contentType(mediaFile.mediaType())
                                .contentLength(mediaFile.contentLength())
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                contentDisposition.toString())
                                .body(mediaFile.resource());
        }
}
