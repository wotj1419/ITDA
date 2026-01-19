package com.itda.backend.scene.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.scene.controller.dto.request.CreateSceneRequest;
import com.itda.backend.scene.controller.dto.response.SceneCreateResponse;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.controller.dto.response.SceneSummaryResponse;
import com.itda.backend.scene.service.SceneService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Scenes", description = "Scene APIs")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @Operation(summary = "Create scene")
    @PostMapping("/projects/{projectId}/scenes")
    public ResponseEntity<ApiResponse<SceneCreateResponse>> createScene(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateSceneRequest request) {
        SceneCreateResponse response = sceneService.createScene(userDetails.getUserId(), projectId, request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List scenes")
    @GetMapping("/projects/{projectId}/scenes")
    public ResponseEntity<ApiResponse<List<SceneSummaryResponse>>> listScenes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        List<SceneSummaryResponse> response = sceneService.listScenes(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get scene detail")
    @GetMapping("/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<SceneDetailResponse>> getSceneDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId) {
        SceneDetailResponse response = sceneService.getSceneDetail(userDetails.getUserId(), sceneId);
        return ApiResponse.success(response);
    }
}
