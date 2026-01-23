import type { TimelineClip } from '../../types'

import { fetchScenesByProjectId } from './scenes'
import { fetchNodesBySceneId } from './nodes'

// Cache (In-memory storage for reordering persistence during session)
const mockTimelineData: Record<number, TimelineClip[]> = {}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function fetchTimelineClips(projectId: number): Promise<TimelineClip[]> {
    await delay(300)

    // 1. If we have cached/reordered data, might want to use it? 
    // BUT user wants data to be "real" when videos are made. 
    // So we should try to sync: Fetch confirmed nodes, and merge with cache if necessary.
    // For simplicity in this mock: Always rebuild from nodes to ensure "freshness", 
    // unless we strictly want to support reordering persistence.
    // Let's rebuilding from sources first.

    const scenes = await fetchScenesByProjectId(projectId)
    const clips: TimelineClip[] = []

    for (const scene of scenes) {
        const nodes = await fetchNodesBySceneId(projectId, scene.sceneId)
        const videoNodes = nodes.filter((n) => n.type === 'VIDEO' && n.isConfirmed)

        videoNodes.forEach((node, index) => {
            clips.push({
                clipId: `${scene.sceneId}-${node.nodeId}`,
                nodeId: node.nodeId,
                thumbnailUrl: node.thumbnailUrl || '',
                duration: node.settings?.duration || 4,
                order: scene.order * 100 + index, // Default order based on scene
                label: node.title || scene.title,
            })
        })
    }

    // Sort by default order
    clips.sort((a, b) => a.order - b.order)

    // Update cache (simple override for now)
    // In a real app, we'd check if 'mockTimelineData' has custom orders and apply them.
    // Here, let's just refresh.
    mockTimelineData[projectId] = clips

    return clips
}

export async function reorderClips(
    projectId: number,
    clipIds: string[]
): Promise<TimelineClip[]> {
    await delay(200)
    const clips = mockTimelineData[projectId]
    if (!clips) return []

    const reordered = clipIds
        .map((id, index) => {
            const clip = clips.find((c) => c.clipId === id)
            return clip ? { ...clip, order: index + 1 } : null
        })
        .filter(Boolean) as TimelineClip[]

    mockTimelineData[projectId] = reordered
    return reordered
}

export async function removeClip(projectId: number, clipId: string): Promise<boolean> {
    await delay(200)
    const clips = mockTimelineData[projectId]
    if (!clips) return false

    const index = clips.findIndex((c) => c.clipId === clipId)
    if (index !== -1) {
        clips.splice(index, 1)
        // 순서 재조정
        clips.forEach((c, i) => (c.order = i + 1))
        return true
    }
    return false
}

// 병합 시뮬레이션
export async function mergeVideos(
    projectId: number,
    onProgress: (percent: number, status: string) => void
): Promise<{ success: boolean; downloadUrl?: string }> {
    const clips = mockTimelineData[projectId]
    if (!clips || clips.length === 0) {
        return { success: false }
    }

    const total = clips.length
    for (let i = 1; i <= total; i++) {
        await delay(400)
        const percent = Math.round((i / total) * 100)
        onProgress(percent, `(${i}/${total} 클립 처리 중)`)
    }

    return {
        success: true,
        downloadUrl: 'https://example.com/merged-video.mp4',
    }
}
