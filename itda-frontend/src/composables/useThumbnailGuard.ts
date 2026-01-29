import { computed, ref, watch } from 'vue';
import { isFallbackThumbnail } from '../utils/fallbacks';

const MIN_THUMBNAIL_SIZE = 2;

export function useThumbnailGuard(getThumbnailUrl: () => string | null | undefined) {
  const isLoaded = ref(false);
  const isBlocked = ref(false);

  const hasSource = computed(() => {
    const url = getThumbnailUrl();
    return Boolean(url && !isFallbackThumbnail(url));
  });

  const isVisible = computed(
    () => hasSource.value && isLoaded.value && !isBlocked.value
  );
  const isLoading = computed(
    () => hasSource.value && !isLoaded.value && !isBlocked.value
  );
  const isBlockedState = computed(() => isBlocked.value);

  watch(
    getThumbnailUrl,
    () => {
      isLoaded.value = false;
      isBlocked.value = false;
    },
    { immediate: true }
  );

  function handleLoad(event: Event): void {
    isLoaded.value = true;
    const img = event.target as HTMLImageElement | null;
    if (!img) return;
    const width = img.naturalWidth || 0;
    const height = img.naturalHeight || 0;
    if (width < MIN_THUMBNAIL_SIZE || height < MIN_THUMBNAIL_SIZE) {
      isBlocked.value = true;
    }
  }

  function handleError(): void {
    isLoaded.value = true;
    isBlocked.value = true;
  }

  return {
    hasSource,
    isVisible,
    isLoading,
    isBlocked: isBlockedState,
    handleLoad,
    handleError,
  };
}
