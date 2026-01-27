import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TimelineClip } from '../types/ui'
import type { TimelineItem } from '../types/api/timeline'
import {
    fetchProjectTimeline,
    fetchSceneTimeline,
    requestProjectMerge,
    requestSceneMerge,
    fetchProjectExport,
} from '../services/api/timeline'
import { fetchProtectedBlobUrl } from '../services/api/media'
import { unconfirmNode } from '../services/api/nodes'
import {
    subscribeProjectEvents,
    type ProjectEventMessage,
    type ProjectEventPayload,
} from '../services/ws/projectEvents'

export type MergeStatus = 'idle' | 'merging' | 'done' | 'error'
const DEFAULT_CLIP_SECONDS = 5

function toDurationSeconds(duration: number): number {
    if (!Number.isFinite(duration)) return 0
    if (duration >= 1000) {
        return Math.max(0, Math.round(duration / 1000))
    }
    return Math.max(0, duration)
}

async function resolveMediaUrl(url?: string | null): Promise<string | undefined> {
    if (!url) return undefined
    const blobUrl = await fetchProtectedBlobUrl(url).catch(() => null)
    return blobUrl ?? url
}

async function mapTimelineItemsToClips(items: TimelineItem[]): Promise<TimelineClip[]> {
    return Promise.all(
        items.map(async (item) => {
            const clipKey = item.videoNodeId ?? item.sceneVideoId ?? `${item.sceneId}-${item.order}`
            const resolvedUrl = await resolveMediaUrl(item.thumbnailUrl)
            const durationSeconds = toDurationSeconds(item.duration)
            const duration = durationSeconds > 0 ? durationSeconds : DEFAULT_CLIP_SECONDS
            const isVideoClip = Boolean(item.videoNodeId || item.sceneVideoId)

            return {
                clipId: `clip-${clipKey}`,
                nodeId: item.videoNodeId ?? `scene-video-${item.sceneVideoId ?? item.order}`,
                sceneId: item.sceneId,
                thumbnailUrl: resolvedUrl || '',
                videoUrl: isVideoClip ? (resolvedUrl || undefined) : undefined,
                duration,
                order: item.order,
                label: item.sceneTitle ? `${item.sceneTitle}` : `Clip ${item.order}`,
            }
        })
    )
}


export const useTimelineStore = defineStore('timeline', () => {
    // State
    const clips = ref<TimelineClip[]>([])
    const isLoading = ref(false)
    const error = ref<string | null>(null)
    const currentProjectId = ref<number | null>(null)
    const currentSceneId = ref<number | null>(null)

    // Merge state
    const mergeStatus = ref<MergeStatus>('idle')
    const mergeProgress = ref(0)
    const mergeStatusText = ref('')
    const downloadUrl = ref<string | null>(null)
    const mergeJobId = ref<number | null>(null)

    let unsubscribeProjectEvents: (() => void) | null = null

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
        mergeStatus.value === 'done' && !!downloadUrl.value
    )

    // Internal
    const handleProjectEvent = async (event: ProjectEventMessage) => {
        const eventType = event.event
        const payload = event.data as ProjectEventPayload

        if (!payload?.type) return

        const targetId = payload.target?.id
        const targetType = payload.target?.type

        const isProjectMerge = payload.type === 'PROJECT_MERGE'
        const isSceneMerge = payload.type === 'SCENE_MERGE'

        if (isProjectMerge) {
            if (targetType && targetType !== 'PROJECT') return
            if (currentProjectId.value && targetId && targetId !== currentProjectId.value) return

            if (eventType === 'job.failed' || payload.status === 'FAILED') {
                mergeStatus.value = 'error'
                mergeStatusText.value = '병합 실패'
                return
            }

            if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
                mergeStatus.value = 'done'
                mergeProgress.value = 100
                mergeStatusText.value = '병합 완료'
                if (currentProjectId.value) {
                    downloadUrl.value = await fetchProjectExport(currentProjectId.value)
                }
            }
        }

        if (isSceneMerge) {
            if (targetType && targetType !== 'SCENE') return
            if (currentSceneId.value && targetId && targetId !== currentSceneId.value) return

            if (eventType === 'job.failed' || payload.status === 'FAILED') {
                mergeStatus.value = 'error'
                mergeStatusText.value = '병합 실패'
                return
            }

            if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
                mergeStatus.value = 'done'
                mergeProgress.value = 100
                mergeStatusText.value = '병합 완료'
            }
        }
    }

    const ensureProjectSubscription = (projectId: number) => {
        if (unsubscribeProjectEvents) {
            unsubscribeProjectEvents()
            unsubscribeProjectEvents = null
        }
        unsubscribeProjectEvents = subscribeProjectEvents(projectId, handleProjectEvent)
    }

    // Actions
    async function loadClips(projectId: number, sceneId?: number): Promise<void> {
        isLoading.value = true
        error.value = null
        currentProjectId.value = projectId
        currentSceneId.value = sceneId ?? null

        try {
            ensureProjectSubscription(projectId)
            if (sceneId) {
                const response = await fetchSceneTimeline(sceneId)
                clips.value = await mapTimelineItemsToClips(response.items)
                return
            }
            const response = await fetchProjectTimeline(projectId)
            clips.value = await mapTimelineItemsToClips(response.items)
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
            const orderMap = new Map(clipIds.map((id, index) => [id, index + 1]))
            clips.value = clips.value.map((clip) => ({
                ...clip,
                order: orderMap.get(clip.clipId) ?? clip.order,
            }))
            return true
        } catch (e) {
            console.error(e)
            return false
        }
    }

    async function removeClip(clipId: string): Promise<boolean> {
        if (!currentProjectId.value) return false

        try {
            const target = clips.value.find((c) => c.clipId === clipId)
            if (target && typeof target.nodeId === 'number') {
                await unconfirmNode(target.nodeId)
            }
            clips.value = clips.value.filter((c) => c.clipId !== clipId)
            clips.value.forEach((c, i) => (c.order = i + 1))
            return true
        } catch (e) {
            console.error(e)
            return false
        }
    }

    async function startMerge(): Promise<boolean> {
        if (!currentProjectId.value || !canMerge.value) return false

        mergeStatus.value = 'merging'
        mergeProgress.value = 0
        mergeStatusText.value = '병합 시작'
        downloadUrl.value = null

        try {
            const result = currentSceneId.value
                ? await requestSceneMerge(currentSceneId.value)
                : await requestProjectMerge(currentProjectId.value)
            mergeJobId.value = result.jobId
            mergeStatusText.value = '병합 진행 중'
            return true
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
        mergeJobId.value = null
    }

    function clearTimeline(): void {
        clips.value = []
        currentProjectId.value = null
        currentSceneId.value = null
        error.value = null
        resetMerge()
        if (unsubscribeProjectEvents) {
            unsubscribeProjectEvents()
            unsubscribeProjectEvents = null
        }
    }

    return {
        // State
        clips,
        isLoading,
        error,
        currentProjectId,
        currentSceneId,
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
