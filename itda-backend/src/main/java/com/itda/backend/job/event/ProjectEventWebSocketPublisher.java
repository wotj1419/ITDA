package com.itda.backend.job.event;

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

    public void jobDone(Long projectId, JobResponse response) {
        publish(projectId, new JobEventMessage("job.done", response));
    }

    public void jobFailed(Long projectId, JobResponse response) {
        publish(projectId, new JobEventMessage("job.failed", response));
    }

    private void publish(Long projectId, JobEventMessage message) {
        messagingTemplate.convertAndSend("/topic/projects/" + projectId, message);
    }
}
