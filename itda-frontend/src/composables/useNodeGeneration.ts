import { ref } from 'vue';
import type { AnyNodeData } from '../types/ui/sceneNodes';
import type { GeneratePromptRequest, GeneratePromptResponse, PromptPreviewRequest, PromptPreviewResponse } from '../types/api/ai';
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
  getImproveInstruction?: () => string;
  getImproveContext?: () => { sceneOneLine?: string };
  getPromptPayload: () => GeneratePromptRequest;
  getPromptUpdate: (result: GeneratePromptResponse) => Partial<AnyNodeData>;
  getApprovedUpdate: () => Partial<AnyNodeData>;
  getJobSettings: () => Record<string, unknown>;
  getReferenceObjectIds?: () => number[] | undefined;
  getPromptOverride?: () => string | undefined;
  getPromptPreviewPayload?: () => PromptPreviewRequest;
  onPromptPreview?: (result: PromptPreviewResponse) => Partial<AnyNodeData>;
  previewEnabled?: boolean;
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

  const refreshPromptPreview = async (force = false, promptOverride?: string): Promise<void> => {
    if (!force && !options.previewEnabled) return;
    if (!options.getPromptPreviewPayload) return;
    try {
      const payload = options.getPromptPreviewPayload();
      if (typeof promptOverride === 'string' && promptOverride.trim()) {
        payload.prompt = promptOverride;
      }
      const result = await aiService.previewPrompt(options.nodeId, payload);
      if (options.onPromptPreview) {
        nodeStore.updateNodeLocal(options.nodeId, options.onPromptPreview(result));
      }
    } catch (error) {
      console.error('Failed to preview prompt:', error);
    }
  };

  const generatePrompt = async (): Promise<void> => {
    if (isGeneratingPrompt.value || isGeneratingJob.value) return;
    isGeneratingPrompt.value = true;
    errorMessage.value = null;

    // 프롬프트 생성 시작 토스트
    const toastId = startGenerationToast('prompt');

    try {
      const currentPrompt = options.getPrompt().trim();
      const instruction = options.getImproveInstruction?.().trim() ?? '';
      const shouldImprove = currentPrompt.length > 0 && instruction.length > 0;
      let promptPayload = options.getPromptPayload();
      if (!shouldImprove && instruction.length === 0 && currentPrompt.length > 0) {
        // Force regeneration from current inputs instead of echoing the existing prompt.
        promptPayload = { ...promptPayload, prompt: '' };
      }
      const improveContext = options.getImproveContext?.();
      const result = shouldImprove
        ? await aiService.improvePrompt(currentPrompt, instruction, options.nodeType, improveContext)
        : await aiService.generatePrompt(promptPayload);
      nodeStore.updateNode(options.nodeId, {
        ...options.getPromptUpdate(result),
        promptStatus: PromptStatus.GENERATED,
      });
      await refreshPromptPreview(true, result.promptEnBase);
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
        promptEnFinalOverride: options.getPromptOverride?.(),
        referenceObjectIds: options.getReferenceObjectIds?.(),
        // Idempotency key reuse로 기존 PENDING/FAILED Job이 반환되면 재큐잉하여 timeout 가능성을 줄인다.
        requeueIfExisting: true,
      });

      const result = await aiService.pollJobUntilComplete(jobId, (status) => {
        console.log('Job status:', status.status);
      });

      if (result.status === 'SUCCEEDED') {
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
    refreshPromptPreview,
    runGeneration,
  };
}
