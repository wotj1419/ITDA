package com.itda.backend.worker;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.worker.image.LocalImageStorage;
import com.itda.backend.worker.video.LocalVideoStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalJobAssetStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void imageStorage_buildsExpectedPath() {
        LocalFileStorage fileStorage = new LocalFileStorage(propertiesWithUploadDir(tempDir));
        LocalImageStorage imageStorage = new LocalImageStorage(fileStorage);

        StoredAsset stored = imageStorage.save(10L, 20L, new byte[]{1});

        assertThat(stored.storageKey()).isEqualTo("ai/images/10/job-20.png");
        assertThat(tempDir.resolve(stored.storageKey())).exists();
    }

    @Test
    void videoStorage_buildsExpectedPath() {
        LocalFileStorage fileStorage = new LocalFileStorage(propertiesWithUploadDir(tempDir));
        LocalVideoStorage videoStorage = new LocalVideoStorage(fileStorage);

        StoredAsset stored = videoStorage.save(10L, 20L, new byte[]{1});

        assertThat(stored.storageKey()).isEqualTo("ai/videos/10/job-20.mp4");
        assertThat(tempDir.resolve(stored.storageKey())).exists();
    }

    private FileStorageProperties propertiesWithUploadDir(Path dir) {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(dir.toString());
        return props;
    }
}
