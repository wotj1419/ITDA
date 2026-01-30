import { ref } from 'vue';
import type { AnyNodeData } from '../types/ui/sceneNodes';
import type { GeneratePromptRequest } from '../types/api/ai';
import { JobStatus, PromptStatus } from '../types/ui/sceneNodes';
import { useSceneNodeStore } from '../stores/sceneNode';
import { resolveApiUrl, isApiResourceUrl } from '../services/api/urls';
import { fetchProtectedBlobUrl } from '../services/api/media';
import { SHOT_FALLBACK_THUMBNAIL } from '../utils/fallbacks';
import { useGenerationToast } from './useGenerationToast';
import { aiService } from '../services';

export type GenerationToastType = 'image' | 'video' | 'shot' | 'grid';

interface UseNodeGenerationOptions {
  nodeId: string;
  nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
  toastType: GenerationToastType;
  getPrompt: () => string;
  getPromptPayload: () => GeneratePromptRequest;
  getPromptUpdate: (prompt: string) => Partial<AnyNodeData>;
  getApprovedUpdate: () => Partial<AnyNodeData>;
  getJobSettings: () => Record<string, unknown>;
  getJobSuccessUpdate: (result: { resultUrl?: string; thumbnailUrl?: string | null }) => Partial<AnyNodeData>;
  messages?: {
    promptError?: string;
    jobError?: string;
  };
}

export function useNodeGeneration(options: UseNodeGenerationOptions) {
  const nodeStore = useSceneNodeStore();
  const { startGenerationToast, finishGenerationToast } = useGenerationToast();

  const isGeneratingPrompt = ref(false);
  const isGeneratingJob = ref(false);
  const errorMessage = ref<string | null>(null);

  const clearError = () => {
    errorMessage.value = null;
  };

  const generatePrompt = async (): Promise<void> => {
    if (isGeneratingPrompt.value || isGeneratingJob.value) return;
    isGeneratingPrompt.value = true;
    errorMessage.value = null;

    // 프롬프트 생성 시작 토스트
    const toastId = startGenerationToast('prompt');

    try {
      const prompt = await aiService.generatePrompt(options.getPromptPayload());
      nodeStore.updateNode(options.nodeId, {
        ...options.getPromptUpdate(prompt),
        promptStatus: PromptStatus.GENERATED,
      });
      // 성공 토스트
      finishGenerationToast(toastId, 'prompt', 'success');
    } catch (error) {
      console.error('Failed to generate prompt:', error);
      errorMessage.value = options.messages?.promptError ?? '프롬프트 생성에 실패했습니다. 다시 시도해주세요.';
      // 실패 토스트
      finishGenerationToast(toastId, 'prompt', 'error', {
        reason: error instanceof Error ? error.message : '알 수 없는 오류',
      });
    } finally {
      isGeneratingPrompt.value = false;
    }
  };

  const approvePrompt = (): void => {
    nodeStore.updateNode(options.nodeId, {
      ...options.getApprovedUpdate(),
      promptStatus: PromptStatus.APPROVED,
    });
  };

  const runGeneration = async (): Promise<void> => {
    if (isGeneratingPrompt.value || isGeneratingJob.value) return;
    const prompt = options.getPrompt();
    if (!prompt) return;

    isGeneratingJob.value = true;
    errorMessage.value = null;
    const toastId = startGenerationToast(options.toastType);

    try {
      nodeStore.updateNodeLocal(options.nodeId, {
        jobStatus: JobStatus.RUNNING,
        generationState: 'requested',
      });

      const jobId = await aiService.generateNode(options.nodeId, prompt, {
        nodeType: options.nodeType,
        settings: options.getJobSettings(),
      });

      const result = await aiService.pollJobUntilComplete(jobId, (status) => {
        console.log('Job status:', status.status);
      });

      if (result.status === 'SUCCEEDED') {
        const node = nodeStore.nodes.find((item) => item.id === options.nodeId);
        const blobUrl = await fetchProtectedBlobUrl(result.resultUrl).catch(() => null);
        const resolvedResultUrl =
          blobUrl ?? (isApiResourceUrl(result.resultUrl) ? null : resolveApiUrl(result.resultUrl));
        const thumbnailCandidate = result.thumbnailUrl ?? result.resultUrl ?? null;
        const resolvedThumbnailUrl =
          options.nodeType === 'VIDEO'
            ? (isApiResourceUrl(thumbnailCandidate) ? null : resolveApiUrl(thumbnailCandidate))
            : blobUrl ?? (isApiResourceUrl(thumbnailCandidate) ? null : resolveApiUrl(thumbnailCandidate));
        nodeStore.updateNodeLocal(options.nodeId, {
          jobStatus: JobStatus.SUCCEEDED,
          generationState: null,
          ...options.getJobSuccessUpdate({
            resultUrl: resolvedResultUrl ?? undefined,
            thumbnailUrl: resolvedThumbnailUrl,
          }),
        });
        finishGenerationToast(toastId, options.toastType, 'success');
      } else {
        throw new Error(result.error?.message || 'Generation failed');
      }
    } catch (error) {
      console.error('Failed to generate node:', error);
      errorMessage.value = options.messages?.jobError ?? '생성에 실패했습니다. 다시 시도해주세요.';
      const node = nodeStore.nodes.find((item) => item.id === options.nodeId);
      const hasUrl = Boolean(node?.data?.thumbnailUrl || node?.data?.imageUrl);
      if (options.nodeType === 'SHOT' && !hasUrl) {
        nodeStore.updateNodeLocal(options.nodeId, {
          jobStatus: JobStatus.FAILED,
          generationState: 'failed',
          imageUrl: SHOT_FALLBACK_THUMBNAIL,
          thumbnailUrl: SHOT_FALLBACK_THUMBNAIL,
        });
      } else {
        nodeStore.updateNodeLocal(options.nodeId, {
          jobStatus: JobStatus.FAILED,
          generationState: 'failed',
        });
      }
      const reason = error instanceof Error ? error.message : '알 수 없는 오류';
      finishGenerationToast(toastId, options.toastType, 'error', { reason });
    } finally {
      isGeneratingJob.value = false;
    }
  };

  return {
    isGeneratingPrompt,
    isGeneratingJob,
    errorMessage,
    clearError,
    generatePrompt,
    approvePrompt,
    runGeneration,
  };
}
