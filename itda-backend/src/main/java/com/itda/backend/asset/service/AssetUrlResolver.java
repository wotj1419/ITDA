package com.itda.backend.asset.service;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.media.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AssetUrlResolver {

    private static final String FILES_PREFIX = "/files/";
    private static final int PRESIGNED_URL_CACHE_MAX_SIZE = 5000;
    private static final long PRESIGNED_URL_CACHE_FALLBACK_TTL_MILLIS = 5 * 60 * 1000L;
    private static final long PRESIGNED_URL_CACHE_SAFETY_WINDOW_MILLIS = 30 * 1000L;
    private static final String CACHE_CONTROL_IMAGE = "public, max-age=86400, stale-while-revalidate=604800";
    private static final String CACHE_CONTROL_VIDEO = "public, max-age=3600, stale-while-revalidate=86400";

    private final AssetMapper assetMapper;
    private final S3StorageProperties s3Properties;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final MediaUrlResolver mediaUrlResolver;
    private final Map<Long, CachedPresignedUrl> presignedUrlCache = new ConcurrentHashMap<>();

    public String resolveNodeUrl(Long assetId, Long nodeId, String fallbackUrl) {
        String presignedUrl = resolveS3PresignedUrl(assetId);
        if (presignedUrl != null) {
            return presignedUrl;
        }
        return mediaUrlResolver.nodeContentUrl(nodeId, fallbackUrl);
    }

    public String resolveUrl(Long assetId, String fallbackUrl) {
        String presignedUrl = resolveS3PresignedUrl(assetId);
        return presignedUrl != null ? presignedUrl : fallbackUrl;
    }

    public String resolvePublicUrl(Long assetId, String fallbackUrl) {
        String presignedUrl = resolveS3PresignedUrl(assetId);
        if (presignedUrl != null) {
            return presignedUrl;
        }

        String localUrl = resolveLocalUrl(assetId);
        if (localUrl != null) {
            return localUrl;
        }

        return normalizePublicFallback(fallbackUrl);
    }

    private String resolveS3PresignedUrl(Long assetId) {
        if (assetId == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        CachedPresignedUrl cached = presignedUrlCache.get(assetId);
        if (cached != null && cached.isValid(now)) {
            return cached.url();
        }

        Asset asset = assetMapper.findById(assetId).orElse(null);
        if (asset == null || asset.getStorageProvider() != StorageProvider.S3) {
            return null;
        }

        String storageKey = asset.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }

        S3Presigner presigner = s3PresignerProvider.getIfAvailable();
        if (presigner == null) {
            return null;
        }

        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            return null;
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket.trim())
                .key(storageKey)
                .responseCacheControl(resolveResponseCacheControl(asset.getContentType()))
                .build();

        long expires = Math.max(60, s3Properties.getPresignExpireSeconds());
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .getObjectRequest(getRequest)
                .build();

        String url = presigner.presignGetObject(presignRequest).url().toString();
        cachePresignedUrl(assetId, url, now, expires);
        return url;
    }

    private void cachePresignedUrl(Long assetId, String url, long nowMillis, long expiresSeconds) {
        if (assetId == null || url == null || url.isBlank()) {
            return;
        }
        if (presignedUrlCache.size() > PRESIGNED_URL_CACHE_MAX_SIZE) {
            cleanupExpiredCacheEntries(nowMillis);
        }

        long ttlMillis = expiresSeconds > 0
                ? Math.max(1000L, (expiresSeconds * 1000L) - PRESIGNED_URL_CACHE_SAFETY_WINDOW_MILLIS)
                : PRESIGNED_URL_CACHE_FALLBACK_TTL_MILLIS;
        long expiresAtMillis = nowMillis + ttlMillis;
        presignedUrlCache.put(assetId, new CachedPresignedUrl(url, expiresAtMillis));
    }

    private void cleanupExpiredCacheEntries(long nowMillis) {
        presignedUrlCache.entrySet().removeIf(entry -> !entry.getValue().isValid(nowMillis));
    }

    private String resolveResponseCacheControl(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return CACHE_CONTROL_VIDEO;
        }
        if (contentType.startsWith("image/")) {
            return CACHE_CONTROL_IMAGE;
        }
        return CACHE_CONTROL_VIDEO;
    }

    private String resolveLocalUrl(Long assetId) {
        if (assetId == null) {
            return null;
        }
        Asset asset = assetMapper.findById(assetId).orElse(null);
        if (asset == null || asset.getStorageProvider() == StorageProvider.S3) {
            return null;
        }
        String storageKey = asset.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        return normalizeFilesPath(storageKey);
    }

    private String normalizePublicFallback(String fallbackUrl) {
        if (fallbackUrl == null) {
            return null;
        }
        String trimmed = fallbackUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/api/")) {
            return null;
        }
        return normalizeFilesPath(trimmed);
    }

    private String normalizeFilesPath(String path) {
        if (path.startsWith(FILES_PREFIX)) {
            return path;
        }
        if (path.startsWith("files/")) {
            return "/" + path;
        }
        if (path.startsWith("/")) {
            return FILES_PREFIX + path.substring(1);
        }
        return FILES_PREFIX + path;
    }

    private record CachedPresignedUrl(String url, long expiresAtMillis) {
        private boolean isValid(long nowMillis) {
            return nowMillis < expiresAtMillis;
        }
    }
}
