package com.itda.backend.worker.image;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageTest {

    @Mock
    private S3Client s3Client;

    private S3StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setBucket("test-bucket");
    }

    @Test
    void save_requiresBucket() {
        properties.setBucket("   ");
        S3ImageStorage storage = new S3ImageStorage(s3Client, properties);

        assertThatThrownBy(() -> storage.save(1L, 2L, new byte[]{1}, "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage.s3.bucket");
    }

    @Test
    void save_requiresBytes() {
        S3ImageStorage storage = new S3ImageStorage(s3Client, properties);

        assertThatThrownBy(() -> storage.save(1L, 2L, new byte[0], "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image bytes must not be empty");
    }

    @Test
    void save_normalizesContentTypeAndExtension() {
        S3ImageStorage storage = new S3ImageStorage(s3Client, properties);

        byte[] bytes = new byte[]{1, 2, 3};
        ImageStorageResult result = storage.save(1L, 2L, bytes, "IMAGE/JPEG; charset=UTF-8");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo("ai/image/1/job-2.jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");

        assertThat(result.storageProvider()).isEqualTo(StorageProvider.S3);
        assertThat(result.storageKey()).isEqualTo("ai/image/1/job-2.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.sizeBytes()).isEqualTo(bytes.length);
    }
}
