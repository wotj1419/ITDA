package com.itda.backend.job.controller.dto;

import com.itda.backend.job.domain.Job;

/**
 * Job 대상 정보 DTO
 *
 * @param type 대상 타입 (NODE, SCENE, PROJECT)
 * @param id   대상 ID
 */
public record JobTarget(
        String type,
        Long id
) {

    /**
     * Job 타입에 따른 대상 정보 추출
     */
    public static JobTarget from(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION, VIDEO_GENERATION -> new JobTarget("NODE", job.getNodeId());
            case SCENE_MERGE -> new JobTarget("SCENE", job.getSceneId());
            case PROJECT_MERGE -> new JobTarget("PROJECT", job.getProjectId());
        };
    }
}
