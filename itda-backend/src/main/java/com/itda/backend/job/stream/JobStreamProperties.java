package com.itda.backend.job.stream;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis Streams 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "job.streams")
public class JobStreamProperties {

    /** 이미지 생성 스트림 키 */
    private String imageStreamKey = "ai:image";

    /** 영상 생성 스트림 키 */
    private String videoStreamKey = "ai:video";

    /** 병합 스트림 키 */
    private String mergeStreamKey = "media:merge";

    /** 이미지 컨슈머 그룹 */
    private String imageGroup = "cg:ai-image";

    /** 영상 컨슈머 그룹 */
    private String videoGroup = "cg:ai-video";

    /** 병합 컨슈머 그룹 */
    private String mergeGroup = "cg:media-merge";

    /** 컨슈머 이름 prefix */
    private String consumerPrefix = "api-worker";

    /** Streams poll timeout (ms) */
    private long pollTimeoutMs = 1000;
}
