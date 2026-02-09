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
    PromptPreviewRequest,
    PromptPreviewResponse,
} from '../../types/api';

const GENERATE_NODE_RETRY_MAX_ATTEMPTS = 2;
const GENERATE_NODE_RETRY_BASE_DELAY_MS = 1500;

function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function shouldRetryGenerateNode(error: unknown): boolean {
    if (!error || typeof error !== 'object') {
        return false;
    }
    const axiosLike = error as {
        response?: { status?: number };
        code?: string;
        message?: string;
    };
    const status = axiosLike.response?.status;
    const code = axiosLike.code;
    if (status === 502 || status === 503 || status === 504) {
        return true;
    }
    if (code === 'ECONNABORTED') {
        return true;
    }
    const message = (axiosLike.message ?? '').toLowerCase();
    return message.includes('timeout');
}

function toFriendlyGenerateNodeError(error: unknown): Error {
    if (error instanceof Error) {
        const axiosLike = error as Error & {
            response?: { status?: number };
            code?: string;
            message?: string;
        };
        const status = axiosLike.response?.status;
        const code = axiosLike.code;
        const message = (axiosLike.message ?? '').toLowerCase();
        const isTimeout = status === 504 || code === 'ECONNABORTED' || message.includes('timeout');
        if (isTimeout) {
            return new Error('생성 요청 시간이 초과되었습니다. 생성은 계속 진행될 수 있으니 잠시 후 결과를 확인해주세요.');
        }
        if (status === 502 || status === 503) {
            return new Error('생성 서버 응답이 일시적으로 지연되고 있습니다. 잠시 후 다시 시도해주세요.');
        }
        return error;
    }
    return new Error('생성 요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
}

// =============================================================================
// AI Prompt Generation
// =============================================================================

/**
 * AI 프롬프트 생성
 * 노드 설정값을 기반으로 AI가 영어 프롬프트 생성
 */
export async function generatePrompt(
    request: GeneratePromptRequest
): Promise<GeneratePromptResponse> {
    const payload = mapGeneratePromptPayload(request);
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/generate',
        payload
    );
    if (!response.data.data?.promptEnBase) {
        throw new Error('Failed to generate prompt');
    }
    return response.data.data;
}

function mapGeneratePromptPayload(request: GeneratePromptRequest): {
    nodeType: GeneratePromptRequest['nodeType'];
    sceneOneLine?: string;
    prompt?: string;
    gridMode?: GeneratePromptRequest['gridMode'];
    layout?: string;
    timelineIntervalSeconds?: number;
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
        prompt: request.prompt,
        gridMode: request.gridMode,
        layout: request.layout,
        timelineIntervalSeconds: request.timelineIntervalSeconds,
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
    nodeType?: GeneratePromptRequest['nodeType'],
    context?: { sceneOneLine?: string }
): Promise<GeneratePromptResponse> {
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/improve',
        {
            nodeType,
            prompt: currentPrompt,
            instruction: userFeedback,
            sceneOneLine: context?.sceneOneLine,
        }
    );
    if (!response.data.data?.promptEnBase) {
        throw new Error('Failed to improve prompt');
    }
    return response.data.data;
}

export async function translatePrompt(
    promptEn: string
): Promise<GeneratePromptResponse> {
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/translate',
        { promptEn }
    );
    if (!response.data.data?.promptEnBase) {
        throw new Error('Failed to translate prompt');
    }
    return response.data.data;
}

export async function rewritePrompt(
    promptKo: string
): Promise<GeneratePromptResponse> {
    const response = await apiClient.post<ApiResponse<GeneratePromptResponse>>(
        '/ai/prompts/rewrite',
        { promptKo }
    );
    if (!response.data.data?.promptEnBase) {
        throw new Error('Failed to rewrite prompt');
    }
    return response.data.data;
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
        promptEnFinalOverride?: string;
        referenceObjectIds?: number[];
        requeueIfExisting?: boolean;
    }
): Promise<number> {
    const payload = {
        prompt,
        nodeType: options?.nodeType,
        settings: options?.settings,
        promptEnFinalOverride: options?.promptEnFinalOverride,
        referenceObjectIds: options?.referenceObjectIds,
        requeueIfExisting: options?.requeueIfExisting,
    };

    let attempt = 0;
    while (true) {
        try {
            const response = await apiClient.post<ApiResponse<GenerateJobResponse>>(
                `/nodes/${nodeId}/generate`,
                payload
            );
            if (!response.data.data?.jobId) {
                throw new Error('Failed to start generation job');
            }
            return response.data.data.jobId;
        } catch (error) {
            const retryable = shouldRetryGenerateNode(error);
            const canRetry =
                retryable &&
                attempt < GENERATE_NODE_RETRY_MAX_ATTEMPTS;
            if (!canRetry) {
                if (retryable) {
                    throw toFriendlyGenerateNodeError(error);
                }
                throw error;
            }
            attempt += 1;
            await sleep(GENERATE_NODE_RETRY_BASE_DELAY_MS * attempt);
        }
    }
}

export async function previewPrompt(
    nodeId: string | number,
    request: PromptPreviewRequest
): Promise<PromptPreviewResponse> {
    const response = await apiClient.post<ApiResponse<PromptPreviewResponse>>(
        `/nodes/${nodeId}/prompt-preview`,
        request
    );
    if (!response.data.data?.promptEnFinal) {
        throw new Error('Failed to preview prompt');
    }
    return response.data.data;
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
 * @param maxAttempts 최대 시도 횟수 (기본 90회 = 3분)
 */
export async function pollJobUntilComplete(
    jobId: number,
    onProgress?: (status: JobStatusResponse) => void,
    intervalMs: number = 2000,
    maxAttempts: number = 90
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
    translatePrompt,
    rewritePrompt,
    previewPrompt,
    generateNode,
    getJobStatus,
    pollJobUntilComplete,
};
