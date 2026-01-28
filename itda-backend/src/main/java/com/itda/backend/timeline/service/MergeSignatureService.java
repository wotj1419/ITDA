package com.itda.backend.timeline.service;

import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class MergeSignatureService {

    public String sceneSignature(Long sceneId, boolean includeMusic, List<SceneTimelineItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("scene:").append(sceneId).append('|')
                .append("includeMusic=").append(includeMusic).append('|');
        for (SceneTimelineItem item : items) {
            builder.append(item.getOrderIndex()).append(':')
                    .append(item.getVideoNodeId()).append(':')
                    .append(sourceKey(item.getAssetId(), item.getFallbackUrl()))
                    .append('|');
        }
        return sha256Hex(builder.toString());
    }

    public String projectSignature(Long projectId, boolean includeMusic, List<ProjectTimelineItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("project:").append(projectId).append('|')
                .append("includeMusic=").append(includeMusic).append('|');
        for (ProjectTimelineItem item : items) {
            builder.append(item.getOrderIndex()).append(':')
                    .append(item.getSceneId()).append(':')
                    .append(item.getSceneVideoId()).append(':')
                    .append(sourceKey(item.getAssetId(), item.getFallbackUrl()))
                    .append('|');
        }
        return sha256Hex(builder.toString());
    }

    private String sourceKey(Long assetId, String fallbackUrl) {
        if (assetId != null) {
            return "A:" + assetId;
        }
        return "U:" + (fallbackUrl == null ? "null" : fallbackUrl);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
