package com.itda.backend.job.domain;

/**
 * Job 타입 (AI 생성 작업 종류)
 * <p>
 * 각 타입은 서로 다른 Worker에서 처리됨:
 * - IMAGE_GENERATION: ImageGenerationWorker (Imagen/Gemini)
 * - VIDEO_GENERATION: VideoGenerationWorker (Veo)
 * - SCENE_MERGE, PROJECT_MERGE: MergeWorker (FFmpeg)
 */
public enum JobType {
    
    /** 이미지 생성 (Imagen/Gemini) */
    IMAGE_GENERATION,
    
    /** 영상 생성 (Veo) */
    VIDEO_GENERATION,
    
    /** 씬 내 클립 병합 (FFmpeg) */
    SCENE_MERGE,
    
    /** 프로젝트 전체 병합 (FFmpeg) */
    PROJECT_MERGE
}
