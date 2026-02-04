import { expect, test } from '@playwright/test'
import { buildScenePreviewFromTimeline } from '../src/pages/project/composables/scenePreviewMapper'

test.describe('scene preview mapping regression', () => {
  test('uses API duration consistently and keeps timeline order', () => {
    const preview = buildScenePreviewFromTimeline(6, [
      {
        sceneId: 6,
        order: 2,
        duration: 8000,
        thumbnailUrl: 'thumb-2.png',
        videoUrl: 'video-2.mp4',
      },
      {
        sceneId: 6,
        order: 1,
        duration: null,
        thumbnailUrl: 'thumb-1.png',
        url: '/api/nodes/11/content',
      },
      {
        sceneId: 7,
        order: 1,
        duration: 3000,
        thumbnailUrl: 'thumb-ignored.png',
        videoUrl: 'video-ignored.mp4',
      },
    ])

    expect(preview.clips).toHaveLength(2)
    expect(preview.clips[0]).toMatchObject({
      label: 'Video 1',
      thumbnailUrl: 'thumb-1.png',
      contentUrl: '/api/nodes/11/content',
      duration: 5,
    })
    expect(preview.clips[1]).toMatchObject({
      label: 'Video 2',
      thumbnailUrl: 'thumb-2.png',
      contentUrl: 'video-2.mp4',
      duration: 8,
    })
    expect(preview.totalDuration).toBe(13)
  })
})
