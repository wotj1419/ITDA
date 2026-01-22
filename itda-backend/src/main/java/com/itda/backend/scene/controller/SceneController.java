package com.itda.backend.scene.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.scene.controller.dto.request.CreateSceneRequest;
import com.itda.backend.scene.controller.dto.request.ReorderScenesRequest;
import com.itda.backend.scene.controller.dto.request.UpdateSceneRequest;
import com.itda.backend.scene.controller.dto.response.SceneCreateResponse;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.controller.dto.response.SceneSummaryResponse;
import com.itda.backend.scene.service.SceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Scenes", description = "Scene APIs")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @Operation(summary = "Create scene")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", ref = "#/components/responses/SceneCreateSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    @PostMapping("/projects/{projectId}/scenes")
    public ResponseEntity<ApiResponse<SceneCreateResponse>> createScene(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSceneRequest request) {
        SceneCreateResponse response = sceneService.createScene(userDetails.getUserId(), projectId, request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List scenes")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/SceneListSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    @GetMapping("/projects/{projectId}/scenes")
    public ResponseEntity<ApiResponse<List<SceneSummaryResponse>>> listScenes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        List<SceneSummaryResponse> response = sceneService.listScenes(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get scene detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/SceneDetailSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/SceneNotFound")
    })
    @GetMapping("/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<SceneDetailResponse>> getSceneDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId) {
        SceneDetailResponse response = sceneService.getSceneDetail(userDetails.getUserId(), sceneId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Update scene", description = "Update scene title/description and linked objects.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/SceneDetailSuccess"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/SceneNotFound")
    })
    @PutMapping("/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<SceneDetailResponse>> updateScene(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId,
            @Valid @RequestBody UpdateSceneRequest request) {
        SceneDetailResponse response = sceneService.updateScene(userDetails.getUserId(), sceneId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Delete scene", description = "Delete a scene and related nodes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/SceneNotFound")
    })
    @DeleteMapping("/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<Void>> deleteScene(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId) {
        sceneService.deleteScene(userDetails.getUserId(), sceneId);
        return ApiResponse.success();
    }

    @Operation(summary = "Reorder scenes", description = "Reorder scenes in a project.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ref = "#/components/responses/Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    @PutMapping("/projects/{projectId}/scenes/order")
    public ResponseEntity<ApiResponse<Void>> reorderScenes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderScenesRequest request) {
        sceneService.reorderScenes(userDetails.getUserId(), projectId, request);
        return ApiResponse.success();
    }
}
