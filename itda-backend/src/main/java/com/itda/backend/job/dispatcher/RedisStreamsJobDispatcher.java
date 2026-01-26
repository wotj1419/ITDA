package com.itda.backend.job.dispatcher;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobDispatcher;
import com.itda.backend.job.stream.JobStreamProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis Streams 기반 Job Dispatcher
 */
@Slf4j
@Service
@Profile("redis-streams")
@RequiredArgsConstructor
public class RedisStreamsJobDispatcher implements JobDispatcher {

    private final StringRedisTemplate redisTemplate;
    private final JobStreamProperties streamProperties;

    @Override
    public void enqueue(Job job) {
        if (job == null || job.getId() == null) {
            log.warn("[RedisStreamsDispatcher] Skip enqueue: job or jobId missing");
            return;
        }

        String streamKey = resolveStreamKey(job.getType());
        Map<String, String> payload = buildPayload(job);
        redisTemplate.opsForStream()
                .add(StreamRecords.string(payload).withStreamKey(streamKey));
        log.info("[RedisStreamsDispatcher] Job enqueued: id={}, type={}, stream={}",
                job.getId(), job.getType(), streamKey);
    }

    private Map<String, String> buildPayload(Job job) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("jobId", job.getId().toString());
        payload.put("type", job.getType().name());
        if (job.getProjectId() != null) {
            payload.put("projectId", job.getProjectId().toString());
        }
        if (job.getSceneId() != null) {
            payload.put("sceneId", job.getSceneId().toString());
        }
        if (job.getNodeId() != null) {
            payload.put("nodeId", job.getNodeId().toString());
        }
        payload.put("createdAt", OffsetDateTime.now().toString());
        return payload;
    }

    private String resolveStreamKey(JobType type) {
        return switch (type) {
            case IMAGE_GENERATION -> streamProperties.getImageStreamKey();
            case VIDEO_GENERATION -> streamProperties.getVideoStreamKey();
            case SCENE_MERGE, PROJECT_MERGE -> streamProperties.getMergeStreamKey();
        };
    }
}
