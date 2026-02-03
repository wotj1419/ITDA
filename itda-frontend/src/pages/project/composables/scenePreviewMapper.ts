import type { TimelineItem } from '../../../services/api/timeline'
import { normalizeDurationSeconds } from '../../../utils/duration'

export interface ScenePreviewClip {
  thumbnailUrl: string
  duration: number
  label?: string
  contentUrl?: string
}

export interface ScenePreview {
  clips: ScenePreviewClip[]
  totalDuration: number
}

export function buildScenePreviewFromTimeline(sceneId: number, items: TimelineItem[]): ScenePreview {
  const resolveThumbnailUrl = (item: TimelineItem): string => item.thumbnailUrl ?? ''
  const resolveVideoUrl = (item: TimelineItem): string => item.videoUrl ?? item.url ?? ''

  const clips = items
    .filter((item) => item.sceneId === sceneId)
    .sort((a, b) => a.order - b.order)
    .map((item) => ({
      thumbnailUrl: resolveThumbnailUrl(item),
      duration: normalizeDurationSeconds(item.duration),
      label: `Video ${item.order}`,
      contentUrl: resolveVideoUrl(item),
    }))

  const totalDuration = clips.reduce((sum, clip) => sum + clip.duration, 0)
  return { clips, totalDuration }
}
