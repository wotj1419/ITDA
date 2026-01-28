package com.itda.backend.worker.image;

import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.worker.LocalFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void save_whenContentTypeNull_usesDefault() {
        LocalImageStorage imageStorage = new LocalImageStorage(localFileStorage(tempDir));

        ImageStorageResult stored = imageStorage.save(10L, 20L, new byte[]{1}, null);

        assertThat(stored.storageKey()).isEqualTo("ai/images/10/job-20.png");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storageProvider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(tempDir.resolve(stored.storageKey())).exists();
    }

    @Test
    void save_whenContentTypeBlank_usesDefault() {
        LocalImageStorage imageStorage = new LocalImageStorage(localFileStorage(tempDir));

        ImageStorageResult stored = imageStorage.save(10L, 20L, new byte[]{1}, "   ");

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storageProvider()).isEqualTo(StorageProvider.LOCAL);
    }

    private LocalFileStorage localFileStorage(Path dir) {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(dir.toString());
        return new LocalFileStorage(props);
    }
}
