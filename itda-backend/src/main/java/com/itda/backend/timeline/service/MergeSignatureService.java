package com.itda.backend.timeline.service;

import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MergeSignatureService {

    private final TimelineMapper timelineMapper;

    public String computeSceneSignature(Long sceneId) {
        return computeSceneSignature(sceneId, false);
    }

    public String computeSceneSignature(Long sceneId, boolean includeMusic) {
        List<SceneTimelineItem> items = timelineMapper.findSceneTimelineItems(sceneId);
        return computeSceneSignature(sceneId, includeMusic, items);
    }

    public String computeSceneSignature(Long sceneId, boolean includeMusic, List<SceneTimelineItem> items) {
        validateSceneItems(items);
        return buildSceneSignature(sceneId, includeMusic, items);
    }

    public String computeProjectSignature(Long projectId) {
        return computeProjectSignature(projectId, false);
    }

    public String computeProjectSignature(Long projectId, boolean includeMusic) {
        List<ProjectTimelineItem> items = timelineMapper.findProjectTimelineItems(projectId);
        return computeProjectSignature(projectId, includeMusic, items);
    }

    public String computeProjectSignature(Long projectId, boolean includeMusic, List<ProjectTimelineItem> items) {
        validateProjectItems(items);
        return buildProjectSignature(projectId, includeMusic, items);
    }

    private String buildSceneSignature(Long sceneId, boolean includeMusic, List<SceneTimelineItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("scene:").append(sceneId).append('|')
                .append("includeMusic=").append(includeMusic).append('|')
                .append("items=");
        for (int i = 0; i < items.size(); i++) {
            SceneTimelineItem item = items.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append(item.getSceneId()).append(':')
                    .append(item.getVideoNodeId()).append(':')
                    .append(item.getOrderIndex());
        }
        return sha256Hex(builder.toString());
    }

    private String buildProjectSignature(Long projectId, boolean includeMusic, List<ProjectTimelineItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("project:").append(projectId).append('|')
                .append("includeMusic=").append(includeMusic).append('|')
                .append("items=");
        for (int i = 0; i < items.size(); i++) {
            ProjectTimelineItem item = items.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append(item.getSceneId()).append(':')
                    .append(item.getSceneVideoId()).append(':')
                    .append(item.getOrderIndex());
        }
        return sha256Hex(builder.toString());
    }

    private void validateSceneItems(List<SceneTimelineItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        for (SceneTimelineItem item : items) {
            if (item.getOrderIndex() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    private void validateProjectItems(List<ProjectTimelineItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        for (ProjectTimelineItem item : items) {
            if (item.getOrderIndex() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
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
