package com.itda.backend.job.event;

import com.itda.backend.collab.service.CollabRedisPublisher;
import com.itda.backend.job.controller.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 단위 WebSocket 이벤트 발행자
 */
@Component
@RequiredArgsConstructor
public class ProjectEventWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final CollabRedisPublisher collabRedisPublisher;

    public void jobDone(Long projectId, JobResponse response) {
        publish(projectId, new JobEventMessage("job.done", response));
    }

    public void jobFailed(Long projectId, JobResponse response) {
        publish(projectId, new JobEventMessage("job.failed", response));
    }

    public void nodeChanged(Long projectId, Long sceneId, Long nodeId, String action, Long actorId) {
        nodeChanged(projectId, sceneId, nodeId, action, actorId, null);
    }

    public void nodeChanged(
            Long projectId,
            Long sceneId,
            Long nodeId,
            String action,
            Long actorId,
            java.util.List<NodePositionPayload> positions
    ) {
        NodeChangedPayload payload = new NodeChangedPayload(sceneId, nodeId, action, actorId, positions);
        publish(projectId, new NodeChangedEventMessage("node.changed", payload));
    }

    private void publish(Long projectId, Object message) {
        String destination = "/topic/projects/" + projectId;
        try {
            collabRedisPublisher.publish(destination, message);
        } catch (Exception e) {
            messagingTemplate.convertAndSend(destination, message);
        }
    }
}
