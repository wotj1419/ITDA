package com.itda.backend.worker;

import com.itda.backend.global.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void save_writesFileAndReturnsSize() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(propertiesWithUploadDir(tempDir));

        StoredAsset stored = storage.save("ai/images/1/job-2.png", "hello".getBytes(StandardCharsets.UTF_8));

        assertThat(stored.storageKey()).isEqualTo("ai/images/1/job-2.png");
        assertThat(stored.sizeBytes()).isEqualTo(5);
        assertThat(Files.readAllBytes(tempDir.resolve(stored.storageKey()))).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void save_normalizesLeadingSlashesAndBackslashes() {
        LocalFileStorage storage = new LocalFileStorage(propertiesWithUploadDir(tempDir));

        StoredAsset stored = storage.save("\\ai\\images\\1\\job-2.png", new byte[]{1, 2, 3});

        assertThat(stored.storageKey()).isEqualTo("ai/images/1/job-2.png");
        assertThat(tempDir.resolve(stored.storageKey())).exists();
    }

    @Test
    void save_rejectsPathTraversal() {
        LocalFileStorage storage = new LocalFileStorage(propertiesWithUploadDir(tempDir));

        assertThatThrownBy(() -> storage.save("../evil.txt", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid relativePath");

        assertThatThrownBy(() -> storage.save("a/../../evil.txt", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid relativePath");
    }

    @Test
    void save_requiresInputs() {
        LocalFileStorage storage = new LocalFileStorage(propertiesWithUploadDir(tempDir));

        assertThatThrownBy(() -> storage.save("  ", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("relativePath is required");

        assertThatThrownBy(() -> storage.save("a.txt", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bytes is required");
    }

    private FileStorageProperties propertiesWithUploadDir(Path dir) {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(dir.toString());
        return props;
    }
}
