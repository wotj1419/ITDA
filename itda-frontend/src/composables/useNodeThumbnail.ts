import { computed } from 'vue';
import { useThumbnailGuard } from './useThumbnailGuard';

interface NodeThumbnailOptions {
  getThumbnailUrl: () => string | null | undefined;
  isRunning: () => boolean;
  isGenerationRequested: () => boolean;
  hasGenerationFailure: () => boolean;
  getPrimaryMediaUrl?: () => string | null | undefined;
}

export function useNodeThumbnail(options: NodeThumbnailOptions) {
  const {
    hasSource,
    isVisible,
    isLoading: isGuardLoading,
    isBlocked,
    handleLoad,
    handleError,
  } = useThumbnailGuard(options.getThumbnailUrl);

  const isThumbnailLoading = computed(
    () =>
      options.isGenerationRequested() ||
      options.isRunning() ||
      isGuardLoading.value
  );

  const hasPrimaryMedia = computed(() => {
    if (!options.getPrimaryMediaUrl) return false;
    return Boolean(options.getPrimaryMediaUrl());
  });

  const showFailureOverlay = computed(() => {
    if (!options.hasGenerationFailure()) return false;
    if (isVisible.value || isThumbnailLoading.value) return false;
    if (hasPrimaryMedia.value) return false;
    return true;
  });

  return {
    hasSource,
    isVisible,
    isBlocked,
    isThumbnailLoading,
    showFailureOverlay,
    handleLoad,
    handleError,
  };
}
