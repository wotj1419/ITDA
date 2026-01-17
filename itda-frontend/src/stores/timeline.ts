import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TimelineClip } from '../types'
import {
    fetchTimelineClips as mockFetchClips,
    reorderClips as mockReorderClips,
    removeClip as mockRemoveClip,
    mergeVideos as mockMergeVideos,
} from '../services/mock/timeline'

export type MergeStatus = 'idle' | 'merging' | 'done' | 'error'

export const useTimelineStore = defineStore('timeline', () => {
    // State
    const clips = ref<TimelineClip[]>([])
    const isLoading = ref(false)
    const error = ref<string | null>(null)
    const currentProjectId = ref<number | null>(null)

    // Merge state
    const mergeStatus = ref<MergeStatus>('idle')
    const mergeProgress = ref(0)
    const mergeStatusText = ref('')
    const downloadUrl = ref<string | null>(null)

    // Getters
    const orderedClips = computed(() =>
        [...clips.value].sort((a, b) => a.order - b.order)
    )

    const totalDuration = computed(() =>
        clips.value.reduce((sum, c) => sum + c.duration, 0)
    )

    const clipCount = computed(() => clips.value.length)

    const canMerge = computed(() =>
        clips.value.length > 0 && mergeStatus.value !== 'merging'
    )

    const canDownload = computed(() =>
        mergeStatus.value === 'done' && downloadUrl.value
    )

    // Actions
    async function loadClips(projectId: number): Promise<void> {
        isLoading.value = true
        error.value = null
        currentProjectId.value = projectId

        try {
            clips.value = await mockFetchClips(projectId)
        } catch (e) {
            error.value = 'Failed to load clips'
            console.error(e)
        } finally {
            isLoading.value = false
        }
    }

    async function reorderClips(clipIds: string[]): Promise<boolean> {
        if (!currentProjectId.value) return false

        try {
            const reordered = await mockReorderClips(currentProjectId.value, clipIds)
            clips.value = reordered
            return true
        } catch (e) {
            console.error(e)
            return false
        }
    }

    async function removeClip(clipId: string): Promise<boolean> {
        if (!currentProjectId.value) return false

        try {
            const success = await mockRemoveClip(currentProjectId.value, clipId)
            if (success) {
                clips.value = clips.value.filter((c) => c.clipId !== clipId)
                clips.value.forEach((c, i) => (c.order = i + 1))
            }
            return success
        } catch (e) {
            console.error(e)
            return false
        }
    }

    async function startMerge(): Promise<boolean> {
        if (!currentProjectId.value || !canMerge.value) return false

        mergeStatus.value = 'merging'
        mergeProgress.value = 0
        mergeStatusText.value = ''
        downloadUrl.value = null

        try {
            const result = await mockMergeVideos(
                currentProjectId.value,
                (percent, status) => {
                    mergeProgress.value = percent
                    mergeStatusText.value = status
                }
            )

            if (result.success) {
                mergeStatus.value = 'done'
                downloadUrl.value = result.downloadUrl || null
                return true
            } else {
                mergeStatus.value = 'error'
                return false
            }
        } catch (e) {
            mergeStatus.value = 'error'
            console.error(e)
            return false
        }
    }

    function resetMerge(): void {
        mergeStatus.value = 'idle'
        mergeProgress.value = 0
        mergeStatusText.value = ''
        downloadUrl.value = null
    }

    function clearTimeline(): void {
        clips.value = []
        currentProjectId.value = null
        error.value = null
        resetMerge()
    }

    return {
        // State
        clips,
        isLoading,
        error,
        currentProjectId,
        mergeStatus,
        mergeProgress,
        mergeStatusText,
        downloadUrl,
        // Getters
        orderedClips,
        totalDuration,
        clipCount,
        canMerge,
        canDownload,
        // Actions
        loadClips,
        reorderClips,
        removeClip,
        startMerge,
        resetMerge,
        clearTimeline,
    }
})
