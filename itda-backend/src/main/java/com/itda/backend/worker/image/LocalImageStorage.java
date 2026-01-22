package com.itda.backend.worker.image;

import com.itda.backend.asset.domain.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalImageStorage implements ImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";

    private final FileStorageProperties fileStorageProperties;

    @Override
    public ImageStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType) {
        if (projectId == null || jobId == null) {
            throw new IllegalArgumentException("projectId/jobId must not be null");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("image bytes must not be empty");
        }

        String normalizedContentType = contentType == null || contentType.isBlank()
                ? DEFAULT_CONTENT_TYPE
                : contentType;

        String storageKey = String.format("ai/image/%d/job-%d.png", projectId, jobId)
                .replace("\\", "/");

        Path root = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("[LocalImageStorage] Failed to write file: {}", target, e);
            throw new IllegalStateException("Failed to store image locally", e);
        }

        return new ImageStorageResult(storageKey, normalizedContentType, bytes.length, StorageProvider.LOCAL);
    }
}
