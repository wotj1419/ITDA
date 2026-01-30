package com.itda.backend.worker;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.node.repository.NodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeContentLoader {

    private static final String FILES_PREFIX = "/files/";
    private static final String DEFAULT_IMAGE_MIME = "image/png";
    private static final String DEFAULT_VIDEO_MIME = "video/mp4";

    private final NodeMapper nodeMapper;
    private final FileStorageProperties fileStorageProperties;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final S3StorageProperties s3Properties;

    public NodeContent loadNodeContent(Long nodeId) {
        if (nodeId == null) {
            throw new IllegalStateException("Node id is required to load content");
        }
        Node node = nodeMapper.findById(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node not found: nodeId=" + nodeId));
        return loadNodeContent(node);
    }

    public NodeContent loadNodeContent(Node node) {
        if (node == null || node.getId() == null) {
            throw new IllegalStateException("Node is required to load content");
        }
        if (node.getStatus() != NodeStatus.SUCCEEDED) {
            throw new IllegalStateException("Node not succeeded: nodeId=" + node.getId() + ", status=" + node.getStatus());
        }
        String contentKey = resolveNodeContentKey(node);
        NodeContent s3Content = tryLoadFromS3(contentKey);
        if (s3Content != null) {
            return s3Content;
        }
        return loadFromLocal(contentKey, node);
    }

    private NodeContent loadFromLocal(String contentKey, Node node) {
        Path targetPath = resolveUnderUploadRoot(contentKey);
        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            throw new IllegalStateException("Node content missing: nodeId=" + node.getId() + ", key=" + contentKey);
        }
        try {
            byte[] bytes = Files.readAllBytes(targetPath);
            String contentType = resolveContentType(targetPath, node);
            return new NodeContent(bytes, contentType, contentKey);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read node content: nodeId=" + node.getId(), e);
        }
    }

    private NodeContent tryLoadFromS3(String contentKey) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null || !hasText(s3Properties.getBucket())) {
            return null;
        }
        String bucket = s3Properties.getBucket().trim();
        try {
            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(contentKey)
                    .build())) {
                GetObjectResponse response = stream.response();
                byte[] bytes = stream.readAllBytes();
                String contentType = normalizeContentType(response == null ? null : response.contentType(), contentKey);
                return new NodeContent(bytes, contentType, contentKey);
            }
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            log.warn("[NodeContentLoader] S3 load failed: key={}, status={}", contentKey, e.statusCode());
            throw new IllegalStateException("Failed to load node content from S3: key=" + contentKey, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read node content from S3: key=" + contentKey, e);
        }
    }

    private String resolveNodeContentKey(Node node) {
        String contentKey = normalizeContentKey(node.getContentUrl());
        if (contentKey == null && node.getStatus() == NodeStatus.SUCCEEDED) {
            contentKey = defaultNodeContentKey(node);
        }
        if (contentKey == null) {
            throw new IllegalStateException("Node content missing: nodeId=" + node.getId());
        }
        return contentKey;
    }

    private Path resolveUnderUploadRoot(String relativePath) {
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Invalid content path");
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
        if (isAbsoluteUrl(trimmed)) {
            String absoluteKey = resolveKeyFromAbsoluteUrl(trimmed);
            if (absoluteKey == null) {
                throw new IllegalStateException("Remote contentUrl not supported in worker: " + trimmed);
            }
            return absoluteKey;
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
        Long nodeId = node.getId();
        if (nodeId == null) {
            return null;
        }
        boolean isVideo = node.getNodeType() == NodeType.VIDEO;
        String extension = isVideo ? "mp4" : "png";
        String folder = isVideo ? "ai/videos" : "ai/images";
        return folder + "/node-" + nodeId + "." + extension;
    }

    private String resolveContentType(Path path, Node node) {
        try {
            String contentType = Files.probeContentType(path);
            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }
        } catch (IOException e) {
            log.debug("[NodeContentLoader] Failed to probe content type: path={}", path, e);
        }
        return defaultContentType(node);
    }

    private String normalizeContentType(String contentType, String contentKey) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        if (contentKey == null) {
            return DEFAULT_IMAGE_MIME;
        }
        String lower = contentKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return DEFAULT_VIDEO_MIME;
        }
        return DEFAULT_IMAGE_MIME;
    }

    private String defaultContentType(Node node) {
        if (node != null && node.getNodeType() == NodeType.VIDEO) {
            return DEFAULT_VIDEO_MIME;
        }
        return DEFAULT_IMAGE_MIME;
    }

    private boolean isAbsoluteUrl(String contentUrl) {
        if (contentUrl == null) {
            return false;
        }
        return contentUrl.startsWith("http://") || contentUrl.startsWith("https://");
    }

    private String resolveKeyFromAbsoluteUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            if (path.startsWith(FILES_PREFIX)) {
                return path.substring(FILES_PREFIX.length());
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
