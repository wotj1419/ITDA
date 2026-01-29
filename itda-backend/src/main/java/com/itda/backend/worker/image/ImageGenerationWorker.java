package com.itda.backend.worker.image;

import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.ai.gemini.ReferenceImage;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.job.domain.Job;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.JobRequestParser;
import com.itda.backend.worker.ParsedJobRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageGenerationWorker {

    private final GeminiImageClient geminiImageClient;
    private final ImageStorage imageStorage;
    private final AssetRegistrar assetRegistrar;
    private final JobRequestParser jobRequestParser;
    private final ObjectReferenceImageLoader referenceImageLoader;

    public ExecutionResult execute(Job job) {
        requireJobIdentifiers(job);
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());
        GeminiImageResult result = generateImage(job, request);
        ImageStorageResult storedImage = imageStorage.save(
                job.getProjectId(),
                job.getId(),
                result.bytes(),
                result.contentType()
        );
        Long assetId = assetRegistrar.registerAsset(
                job,
                storedImage.storageKey(),
                storedImage.sizeBytes(),
                AssetType.IMAGE,
                storedImage.contentType(),
                storedImage.storageProvider()
        );
        return new ExecutionResult(assetId, storedImage.storageKey());
    }

    private GeminiImageResult generateImage(Job job, ParsedJobRequest request) {
        List<Long> referenceObjectIds = request.referenceObjectIds();
        if (referenceObjectIds == null || referenceObjectIds.isEmpty()) {
            return geminiImageClient.generateImage(request.prompt(), request.settings());
        }
        List<ReferenceImage> referenceImages = referenceImageLoader.load(job.getProjectId(), referenceObjectIds);
        return geminiImageClient.generateImage(request.prompt(), request.settings(), referenceImages);
    }

    private void requireJobIdentifiers(Job job) {
        if (job.getId() == null || job.getProjectId() == null) {
            throw new IllegalStateException("Job missing id/projectId");
        }
    }
}
