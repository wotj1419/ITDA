package com.itda.backend.media;

import com.itda.backend.node.domain.Node;
import org.springframework.stereotype.Component;

/**
 * 미디어 접근 URL 생성기
 */
@Component
public class MediaUrlResolver {

    private static final String NODE_CONTENT_PREFIX = "/api/nodes/";
    private static final String NODE_CONTENT_SUFFIX = "/content";
    private static final String PROJECT_EXPORT_PREFIX = "/api/projects/";
    private static final String PROJECT_EXPORT_SUFFIX = "/export/file";
    private static final String PROJECT_EXPORT_PREVIEW_SUFFIX = "/export/preview";
    private static final String SCENE_EXPORT_PREFIX = "/api/scenes/";
    private static final String SCENE_EXPORT_SUFFIX = "/export/file";
    private static final String SCENE_EXPORT_ITEM_SUFFIX = "/exports/";

    public String nodeContentUrl(Node node) {
        if (node == null) {
            return null;
        }
        return nodeContentUrl(node.getId(), node.getContentUrl());
    }

    public String nodeContentUrl(Long nodeId, String contentUrl) {
        if (isAbsoluteUrl(contentUrl)) {
            return contentUrl;
        }
        if (nodeId == null) {
            return null;
        }
        return buildNodeContentUrl(nodeId);
    }

    public String projectExportUrl(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return PROJECT_EXPORT_PREFIX + projectId + PROJECT_EXPORT_SUFFIX;
    }

    public String projectExportPreviewUrl(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return PROJECT_EXPORT_PREFIX + projectId + PROJECT_EXPORT_PREVIEW_SUFFIX;
    }

    public String sceneExportUrl(Long sceneId) {
        if (sceneId == null) {
            return null;
        }
        return SCENE_EXPORT_PREFIX + sceneId + SCENE_EXPORT_SUFFIX;
    }

    public String sceneExportFileUrl(Long sceneId, Long sceneVideoId) {
        if (sceneId == null || sceneVideoId == null) {
            return null;
        }
        return SCENE_EXPORT_PREFIX + sceneId + SCENE_EXPORT_ITEM_SUFFIX + sceneVideoId + "/file";
    }

    public String sceneExportPreviewUrl(Long sceneId, Long sceneVideoId) {
        if (sceneId == null || sceneVideoId == null) {
            return null;
        }
        return SCENE_EXPORT_PREFIX + sceneId + SCENE_EXPORT_ITEM_SUFFIX + sceneVideoId + "/preview";
    }

    private String buildNodeContentUrl(Long nodeId) {
        return NODE_CONTENT_PREFIX + nodeId + NODE_CONTENT_SUFFIX;
    }

    private boolean isAbsoluteUrl(String contentUrl) {
        if (contentUrl == null) {
            return false;
        }
        return contentUrl.startsWith("http://") || contentUrl.startsWith("https://");
    }
}
