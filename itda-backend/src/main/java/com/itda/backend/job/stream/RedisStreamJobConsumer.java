package com.itda.backend.job.stream;

import com.itda.backend.job.service.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Redis Streams 기반 Job Consumer
 */
@Slf4j
@Component
@Profile("redis-streams")
@RequiredArgsConstructor
public class RedisStreamJobConsumer {

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redisTemplate;
    private final JobExecutor jobExecutor;
    private final JobStreamProperties streamProperties;
    @Qualifier("jobExecutorPool")
    private final Executor jobExecutorPool;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private String consumerName;

    @PostConstruct
    public void start() {
        consumerName = streamProperties.getConsumerPrefix() + "-" + UUID.randomUUID();
        createGroupIfMissing(streamProperties.getImageStreamKey(), streamProperties.getImageGroup());
        createGroupIfMissing(streamProperties.getVideoStreamKey(), streamProperties.getVideoGroup());
        createGroupIfMissing(streamProperties.getMergeStreamKey(), streamProperties.getMergeGroup());

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(streamProperties.getPollTimeoutMs()))
                        .executor(jobExecutorPool)
                        .build();

        container = StreamMessageListenerContainer.create(connectionFactory, options);

        registerConsumer(streamProperties.getImageStreamKey(), streamProperties.getImageGroup());
        registerConsumer(streamProperties.getVideoStreamKey(), streamProperties.getVideoGroup());
        registerConsumer(streamProperties.getMergeStreamKey(), streamProperties.getMergeGroup());

        container.start();
        log.info("[RedisStreamConsumer] started. consumerName={}", consumerName);
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private void registerConsumer(String streamKey, String group) {
        container.receive(
                Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                message -> handleMessage(streamKey, group, message)
        );
    }

    private void handleMessage(String streamKey, String group, MapRecord<String, String, String> message) {
        String jobIdValue = message.getValue().get("jobId");
        try {
            if (jobIdValue == null || jobIdValue.isBlank()) {
                log.warn("[RedisStreamConsumer] Missing jobId: stream={}, id={}", streamKey, message.getId());
                return;
            }
            long jobId = Long.parseLong(jobIdValue);
            jobExecutor.execute(jobId);
        } catch (Exception e) {
            log.error("[RedisStreamConsumer] Job execution failed: stream={}, id={}, error={}",
                    streamKey, message.getId(), e.getMessage(), e);
        } finally {
            try {
                redisTemplate.opsForStream().acknowledge(streamKey, group, message.getId());
            } catch (Exception e) {
                log.warn("[RedisStreamConsumer] Failed to ack: stream={}, id={}",
                        streamKey, message.getId(), e);
            }
        }
    }

    private void createGroupIfMissing(String streamKey, String group) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), group);
            log.info("[RedisStreamConsumer] Group created: stream={}, group={}", streamKey, group);
        } catch (Exception e) {
            if (isBusyGroupError(e)) {
                log.debug("[RedisStreamConsumer] Group already exists: stream={}, group={}", streamKey, group);
                return;
            }
            if (isNoStreamError(e)) {
                ensureStreamExists(streamKey);
                redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), group);
                log.info("[RedisStreamConsumer] Group created after stream init: stream={}, group={}", streamKey, group);
                return;
            }
            throw e;
        }
    }

    private boolean isBusyGroupError(Exception e) {
        return hasMessageInChain(e, "BUSYGROUP")
                || hasMessageInChain(e, "Consumer Group name already exists")
                || hasCauseType(e, "io.lettuce.core.RedisBusyException");
    }

    private boolean isNoStreamError(Exception e) {
        return hasMessageInChain(e, "NOGROUP")
                || hasMessageInChain(e, "ERR The XGROUP subcommand requires the key to exist");
    }

    private void ensureStreamExists(String streamKey) {
        redisTemplate.opsForStream()
                .add(StreamRecords.string(java.util.Map.of("init", "0")).withStreamKey(streamKey));
    }

    private boolean hasMessageInChain(Throwable throwable, String needle) {
        if (needle == null || needle.isBlank()) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(needle)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasCauseType(Throwable throwable, String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            if (current.getClass().getName().equals(className)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
