import { computed } from 'vue';
import type { ComputedRef } from 'vue';

export function useLayoutButtonPosition(timelineClips: ComputedRef<unknown[]>) {
  const layoutButtonBottom = computed(() => {
    const baseBottom = 80;
    const timelineHeight = timelineClips.value.length > 0 ? 30 : 0;
    return baseBottom + timelineHeight;
  });

  return { layoutButtonBottom };
}
