package com.itda.backend.worker.image;

import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.JobRequestParser;
import com.itda.backend.worker.ParsedJobRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageGenerationWorkerTest {

    @Mock
    private GeminiImageClient geminiImageClient;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private AssetRegistrar assetRegistrar;

    @InjectMocks
    private ImageGenerationWorker imageGenerationWorker;

    @Mock
    private JobRequestParser jobRequestParser;

    @Test
    void execute_ShouldStoreAssetAndReturnExecutionResult() {
        Job job = Job.builder()
                .id(10L)
                .projectId(1L)
                .requestJson("{\"prompt\":\"test prompt\"}")
                .build();

        ParsedJobRequest parsed = new ParsedJobRequest("test prompt", Map.of("aspectRatio", "1:1"));
        when(jobRequestParser.parse(job.getRequestJson())).thenReturn(parsed);

        byte[] imageBytes = new byte[] { 1, 2, 3 };
        when(geminiImageClient.generateImage("test prompt", parsed.settings()))
                .thenReturn(new GeminiImageResult(imageBytes, "image/png"));

        ImageStorageResult storedImage = new ImageStorageResult(
                "ai/images/1/job-10.png",
                "image/png",
                imageBytes.length,
                StorageProvider.LOCAL
        );
        when(imageStorage.save(1L, 10L, imageBytes, "image/png")).thenReturn(storedImage);
        when(assetRegistrar.registerAsset(
                job,
                storedImage.storageKey(),
                storedImage.sizeBytes(),
                AssetType.IMAGE,
                storedImage.contentType(),
                storedImage.storageProvider()
        ))
                .thenReturn(100L);

        ExecutionResult result = imageGenerationWorker.execute(job);

        assertThat(result).isNotNull();
        assertThat(result.resultAssetId()).isEqualTo(100L);
        assertThat(result.nodeContentKey()).isEqualTo("ai/images/1/job-10.png");
    }

    @Test
    void execute_ShouldFailWhenPromptMissing() {
        Job job = Job.builder()
                .id(11L)
                .projectId(1L)
                .requestJson("{\"foo\":\"bar\"}")
                .build();

        when(jobRequestParser.parse(job.getRequestJson()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty"));

        assertThatThrownBy(() -> imageGenerationWorker.execute(job))
                .isInstanceOf(com.itda.backend.global.exception.BusinessException.class)
                .hasMessageContaining("Prompt is empty");
    }

    @Test
    void execute_ShouldFailWhenIdentifiersMissing() {
        Job job = Job.builder()
                .requestJson("{\"prompt\":\"test\"}")
                .build();

        assertThatThrownBy(() -> imageGenerationWorker.execute(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Job missing id/projectId");
    }
}
