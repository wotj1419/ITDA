package com.itda.backend.worker.video;

import com.itda.backend.ai.veo.VeoClient;
import com.itda.backend.ai.veo.VeoRequest;
import com.itda.backend.ai.veo.VeoResult;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.job.domain.Job;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.JobRequestParser;
import com.itda.backend.worker.ParsedJobRequest;
import com.itda.backend.worker.StoredAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoGenerationWorker {

    private final VeoClient veoClient;
    private final LocalVideoStorage localVideoStorage;
    private final AssetRegistrar assetRegistrar;
    private final JobRequestParser jobRequestParser;

    public ExecutionResult execute(Job job) {
        requireJobIdentifiers(job);
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());
        VeoResult result = veoClient.generateVideo(new VeoRequest(request.prompt(), request.settings()));
        StoredAsset storedAsset = localVideoStorage.save(job.getProjectId(), job.getId(), result.bytes());
        Long assetId = assetRegistrar.registerLocalAsset(job, storedAsset, AssetType.VIDEO, result.contentType());
        return new ExecutionResult(assetId, storedAsset.storageKey());
    }

    private void requireJobIdentifiers(Job job) {
        if (job.getId() == null || job.getProjectId() == null) {
            throw new IllegalStateException("Job missing id/projectId");
        }
    }
}
