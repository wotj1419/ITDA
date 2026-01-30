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
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
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
        if (referenceObjectIds != null && !referenceObjectIds.isEmpty()) {
            List<ReferenceImage> referenceImages = referenceImageLoader.load(job.getProjectId(), referenceObjectIds);
            return geminiImageClient.generateImage(request.prompt(), request.settings(), referenceImages);
        }
        Node node = loadNode(job);
        NodeContent referenceImage = resolveReferenceImage(node);
        if (referenceImage == null) {
            return geminiImageClient.generateImage(request.prompt(), request.settings());
        }
        return geminiImageClient.generateImage(
                request.prompt(),
                request.settings(),
                referenceImage.bytes(),
                referenceImage.contentType()
        );
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
