package com.itda.backend.timeline.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.timeline.controller.dto.request.MergeRequest;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;
import com.itda.backend.timeline.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.controller.dto.response.SceneTimelineResponse;
import com.itda.backend.timeline.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Timeline", description = "Timeline APIs")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @Operation(summary = "Get scene timeline", description = "List confirmed video clips in a scene.")
    @GetMapping("/scenes/{sceneId}/timeline")
    public ResponseEntity<ApiResponse<SceneTimelineResponse>> getSceneTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId) {
        SceneTimelineResponse response = timelineService.getSceneTimeline(userDetails.getUserId(), sceneId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get project timeline", description = "List merged scene clips in a project.")
    @GetMapping("/timeline/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectTimelineResponse>> getProjectTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectTimelineResponse response = timelineService.getProjectTimeline(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Request scene merge", description = "Create a merge job for confirmed scene clips.")
    @PostMapping("/scenes/{sceneId}/merge")
    public ResponseEntity<ApiResponse<MergeResponse>> requestSceneMerge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId,
            @RequestBody(required = false) MergeRequest request) {
        MergeResponse response = timelineService.requestSceneMerge(userDetails.getUserId(), sceneId, request);
        return ApiResponse.accepted(response);
    }

    @Operation(summary = "Request project merge", description = "Create a merge job for project timeline clips.")
    @PostMapping("/projects/{projectId}/merge")
    public ResponseEntity<ApiResponse<MergeResponse>> requestProjectMerge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestBody(required = false) MergeRequest request) {
        MergeResponse response = timelineService.requestProjectMerge(userDetails.getUserId(), projectId, request);
        return ApiResponse.accepted(response);
    }
}
