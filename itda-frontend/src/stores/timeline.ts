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
  fetchSceneExport,
  reorderProjectTimeline,
  reorderSceneTimeline,
} from '../services/api/timeline'
import { fetchProtectedBlobUrl } from '../services/api/media'
import { resolveApiUrl, isApiResourceUrl } from '../services/api/urls'
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
  if (!isApiResourceUrl(url)) {
    return url
  }
  const blobUrl = await fetchProtectedBlobUrl(url).catch(() => null)
  return blobUrl ?? url
}

async function mapTimelineItemsToClips(items: TimelineItem[]): Promise<TimelineClip[]> {
  return Promise.all(
    items.map(async (item) => {
      const clipKey = item.videoNodeId ?? item.sceneVideoId ?? `${item.sceneId}-${item.order}`
      const resolvedThumbnail = await resolveMediaUrl(item.thumbnailUrl)
      const videoCandidate = item.videoUrl ?? item.url
      const resolvedVideo = await resolveMediaUrl(videoCandidate)
      const durationSeconds = toDurationSeconds(item.duration)
      const duration = durationSeconds > 0 ? durationSeconds : DEFAULT_CLIP_SECONDS
      const isVideoClip = Boolean(item.videoNodeId || item.sceneVideoId)

      return {
        clipId: `clip-${clipKey}`,
        nodeId: item.videoNodeId ?? `scene-video-${item.sceneVideoId ?? item.order}`,
        videoNodeId: item.videoNodeId,
        sceneVideoId: item.sceneVideoId,
        sceneId: item.sceneId,
        thumbnailUrl: resolvedThumbnail || '',
        videoUrl: isVideoClip ? resolvedVideo || undefined : undefined,
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
  let mergePollTimer: ReturnType<typeof setInterval> | null = null
  const durationCache = new Map<string, number>()

  let unsubscribeProjectEvents: (() => void) | null = null

  // Getters
  const orderedClips = computed(() => [...clips.value].sort((a, b) => a.order - b.order))
  const totalDuration = computed(() => clips.value.reduce((sum, c) => sum + c.duration, 0))
  const clipCount = computed(() => clips.value.length)
  const canMerge = computed(() => clips.value.length > 0 && mergeStatus.value !== 'merging')
  const canDownload = computed(() => mergeStatus.value === 'done' && !!downloadUrl.value)

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
        const duration = Number.isFinite(video.duration) ? Math.round(video.duration) : null
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
    if (resolved.startsWith('blob:') || resolved.startsWith('data:')) {
      const duration = await readDurationFromUrl(resolved)
      if (duration && duration > 0) {
        durationCache.set(url, duration)
      }
      return duration
    }
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

  const fetchExportUrl = async (): Promise<string | null> => {
    if (currentSceneId.value) {
      return fetchSceneExport(currentSceneId.value)
    }
    if (currentProjectId.value) {
      return fetchProjectExport(currentProjectId.value)
    }
    return null
  }

  // Internal
  const stopMergePolling = () => {
    if (mergePollTimer) {
      clearInterval(mergePollTimer)
      mergePollTimer = null
    }
  }

  const markMergeDone = async () => {
    mergeStatus.value = 'done'
    mergeProgress.value = 100
    mergeStatusText.value = '병합 완료'
    stopMergePolling()
    if (!downloadUrl.value) {
      try {
        downloadUrl.value = await fetchExportUrl()
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
      if (mergeStatus.value !== 'merging') {
        stopMergePolling()
        return
      }

      if (Date.now() - startedAt > maxDurationMs) {
        markMergeError()
        return
      }

      try {
        const url = await fetchExportUrl()
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

    const isProjectMerge = payload?.type === 'PROJECT_MERGE'
    const isSceneMerge = payload?.type === 'SCENE_MERGE'

    if (isProjectMerge) {
      const targetId = payload.target?.id
      const targetType = payload.target?.type
      if (targetType && targetType !== 'PROJECT') return
      if (currentProjectId.value && targetId && targetId !== currentProjectId.value) return

      if (eventType === 'job.failed' || payload.status === 'FAILED') {
        markMergeError()
        return
      }
      if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
        await markMergeDone()
      }
      return
    }

    if (isSceneMerge) {
      const targetId = payload.target?.id
      const targetType = payload.target?.type
      if (targetType && targetType !== 'SCENE') return
      if (currentSceneId.value && targetId && targetId !== currentSceneId.value) return

      if (eventType === 'job.failed' || payload.status === 'FAILED') {
        markMergeError()
        return
      }
      if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
        await markMergeDone()
      }
      return
    }

    const isMergeEvent =
      payload?.type === 'PROJECT_MERGE' ||
      payload?.type === 'SCENE_MERGE' ||
      (mergeJobId.value != null && payload?.jobId === mergeJobId.value)

    if (isMergeEvent) {
      if (eventType === 'job.failed' || payload.status === 'FAILED') {
        markMergeError()
        return
      }
      if (eventType === 'job.done' || payload.status === 'SUCCEEDED') {
        await markMergeDone()
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
        const nextClips = await mapTimelineItemsToClips(response.items)
        clips.value = nextClips
        void hydrateClipDurations(nextClips)
        return
      }
      const response = await fetchProjectTimeline(projectId)
      const nextClips = await mapTimelineItemsToClips(response.items)
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
      const orderedClips = clipIds
        .map((clipId) => clips.value.find((clip) => clip.clipId === clipId))
        .filter((clip): clip is TimelineClip => Boolean(clip))

      if (orderedClips.length !== clipIds.length) {
        return false
      }

      if (currentSceneId.value) {
        const orderedVideoNodeIds = orderedClips
          .map((clip) => clip.videoNodeId)
          .filter((id): id is number => typeof id === 'number')

        if (orderedVideoNodeIds.length !== orderedClips.length) {
          return false
        }

        await reorderSceneTimeline(currentSceneId.value, orderedVideoNodeIds)
      } else {
        const orderedSceneVideoIds = orderedClips
          .map((clip) => clip.sceneVideoId)
          .filter((id): id is number => typeof id === 'number')

        if (orderedSceneVideoIds.length !== orderedClips.length) {
          return false
        }

        await reorderProjectTimeline(currentProjectId.value, orderedSceneVideoIds)
      }

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
      const result = currentSceneId.value
        ? await requestSceneMerge(currentSceneId.value)
        : await requestProjectMerge(currentProjectId.value)
      mergeJobId.value = result.jobId

      if (result.status === 'SUCCEEDED') {
        mergeStatus.value = 'done'
        mergeProgress.value = 100
        mergeStatusText.value = '병합 완료 (캐시됨)'
        downloadUrl.value = await fetchExportUrl()
      } else if (result.status === 'FAILED') {
        mergeStatus.value = 'error'
        mergeStatusText.value = '병합 실패'
      } else {
        mergeStatusText.value = '병합 진행 중'
      }

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
