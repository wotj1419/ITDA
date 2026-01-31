package com.itda.backend.worker.image;

import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.ai.gemini.ReferenceImage;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.job.domain.Job;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.JobRequestParser;
import com.itda.backend.worker.NodeContent;
import com.itda.backend.worker.NodeContentLoader;
import com.itda.backend.worker.ParsedJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationWorker {

    private final GeminiImageClient geminiImageClient;
    private final ImageStorage imageStorage;
    private final AssetRegistrar assetRegistrar;
    private final JobRequestParser jobRequestParser;
    private final NodeMapper nodeMapper;
    private final NodeContentLoader nodeContentLoader;
    private final ObjectReferenceImageLoader referenceImageLoader;

    public ExecutionResult execute(Job job) {
        requireJobIdentifiers(job);
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());
        log.info(
                "Image generation prompt resolved: jobId={}, nodeId={}, projectId={}, prompt={}"
                , job.getId()
                , job.getNodeId()
                , job.getProjectId()
                , request.prompt()
        );
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
        Node node = loadNode(job);
        List<ReferenceImage> referenceImages = new java.util.ArrayList<>();
        List<Long> referenceObjectIds = request.referenceObjectIds();
        if (referenceObjectIds != null && !referenceObjectIds.isEmpty()) {
            referenceImages.addAll(referenceImageLoader.load(job.getProjectId(), referenceObjectIds));
        }

        NodeContent referenceImage = resolveReferenceImage(node);
        if (referenceImage != null && referenceImage.bytes() != null && referenceImage.bytes().length > 0) {
            // Place parent frame last to preserve its aspect ratio in multi-image mode.
            referenceImages.add(new ReferenceImage(referenceImage.bytes(), referenceImage.contentType()));
        }

        if (!referenceImages.isEmpty()) {
            return geminiImageClient.generateImage(request.prompt(), request.settings(), referenceImages);
        }
        return geminiImageClient.generateImage(request.prompt(), request.settings());
    }

    private Node loadNode(Job job) {
        Long nodeId = job.getNodeId();
        if (nodeId == null) {
            throw new IllegalStateException("Job missing nodeId");
        }
        return nodeMapper.findById(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node not found: nodeId=" + nodeId));
    }

    private NodeContent resolveReferenceImage(Node node) {
        if (node == null || node.getNodeType() == null) {
            return null;
        }
        if (node.getNodeType() != NodeType.GRID && node.getNodeType() != NodeType.SHOT) {
            return null;
        }
        Long parentNodeId = node.getParentNodeId();
        if (parentNodeId == null) {
            throw new IllegalStateException("Parent node missing for image generation: nodeId=" + node.getId());
        }
        return nodeContentLoader.loadNodeContent(parentNodeId);
    }

    private void requireJobIdentifiers(Job job) {
        if (job.getId() == null || job.getProjectId() == null) {
            throw new IllegalStateException("Job missing id/projectId");
        }
    }
}
