/**
 * useZoom - 줌 기능을 위한 컴포저블
 * @module composables/useZoom
 */
import { ref, computed, type ComputedRef, type Ref } from 'vue';

interface ZoomOptions {
    initialZoom?: number;
    minZoom?: number;
    maxZoom?: number;
    step?: number;
}

interface UseZoomReturn {
    zoomLevel: Ref<number>;
    zoomPercentage: ComputedRef<string>;
    zoomScale: ComputedRef<number>;
    zoomIn: (customStep?: number) => void;
    zoomOut: (customStep?: number) => void;
    zoomFit: () => void;
    setZoom: (level: number) => void;
}

/**
 * 줌 기능을 제공하는 컴포저블
 *
 * @example
 * ```ts
 * const { zoomLevel, zoomPercentage, zoomIn, zoomOut, zoomFit } = useZoom()
 * ```
 */
export function useZoom(options: ZoomOptions = {}): UseZoomReturn {
    const {
        initialZoom = 100,
        minZoom = 50,
        maxZoom = 200,
        step = 10,
    } = options;

    // State
    const zoomLevel = ref(initialZoom);

    // Computed
    const zoomPercentage = computed(() => `${zoomLevel.value}%`);
    const zoomScale = computed(() => zoomLevel.value / 100);

    // Methods
    function zoomIn(customStep = step): void {
        zoomLevel.value = Math.min(maxZoom, zoomLevel.value + customStep);
    }

    function zoomOut(customStep = step): void {
        zoomLevel.value = Math.max(minZoom, zoomLevel.value - customStep);
    }

    function zoomFit(): void {
        zoomLevel.value = 100;
    }

    function setZoom(level: number): void {
        zoomLevel.value = Math.max(minZoom, Math.min(maxZoom, level));
    }

    return {
        zoomLevel,
        zoomPercentage,
        zoomScale,
        zoomIn,
        zoomOut,
        zoomFit,
        setZoom,
    };
}
