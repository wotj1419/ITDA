import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TimelineClip } from '../types/ui'
import {
    fetchProjectTimeline,
    requestProjectMerge,
    fetchProjectExport,
    type TimelineItem,
} from '../services/api/timeline'
import { fetchProtectedBlobUrl } from '../services/api/media'
import { resolveApiUrl } from '../services/api/urls'
import { unconfirmNode } from '../services/api/nodes'
import {
    subscribeProjectEvents,
    type ProjectEventMessage,
    type ProjectEventPayload,
} from '../services/ws/projectEvents'

export type MergeStatus = 'idle' | 'merging' | 'done' | 'error'

function mapTimelineItemsToClips(items: TimelineItem[]): TimelineClip[] {
    return items.map((item) => ({
        clipId: `node-${item.videoNodeId}`,
        nodeId: item.videoNodeId,
        sceneId: item.sceneId,
        thumbnailUrl: item.thumbnailUrl || item.url || '',
        videoUrl: item.url || undefined,
        duration: typeof item.duration === 'number' && item.duration > 0 ? item.duration : 5,
        order: item.order,
        label: `영상 ${item.order}`,
    }))
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
    let mergePollTimer: ReturnType<typeof setInterval> | null = null
    const durationCache = new Map<string, number>()

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

    const readDurationFromUrl = (url: string): Promise<number | null> =>
        new Promise((resolve) => {
            const video = document.createElement('video')
            let settled = false
            const timeoutId = setTimeout(() => {
                if (settled) return
                settled = true
                resolve(null)
            }, 8000)

            const cleanup = () => {
                clearTimeout(timeoutId)
                video.removeAttribute('src')
                video.load()
            }

            video.preload = 'metadata'
            video.muted = true
            video.playsInline = true
            video.onloadedmetadata = () => {
                if (settled) return
                settled = true
                cleanup()
                const duration = Number.isFinite(video.duration)
                    ? Math.round(video.duration)
                    : null
                resolve(duration)
            }
            video.onerror = () => {
                if (settled) return
                settled = true
                cleanup()
                resolve(null)
            }
            video.src = url
        })

    const fetchVideoDuration = async (url: string): Promise<number | null> => {
        const cached = durationCache.get(url)
        if (cached) return Promise.resolve(cached)
        const resolved = resolveApiUrl(url) ?? url
        let duration = await readDurationFromUrl(resolved)

        if (!duration) {
            const blobUrl = await fetchProtectedBlobUrl(url).catch(() => null)
            if (blobUrl) {
                duration = await readDurationFromUrl(blobUrl)
                if (blobUrl.startsWith('blob:')) {
                    URL.revokeObjectURL(blobUrl)
                }
            }
        }

        if (duration && duration > 0) {
            durationCache.set(url, duration)
            return duration
        }

        return null
    }

    const hydrateClipDurations = async (targets: TimelineClip[]): Promise<void> => {
        await Promise.all(
            targets.map(async (clip) => {
                if (!clip.videoUrl) return
                const duration = await fetchVideoDuration(clip.videoUrl)
                if (duration && duration !== clip.duration) {
                    clip.duration = duration
                }
            })
        )
    }

    // Internal
    const stopMergePolling = () => {
        if (mergePollTimer) {
            clearInterval(mergePollTimer)
            mergePollTimer = null
        }
    }

    const markMergeDone = async () => {
        if (!currentProjectId.value) return
        mergeStatus.value = 'done'
        mergeProgress.value = 100
        mergeStatusText.value = '병합 완료'
        stopMergePolling()
        if (!downloadUrl.value) {
            try {
                downloadUrl.value = await fetchProjectExport(currentProjectId.value)
            } catch (error) {
                console.error('Failed to fetch export url', error)
            }
        }
    }

    const markMergeError = () => {
        mergeStatus.value = 'error'
        mergeStatusText.value = '병합 실패'
        stopMergePolling()
    }

    const startMergePolling = () => {
        stopMergePolling()
        const startedAt = Date.now()
        const maxDurationMs = 120000

        mergePollTimer = setInterval(async () => {
            if (!currentProjectId.value) return
            if (mergeStatus.value !== 'merging') {
                stopMergePolling()
                return
            }

            if (Date.now() - startedAt > maxDurationMs) {
                markMergeError()
                return
            }

            try {
                const url = await fetchProjectExport(currentProjectId.value)
                if (url) {
                    downloadUrl.value = url
                    await markMergeDone()
                }
            } catch (error) {
                // Ignore intermittent export errors while polling
                console.warn('Merge polling failed', error)
            }
        }, 3000)
    }

    const handleProjectEvent = async (event: ProjectEventMessage) => {
        const eventType = event.event
        const payload = event.data as ProjectEventPayload

        const isMergeEvent =
            payload?.type === 'PROJECT_MERGE' ||
            (mergeJobId.value != null && payload?.jobId === mergeJobId.value)

        if (isMergeEvent) {
            const targetId = payload.target?.id
            if (currentProjectId.value && targetId && targetId !== currentProjectId.value) return

            if (eventType === 'job.failed' || payload.status === 'FAILED') {
                markMergeError()
                return
            }

            if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
                await markMergeDone()
            }
        } else if (payload?.status === 'SUCCEEDED' && mergeStatus.value === 'merging') {
            // Fallback: some events may omit type/jobId, but signal completion
            await markMergeDone()
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
            const response = await fetchProjectTimeline(projectId)
            let nextClips = mapTimelineItemsToClips(response.items)
            if (sceneId) {
                nextClips = nextClips.filter((clip) => clip.sceneId === sceneId)
            }
            clips.value = nextClips
            void hydrateClipDurations(nextClips)
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
        startMergePolling()

        try {
            const result = await requestProjectMerge(currentProjectId.value)
            mergeJobId.value = result.jobId
            mergeStatusText.value = '병합 진행 중'
            return true
        } catch (e) {
            markMergeError()
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
        stopMergePolling()
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
