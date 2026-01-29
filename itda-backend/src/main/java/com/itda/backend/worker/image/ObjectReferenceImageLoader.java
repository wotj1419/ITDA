package com.itda.backend.worker.image;

import com.itda.backend.ai.gemini.ReferenceImage;
import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.object.domain.ObjectSheet;
import com.itda.backend.object.repository.ObjectMapper;
import com.itda.backend.worker.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectReferenceImageLoader {

    private static final int MAX_REFERENCE_COUNT = 3;
    private static final long MAX_REFERENCE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final ObjectMapper objectMapper;
    private final AssetMapper assetMapper;
    private final LocalFileStorage localFileStorage;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final S3StorageProperties s3Properties;

    public List<ReferenceImage> load(Long projectId, List<Long> referenceObjectIds) {
        if (referenceObjectIds == null || referenceObjectIds.isEmpty()) {
            return List.of();
        }
        Objects.requireNonNull(projectId, "projectId is required");

        List<Long> deduped = referenceObjectIds.stream()
                .distinct()
                .toList();
        if (deduped.size() > MAX_REFERENCE_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "referenceObjectIds exceeds limit");
        }

        List<ReferenceImage> images = new ArrayList<>(deduped.size());
        for (Long objectId : deduped) {
            if (objectId == null || objectId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid referenceObjectIds");
            }
            ObjectSheet objectSheet = objectMapper.findById(objectId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.OBJECT_NOT_FOUND, "Reference object not found"));
            if (!projectId.equals(objectSheet.getProjectId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Reference object not in project");
            }
            Long assetId = objectSheet.getSheetImageAssetId();
            if (assetId == null) {
                throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference object image missing");
            }
            Asset asset = assetMapper.findById(assetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference asset missing"));
            validateAsset(projectId, asset);
            Long sizeBytes = asset.getSizeBytes();
            if (sizeBytes != null && sizeBytes > MAX_REFERENCE_BYTES) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Reference image too large");
            }
            images.add(loadReferenceImage(asset));
        }

        return images;
    }

    private ReferenceImage loadReferenceImage(Asset asset) {
        String storageKey = requireStorageKey(asset);
        StorageProvider provider = requireStorageProvider(asset);
        String contentType = ensureSupportedContentType(normalizeContentType(asset.getContentType()));

        if (provider == StorageProvider.S3) {
            return loadFromS3(storageKey, contentType);
        }
        if (provider == StorageProvider.LOCAL) {
            return loadFromLocal(storageKey, contentType);
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unsupported storage provider");
    }

    private ReferenceImage loadFromLocal(String storageKey, String contentType) {
        try {
            long size = localFileStorage.size(storageKey);
            if (size > MAX_REFERENCE_BYTES) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Reference image too large");
            }
            if (size == 0) {
                throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image is empty");
            }
            byte[] bytes = localFileStorage.readBytes(storageKey);
            if (bytes.length == 0) {
                throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image is empty");
            }
            return new ReferenceImage(bytes, contentType);
        } catch (NoSuchFileException e) {
            log.warn("Reference image load failed (LOCAL). key={}, reason={}", storageKey, e.toString());
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image not found");
        } catch (IOException e) {
            log.warn("Reference image load failed (LOCAL). key={}, reason={}", storageKey, e.toString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Reference image load failed");
        }
    }

    private ReferenceImage loadFromS3(String storageKey, String contentType) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "S3 client not available");
        }
        String bucket = requireBucket();
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
            long contentLength = head.contentLength();
            if (contentLength > MAX_REFERENCE_BYTES) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Reference image too large");
            }
            if (contentLength == 0) {
                throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image is empty");
            }
            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build())) {
                byte[] bytes = stream.readAllBytes();
                if (bytes.length == 0) {
                    throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image is empty");
                }
                return new ReferenceImage(bytes, contentType);
            }
        } catch (S3Exception e) {
            log.warn("Reference image load failed (S3). key={}, status={}, reason={}",
                    storageKey,
                    e.statusCode(),
                    e.toString());
            throw mapS3Exception(e);
        } catch (IOException e) {
            log.warn("Reference image load failed (S3). key={}, reason={}", storageKey, e.toString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Reference image load failed");
        }
    }

    private String requireBucket() {
        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "storage.s3.bucket must be configured");
        }
        return bucket.trim();
    }

    private String requireStorageKey(Asset asset) {
        String storageKey = asset.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference storageKey missing");
        }
        return storageKey;
    }

    private StorageProvider requireStorageProvider(Asset asset) {
        StorageProvider provider = asset.getStorageProvider();
        if (provider == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Storage provider missing");
        }
        return provider;
    }

    private void validateAsset(Long projectId, Asset asset) {
        if (asset.getProjectId() == null || !asset.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reference asset not in project");
        }
        if (asset.getAssetType() != AssetType.IMAGE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Reference asset is not an image");
        }
    }

    private String ensureSupportedContentType(String contentType) {
        String normalized = contentType == null ? DEFAULT_CONTENT_TYPE : contentType.trim().toLowerCase();
        if (normalized.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        if (SUPPORTED_CONTENT_TYPES.contains(normalized)) {
            return normalized.equals("image/jpg") ? "image/jpeg" : normalized;
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "Reference image content type not supported");
    }

    private BusinessException mapS3Exception(S3Exception exception) {
        int status = exception.statusCode();
        if (status == 404) {
            return new BusinessException(ErrorCode.CONTENT_NOT_FOUND, "Reference image not found");
        }
        if (status == 403) {
            return new BusinessException(ErrorCode.FORBIDDEN, "Reference image access denied");
        }
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "Reference image storage error");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        String trimmed = contentType.trim().toLowerCase();
        int semicolon = trimmed.indexOf(';');
        return semicolon > 0 ? trimmed.substring(0, semicolon).trim() : trimmed;
    }
}
