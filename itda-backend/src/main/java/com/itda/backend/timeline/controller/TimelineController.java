package com.itda.backend.timeline.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.timeline.controller.dto.request.MergeRequest;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;

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

    @Operation(summary = "Request scene merge", description = "Create a merge job for confirmed scene clips.")
    @PostMapping("/scenes/{sceneId}/merge")
    public ResponseEntity<ApiResponse<MergeResponse>> requestSceneMerge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId,
            @RequestBody(required = false) MergeRequest request) {
        MergeResponse response = timelineService.requestSceneMerge(userDetails.getUserId(), sceneId, request);
        return ApiResponse.accepted(response);
    }
}
