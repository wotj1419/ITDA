package com.itda.backend.worker;

import com.itda.backend.global.config.FileStorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

    public StoredAsset save(Path sourceFile, String relativePath) {
        try {
            requireRelativePath(relativePath);
            requireSourceFile(sourceFile);

            String storageKey = normalizeRelativePath(relativePath);
            Path targetPath = resolveTargetPath(storageKey);
            copyFile(sourceFile, targetPath);

            return new StoredAsset(storageKey, Files.size(targetPath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    public byte[] readBytes(String relativePath) throws IOException {
        requireRelativePath(relativePath);
        String storageKey = normalizeRelativePath(relativePath);
        Path targetPath = resolveTargetPath(storageKey);
        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            throw new NoSuchFileException(targetPath.toString());
        }
        return Files.readAllBytes(targetPath);
    }

    public long size(String relativePath) throws IOException {
        requireRelativePath(relativePath);
        String storageKey = normalizeRelativePath(relativePath);
        Path targetPath = resolveTargetPath(storageKey);
        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            throw new NoSuchFileException(targetPath.toString());
        }
        return Files.size(targetPath);
    }

    public void delete(String relativePath) {
        try {
            requireRelativePath(relativePath);
            String storageKey = normalizeRelativePath(relativePath);
            Path targetPath = resolveTargetPath(storageKey);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file", e);
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

    private void requireSourceFile(Path sourceFile) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile is required");
        }
        if (!Files.exists(sourceFile)) {
            throw new IllegalArgumentException("sourceFile does not exist");
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

    private void copyFile(Path sourceFile, Path targetPath) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
