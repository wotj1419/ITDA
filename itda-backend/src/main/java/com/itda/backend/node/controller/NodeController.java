package com.itda.backend.node.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.node.controller.dto.request.CreateNodeRequest;
import com.itda.backend.node.controller.dto.request.GenerateNodeJobRequest;
import com.itda.backend.node.controller.dto.request.UpdateNodePositionsRequest;
import com.itda.backend.node.controller.dto.request.UpdateNodeRequest;
import com.itda.backend.node.controller.dto.response.NodeCreateResponse;
import com.itda.backend.node.controller.dto.response.NodeDetailResponse;
import com.itda.backend.node.controller.dto.response.NodeTreeResponse;
import com.itda.backend.node.controller.dto.response.GenerateNodeJobResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.node.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Node REST Controller
 * 노드 CRUD 및 위치 관리 API
 */
@Tag(name = "Nodes", description = "Node & Canvas APIs")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @Operation(summary = "Create node", description = "Create a new node in the scene canvas")
    @PostMapping("/scenes/{sceneId}/nodes")
    public ResponseEntity<ApiResponse<NodeCreateResponse>> createNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId,
            @Valid @RequestBody CreateNodeRequest request) {
        NodeCreateResponse response = nodeService.createNode(userDetails.getUserId(), sceneId, request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List nodes", description = "Get all nodes in a scene (including virtual SCENE_HEADER)")
    @GetMapping("/scenes/{sceneId}/nodes")
    public ResponseEntity<ApiResponse<NodeTreeResponse>> listNodes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId) {
        NodeTreeResponse response = nodeService.listNodes(userDetails.getUserId(), sceneId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get node detail", description = "Get detailed information of a specific node")
    @GetMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<NodeDetailResponse>> getNodeDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        NodeDetailResponse response = nodeService.getNodeDetail(userDetails.getUserId(), id);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Update node", description = "Update node prompt and settings")
    @PutMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Void>> updateNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateNodeRequest request) {
        nodeService.updateNode(userDetails.getUserId(), id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Delete node", description = "Delete a node and its children (cascade)")
    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        nodeService.deleteNode(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Activate master", description = "Set a master node as active (one per scene)")
    @PostMapping("/nodes/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateMaster(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        nodeService.setActiveMaster(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Confirm video", description = "Confirm a video node (one per shot)")
    @PostMapping("/nodes/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        nodeService.confirmVideo(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Unconfirm video", description = "Unconfirm a video node")
    @DeleteMapping("/nodes/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> unconfirmVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        nodeService.unconfirmVideo(userDetails.getUserId(), id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Update node positions", description = "Bulk update node positions (SCENE_HEADER ignored)")
    @PutMapping("/scenes/{sceneId}/nodes/positions")
    public ResponseEntity<ApiResponse<Void>> updatePositions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sceneId,
            @Valid @RequestBody UpdateNodePositionsRequest request) {
        nodeService.updatePositions(userDetails.getUserId(), sceneId, request.positions());
        return ApiResponse.success(null);
    }

    @Operation(summary = "Generate node output", description = "Enqueue image/video generation job for the node")
    @PostMapping("/nodes/{id}/generate")
    public ResponseEntity<ApiResponse<GenerateNodeJobResponse>> generateNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) GenerateNodeJobRequest request) {
        Job job = nodeService.enqueueGenerateJob(userDetails.getUserId(), id, request);
        GenerateNodeJobResponse response = GenerateNodeJobResponse.from(job, id);
        return ApiResponse.accepted(response);
    }
}
