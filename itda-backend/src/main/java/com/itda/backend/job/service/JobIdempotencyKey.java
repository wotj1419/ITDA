package com.itda.backend.job.service;

import com.itda.backend.job.domain.JobType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Idempotency Key 생성 유틸리티
 * <p>
 * 클라이언트가 Idempotency-Key 헤더를 제공하지 않은 경우,
 * 시스템에서 자동으로 키를 생성하여 중복 요청을 방지함.
 */
public final class JobIdempotencyKey {

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
        String requestHash = sha256Hex(requestJson == null ? "" : requestJson);
        return projectId + ":" + type.name() + ":" + target + ":" + requestHash;
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
