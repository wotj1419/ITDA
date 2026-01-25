package com.itda.backend.worker;

import com.itda.backend.global.config.FileStorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalFileStorage {

    private final Path uploadRoot;

    public LocalFileStorage(FileStorageProperties fileStorageProperties) {
        this.uploadRoot = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
    }

    public StoredAsset save(String relativePath, byte[] bytes) {
        try {
            requireRelativePath(relativePath);
            requireBytes(bytes);

            String storageKey = normalizeRelativePath(relativePath);
            Path targetPath = resolveTargetPath(storageKey);
            writeBytes(targetPath, bytes);

            return new StoredAsset(storageKey, Files.size(targetPath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    private void requireRelativePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("relativePath is required");
        }
    }

    private void requireBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes is required");
        }
    }

    private String normalizeRelativePath(String relativePath) {
        String normalized = relativePath.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid relativePath");
        }
        return normalized;
    }

    private Path resolveTargetPath(String storageKey) {
        Path targetPath = uploadRoot.resolve(storageKey).normalize();
        if (!targetPath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid relativePath");
        }
        return targetPath;
    }

    private void writeBytes(Path targetPath, byte[] bytes) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(targetPath, bytes);
    }
}
