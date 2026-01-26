package com.itda.backend.media;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.project.service.ProjectAccessService;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 미디어 파일 로드 서비스
 */
@Service
@RequiredArgsConstructor
public class MediaFileService {

    private static final String FILES_PREFIX = "/files/";

    private final NodeMapper nodeMapper;
    private final SceneMapper sceneMapper;
    private final ProjectAccessService projectAccessService;
    private final FileStorageProperties fileStorageProperties;

    public MediaFile loadNodeContent(Long userId, Long nodeId) {
        Node node = nodeMapper.findById(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
        Scene scene = sceneMapper.findById(node.getSceneId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);

        Path targetPath = resolveNodeContentPath(node);
        return toMediaFile(targetPath);
    }

    public MediaFile loadProjectExport(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        Path targetPath = resolveProjectExportPath(projectId);
        return toMediaFile(targetPath);
    }

    public MediaFile loadSceneExport(Long userId, Long sceneId) {
        Scene scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);
        Path targetPath = resolveSceneExportPath(sceneId);
        return toMediaFile(targetPath);
    }

    private Path resolveNodeContentPath(Node node) {
        String contentKey = normalizeContentKey(node.getContentUrl());
        if (contentKey == null && node.getStatus() == NodeStatus.SUCCEEDED) {
            contentKey = defaultNodeContentKey(node);
        }
        if (contentKey == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        return resolveUnderUploadRoot(contentKey, ErrorCode.CONTENT_NOT_FOUND);
    }

    private Path resolveProjectExportPath(Long projectId) {
        String relativePath = "exports/" + projectId + "/final.mp4";
        return resolveUnderUploadRoot(relativePath, ErrorCode.EXPORT_NOT_FOUND);
    }

    private Path resolveSceneExportPath(Long sceneId) {
        String relativePath = "exports/scenes/" + sceneId + "/final.mp4";
        return resolveUnderUploadRoot(relativePath, ErrorCode.EXPORT_NOT_FOUND);
    }

    private Path resolveUnderUploadRoot(String relativePath, ErrorCode notFoundCode) {
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new BusinessException(notFoundCode);
        }
        return target;
    }

    private String normalizeContentKey(String contentUrl) {
        if (contentUrl == null) {
            return null;
        }
        String trimmed = contentUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return null;
        }
        if (trimmed.startsWith(FILES_PREFIX)) {
            return trimmed.substring(FILES_PREFIX.length());
        }
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private String defaultNodeContentKey(Node node) {
        if (node.getId() == null) {
            return null;
        }
        boolean isVideo = node.getNodeType() == NodeType.VIDEO;
        String extension = isVideo ? "mp4" : "png";
        String folder = isVideo ? "ai/videos" : "ai/images";
        return folder + "/node-" + node.getId() + "." + extension;
    }

    private MediaFile toMediaFile(Path path) {
        try {
            Resource resource = new UrlResource(path.toUri());
            MediaType mediaType = resolveMediaType(path);
            long contentLength = Files.size(path);
            String filename = path.getFileName().toString();
            return new MediaFile(resource, mediaType, contentLength, filename);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private MediaType resolveMediaType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (contentType == null || contentType.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return MediaType.parseMediaType(contentType);
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
