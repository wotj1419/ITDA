package com.itda.backend.worker.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.job.domain.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageGenerationWorkerTest {

    @Mock
    private GeminiImageClient geminiImageClient;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private AssetMapper assetMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ImageGenerationWorker imageGenerationWorker;

    @Test
    void execute_ShouldStoreAssetAndReturnId() throws Exception {
        Job job = Job.builder()
                .id(10L)
                .projectId(1L)
                .requestJson("{\"prompt\":\"test prompt\"}")
                .build();

        byte[] imageBytes = new byte[] { 1, 2, 3 };
        when(geminiImageClient.generateImage("test prompt"))
                .thenReturn(new GeminiImageResult(imageBytes, "image/png"));

        ImageStorageResult storageResult = new ImageStorageResult(
                "ai/image/1/job-10.png",
                "image/png",
                imageBytes.length,
                StorageProvider.LOCAL
        );
        when(imageStorage.save(eq(1L), eq(10L), eq(imageBytes), eq("image/png")))
                .thenReturn(storageResult);

        doAnswer(invocation -> {
            Asset asset = invocation.getArgument(0);
            Field idField = Asset.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(asset, 100L);
            return null;
        }).when(assetMapper).insert(any(Asset.class));

        Long assetId = imageGenerationWorker.execute(job);

        assertThat(assetId).isEqualTo(100L);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetMapper).insert(captor.capture());
        Asset saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(1L);
        assertThat(saved.getStorageKey()).isEqualTo("ai/image/1/job-10.png");
        assertThat(saved.getStorageProvider()).isEqualTo(StorageProvider.LOCAL);

        verify(geminiImageClient).generateImage("test prompt");
        verify(imageStorage).save(1L, 10L, imageBytes, "image/png");
        Mockito.verifyNoMoreInteractions(geminiImageClient, imageStorage, assetMapper);
    }

    @Test
    void execute_ShouldFailWhenPromptMissing() {
        Job job = Job.builder()
                .id(11L)
                .projectId(1L)
                .requestJson("{\"foo\":\"bar\"}")
                .build();

        assertThatThrownBy(() -> imageGenerationWorker.execute(job))
                .isInstanceOf(com.itda.backend.global.exception.BusinessException.class)
                .hasMessageContaining("Prompt is required");
    }
}
