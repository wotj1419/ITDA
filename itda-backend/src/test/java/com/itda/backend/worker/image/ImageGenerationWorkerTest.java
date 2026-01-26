package com.itda.backend.worker.image;

import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.job.domain.Job;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.JobRequestParser;
import com.itda.backend.worker.ParsedJobRequest;
import com.itda.backend.worker.StoredAsset;
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
    private LocalImageStorage localImageStorage;

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

        StoredAsset storedAsset = new StoredAsset("ai/images/1/job-10.png", imageBytes.length);
        when(localImageStorage.save(1L, 10L, imageBytes)).thenReturn(storedAsset);
        when(assetRegistrar.registerLocalAsset(job, storedAsset, com.itda.backend.asset.domain.AssetType.IMAGE, "image/png"))
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
}
