package com.itda.backend.node.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.prompt.PromptRenderer;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobService;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.controller.dto.request.CreateNodeRequest;
import com.itda.backend.node.controller.dto.request.GenerateNodeRequest;
import com.itda.backend.node.controller.dto.request.NodePosition;
import com.itda.backend.node.controller.dto.request.UpdateNodeRequest;
import com.itda.backend.node.controller.dto.response.NodeCreateResponse;
import com.itda.backend.node.controller.dto.response.NodeDetailResponse;
import com.itda.backend.node.controller.dto.response.NodeSummaryResponse;
import com.itda.backend.node.controller.dto.response.NodeTreeResponse;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.node.generation.GenerationSettingsResolver;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Node 서비스
 * 노드 CRUD 및 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NodeService {

    private static final int MAX_MASTER_NODES_PER_SCENE = 3;
    private static final float SCENE_HEADER_POSITION_X = 0f;
    private static final float SCENE_HEADER_POSITION_Y = -200f;

    private final NodeMapper nodeMapper;
    private final SceneMapper sceneMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ObjectMapper objectMapper;
    private final JobService jobService;
    private final MediaUrlResolver mediaUrlResolver;
    private final PromptRenderer promptRenderer;
    private final GenerationSettingsResolver generationSettingsResolver;

    private record VideoShotIds(Long startShotNodeId, Long endShotNodeId) {}

    /**
     * 노드 생성
     */
    @Transactional
    public NodeCreateResponse createNode(Long userId, Long sceneId, CreateNodeRequest request) {
        getSceneAndEnsureMemberForUpdate(sceneId, userId);
        NodeType nodeType = requireNodeType(request.nodeType());

        VideoShotIds shotIds = validateCreateRequestAndExtractShots(
                nodeType,
                sceneId,
                request.parentNodeId(),
                request.settings()
        );

        Node node = buildNewNode(sceneId, userId, request, nodeType, shotIds);

        nodeMapper.insertNode(node);
        log.debug("Created node: id={}, type={}, sceneId={}", node.getId(), node.getNodeType(), sceneId);

        return new NodeCreateResponse(node.getId());
    }

    /**
     * 씬 노드 목록 조회 (scene_header 가상 노드 포함)
     */
    @Transactional(readOnly = true)
    public NodeTreeResponse listNodes(Long userId, Long sceneId) {
        Scene scene = getSceneAndEnsureMember(sceneId, userId);
        List<Node> nodes = nodeMapper.findAllBySceneId(sceneId);
        List<NodeSummaryResponse> responses = new ArrayList<>(nodes.size() + 1);

        // 가상 SCENE_HEADER 노드 추가
        responses.add(createSceneHeaderResponse(scene, sceneId));

        // 실제 노드들 변환
        for (Node node : nodes) {
            String contentUrl = mediaUrlResolver.nodeContentUrl(node);
            responses.add(NodeSummaryResponse.from(node, contentUrl));
        }

        return new NodeTreeResponse(responses);
    }

    /**
     * 노드 상세 조회
     */
    @Transactional(readOnly = true)
    public NodeDetailResponse getNodeDetail(Long userId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        Scene scene = getSceneAndEnsureMember(node.getSceneId(), userId);

        Map<String, Object> settings = deserializeSettings(node.getDataJson());
        String contentUrl = mediaUrlResolver.nodeContentUrl(node);
        return NodeDetailResponse.from(node, settings, contentUrl);
    }

    /**
     * 노드 수정
     */
    @Transactional
    public void updateNode(Long userId, Long nodeId, UpdateNodeRequest request) {
        Node node = getNodeOrThrow(nodeId);
        getSceneAndEnsureMember(node.getSceneId(), userId);
        NodeType nodeType = node.getNodeType();

        // SCENE_HEADER 수정 금지 (가상 노드이므로 DB에 없지만 방어적 체크)
        assertNotSceneHeader(nodeType);

        VideoShotIds shotIds = resolveUpdatedVideoShotIds(node, request);
        Node updatedNode = buildUpdatedNode(nodeId, node, request, shotIds);

        nodeMapper.updateNode(updatedNode);
        log.debug("Updated node: id={}", nodeId);
    }

    /**
     * 노드 삭제
     */
    @Transactional
    public void deleteNode(Long userId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        getSceneAndEnsureMember(node.getSceneId(), userId);

        // SCENE_HEADER 삭제 금지
        assertNotSceneHeader(node.getNodeType());

        // FK ON DELETE CASCADE로 하위 노드도 함께 삭제됨
        nodeMapper.deleteById(nodeId);
        log.debug("Deleted node: id={}", nodeId);
    }

    /**
     * 노드 위치 일괄 저장
     */
    @Transactional
    public void updatePositions(Long userId, Long sceneId, List<NodePosition> positions) {
        getSceneAndEnsureMember(sceneId, userId);

        List<NodePosition> validPositions = filterSceneHeaderPositions(positions);
        if (validPositions.isEmpty()) {
            return;
        }

        nodeMapper.updatePositions(sceneId, validPositions);
        log.debug("Updated {} node positions for sceneId={}", validPositions.size(), sceneId);
    }

    /**
     * Active master 설정 (씬 단위 1개)
     */
    @Transactional
    public void setActiveMaster(Long userId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        Scene scene = getSceneAndEnsureMemberForUpdate(node.getSceneId(), userId);

        assertMasterNode(node.getNodeType());

        nodeMapper.clearActiveMasterBySceneId(scene.getId());
        int updated = nodeMapper.setActiveMaster(nodeId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        log.debug("Set active master: nodeId={}, sceneId={}", nodeId, scene.getId());
    }

    /**
     * VIDEO 확정 (SHOT 당 1개)
     */
    @Transactional
    public void confirmVideo(Long userId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        Scene scene = getSceneAndEnsureMemberForUpdate(node.getSceneId(), userId);

        assertVideoNode(node.getNodeType());
        Long shotNodeId = node.getParentNodeId();
        if (shotNodeId == null) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }
        validateShotNodeReference(scene.getId(), shotNodeId);

        nodeMapper.clearConfirmedByShotId(shotNodeId);
        int updated = nodeMapper.setConfirmedVideo(nodeId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        log.debug("Confirmed video: nodeId={}, shotNodeId={}, sceneId={}", nodeId, shotNodeId, scene.getId());
    }

    /**
     * VIDEO 확정 해제
     */
    @Transactional
    public void unconfirmVideo(Long userId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        getSceneAndEnsureMemberForUpdate(node.getSceneId(), userId);

        assertVideoNode(node.getNodeType());
        nodeMapper.clearConfirmedVideo(nodeId);

        log.debug("Unconfirmed video: nodeId={}", nodeId);
    }

    /**
     * 노드 AI 생성 요청
     */
    @Transactional
    public Job generateNode(Long userId, Long nodeId, GenerateNodeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Node node = getNodeOrThrow(nodeId);
        Scene scene = getSceneAndEnsureMemberForUpdate(node.getSceneId(), userId);
        assertNotSceneHeader(node.getNodeType());

        String promptKo = requirePromptKo(request.prompt());
        Map<String, Object> existingSettings = deserializeSettings(node.getDataJson());
        Map<String, Object> activeMasterSettings = resolveActiveMasterSettings(scene, node);
        Map<String, Object> effectiveSettings = generationSettingsResolver.resolve(
                node.getNodeType(),
                existingSettings,
                request.settings(),
                activeMasterSettings
        );

        String promptEn = promptRenderer.render(node.getNodeType(), scene, promptKo, effectiveSettings);
        Map<String, Object> cachedSettings = new LinkedHashMap<>(effectiveSettings);
        cachedSettings.put("promptEn", promptEn);
        if (node.getNodeType() == NodeType.VIDEO) {
            cachedSettings.putIfAbsent("promptEnRewritten", "");
        }

        UpdateNodeRequest updateRequest = new UpdateNodeRequest(promptKo, cachedSettings);
        VideoShotIds shotIds = resolveUpdatedVideoShotIds(node, updateRequest);

        // Always persist promptKo + settings(promptEn cache) without touching status/contentUrl.
        nodeMapper.updateGenerationInputs(Node.builder()
                .id(nodeId)
                .prompt(promptKo)
                .dataJson(serializeSettings(cachedSettings))
                .startShotNodeId(shotIds.startShotNodeId())
                .endShotNodeId(shotIds.endShotNodeId())
                .build());

        JobType jobType = resolveJobType(node.getNodeType());
        String requestJson = buildGenerationRequestJson(promptEn, cachedSettings);
        String idempotencyKey = request.force()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey();
        boolean requeueIfExisting = !request.force()
                && Boolean.TRUE.equals(request.requeueIfExisting());

        Job job = jobService.createAndEnqueue(
                jobType,
                scene.getProjectId(),
                scene.getId(),
                node.getId(),
                requestJson,
                idempotencyKey,
                requeueIfExisting
        );

        if (job.isInProgress()) {
            NodeStatus targetStatus = job.isRunning()
                    ? NodeStatus.RUNNING
                    : NodeStatus.PENDING;
            Node updatedNode = buildGenerationNode(nodeId, node, updateRequest, shotIds, targetStatus);
            nodeMapper.updateNode(updatedNode);
        }
        return job;
    }

    // ========== Private Helper Methods ==========

    private Scene getSceneOrThrow(Long sceneId) {
        return sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
    }

    private Node getNodeOrThrow(Long nodeId) {
        return nodeMapper.findById(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
    }

    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Scene getSceneAndEnsureMember(Long sceneId, Long userId) {
        Scene scene = getSceneOrThrow(sceneId);
        ensureMember(scene.getProjectId(), userId);
        return scene;
    }

    private Scene getSceneAndEnsureMemberForUpdate(Long sceneId, Long userId) {
        Scene scene = sceneMapper.findByIdForUpdate(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        ensureMember(scene.getProjectId(), userId);
        return scene;
    }

    private NodeType requireNodeType(NodeType nodeType) {
        if (nodeType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return nodeType;
    }

    private void assertNotSceneHeader(NodeType nodeType) {
        if (nodeType == NodeType.SCENE_HEADER) {
            throw new BusinessException(ErrorCode.SCENE_HEADER_NOT_MODIFIABLE);
        }
    }

    private void assertMasterNode(NodeType nodeType) {
        if (nodeType != NodeType.MASTER) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void assertVideoNode(NodeType nodeType) {
        if (nodeType != NodeType.VIDEO) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateMasterLimit(NodeType nodeType, Long sceneId) {
        if (nodeType != NodeType.MASTER) {
            return;
        }
        int masterCount = nodeMapper.countMasterNodesBySceneId(sceneId);
        if (masterCount >= MAX_MASTER_NODES_PER_SCENE) {
            throw new BusinessException(ErrorCode.MASTER_NODE_LIMIT_EXCEEDED);
        }
    }

    private int nextOrderIndex(Long sceneId) {
        return nodeMapper.findMaxOrderIndex(sceneId) + 1;
    }

    /**
     * 연결 규칙 검증: master → grid → shot → video
     */
    private void validateNodeRelation(NodeType nodeType, Long parentNodeId, Long sceneId) {
        if (nodeType == NodeType.MASTER) {
            // MASTER는 부모가 null (루트 노드)
            if (parentNodeId != null) {
                throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
            }
            return;
        }

        if (parentNodeId == null) {
            // MASTER 외에는 부모 필수
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }

        Node parentNode = getNodeOrThrow(parentNodeId);
        if (!parentNode.getSceneId().equals(sceneId)) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }
        NodeType parentType = parentNode.getNodeType();

        boolean valid = switch (nodeType) {
            case GRID -> parentType == NodeType.MASTER;
            case SHOT -> parentType == NodeType.GRID;
            case VIDEO -> parentType == NodeType.SHOT;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }
    }

    /**
     * 가상 SCENE_HEADER 노드 생성
     */
    private NodeSummaryResponse createSceneHeaderResponse(Scene scene, Long sceneId) {
        return new NodeSummaryResponse(
                -sceneId,  // 음수 ID 규칙
                NodeType.SCENE_HEADER,
                scene.getTitle(),
                scene.getDescription(),
                null,  // 부모 없음
                null,  // 상태 없음
                null,
                null,
                null,
                new NodeSummaryResponse.PositionDto(SCENE_HEADER_POSITION_X, SCENE_HEADER_POSITION_Y)
        );
    }

    private void validateShotNodeReference(Long sceneId, Long nodeId) {
        Node node = getNodeOrThrow(nodeId);
        if (!node.getSceneId().equals(sceneId) || node.getNodeType() != NodeType.SHOT) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }
    }

    private VideoShotIds validateCreateRequestAndExtractShots(
            NodeType nodeType,
            Long sceneId,
            Long parentNodeId,
            Map<String, Object> settings
    ) {
        assertNotSceneHeader(nodeType);
        validateMasterLimit(nodeType, sceneId);
        validateNodeRelation(nodeType, parentNodeId, sceneId);

        return validateAndExtractVideoShotIdsIfVideo(
                nodeType,
                parentNodeId,
                sceneId,
                settings
        );
    }

    private Node buildNewNode(
            Long sceneId,
            Long userId,
            CreateNodeRequest request,
            NodeType nodeType,
            VideoShotIds shotIds
    ) {
        return Node.builder()
                .sceneId(sceneId)
                .nodeType(nodeType)
                .parentNodeId(request.parentNodeId())
                .orderIndex(nextOrderIndex(sceneId))
                .prompt(request.prompt())
                .dataJson(serializeSettings(request.settings()))
                .status(NodeStatus.PENDING)
                .isActive(false)
                .isConfirmed(false)
                .startShotNodeId(shotIds.startShotNodeId())
                .endShotNodeId(shotIds.endShotNodeId())
                .createdBy(userId)
                .build();
    }

    private VideoShotIds resolveUpdatedVideoShotIds(Node node, UpdateNodeRequest request) {
        if (node.getNodeType() != NodeType.VIDEO || request.settings() == null) {
            return new VideoShotIds(node.getStartShotNodeId(), node.getEndShotNodeId());
        }

        return validateAndExtractVideoShotIdsIfVideo(
                node.getNodeType(),
                node.getParentNodeId(),
                node.getSceneId(),
                request.settings()
        );
    }

    private Node buildUpdatedNode(Long nodeId, Node node, UpdateNodeRequest request, VideoShotIds shotIds) {
        return Node.builder()
                .id(nodeId)
                .prompt(resolvePrompt(request, node))
                .dataJson(resolveDataJson(request, node))
                .status(node.getStatus())
                .isActive(node.getIsActive())
                .isConfirmed(node.getIsConfirmed())
                .contentUrl(node.getContentUrl())
                .startShotNodeId(shotIds.startShotNodeId())
                .endShotNodeId(shotIds.endShotNodeId())
                .build();
    }

    private Node buildGenerationNode(Long nodeId, Node node, UpdateNodeRequest request, VideoShotIds shotIds, NodeStatus status) {
        return Node.builder()
                .id(nodeId)
                .prompt(resolvePrompt(request, node))
                .dataJson(resolveDataJson(request, node))
                .status(status)
                .isActive(node.getIsActive())
                .isConfirmed(node.getIsConfirmed())
                .contentUrl(null)
                .startShotNodeId(shotIds.startShotNodeId())
                .endShotNodeId(shotIds.endShotNodeId())
                .build();
    }

    private String resolvePrompt(UpdateNodeRequest request, Node node) {
        return request.prompt() != null ? request.prompt() : node.getPrompt();
    }

    private String resolveDataJson(UpdateNodeRequest request, Node node) {
        if (request.settings() == null) {
            return node.getDataJson();
        }
        Map<String, Object> settings = normalizeMasterObjectIds(node, request.settings());
        return serializeSettings(settings);
    }

    private JobType resolveJobType(NodeType nodeType) {
        return nodeType == NodeType.VIDEO ? JobType.VIDEO_GENERATION : JobType.IMAGE_GENERATION;
    }

    private String buildGenerationRequestJson(String promptEn, Map<String, Object> settings) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("prompt", promptEn);
            payload.put("settings", settings);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private List<NodePosition> filterSceneHeaderPositions(List<NodePosition> positions) {
        // SCENE_HEADER 위치 변경 요청 필터링 (음수 ID 규칙)
        return positions.stream()
                .filter(p -> p.nodeId() > 0)
                .toList();
    }

    private VideoShotIds validateAndExtractVideoShotIdsIfVideo(
            NodeType nodeType,
            Long parentNodeId,
            Long sceneId,
            Map<String, Object> settings
    ) {
        if (nodeType != NodeType.VIDEO) {
            return new VideoShotIds(null, null);
        }
        if (settings == null) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }

        Long startShotNodeId = extractLongFromSettings(settings, "startShotNodeId");
        Long endShotNodeId = extractLongFromSettings(settings, "endShotNodeId");

        if (startShotNodeId == null) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }
        if (!startShotNodeId.equals(parentNodeId)) {
            throw new BusinessException(ErrorCode.INVALID_NODE_RELATION);
        }

        validateShotNodeReference(sceneId, startShotNodeId);
        if (endShotNodeId != null) {
            validateShotNodeReference(sceneId, endShotNodeId);
        }

        return new VideoShotIds(startShotNodeId, endShotNodeId);
    }

    private String serializeSettings(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize settings", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeSettings(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(dataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize settings", e);
            return Map.of();
        }
    }

    private Long extractLongFromSettings(Map<String, Object> settings, String key) {
        Object value = settings.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> normalizeMasterObjectIds(Node node, Map<String, Object> settings) {
        if (node.getNodeType() != NodeType.MASTER || settings == null) {
            return settings;
        }
        if (!settings.containsKey("objectIds") || settings.get("objectIds") != null) {
            return settings;
        }
        Map<String, Object> normalized = new java.util.LinkedHashMap<>(settings);
        normalized.put("objectIds", List.of());
        return normalized;
    }

    private String requirePromptKo(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty");
        }
        return prompt.trim();
    }

    private Map<String, Object> resolveActiveMasterSettings(Scene scene, Node node) {
        if (scene == null) {
            return Map.of();
        }
        if (node != null && node.getNodeType() == NodeType.MASTER) {
            return Map.of();
        }
        Long activeMasterNodeId = scene.getActiveMasterNodeId();
        if (activeMasterNodeId == null) {
            return Map.of();
        }
        Node master = nodeMapper.findById(activeMasterNodeId).orElse(null);
        if (master == null) {
            return Map.of();
        }
        return deserializeSettings(master.getDataJson());
    }
}
