package com.itda.backend.scene.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.media.MediaFile;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.scene.controller.dto.request.CreateSceneRequest;
import com.itda.backend.scene.controller.dto.request.ReorderScenesRequest;
import com.itda.backend.scene.controller.dto.request.UpdateSceneRequest;
import com.itda.backend.scene.controller.dto.response.SceneCreateResponse;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.controller.dto.response.SceneExportResponse;
import com.itda.backend.scene.controller.dto.response.SceneSummaryResponse;
import com.itda.backend.scene.service.SceneMediaService;
import com.itda.backend.scene.service.SceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "씬", description = "씬 생성/조회/수정/삭제 및 내보내기 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SceneController {

        private final SceneService sceneService;
        private final SceneMediaService sceneMediaService;
        private final MediaFileService mediaFileService;

        @Operation(summary = "씬 생성", description = "프로젝트에 새 씬을 생성합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공", content = @Content(schema = @Schema(implementation = SceneCreateResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음")
        })
        @PostMapping("/projects/{projectId}/scenes")
        public ResponseEntity<ApiResponse<SceneCreateResponse>> createScene(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Valid @RequestBody CreateSceneRequest request) {
                Long userId = userId(userDetails);
                return ApiResponse.created(sceneService.createScene(userId, projectId, request));
        }

        @Operation(summary = "씬 목록 조회", description = "프로젝트의 씬 목록을 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SceneSummaryResponse.class)))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음")
        })
        @GetMapping("/projects/{projectId}/scenes")
        public ResponseEntity<ApiResponse<List<SceneSummaryResponse>>> listScenes(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId) {
                Long userId = userId(userDetails);
                return ApiResponse.success(sceneService.listScenes(userId, projectId));
        }

        @Operation(summary = "씬 상세 조회", description = "씬의 상세 정보를 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SceneDetailResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "씬을 찾을 수 없음")
        })
        @GetMapping("/scenes/{sceneId}")
        public ResponseEntity<ApiResponse<SceneDetailResponse>> getSceneDetail(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "씬 ID") @PathVariable Long sceneId) {
                Long userId = userId(userDetails);
                return ApiResponse.success(sceneService.getSceneDetail(userId, sceneId));
        }

        @Operation(summary = "씬 수정", description = "씬 제목/설명 및 연결된 객체 정보를 수정합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = SceneDetailResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "씬을 찾을 수 없음")
        })
        @PutMapping("/scenes/{sceneId}")
        public ResponseEntity<ApiResponse<SceneDetailResponse>> updateScene(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "씬 ID") @PathVariable Long sceneId,
                        @Valid @RequestBody UpdateSceneRequest request) {
                Long userId = userId(userDetails);
                return ApiResponse.success(sceneService.updateScene(userId, sceneId, request));
        }

        @Operation(summary = "씬 삭제", description = "씬 및 관련 노드를 삭제합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "씬을 찾을 수 없음")
        })
        @DeleteMapping("/scenes/{sceneId}")
        public ResponseEntity<ApiResponse<Void>> deleteScene(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "씬 ID") @PathVariable Long sceneId) {
                sceneService.deleteScene(userId(userDetails), sceneId);
                return ApiResponse.success();
        }

        @Operation(summary = "씬 순서 변경", description = "프로젝트 내 씬 순서를 변경합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음")
        })
        @PutMapping("/projects/{projectId}/scenes/order")
        public ResponseEntity<ApiResponse<Void>> reorderScenes(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Valid @RequestBody ReorderScenesRequest request) {
                sceneService.reorderScenes(userId(userDetails), projectId, request);
                return ApiResponse.success();
        }

        @Operation(summary = "Scene export info", description = "Get merged scene export info.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Export info", content = @Content(schema = @Schema(implementation = SceneExportResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Export not found")
        })
        @GetMapping("/scenes/{sceneId}/export")
        public ResponseEntity<ApiResponse<SceneExportResponse>> getSceneExport(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "Scene ID") @PathVariable Long sceneId) {
                SceneExportResponse response = sceneMediaService.getExport(userId(userDetails), sceneId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "Scene export download", description = "Download merged scene file. Content type is resolved by server.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Download success", content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Export file not found")
        })
        @GetMapping("/scenes/{sceneId}/export/file")
        public ResponseEntity<Resource> downloadSceneExport(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Parameter(description = "Scene ID") @PathVariable Long sceneId) {
                Long userId = userId(userDetails);
                MediaFile mediaFile = mediaFileService.loadSceneExport(userId, sceneId);
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

        private static Long userId(CustomUserDetails userDetails) {
                return userDetails.getUserId();
        }
}
