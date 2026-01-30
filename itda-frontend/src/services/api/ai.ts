/**
 * AI API Service
 * @module services/api/ai
 * 
 * AI 프롬프트 생성, 이미지/영상 생성, Job 상태 조회 API
 */
import apiClient from './client';
import type {
    ApiResponse,
    GeneratePromptRequest,
    GeneratePromptResponse,
    GenerateNodeRequest,
    GenerateJobResponse,
    JobStatusResponse,
} from '../../types/api';

// =============================================================================
// AI Prompt Generation
// =============================================================================

/**
 * AI 프롬프트 생성
 * 노드 설정값을 기반으로 AI가 영어 프롬프트 생성
 */
export async function generatePrompt(
    request: GeneratePromptRequest
): Promise<string> {
    const payload = mapGeneratePromptPayload(request);
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/generate',
        payload
    );
    if (!response.data.data?.prompt) {
        throw new Error('Failed to generate prompt');
    }
    return response.data.data.prompt;
}

function mapGeneratePromptPayload(request: GeneratePromptRequest): {
    nodeType: GeneratePromptRequest['nodeType'];
    sceneOneLine?: string;
    style?: string;
    timeOfDay?: string;
    mood?: string;
    objects?: string[];
} {
    const objects =
        request.objects && request.objects.length
            ? request.objects
            : request.objectIds && request.objectIds.length
                ? request.objectIds.map((id) => String(id))
                : undefined;

    const derivedSceneOneLine = [
        request.shotType ? `shotType: ${request.shotType}` : null,
        request.expression ? `expression: ${request.expression}` : null,
        request.additionalDetail ? `detail: ${request.additionalDetail}` : null,
        request.layout ? `layout: ${request.layout}` : null,
        request.shotTypes && request.shotTypes.length
            ? `shotTypes: ${request.shotTypes.join(', ')}`
            : null,
        request.compositionHint ? `composition: ${request.compositionHint}` : null,
        request.cameraMotion ? `cameraMotion: ${request.cameraMotion}` : null,
        typeof request.duration === 'number' ? `duration: ${request.duration}` : null,
        request.motionDescription ? `motion: ${request.motionDescription}` : null,
    ]
        .filter(Boolean)
        .join(', ');

    return {
        nodeType: request.nodeType,
        sceneOneLine: request.sceneOneLine || derivedSceneOneLine || undefined,
        style: request.style,
        timeOfDay: request.timeOfDay,
        mood: request.mood,
        objects,
    };
}

/**
 * AI 프롬프트 개선
 * 사용자가 수정한 프롬프트를 기반으로 AI가 개선된 프롬프트 생성
 */
export async function improvePrompt(
    currentPrompt: string,
    userFeedback: string,
    nodeType?: GeneratePromptRequest['nodeType']
): Promise<string> {
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/improve',
        { nodeType, prompt: currentPrompt, instruction: userFeedback }
    );
    if (!response.data.data?.prompt) {
        throw new Error('Failed to improve prompt');
    }
    return response.data.data.prompt;
}

// =============================================================================
// Node Generation (Image/Video)
// =============================================================================

/**
 * 노드 이미지/영상 생성 요청
 * 승인된 프롬프트로 AI가 이미지/영상 생성
 * @returns jobId (비동기 작업 ID)
 */
export async function generateNode(
    nodeId: string | number,
    prompt: string,
    options?: {
        nodeType?: GenerateNodeRequest['nodeType'];
        settings?: Record<string, unknown>;
        referenceObjectIds?: number[];
    }
): Promise<number> {
    const payload = {
        prompt,
        nodeType: options?.nodeType,
        settings: options?.settings,
        referenceObjectIds: options?.referenceObjectIds,
    };
    const response = await apiClient.post<ApiResponse<GenerateJobResponse>>(
        `/nodes/${nodeId}/generate`,
        payload
    );
    if (!response.data.data?.jobId) {
        throw new Error('Failed to start generation job');
    }
    return response.data.data.jobId;
}

// =============================================================================
// Job Status
// =============================================================================

/**
 * Job 상태 조회 (폴링용)
 */
export async function getJobStatus(jobId: number): Promise<JobStatusResponse> {
    const response = await apiClient.get<ApiResponse<JobStatusResponse>>(
        `/ai/jobs/${jobId}`
    );
    if (!response.data.data) {
        throw new Error('Failed to get job status');
    }
    return response.data.data;
}

/**
 * Job 완료까지 폴링
 * @param jobId 작업 ID
 * @param onProgress 진행 상태 콜백 (선택)
 * @param intervalMs 폴링 간격 (기본 2초)
 * @param maxAttempts 최대 시도 횟수 (기본 60회 = 2분)
 */
export async function pollJobUntilComplete(
    jobId: number,
    onProgress?: (status: JobStatusResponse) => void,
    intervalMs: number = 2000,
    maxAttempts: number = 60
): Promise<JobStatusResponse> {
    let attempts = 0;

    while (attempts < maxAttempts) {
        const status = await getJobStatus(jobId);

        if (onProgress) {
            onProgress(status);
        }

        if (status.status === 'SUCCEEDED' || status.status === 'FAILED') {
            return status;
        }

        await new Promise((resolve) => setTimeout(resolve, intervalMs));
        attempts++;
    }

    throw new Error('Job polling timeout');
}

// =============================================================================
// Export as service object
// =============================================================================

export const apiAiService = {
    generatePrompt,
    improvePrompt,
    generateNode,
    getJobStatus,
    pollJobUntilComplete,
};
