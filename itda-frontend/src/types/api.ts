/**
 * AI API Types
 * @module types/api
 * 
 * 백엔드 AI API 요청/응답 타입 정의
 * API 스펙이 확정되면 이 파일만 수정하면 됩니다.
 */

// =============================================================================
// AI Prompt Generation
// =============================================================================

/**
 * AI 프롬프트 생성 요청
 * 노드의 설정값을 기반으로 AI가 영어 프롬프트를 생성
 */
export interface GeneratePromptRequest {
    nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
    // Master Image
    style?: string;
    timeOfDay?: string;
    mood?: string;
    objectIds?: string[];
    // Storyboard Grid
    layout?: string;
    shotTypes?: string[];
    compositionHint?: string;
    // Shot
    shotType?: string;
    expression?: string;
    additionalDetail?: string;
    // Video
    cameraMotion?: string;
    duration?: number;
    motionDescription?: string;
}

/**
 * AI 프롬프트 생성 응답
 */
export interface GeneratePromptResponse {
    prompt: string;
}

// =============================================================================
// Node Generation (Image/Video)
// =============================================================================

/**
 * 노드 생성 요청 (이미지/영상 생성)
 * 프롬프트가 승인된 노드에 대해 AI가 이미지/영상 생성
 */
export interface GenerateNodeRequest {
    prompt: string;
    nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
}

/**
 * 노드 생성 응답 (Job ID 반환)
 */
export interface GenerateJobResponse {
    jobId: number;
    status: 'PENDING' | 'QUEUED';
}

// =============================================================================
// Job Status
// =============================================================================

export type JobStatusType = 'PENDING' | 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

/**
 * Job 상태 조회 응답
 */
export interface JobStatusResponse {
    jobId: number;
    type: 'IMAGE_GENERATION' | 'VIDEO_GENERATION' | 'SCENE_MERGE' | 'PROJECT_MERGE';
    status: JobStatusType;
    resultUrl?: string;
    thumbnailUrl?: string;
    error?: {
        code: string;
        message: string;
    };
}

// =============================================================================
// Common API Response Wrapper
// =============================================================================

/**
 * 공통 API 응답 포맷
 */
export interface ApiResponse<T> {
    code: 'SUCCESS' | 'ACCEPTED' | string;
    message?: string;
    data?: T;
    details?: Record<string, unknown>;
}
