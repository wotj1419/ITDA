package com.itda.backend.job.service;

import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.domain.MergeSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Idempotency Key 생성 유틸리티
 * <p>
 * 클라이언트가 Idempotency-Key 헤더를 제공하지 않은 경우,
 * 시스템에서 자동으로 키를 생성하여 중복 요청을 방지함.
 * requestJson은 JSON 정규화 후 해시하여 키 안정성을 높임.
 */
public final class JobIdempotencyKey {

    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

    private JobIdempotencyKey() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 대상 + 요청 기반 Idempotency Key 생성
     * <p>
     * 형식: {projectId}:{jobType}:{target}:{requestHash}
     * - target: node:{nodeId} 또는 scene:{sceneId} 또는 project
     * - requestHash: requestJson의 SHA-256 해시 (UTF-8)
     *
     * @param projectId   프로젝트 ID
     * @param type        Job 타입
     * @param nodeId      노드 ID (nullable)
     * @param sceneId     씬 ID (nullable)
     * @param requestJson 요청 파라미터 JSON (nullable)
     * @return 생성된 Idempotency Key
     */
    public static String of(Long projectId, JobType type, Long nodeId, Long sceneId, String requestJson) {
        String target = resolveTarget(nodeId, sceneId);
        String normalizedRequest = normalizeRequestJson(requestJson);
        String requestHash = sha256Hex(normalizedRequest);
        return projectId + ":" + type.name() + ":" + target + ":" + requestHash;
    }

    /**
     * Merge 작업을 위한 Idempotency Key 생성
     * <p>
     * 형식: {projectId}:{jobType}:{mergeSource}:{mergeSignature}
     *
     * @param projectId      프로젝트 ID
     * @param type           Job 타입(SCENE_MERGE/PROJECT_MERGE)
     * @param mergeSource    MERGE 소스 (SCENE/PROJECT)
     * @param mergeSignature timeline_items 기반 다이제스트(해시)
     * @return 생성된 Idempotency Key
     */
    public static String forMerge(Long projectId, JobType type, MergeSource mergeSource, String mergeSignature) {
        if (mergeSignature == null || mergeSignature.isBlank()) {
            throw new IllegalArgumentException("mergeSignature is required");
        }
        if (mergeSource == null) {
            throw new IllegalArgumentException("mergeSource is required");
        }
        return projectId + ":" + type.name() + ":" + mergeSource.name() + ":" + mergeSignature.trim();
    }

    private static String resolveTarget(Long nodeId, Long sceneId) {
        if (nodeId != null) {
            return "node:" + nodeId;
        }
        if (sceneId != null) {
            return "scene:" + sceneId;
        }
        return "project";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String normalizeRequestJson(String requestJson) {
        if (requestJson == null) {
            return "";
        }
        String trimmed = requestJson.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            JsonNode node = CANONICAL_MAPPER.readTree(trimmed);
            return CANONICAL_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return trimmed;
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}
