# Phase 5: 타임라인 에디터 구현 계획서

> **버전**: 1.0 | **작성일**: 2026-01-17  
> **목표**: `timeline.html` 디자인을 Vue 컴포넌트로 변환

---

## 1. 개요

### 목표
- 프로젝트 타임라인 화면 구현
- 확정된 클립 순서 조정 (드래그 앤 드롭)
- 영상 병합 + 다운로드 기능

### 레퍼런스
- HTML: `ams-v2_5-rose/pages/timeline.html`

---

## 2. 파일 목록

### 새로 생성 (8개)

| # | 경로 | 설명 |
|---|------|------|
| 1 | `src/services/mock/timeline.ts` | 타임라인 Mock 데이터 |
| 2 | `src/stores/timeline.ts` | 타임라인 상태 관리 |
| 3 | `src/layouts/TimelineLayout.vue` | 타임라인 전용 레이아웃 |
| 4 | `src/components/timeline/VideoPreview.vue` | 영상 미리보기 |
| 5 | `src/components/timeline/VideoTrack.vue` | 비디오 트랙 |
| 6 | `src/components/timeline/TimeRuler.vue` | 시간 룰러 |
| 7 | `src/components/timeline/ClipItem.vue` | 클립 아이템 |
| 8 | `src/components/timeline/MergeProgress.vue` | 병합 진행률 |

### 수정 (1개)

| 경로 | 변경 내용 |
|------|----------|
| `src/pages/TimelinePage.vue` | 전체 타임라인 구현 |

---

## 3. Mock 데이터

### `src/services/mock/timeline.ts`

```typescript
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

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

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

  const reordered = clipIds.map((id, index) => {
    const clip = clips.find(c => c.clipId === id)
    return clip ? { ...clip, order: index + 1 } : null
  }).filter(Boolean) as TimelineClip[]

  mockTimelineData[projectId] = reordered
  return reordered
}

export async function removeClip(
  projectId: number,
  clipId: string
): Promise<boolean> {
  await delay(200)
  const clips = mockTimelineData[projectId]
  if (!clips) return false

  const index = clips.findIndex(c => c.clipId === clipId)
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
```

---

## 4. Pinia 스토어

### `src/stores/timeline.ts`

```typescript
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
        clips.value = clips.value.filter(c => c.clipId !== clipId)
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
```

---

## 5. 컴포넌트

### 5.1 VideoPreview.vue

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Play, Pause } from 'lucide-vue-next'

interface Props {
  thumbnailUrl?: string
  currentTime: number
  totalTime: number
}

const props = defineProps<Props>()

const isPlaying = ref(false)

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = Math.round(seconds % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function togglePlay() {
  isPlaying.value = !isPlaying.value
}
</script>

<template>
  <div class="video-preview">
    <div
      class="preview-container"
      :style="thumbnailUrl ? { backgroundImage: `url(${thumbnailUrl})` } : {}"
    >
      <!-- Play Overlay -->
      <div class="play-overlay" @click="togglePlay">
        <div class="play-button">
          <Pause v-if="isPlaying" class="play-icon" />
          <Play v-else class="play-icon" />
        </div>
      </div>

      <!-- Timecode -->
      <div class="timecode">
        <span>{{ formatTime(currentTime) }}</span>
        <span> / </span>
        <span>{{ formatTime(totalTime) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-preview {
  width: 100%;
}

.preview-container {
  aspect-ratio: 16 / 9;
  max-width: 700px;
  margin: 0 auto;
  background: #111 center/cover no-repeat;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: background 0.2s;
}

.play-overlay:hover {
  background: rgba(0, 0, 0, 0.4);
}

.play-button {
  width: 64px;
  height: 64px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}

.play-overlay:hover .play-button {
  transform: scale(1.1);
}

.play-icon {
  width: 24px;
  height: 24px;
  color: var(--rose-500);
}

.timecode {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-family: monospace;
  color: white;
}
</style>
```

### 5.2 ClipItem.vue

```vue
<script setup lang="ts">
import type { TimelineClip } from '../../types'
import { X } from 'lucide-vue-next'

interface Props {
  clip: TimelineClip
  draggable?: boolean
}

withDefaults(defineProps<Props>(), {
  draggable: true,
})

const emit = defineEmits<{
  (e: 'remove'): void
}>()
</script>

<template>
  <div
    class="clip-item"
    :draggable="draggable"
    :style="{ width: `${Math.max(120, clip.duration * 12)}px` }"
  >
    <img :src="clip.thumbnailUrl" :alt="clip.label" class="clip-thumbnail" />
    <div class="clip-info">
      <div class="clip-label">{{ clip.label || '확정 클립' }}</div>
      <div class="clip-duration">{{ clip.duration }}초</div>
    </div>
    <span class="clip-badge">OK</span>
    <button class="clip-remove" @click.stop="emit('remove')">
      <X class="remove-icon" />
    </button>
  </div>
</template>

<style scoped>
.clip-item {
  flex-shrink: 0;
  background: white;
  border: 2px solid var(--rose-300);
  border-radius: 8px;
  overflow: hidden;
  cursor: grab;
  position: relative;
  transition: box-shadow 0.2s;
}

.clip-item:active {
  cursor: grabbing;
}

.clip-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.clip-thumbnail {
  width: 100%;
  height: 48px;
  object-fit: cover;
}

.clip-info {
  padding: 4px 8px;
  background: var(--rose-100);
  font-size: 0.625rem;
  color: var(--gray-700);
}

.clip-label {
  font-weight: 600;
}

.clip-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: var(--success);
  color: white;
  font-size: 0.5rem;
  padding: 1px 4px;
  border-radius: 2px;
}

.clip-remove {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 16px;
  height: 16px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.clip-item:hover .clip-remove {
  opacity: 1;
}

.remove-icon {
  width: 10px;
  height: 10px;
  color: white;
}
</style>
```

### 5.3 VideoTrack.vue

```vue
<script setup lang="ts">
import type { TimelineClip } from '../../types'
import ClipItem from './ClipItem.vue'

interface Props {
  clips: TimelineClip[]
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'reorder', clipIds: string[]): void
  (e: 'remove', clipId: string): void
}>()

let draggedId: string | null = null

function handleDragStart(clipId: string, event: DragEvent) {
  draggedId = clipId
  if (event.target instanceof HTMLElement) {
    event.target.style.opacity = '0.6'
  }
}

function handleDragEnd(event: DragEvent) {
  if (event.target instanceof HTMLElement) {
    event.target.style.opacity = '1'
  }
  draggedId = null
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
}

function handleDrop(targetClipId: string) {
  if (!draggedId || draggedId === targetClipId) return

  // Calculate new order
  const clipIds = [...emit.clips].map(c => c.clipId)
  const draggedIndex = clipIds.indexOf(draggedId)
  const targetIndex = clipIds.indexOf(targetClipId)

  if (draggedIndex !== -1 && targetIndex !== -1) {
    clipIds.splice(draggedIndex, 1)
    clipIds.splice(targetIndex, 0, draggedId)
    emit('reorder', clipIds)
  }
}
</script>

<template>
  <div class="video-track">
    <ClipItem
      v-for="clip in clips"
      :key="clip.clipId"
      :clip="clip"
      @dragstart="handleDragStart(clip.clipId, $event)"
      @dragend="handleDragEnd"
      @dragover="handleDragOver"
      @drop="handleDrop(clip.clipId)"
      @remove="emit('remove', clip.clipId)"
    />

    <div v-if="clips.length === 0" class="track-empty">
      확정된 클립이 없습니다
    </div>
  </div>
</template>

<style scoped>
.video-track {
  display: flex;
  gap: 2px;
  min-height: 80px;
  background: var(--rose-50);
  border-radius: 8px;
  padding: 8px;
  min-width: 600px;
  overflow-x: auto;
}

.track-empty {
  flex: 1;
  min-width: 60px;
  border: 2px dashed var(--rose-200);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: var(--gray-400);
}
</style>
```

### 5.4 MergeProgress.vue

```vue
<script setup lang="ts">
import type { MergeStatus } from '../../stores/timeline'
import Button from '../common/Button.vue'
import { Check, Play, Download, Loader2 } from 'lucide-vue-next'

interface Props {
  status: MergeStatus
  progress: number
  statusText: string
  downloadUrl?: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'preview'): void
  (e: 'download'): void
}>()
</script>

<template>
  <div v-if="status !== 'idle'" class="merge-section">
    <!-- Progress -->
    <div v-if="status === 'merging'" class="merge-progress">
      <div class="progress-header">
        <Loader2 class="spinner" />
        <div>
          <span class="progress-title">영상 병합 중...</span>
          <span class="progress-status">{{ statusText }}</span>
        </div>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
      </div>
    </div>

    <!-- Complete -->
    <div v-if="status === 'done'" class="merge-complete">
      <div class="complete-header">
        <div class="complete-icon">
          <Check class="check-icon" />
        </div>
        <div>
          <h3 class="complete-title">병합 완료!</h3>
          <p class="complete-subtitle">영상이 준비되었습니다.</p>
        </div>
      </div>
      <div class="complete-actions">
        <Button variant="secondary" @click="emit('preview')">
          <Play class="btn-icon" />
          미리보기
        </Button>
        <Button variant="primary" @click="emit('download')">
          <Download class="btn-icon" />
          다운로드 (MP4)
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.merge-section {
  background: linear-gradient(135deg, var(--rose-50), var(--rose-100));
  padding: 1.5rem;
  border-radius: 12px;
}

.merge-progress {
  /* Progress UI */
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.spinner {
  width: 20px;
  height: 20px;
  color: var(--rose-500);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.progress-title {
  font-size: 0.875rem;
  font-weight: 600;
}

.progress-status {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-left: 0.5rem;
}

.progress-bar {
  height: 8px;
  background: var(--rose-200);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  transition: width 0.3s;
}

.merge-complete {
  /* Complete UI */
}

.complete-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.complete-icon {
  width: 48px;
  height: 48px;
  background: var(--success-bg);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  width: 24px;
  height: 24px;
  color: var(--success);
}

.complete-title {
  font-weight: 600;
  margin: 0 0 0.125rem;
}

.complete-subtitle {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0;
}

.complete-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-icon {
  width: 16px;
  height: 16px;
}
</style>
```

---

## 6. 메인 페이지

### `src/pages/TimelinePage.vue`

```vue
<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useTimelineStore } from '../stores/timeline'
import { useUIStore } from '../stores/ui'

import TimelineLayout from '../layouts/TimelineLayout.vue'
import VideoPreview from '../components/timeline/VideoPreview.vue'
import VideoTrack from '../components/timeline/VideoTrack.vue'
import TimeRuler from '../components/timeline/TimeRuler.vue'
import MergeProgress from '../components/timeline/MergeProgress.vue'
import Button from '../components/common/Button.vue'
import Card from '../components/common/Card.vue'
import { GitMerge, Download } from 'lucide-vue-next'

const route = useRoute()
const projectStore = useProjectStore()
const timelineStore = useTimelineStore()
const uiStore = useUIStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

onMounted(async () => {
  if (projectId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      timelineStore.loadClips(projectId.value),
    ])
  }
})

async function handleReorder(clipIds: string[]) {
  const success = await timelineStore.reorderClips(clipIds)
  if (success) {
    uiStore.showToast('success', '순서 변경', '클립 순서가 변경되었습니다.')
  }
}

async function handleRemove(clipId: string) {
  const success = await timelineStore.removeClip(clipId)
  if (success) {
    uiStore.showToast('success', '삭제', '클립이 제거되었습니다.')
  }
}

async function handleMerge() {
  const success = await timelineStore.startMerge()
  if (success) {
    uiStore.showToast('success', '병합 완료', '영상이 준비되었습니다.')
  }
}

function handleDownload() {
  if (timelineStore.downloadUrl) {
    window.open(timelineStore.downloadUrl, '_blank')
  }
}
</script>

<template>
  <TimelineLayout
    :project-title="project?.title || 'Project'"
    :clip-count="timelineStore.clipCount"
    :total-duration="timelineStore.totalDuration"
  >
    <div class="timeline-content">
      <!-- Preview -->
      <section>
        <h3 class="section-title">미리보기</h3>
        <Card class="preview-card">
          <VideoPreview
            :thumbnail-url="timelineStore.orderedClips[0]?.thumbnailUrl"
            :current-time="0"
            :total-time="timelineStore.totalDuration"
          />
        </Card>
      </section>

      <!-- Video Track -->
      <section>
        <div class="section-header">
          <h3 class="section-title">메인 비디오 트랙 (확정 클립)</h3>
          <span class="section-hint">드래그하여 순서 변경</span>
        </div>

        <Card class="track-card">
          <TimeRuler :max-time="60" />
          <VideoTrack
            :clips="timelineStore.orderedClips"
            @reorder="handleReorder"
            @remove="handleRemove"
          />
          <div class="track-info">
            <span class="clip-count">{{ timelineStore.clipCount }}개 클립</span>
            <span class="duration-text">
              총 길이: {{ timelineStore.totalDuration }}초 / 60초
            </span>
          </div>
        </Card>
      </section>

      <!-- Merge -->
      <section>
        <MergeProgress
          :status="timelineStore.mergeStatus"
          :progress="timelineStore.mergeProgress"
          :status-text="timelineStore.mergeStatusText"
          :download-url="timelineStore.downloadUrl"
          @preview="() => {}"
          @download="handleDownload"
        />
      </section>

      <!-- Actions -->
      <div class="timeline-actions">
        <Button
          variant="primary"
          :disabled="!timelineStore.canMerge"
          @click="handleMerge"
        >
          <GitMerge class="btn-icon" />
          영상 병합하기
        </Button>
        <Button
          variant="secondary"
          :disabled="!timelineStore.canDownload"
          @click="handleDownload"
        >
          <Download class="btn-icon" />
          다운로드 (MP4)
        </Button>
      </div>
    </div>
  </TimelineLayout>
</template>

<style scoped>
.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-title {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.section-hint {
  font-size: 0.75rem;
  color: var(--gray-400);
}

.preview-card,
.track-card {
  padding: 1.5rem;
}

.track-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.75rem;
}

.clip-count {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.duration-text {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--rose-500);
}

.timeline-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.btn-icon {
  width: 16px;
  height: 16px;
}
</style>
```

---

## 7. 구현 순서 체크리스트

1. [ ] `src/services/mock/timeline.ts`
2. [ ] `src/stores/timeline.ts`
3. [ ] `src/layouts/TimelineLayout.vue`
4. [ ] `src/components/timeline/VideoPreview.vue`
5. [ ] `src/components/timeline/TimeRuler.vue`
6. [ ] `src/components/timeline/ClipItem.vue`
7. [ ] `src/components/timeline/VideoTrack.vue`
8. [ ] `src/components/timeline/MergeProgress.vue`
9. [ ] `src/pages/TimelinePage.vue`
10. [ ] `npm run build`

---

## 8. 검증

1. `/projects/1/timeline` 이동
2. 4개 클립 표시 확인
3. 클립 드래그로 순서 변경
4. "영상 병합하기" 클릭 → 진행률 표시
5. 완료 후 다운로드 버튼 활성화
