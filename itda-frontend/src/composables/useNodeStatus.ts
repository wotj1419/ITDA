import { computed } from 'vue';
import type { Component } from 'vue';
import { AlertCircle, CheckCircle, Loader2 } from 'lucide-vue-next';
import { JobStatus, type GenerationState } from '../types/ui/sceneNodes';

type StatusKey = JobStatus | 'idle';

export function useNodeStatus(
  getJobStatus: () => JobStatus | null,
  getGenerationState: () => GenerationState | null
) {
  const statusKey = computed<StatusKey>(() => getJobStatus() ?? 'idle');
  const statusIcon = computed<Component | null>(() => {
    const icons: Record<JobStatus, Component | null> = {
      [JobStatus.PENDING]: null,
      [JobStatus.RUNNING]: Loader2,
      [JobStatus.SUCCEEDED]: CheckCircle,
      [JobStatus.FAILED]: AlertCircle,
    };
    return icons[getJobStatus() as JobStatus] ?? null;
  });

  const isRunning = computed(() => getJobStatus() === JobStatus.RUNNING);
  const isGenerationRequested = computed(() => getGenerationState() === 'requested');
  const isGenerationFailed = computed(() => getGenerationState() === 'failed');
  const hasGenerationFailure = computed(
    () => isGenerationFailed.value || getJobStatus() === JobStatus.FAILED
  );

  return {
    statusKey,
    statusIcon,
    isRunning,
    isGenerationRequested,
    isGenerationFailed,
    hasGenerationFailure,
  };
}
