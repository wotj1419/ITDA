package com.itda.backend.worker.video;

import com.itda.backend.ai.veo.VeoClient;
import com.itda.backend.ai.veo.VeoImage;
import com.itda.backend.ai.veo.VeoRequest;
import com.itda.backend.ai.veo.VeoResult;
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

@Component
@RequiredArgsConstructor
public class VideoGenerationWorker {

    private final VeoClient veoClient;
    private final VideoStorage videoStorage;
    private final AssetRegistrar assetRegistrar;
    private final JobRequestParser jobRequestParser;
    private final NodeMapper nodeMapper;
    private final NodeContentLoader nodeContentLoader;

    public ExecutionResult execute(Job job) {
        requireJobIdentifiers(job);
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());
        Node node = loadNode(job);
        NodeContent firstFrame = resolveFirstFrame(node);
        NodeContent lastFrame = resolveLastFrame(node);
        VeoRequest veoRequest = new VeoRequest(
                request.prompt(),
                request.settings(),
                toVeoImage(firstFrame),
                toVeoImage(lastFrame)
        );
        VeoResult result = veoClient.generateVideo(veoRequest);
        VideoStorageResult storedVideo = videoStorage.save(
                job.getProjectId(),
                job.getId(),
                result.bytes(),
                result.contentType()
        );
        Long assetId = assetRegistrar.registerAsset(
                job,
                storedVideo.storageKey(),
                storedVideo.sizeBytes(),
                AssetType.VIDEO,
                storedVideo.contentType(),
                storedVideo.storageProvider()
        );
        return new ExecutionResult(assetId, storedVideo.storageKey());
    }

    private Node loadNode(Job job) {
        Long nodeId = job.getNodeId();
        if (nodeId == null) {
            throw new IllegalStateException("Job missing nodeId");
        }
        return nodeMapper.findById(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node not found: nodeId=" + nodeId));
    }

    private NodeContent resolveFirstFrame(Node node) {
        if (node == null || node.getNodeType() == null) {
            throw new IllegalStateException("Node is required for video generation");
        }
        if (node.getNodeType() != NodeType.VIDEO) {
            throw new IllegalStateException("Video generation node type mismatch: nodeId=" + node.getId());
        }
        Long startShotNodeId = node.getStartShotNodeId();
        if (startShotNodeId == null) {
            throw new IllegalStateException("startShotNodeId missing for video generation: nodeId=" + node.getId());
        }
        return nodeContentLoader.loadNodeContent(startShotNodeId);
    }

    private NodeContent resolveLastFrame(Node node) {
        if (node == null || node.getNodeType() != NodeType.VIDEO) {
            return null;
        }
        Long endShotNodeId = node.getEndShotNodeId();
        if (endShotNodeId == null) {
            return null;
        }
        return nodeContentLoader.loadNodeContent(endShotNodeId);
    }

    private VeoImage toVeoImage(NodeContent content) {
        if (content == null || content.bytes() == null || content.bytes().length == 0) {
            return null;
        }
        return new VeoImage(content.bytes(), content.contentType());
    }

    private void requireJobIdentifiers(Job job) {
        if (job.getId() == null || job.getProjectId() == null) {
            throw new IllegalStateException("Job missing id/projectId");
        }
    }
}
