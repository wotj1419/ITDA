package com.itda.backend.worker.video;

import com.itda.backend.ai.veo.VeoClient;
import com.itda.backend.ai.veo.VeoRequest;
import com.itda.backend.ai.veo.VeoResult;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoGenerationWorkerTest {

    @Mock
    private VeoClient veoClient;

    @Mock
    private LocalVideoStorage localVideoStorage;

    @Mock
    private AssetRegistrar assetRegistrar;

    @Mock
    private JobRequestParser jobRequestParser;

    @InjectMocks
    private VideoGenerationWorker videoGenerationWorker;

    @Test
    void execute_ShouldStoreAssetAndReturnExecutionResult() {
        Job job = Job.builder()
                .id(10L)
                .projectId(1L)
                .requestJson("{\"prompt\":\"test prompt\"}")
                .build();

        ParsedJobRequest parsed = new ParsedJobRequest("test prompt", Map.of("duration", 4));
        when(jobRequestParser.parse(job.getRequestJson())).thenReturn(parsed);

        byte[] videoBytes = new byte[] { 1, 2, 3 };
        VeoResult veoResult = new VeoResult(videoBytes, "video/mp4");
        when(veoClient.generateVideo(new VeoRequest(parsed.prompt(), parsed.settings())))
                .thenReturn(veoResult);

        StoredAsset storedAsset = new StoredAsset("ai/videos/1/job-10.mp4", videoBytes.length);
        when(localVideoStorage.save(1L, 10L, videoBytes)).thenReturn(storedAsset);
        when(assetRegistrar.registerLocalAsset(job, storedAsset, AssetType.VIDEO, "video/mp4"))
                .thenReturn(200L);

        ExecutionResult result = videoGenerationWorker.execute(job);

        assertThat(result).isNotNull();
        assertThat(result.resultAssetId()).isEqualTo(200L);
        assertThat(result.nodeContentKey()).isEqualTo("ai/videos/1/job-10.mp4");
        verify(veoClient).generateVideo(new VeoRequest(parsed.prompt(), parsed.settings()));
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

        assertThatThrownBy(() -> videoGenerationWorker.execute(job))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Prompt is empty");
    }

    @Test
    void execute_ShouldFailWhenIdentifiersMissing() {
        Job job = Job.builder()
                .requestJson("{\"prompt\":\"test\"}")
                .build();

        assertThatThrownBy(() -> videoGenerationWorker.execute(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Job missing id/projectId");
    }
}
