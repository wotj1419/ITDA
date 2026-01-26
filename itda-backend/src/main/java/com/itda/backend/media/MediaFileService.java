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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final com.itda.backend.asset.config.S3StorageProperties s3Properties;

    public MediaFile loadNodeContent(Long userId, Long nodeId) {
        Node node = nodeMapper.findById(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
        Scene scene = sceneMapper.findById(node.getSceneId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);

        String rawContentUrl = node.getContentUrl();
        if (isAbsoluteUrl(rawContentUrl)) {
            return toRemoteMediaFile(rawContentUrl);
        }

        String contentKey = resolveNodeContentKey(node);
        MediaFile s3MediaFile = tryLoadFromS3(contentKey);
        if (s3MediaFile != null) {
            return s3MediaFile;
        }

        Path targetPath = resolveUnderUploadRoot(contentKey, ErrorCode.CONTENT_NOT_FOUND);
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

    private String resolveNodeContentKey(Node node) {
        String contentKey = normalizeContentKey(node.getContentUrl());
        if (contentKey == null && node.getStatus() == NodeStatus.SUCCEEDED) {
            contentKey = defaultNodeContentKey(node);
        }
        if (contentKey == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        return contentKey;
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

    private MediaFile toRemoteMediaFile(String url) {
        try {
            Resource resource = new UrlResource(url);
            return new MediaFile(resource, MediaType.APPLICATION_OCTET_STREAM, -1L, resolveFilename(url));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private MediaFile tryLoadFromS3(String contentKey) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null || !hasText(s3Properties.getBucket())) {
            return null;
        }

        String bucket = s3Properties.getBucket().trim();
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(contentKey)
                    .build());

            ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(contentKey)
                            .build()
            );

            MediaType mediaType = resolveMediaTypeFromContentType(head.contentType());
            long contentLength = head.contentLength();
            String filename = resolveFilename(contentKey);
            return new MediaFile(new InputStreamResource(stream), mediaType, contentLength, filename);
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
            }
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

    private MediaType resolveMediaTypeFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean isAbsoluteUrl(String contentUrl) {
        if (contentUrl == null) {
            return false;
        }
        return contentUrl.startsWith("http://") || contentUrl.startsWith("https://");
    }

    private String resolveFilename(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return "content";
        }
        String normalized = pathOrUrl;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
