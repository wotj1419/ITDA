import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TimelineClip } from '../types'
import type { AnyNodeData, VideoNodeData } from '../types/node'
import { NodeType } from '../types/node'
import {
    fetchTimelineClips as mockFetchClips,
    reorderClips as mockReorderClips,
    removeClip as mockRemoveClip,
    mergeVideos as mockMergeVideos,
} from '../services/mock/timeline'

export type MergeStatus = 'idle' | 'merging' | 'done' | 'error'

type StoredSceneNode = {
    id: string
    data?: AnyNodeData
}

type StoredSceneData = {
    nodes: StoredSceneNode[]
    edges?: unknown[]
    updatedAt?: string
}

function getStorageKey(sceneId: number | string): string {
    return `scene-nodes-${sceneId}`
}

function readSceneData(sceneId: number | string): StoredSceneData | null {
    const saved = localStorage.getItem(getStorageKey(sceneId))
    if (!saved) return null
    try {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed.nodes)) {
            return parsed as StoredSceneData
        }
    } catch (e) {
        console.error('Failed to parse scene nodes:', e)
    }
    return null
}

function writeSceneData(sceneId: number | string, data: StoredSceneData): void {
    localStorage.setItem(
        getStorageKey(sceneId),
        JSON.stringify({ ...data, updatedAt: new Date().toISOString() })
    )
}

function buildClipsFromScene(sceneId: number | string): TimelineClip[] {
    const data = readSceneData(sceneId)
    if (!data) return []

    const confirmed = data.nodes
        .filter(
            (node) =>
                node.data?.type === NodeType.VIDEO &&
                (node.data as VideoNodeData).isConfirmed
        )
        .sort((a, b) => {
            const orderA = (a.data as VideoNodeData).timelineOrder ?? Number.MAX_SAFE_INTEGER
            const orderB = (b.data as VideoNodeData).timelineOrder ?? Number.MAX_SAFE_INTEGER
            if (orderA !== orderB) return orderA - orderB
            const createdA = a.data?.createdAt ?? ''
            const createdB = b.data?.createdAt ?? ''
            return createdA.localeCompare(createdB)
        })

    return confirmed.map((node, index) => {
        const videoData = node.data as VideoNodeData
        return {
            clipId: node.id,
            nodeId: node.id,
            sceneId: Number(sceneId) || undefined,
            sourceNodeId: node.id,
            thumbnailUrl: videoData.thumbnailUrl || '',
            videoUrl: videoData.videoUrl || undefined,
            duration: videoData.duration || 5,
            order: index + 1,
            label: `영상 ${videoData.version || 1}`,
        }
    })
}

function updateSceneTimelineOrder(
    sceneId: number | string,
    clipIds: string[]
): TimelineClip[] {
    const data = readSceneData(sceneId)
    if (!data) return []

    const orderMap = new Map(clipIds.map((id, index) => [id, index + 1]))
    data.nodes.forEach((node) => {
        if (node.data?.type !== NodeType.VIDEO) return
        const videoData = node.data as VideoNodeData
        if (!videoData.isConfirmed) return
        const nextOrder = orderMap.get(node.id)
        videoData.timelineOrder = nextOrder
        videoData.updatedAt = new Date().toISOString()
    })

    writeSceneData(sceneId, data)
    return buildClipsFromScene(sceneId)
}

function unconfirmSceneClip(sceneId: number | string, clipId: string): boolean {
    const data = readSceneData(sceneId)
    if (!data) return false

    const target = data.nodes.find((node) => node.id === clipId)
    if (!target?.data || target.data.type !== NodeType.VIDEO) return false

    const videoData = target.data as VideoNodeData
    videoData.isConfirmed = false
    videoData.timelineOrder = undefined
    videoData.updatedAt = new Date().toISOString()

    writeSceneData(sceneId, data)
    return true
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
    async function loadClips(projectId: number, sceneId?: number): Promise<void> {
        isLoading.value = true
        error.value = null
        currentProjectId.value = projectId
        currentSceneId.value = sceneId ?? null

        try {
            if (sceneId) {
                clips.value = buildClipsFromScene(sceneId)
                return
            }
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
            if (currentSceneId.value) {
                clips.value = updateSceneTimelineOrder(currentSceneId.value, clipIds)
                return true
            }
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
            if (currentSceneId.value) {
                const success = unconfirmSceneClip(currentSceneId.value, clipId)
                if (success) {
                    clips.value = clips.value.filter((c) => c.clipId !== clipId)
                    clips.value.forEach((c, i) => (c.order = i + 1))
                }
                return success
            }
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
        currentSceneId.value = null
        error.value = null
        resetMerge()
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
