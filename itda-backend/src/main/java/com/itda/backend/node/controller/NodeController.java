package com.itda.backend.node.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.job.controller.dto.JobAcceptedResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.media.MediaFile;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.node.controller.dto.request.CreateNodeRequest;
import com.itda.backend.node.controller.dto.request.GenerateNodeRequest;
import com.itda.backend.node.controller.dto.request.UpdateNodePositionsRequest;
import com.itda.backend.node.controller.dto.request.UpdateNodeRequest;
import com.itda.backend.node.controller.dto.response.NodeCreateResponse;
import com.itda.backend.node.controller.dto.response.NodeDetailResponse;
import com.itda.backend.node.controller.dto.response.NodeTreeResponse;
import com.itda.backend.node.service.NodeService;
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
import org.springframework.web.bind.annotation.*;

/**
 * 노드/캔버스 API
 */
@Tag(name = "노드", description = "노드 CRUD 및 캔버스(위치) 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;
    private final MediaFileService mediaFileService;

    @Operation(
            summary = "노드 생성",
            description = "씬 캔버스에 새 노드를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = NodeCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "씬/프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "씬을 찾을 수 없음"
            )
    })
    @PostMapping("/scenes/{sceneId}/nodes")
    public ResponseEntity<ApiResponse<NodeCreateResponse>> createNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "씬 ID") @PathVariable Long sceneId,
            @Valid @RequestBody CreateNodeRequest request) {
        NodeCreateResponse response = nodeService.createNode(userDetails.getUserId(), sceneId, request);
        return ApiResponse.created(response);
    }

    @Operation(
            summary = "노드 목록 조회",
            description = "씬의 전체 노드를 조회합니다. (가상 노드인 SCENE_HEADER 포함)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NodeTreeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "씬/프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "씬을 찾을 수 없음"
            )
    })
    @GetMapping("/scenes/{sceneId}/nodes")
    public ResponseEntity<ApiResponse<NodeTreeResponse>> listNodes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "씬 ID") @PathVariable Long sceneId) {
        NodeTreeResponse response = nodeService.listNodes(userDetails.getUserId(), sceneId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "노드 상세 조회",
            description = "특정 노드의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NodeDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @GetMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<NodeDetailResponse>> getNodeDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        NodeDetailResponse response = nodeService.getNodeDetail(userDetails.getUserId(), id);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "노드 수정",
            description = "노드의 프롬프트/설정 등 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @PutMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Void>> updateNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id,
            @Valid @RequestBody UpdateNodeRequest request) {
        nodeService.updateNode(userDetails.getUserId(), id, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "노드 AI 생성 요청",
            description = "노드에 대해 AI 생성 작업(Job)을 요청합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "요청 수락",
                    content = @Content(schema = @Schema(implementation = JobAcceptedResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패 또는 요청 값이 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @PostMapping("/nodes/{id}/generate")
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> generateNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id,
            @Valid @RequestBody GenerateNodeRequest request) {
        Job job = nodeService.generateNode(userDetails.getUserId(), id, request);
        return ApiResponse.accepted(JobAcceptedResponse.from(job));
    }

    @Operation(
            summary = "노드 생성 결과 다운로드",
            description = "노드의 생성 결과(콘텐츠 파일)를 다운로드합니다. (실제 Content-Type은 서버에서 설정됩니다.)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "다운로드 성공",
                    content = @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드 또는 파일을 찾을 수 없음"
            )
    })
    @GetMapping("/nodes/{id}/content")
    public ResponseEntity<Resource> downloadNodeContent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        MediaFile mediaFile = mediaFileService.loadNodeContent(userDetails.getUserId(), id);
        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(mediaFile.filename())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaFile.mediaType())
                .contentLength(mediaFile.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString())
                .body(mediaFile.resource());
    }

    @Operation(
            summary = "노드 삭제",
            description = "노드 및 하위 노드를 함께 삭제합니다. (cascade)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        nodeService.deleteNode(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "마스터 노드 활성화",
            description = "마스터 노드를 활성화합니다. (씬당 1개)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @PostMapping("/nodes/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateMaster(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        nodeService.setActiveMaster(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "비디오 노드 확정",
            description = "비디오 노드를 확정합니다. (샷당 1개)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "노드 관계가 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드 또는 씬을 찾을 수 없음"
            )
    })
    @PostMapping("/nodes/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        nodeService.confirmVideo(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "비디오 노드 확정 해제",
            description = "비디오 노드 확정을 해제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "노드를 찾을 수 없음"
            )
    })
    @DeleteMapping("/nodes/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> unconfirmVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "노드 ID") @PathVariable Long id) {
        nodeService.unconfirmVideo(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "노드 위치 일괄 수정",
            description = "노드 위치를 일괄 수정합니다. (SCENE_HEADER는 무시됩니다.)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패 또는 요청 값이 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "씬/프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "씬을 찾을 수 없음"
            )
    })
    @PutMapping("/scenes/{sceneId}/nodes/positions")
    public ResponseEntity<ApiResponse<Void>> updatePositions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "씬 ID") @PathVariable Long sceneId,
            @Valid @RequestBody UpdateNodePositionsRequest request) {
        nodeService.updatePositions(userDetails.getUserId(), sceneId, request.positions());
        return ApiResponse.success(null);
    }
}
