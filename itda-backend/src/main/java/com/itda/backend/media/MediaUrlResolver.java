package com.itda.backend.media;

import com.itda.backend.node.domain.Node;
import org.springframework.stereotype.Component;

/**
 * 미디어 접근 URL 생성기
 */
@Component
public class MediaUrlResolver {

    public String nodeContentUrl(Node node) {
        if (node == null || node.getId() == null || node.getContentUrl() == null) {
            return null;
        }
        return "/api/nodes/" + node.getId() + "/content";
    }

    public String nodeContentUrl(Long nodeId, String contentUrl) {
        if (nodeId == null || contentUrl == null) {
            return null;
        }
        return "/api/nodes/" + nodeId + "/content";
    }

    public String projectExportUrl(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return "/api/projects/" + projectId + "/export/file";
    }
}
