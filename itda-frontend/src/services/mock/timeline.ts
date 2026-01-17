import type { TimelineClip } from '../../types'

// 타임라인 클립 데이터 (확정된 영상들)
const mockTimelineData: Record<number, TimelineClip[]> = {
    1: [
        {
            clipId: 'clip-1',
            nodeId: 4,
            thumbnailUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=300&auto=format',
            duration: 10,
            order: 1,
            label: '씬 1: 사막',
        },
        {
            clipId: 'clip-2',
            nodeId: 8,
            thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=300&auto=format',
            duration: 15,
            order: 2,
            label: '씬 2: 탐사',
        },
        {
            clipId: 'clip-3',
            nodeId: 12,
            thumbnailUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300&auto=format',
            duration: 20,
            order: 3,
            label: '씬 3: 구조물',
        },
        {
            clipId: 'clip-4',
            nodeId: 16,
            thumbnailUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format',
            duration: 15,
            order: 4,
            label: '씬 4: 출발',
        },
    ],
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function fetchTimelineClips(projectId: number): Promise<TimelineClip[]> {
    await delay(300)
    return mockTimelineData[projectId] || []
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
