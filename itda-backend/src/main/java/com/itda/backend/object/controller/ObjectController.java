package com.itda.backend.object.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.media.MediaFile;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.object.controller.dto.request.CreateObjectRequest;
import com.itda.backend.object.controller.dto.request.UpdateObjectRequest;
import com.itda.backend.object.controller.dto.response.ObjectSheetResponse;
import com.itda.backend.object.service.ObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "오브젝트", description = "오브젝트 시트 CRUD API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;
    private final MediaFileService mediaFileService;

    @Operation(
            summary = "오브젝트 생성",
            description = "프로젝트에 새 오브젝트 시트를 생성합니다. (이미지 파일 필수)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ObjectSheetResponse.class))
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
            )
    })
    @PostMapping(value = "/projects/{projectId}/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObjectSheetResponse>> createObject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(
                    description = "오브젝트 이미지 파일",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file,
            @ParameterObject @Valid @ModelAttribute CreateObjectRequest request
    ) {
        Long userId = userId(userDetails);
        return ApiResponse.created(objectService.createObject(userId, projectId, request, file));
    }

    @Operation(
            summary = "오브젝트 목록 조회",
            description = "프로젝트의 오브젝트 시트 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ObjectSheetResponse.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            )
    })
    @GetMapping("/projects/{projectId}/objects")
    public ResponseEntity<ApiResponse<List<ObjectSheetResponse>>> listObjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId
    ) {
        Long userId = userId(userDetails);
        return ApiResponse.success(objectService.listObjects(userId, projectId));
    }

    @Operation(
            summary = "오브젝트 상세 조회",
            description = "오브젝트 시트의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ObjectSheetResponse.class))
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
                    description = "오브젝트를 찾을 수 없음"
            )
    })
    @GetMapping("/objects/{objectId}")
    public ResponseEntity<ApiResponse<ObjectSheetResponse>> getObject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "오브젝트 ID") @PathVariable Long objectId
    ) {
        Long userId = userId(userDetails);
        return ApiResponse.success(objectService.getObject(userId, objectId));
    }

    @Operation(
            summary = "오브젝트 수정",
            description = "오브젝트 시트 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ObjectSheetResponse.class))
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
                    description = "오브젝트를 찾을 수 없음"
            )
    })
    @PutMapping("/objects/{objectId}")
    public ResponseEntity<ApiResponse<ObjectSheetResponse>> updateObject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "오브젝트 ID") @PathVariable Long objectId,
            @Valid @RequestBody UpdateObjectRequest request
    ) {
        Long userId = userId(userDetails);
        return ApiResponse.success(objectService.updateObject(userId, objectId, request));
    }

    @Operation(
            summary = "오브젝트 이미지 교체",
            description = "오브젝트 시트 이미지를 교체합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "교체 성공",
                    content = @Content(schema = @Schema(implementation = ObjectSheetResponse.class))
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
                    description = "오브젝트를 찾을 수 없음"
            )
    })
    @PatchMapping(value = "/objects/{objectId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObjectSheetResponse>> replaceObjectImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "오브젝트 ID") @PathVariable Long objectId,
            @Parameter(
                    description = "오브젝트 이미지 파일",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = userId(userDetails);
        return ApiResponse.success(objectService.replaceObjectImage(userId, objectId, file));
    }

    @Operation(
            summary = "오브젝트 이미지 조회",
            description = "오브젝트 시트 이미지를 다운로드합니다."
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
                    description = "오브젝트 또는 파일을 찾을 수 없음"
            )
    })
    @GetMapping("/objects/{objectId}/image")
    public ResponseEntity<Resource> downloadObjectImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "오브젝트 ID") @PathVariable Long objectId
    ) {
        MediaFile mediaFile = mediaFileService.loadObjectImage(userId(userDetails), objectId);
        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(mediaFile.filename())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaFile.mediaType())
                .contentLength(mediaFile.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(mediaFile.resource());
    }

    @Operation(
            summary = "오브젝트 삭제",
            description = "오브젝트 시트를 삭제합니다."
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
                    description = "오브젝트를 찾을 수 없음"
            )
    })
    @DeleteMapping("/objects/{objectId}")
    public ResponseEntity<ApiResponse<Void>> deleteObject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "오브젝트 ID") @PathVariable Long objectId
    ) {
        objectService.deleteObject(userId(userDetails), objectId);
        return ApiResponse.success();
    }

    private static Long userId(CustomUserDetails userDetails) {
        return userDetails.getUserId();
    }
}
