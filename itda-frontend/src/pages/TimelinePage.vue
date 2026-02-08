<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useSceneStore } from '../stores/scene'
import { useTimelineStore } from '../stores/timeline'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'

import TimelineLayout from '../layouts/TimelineLayout.vue'
import VideoPreview from '../components/timeline/VideoPreview.vue'
import VideoTrack from '../components/timeline/VideoTrack.vue'
import TimeRuler from '../components/timeline/TimeRuler.vue'
import MergeProgress from '../components/timeline/MergeProgress.vue'
import Button from '../components/common/Button.vue'
import Badge from '../components/common/Badge.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'
import SceneVideoPreviewModal from '../components/scene-editor/SceneVideoPreviewModal.vue'
import { GitMerge, RefreshCw } from 'lucide-vue-next'
import { triggerDownload } from '../utils/download'
import { formatRelativeTime } from '../utils/date'
import { fetchSceneExports, deleteSceneExport, activateSceneExport, fetchProjectExportPreview } from '../services/api/timeline'
import { SCENE_VIDEO_PREVIEW_MODAL_ID } from '../constants/ui'
import type { SceneExportItem } from '../types/api/timeline'
import type { TimelineClip } from '../types/ui'

const route = useRoute()
const projectStore = useProjectStore()
const sceneStore = useSceneStore()
const timelineStore = useTimelineStore()
const uiStore = useUIStore()
const collabStore = useCollabStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)
const sceneId = computed(() => {
  const raw = route.params.sceneId
  const value = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
})

const sceneTitle = computed(() => {
  if (sceneId.value === null) return ''
  const scene = sceneStore.scenes.find(s => s.sceneId === sceneId.value)
  return scene ? `씬 ${scene.order}: ${scene.title}` : ''
})

const isSceneTimeline = computed(() => sceneId.value !== null)

const orderedScenes = computed(() => sceneStore.orderedScenes)
const sceneCount = computed(() => sceneStore.sceneCount)
const sceneProgress = computed(() => sceneStore.progress)

const orderedClips = computed(() => timelineStore.orderedClips)
const selectedClipId = ref<string | null>(null)

type ProjectClipGroup = {
  sceneId: number
  order: number
  title: string
  clips: TimelineClip[]
  previewClips: TimelineClip[]
  clipCount: number
  totalDurationMs: number
}

const projectClipPreviewLimit = 6
const expandedSceneIds = ref<Set<number>>(new Set())

const isSceneListExpanded = ref(true)
const toggleSceneList = () => {
  isSceneListExpanded.value = !isSceneListExpanded.value
}


const projectClipGroups = computed<ProjectClipGroup[]>(() => {
  if (isSceneTimeline.value) return []
  const clipsByScene = new Map<number, TimelineClip[]>()
  orderedClips.value.forEach((clip) => {
    if (!clip.sceneId) return
    const list = clipsByScene.get(clip.sceneId) ?? []
    list.push(clip)
    clipsByScene.set(clip.sceneId, list)
  })

  return orderedScenes.value
    .map((scene) => {
      const clips = (clipsByScene.get(scene.sceneId) ?? [])
        .slice()
        .sort((a, b) => a.order - b.order)
      const clipCount = clips.length
      const totalDurationMs = clips.reduce((sum, clip) => sum + (clip.duration || 0) * 1000, 0)
      return {
        sceneId: scene.sceneId,
        order: scene.order,
        title: scene.title,
        clips,
        previewClips: clips.slice(0, projectClipPreviewLimit),
        clipCount,
        totalDurationMs,
      }
    })
    .filter((group) => group.clipCount > 0)
})

const projectClipGroupBySceneId = computed(() => {
  const map = new Map<number, ProjectClipGroup>()
  projectClipGroups.value.forEach((group) => {
    map.set(group.sceneId, group)
  })
  return map
})

const projectSceneRows = computed(() => {
  if (isSceneTimeline.value) return []
  const groupMap = projectClipGroupBySceneId.value
  return orderedScenes.value.map((scene) => ({
    scene,
    clipGroup: groupMap.get(scene.sceneId) ?? null,
  }))
})

const isSceneGroupExpanded = (sceneId: number) => expandedSceneIds.value.has(sceneId)

const toggleSceneGroup = (sceneId: number) => {
  const next = new Set(expandedSceneIds.value)
  if (next.has(sceneId)) {
    next.delete(sceneId)
  } else {
    next.add(sceneId)
  }
  expandedSceneIds.value = next
}


const sceneExports = ref<SceneExportItem[]>([])
const activeExport = ref<SceneExportItem | null>(null)
const isExportLoading = ref(false)
const exportError = ref<string | null>(null)
const deleteTarget = ref<SceneExportItem | null>(null)
const isDeletingExport = ref(false)
const activatingExportId = ref<number | null>(null)
const exportPage = ref(1)
const exportPageSize = 8
const exportTotalCount = ref(0)
const exportPrevLabel = '이전'
const exportNextLabel = '다음'
const exportEllipsis = '...'

const exportCountLabel = computed(() => (
  exportTotalCount.value > 0 ? exportTotalCount.value : sceneExports.value.length
))

const filteredExports = computed(() => {
  if (!activeExport.value) return sceneExports.value
  return sceneExports.value.filter(item => item.sceneVideoId !== activeExport.value?.sceneVideoId)
})

const exportTotalPages = computed(() => {
  if (exportTotalCount.value <= 0) return 0
  return Math.ceil(exportTotalCount.value / exportPageSize)
})

const exportPageItems = computed<(number | typeof exportEllipsis)[]>(() => {
  const total = exportTotalPages.value
  if (total <= 1) return []
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }
  const current = exportPage.value
  if (current <= 4) {
    return [1, 2, 3, 4, 5, exportEllipsis, total]
  }
  if (current >= total - 3) {
    return [1, exportEllipsis, total - 4, total - 3, total - 2, total - 1, total]
  }
  return [1, exportEllipsis, current - 1, current, current + 1, exportEllipsis, total]
})

const canGoPrev = computed(() => exportPage.value > 1)
const canGoNext = computed(() => exportTotalPages.value > 0 && exportPage.value < exportTotalPages.value)

watch(
  orderedClips,
  (clips) => {
    if (clips.length === 0) {
      selectedClipId.value = null
      return
    }
    if (selectedClipId.value && clips.some((clip) => clip.clipId === selectedClipId.value)) {
      return
    }
    selectedClipId.value = clips[0]?.clipId ?? null
  },
  { immediate: true }
)

onMounted(async () => {
  if (projectId.value) {
    collabStore.joinRoom(projectId.value)
    collabStore.updateLocation('TIMELINE', sceneId.value ?? undefined)
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      timelineStore.loadClips(projectId.value, sceneId.value ?? undefined),
    ])
    if (sceneId.value !== null) {
      await loadSceneExports()
    }
  }
})

watch([projectId, sceneId], async ([nextProjectId, nextSceneId]) => {
  if (!nextProjectId) return
  collabStore.joinRoom(nextProjectId)
  collabStore.updateLocation('TIMELINE', nextSceneId ?? undefined)
  await timelineStore.loadClips(nextProjectId, nextSceneId ?? undefined)
  if (nextSceneId !== null) {
    await loadSceneExports()
  } else {
    sceneExports.value = []
    activeExport.value = null
    exportError.value = null
    exportPage.value = 1
    exportTotalCount.value = 0
  }
})

watch(
  () => timelineStore.mergeStatus,
  (status) => {
    if (status === 'done') {
      uiStore.showToast({
        type: 'success',
        title: '병합 완료',
        message: '씬 병합 영상이 생성되었습니다.',
      })
      if (sceneId.value !== null) {
        void loadSceneExports()
      }
    } else if (status === 'error') {
      uiStore.showToast({
        type: 'error',
        title: '병합 실패',
        message: '씬 병합에 실패했습니다. 잠시 후 다시 시도해 주세요.',
      })
    }
  }
)

async function handleReorder(clipIds: string[]) {
  const success = await timelineStore.reorderClips(clipIds)
  if (success) {
    timelineStore.resetMerge()
    uiStore.showToast({
      type: 'success',
      title: '정렬 완료',
      message: '클립 순서가 변경되었습니다.',
    })
  }
}

async function handleRemove(clipId: string) {
  const success = await timelineStore.removeClip(clipId)
  if (success) {
    timelineStore.resetMerge()
    uiStore.showToast({
      type: 'success',
      title: '삭제 완료',
      message: '선택한 클립이 삭제되었습니다.',
    })
  }
}

async function handleMerge() {
  await timelineStore.startMerge()
}

async function handleMergePreview(): Promise<void> {
  if (sceneId.value !== null) {
    if (!activeExport.value) {
      await loadSceneExports(exportPage.value)
    }

    const target = activeExport.value
    if (target && canPreviewExport(target)) {
      handlePreviewExport(target)
      return
    }

    uiStore.showToast({
      type: 'error',
      title: '미리보기 불가',
      message: '아직 미리보기 가능한 병합 영상이 없습니다.',
    })
    return
  }

  if (!projectId.value) return

  const previewUrl = await fetchProjectExportPreview(projectId.value).catch(() => null)
  if (!previewUrl) {
    uiStore.showToast({
      type: 'error',
      title: '미리보기 불가',
      message: '프로젝트 병합 영상 미리보기를 불러오지 못했습니다.',
    })
    return
  }

  const title = project.value?.title ? `${project.value.title} 병합 영상` : '프로젝트 병합 영상'
  uiStore.openModal(SCENE_VIDEO_PREVIEW_MODAL_ID, {
    title,
    videoUrl: previewUrl,
    posterUrl: null,
  })
}

function handleDownload() {
  if (timelineStore.downloadUrl) {
    triggerDownload(timelineStore.downloadUrl)
  }
}

function handleReset() {
  timelineStore.resetMerge()
}

function handleSelectClip(clipId: string | null) {
  selectedClipId.value = clipId
}

async function loadSceneExports(page?: number): Promise<void> {
  if (sceneId.value === null) return
  isExportLoading.value = true
  exportError.value = null
  const resolvedPage = Number.isFinite(page)
    ? Math.max(1, page as number)
    : Math.max(1, exportPage.value)
  exportPage.value = resolvedPage
  exportTotalCount.value = 0
  try {
    const response = await fetchSceneExports(sceneId.value, {
      page: resolvedPage,
      size: exportPageSize,
    })
    sceneExports.value = response.items
    activeExport.value = response.activeItem ?? null
    exportTotalCount.value = response.totalCount
    exportPage.value = response.page
  } catch (error) {
    console.error('Failed to load scene exports', error)
    exportError.value = '병합 영상 목록을 불러오지 못했습니다.'
    sceneExports.value = []
    activeExport.value = null
    exportTotalCount.value = 0
  } finally {
    isExportLoading.value = false
  }
}

function handleExportRefresh(): void {
  void loadSceneExports(exportPage.value)
}

function goToExportPage(page: number): void {
  if (page === exportPage.value) return
  if (page < 1 || page > exportTotalPages.value) return
  void loadSceneExports(page)
}

function formatDurationMs(durationMs?: number | null): string {
  const safeMs = Number.isFinite(durationMs) ? Math.max(0, durationMs || 0) : 0
  const totalSeconds = Math.floor(safeMs / 1000)
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function formatCreatedAt(value?: string | null): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return formatRelativeTime(date)
}

function getStatusMeta(status?: string | null): { label: string; variant: 'warning' | 'error' | 'info'; visible: boolean } {
  switch (status) {
    case 'FAILED':
      return { label: '실패', variant: 'error', visible: true }
    case 'GENERATING':
      return { label: '생성 중', variant: 'warning', visible: true }
    case 'QUEUED':
      return { label: '대기', variant: 'info', visible: true }
    default:
      return { label: '', variant: 'info', visible: false }
  }
}

function getSceneStatusMeta(status?: string | null): { label: string; variant: 'success' | 'info' | 'default' } {
  switch (status) {
    case 'COMPLETED':
      return { label: '완료', variant: 'success' }
    case 'IN_PROGRESS':
      return { label: '진행 중', variant: 'info' }
    default:
      return { label: '초안', variant: 'default' }
  }
}

function canPreviewExport(item: SceneExportItem): boolean {
  return item.status === 'COMPLETED' && Boolean(item.previewUrl)
}

function canDownloadExport(item: SceneExportItem): boolean {
  return item.status === 'COMPLETED' && Boolean(item.downloadUrl)
}

function canActivateExport(item: SceneExportItem): boolean {
  return item.status === 'COMPLETED' && !item.active
}

function handlePreviewExport(item: SceneExportItem): void {
  if (!item.previewUrl) return
  const title = sceneTitle.value ? `${sceneTitle.value} 병합 영상` : '씬 병합 영상'
  uiStore.openModal(SCENE_VIDEO_PREVIEW_MODAL_ID, {
    title,
    videoUrl: item.previewUrl,
    posterUrl: item.thumbnailUrl || null,
  })
}

function handleDownloadExport(item: SceneExportItem): void {
  if (!item.downloadUrl) return
  triggerDownload(item.downloadUrl)
}

async function handleActivateExport(item: SceneExportItem): Promise<void> {
  if (sceneId.value === null || !canActivateExport(item)) return
  activatingExportId.value = item.sceneVideoId
  try {
    await activateSceneExport(sceneId.value, item.sceneVideoId)
    uiStore.showToast({
      type: 'success',
      title: '활성 병합 영상 변경',
      message: '씬 활성 병합 영상이 업데이트되었습니다.',
    })
    await loadSceneExports(exportPage.value)
  } catch (error) {
    console.error('Failed to activate scene export', error)
    uiStore.showToast({
      type: 'error',
      title: '활성 병합 영상 변경 실패',
      message: '활성 병합 영상 설정에 실패했습니다.',
    })
  } finally {
    if (activatingExportId.value === item.sceneVideoId) {
      activatingExportId.value = null
    }
  }
}

function requestDeleteExport(item: SceneExportItem): void {
  deleteTarget.value = item
}

function closeDeleteExportModal(): void {
  deleteTarget.value = null
}

async function confirmDeleteExport(): Promise<void> {
  if (!deleteTarget.value || sceneId.value === null) return
  const target = deleteTarget.value
  isDeletingExport.value = true
  try {
    await deleteSceneExport(sceneId.value, target.sceneVideoId)
    await loadSceneExports()
    uiStore.showToast({
      type: 'success',
      title: '삭제 완료',
      message: '병합 영상이 삭제되었습니다.',
    })
  } catch (error) {
    console.error('Failed to delete scene export', error)
    uiStore.showToast({
      type: 'error',
      title: '삭제 실패',
      message: '병합 영상을 삭제하지 못했습니다.',
    })
  } finally {
    isDeletingExport.value = false
    deleteTarget.value = null
  }
}

const timelineMaxTime = computed(() => {
  // Base 10 mins (600s), or total duration + 5 mins buffer (300s)
  return Math.max(600, timelineStore.totalDuration + 300)
})

function handleWheel(e: WheelEvent) {
  if (timelineStore.clipCount === 0) return
  const container = e.currentTarget as HTMLElement
  if (e.deltaY !== 0) {
    e.preventDefault()
    container.scrollLeft += e.deltaY * 3
  }
}
</script>

<template>
  <TimelineLayout
    :project-title="project?.title || 'Project'"
    :clip-count="timelineStore.clipCount"
    :total-duration="timelineStore.totalDuration"
    :is-scene-timeline="sceneId !== null"
    :scene-title="sceneTitle"
    :scene-id="sceneId"
  >
    <template #actions>
      <div class="header-buttons">
        <Button
          v-if="timelineStore.mergeStatus === 'done'"
          variant="ghost"
          @click="handleReset"
        >
          <RefreshCw class="icon-md" />
          병합 초기화
        </Button>
        <Button
          variant="primary"
          :disabled="!timelineStore.canMerge"
          @click="handleMerge"
        >
          <GitMerge class="icon-md" />
          씬 병합 요청
        </Button>
      </div>
    </template>
    <div class="timeline-content">
      <!-- Loading -->
      <div v-if="timelineStore.isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>타임라인 데이터를 불러오는 중...</span>
      </div>
      <template v-else>
        <section v-if="!isSceneTimeline" class="project-overview-section">
          <div class="section-header">
            <div>
              <h3 class="section-title">프로젝트 타임라인</h3>
              <p class="section-subtitle text-muted">프로젝트의 흐름과 진행 상황을 한눈에 확인하세요.</p>
            </div>
          </div>
          <Card class="project-overview-card">
            <div class="project-metrics">
              <div class="project-metric">
                <span class="metric-label">씬</span>
                <span class="metric-value">{{ sceneCount }}</span>
              </div>
              <div class="project-metric">
                <span class="metric-label">완료</span>
                <span class="metric-value">{{ sceneProgress.completed }}</span>
              </div>
              <div class="project-metric">
                <span class="metric-label">완료율</span>
                <span class="metric-value">{{ sceneProgress.percentage }}%</span>
              </div>
              <div class="project-metric">
                <span class="metric-label">클립</span>
                <span class="metric-value">{{ timelineStore.clipCount }}</span>
              </div>
              <div class="project-metric">
                <span class="metric-label">총 길이</span>
                <span class="metric-value">{{ formatDurationMs(timelineStore.totalDuration * 1000) }}</span>
              </div>
            </div>
          </Card>
        </section>

        

        <!-- Preview -->
        <section>
          <div class="section-header">
            <h3 class="section-title">미리보기</h3>
          </div>
          <Card class="preview-card">
            <VideoPreview
              :clips="orderedClips"
              :selected-clip-id="selectedClipId"
              @update:selected-clip-id="handleSelectClip"
            />
          </Card>
        </section>
        <!-- Video Track -->
        <section v-if="isSceneTimeline">
          <div class="section-header">
            <h3 class="section-title">타임라인 클립</h3>
          </div>
          <p v-if="isSceneTimeline" class="section-hint">드래그로 순서를 변경할 수 있습니다.</p>
          <Card class="track-card">
            <template v-if="isSceneTimeline">
              <div
                class="timeline-scroll-container"
                :style="{ overflowX: timelineStore.clipCount === 0 ? 'hidden' : 'auto' }"
                @wheel="handleWheel"
              >
                <div
                  class="timeline-inner-wrapper"
                  :style="{ width: timelineStore.clipCount === 0 ? '100%' : `${timelineMaxTime * 20}px` }"
                >
                  <TimeRuler :max-time="timelineMaxTime" :px-per-sec="20" />
                  <VideoTrack
                    :clips="orderedClips"
                    :selected-clip-id="selectedClipId"
                    @reorder="handleReorder"
                    @remove="handleRemove"
                    @select="handleSelectClip"
                  />
                </div>
              </div>
              <div class="track-info">
                <span class="clip-count">{{ timelineStore.clipCount }}개 클립</span>
                <span class="duration-text">총 길이: {{ timelineStore.totalDuration }}초</span>
              </div>
            </template>
          </Card>
        </section>
<!-- Merge Progress -->
        <section>
          <MergeProgress
            :status="timelineStore.mergeStatus"
            :progress="timelineStore.mergeProgress"
            :status-text="timelineStore.mergeStatusText"
            :download-url="timelineStore.downloadUrl"
            @preview="handleMergePreview"
            @download="handleDownload"
          />
        </section>
<section v-if="!isSceneTimeline" class="project-scenes-section">
          <div class="section-header">
            <div class="section-title-wrap">
              <h3 class="section-title">씬 목록</h3>
              <span class="section-count">{{ sceneCount }}</span>
              <Button variant="ghost" size="sm" class="section-toggle" @click="toggleSceneList">
                {{ isSceneListExpanded ? '접기' : '펼침' }}
              </Button>
            </div>
          </div>
          <div v-show="isSceneListExpanded" class="project-scenes-body">
            <p class="section-subtitle text-muted">씬 편집과 타임라인 화면으로 빠르게 이동할 수 있습니다.</p>
          <Card v-if="orderedScenes.length === 0" dashed class="project-scenes-empty">
            아직 생성된 씬이 없습니다.
          </Card>
          <div v-else class="project-scene-list">
            <Card v-for="row in projectSceneRows" :key="row.scene.sceneId" class="project-scene-card">
              <div class="project-scene-row">
                <div class="project-scene-info">
                  <div class="project-scene-header">
                    <Badge variant="default" size="sm">씬 {{ row.scene.order }}</Badge>
                    <Badge :variant="getSceneStatusMeta(row.scene.status).variant" size="sm">
                      {{ getSceneStatusMeta(row.scene.status).label }}
                    </Badge>
                  </div>
                  <div class="project-scene-title">{{ row.scene.title }}</div>
                  <p v-if="row.scene.description" class="project-scene-description">{{ row.scene.description }}</p>
                </div>
                <div class="project-scene-actions">
                  <RouterLink :to="{ name: 'scene-edit', params: { projectId: projectId, sceneId: row.scene.sceneId } }" custom v-slot="{ navigate }">
                    <Button variant="secondary" size="sm" @click="navigate">
                      씬 편집
                    </Button>
                  </RouterLink>
                  <RouterLink :to="{ name: 'scene-timeline', params: { id: projectId, sceneId: row.scene.sceneId } }" custom v-slot="{ navigate }">
                    <Button variant="primary" size="sm" @click="navigate">
                      타임라인
                    </Button>
                  </RouterLink>
                </div>
              </div>
              <div v-if="row.clipGroup" class="project-scene-clips">
                <div class="project-scene-clip-meta">
                  <div class="project-scene-clip-meta-items">
                    <span class="project-scene-clip-count">{{ row.clipGroup.clipCount }}개</span>
                    <span class="project-scene-clip-duration">{{ formatDurationMs(row.clipGroup.totalDurationMs) }}</span>
                  </div>
                  <Button
                    v-if="row.clipGroup.clipCount > projectClipPreviewLimit"
                    variant="ghost"
                    size="sm"
                    class="project-clip-toggle"
                    @click="toggleSceneGroup(row.scene.sceneId)"
                  >
                    {{ isSceneGroupExpanded(row.scene.sceneId) ? '접기' : `+${row.clipGroup.clipCount - projectClipPreviewLimit}개 더보기` }}
                  </Button>
                </div>
                <div class="project-clip-strip">
                  <button
                    v-for="clip in (isSceneGroupExpanded(row.scene.sceneId) ? row.clipGroup.clips : row.clipGroup.previewClips)"
                    :key="clip.clipId"
                    type="button"
                    class="project-clip-thumb"
                    :class="{ active: selectedClipId === clip.clipId }"
                    @click="handleSelectClip(clip.clipId)"
                  >
                    <img v-if="clip.thumbnailUrl" :src="clip.thumbnailUrl" :alt="clip.label || row.scene.title" />
                    <div v-else class="project-clip-thumb-placeholder">No Preview</div>
                    <span class="project-clip-duration">{{ formatDurationMs(clip.duration * 1000) }}</span>
                  </button>
                </div>
              </div>
              <div v-else class="project-scene-clip-empty">
                확정된 클립이 없습니다.
              </div>
            </Card>
          </div>
          </div>
        </section>
        
        <section v-if="isSceneTimeline" class="export-section">
          <div class="section-header export-header">
            <div class="section-title-wrap">
              <h3 class="section-title">씬 병합 영상</h3>
              <span class="section-count">{{ exportCountLabel }}</span>
            </div>
            <div class="section-actions">
              <Button
                variant="secondary"
                size="sm"
                :disabled="isExportLoading"
                @click="handleExportRefresh"
              >
                새로고침
              </Button>
            </div>
          </div>
          <p class="section-hint export-hint">
            씬에서 생성한 병합 영상을 미리보기/다운로드/삭제할 수 있습니다.
          </p>

          <div v-if="isExportLoading" class="export-loading">병합 영상 목록을 불러오는 중...</div>
          <Card v-else-if="sceneExports.length === 0 && !activeExport" dashed class="export-empty">
            아직 생성된 병합 영상이 없습니다.
          </Card>
          <div v-else class="export-list">
            <Card
              v-if="activeExport"
              :key="`active-${activeExport.sceneVideoId}`"
              class="export-card export-card-active"
            >
              <div class="export-thumb">
                <img
                  v-if="activeExport.thumbnailUrl"
                  :src="activeExport.thumbnailUrl"
                  alt="merge thumbnail"
                />
                <div v-else class="export-thumb-placeholder">No Preview</div>
              </div>
              <div class="export-info">
                <div class="export-badges">
                  <Badge
                    v-if="getStatusMeta(activeExport.status).visible"
                    :variant="getStatusMeta(activeExport.status).variant"
                  >
                    {{ getStatusMeta(activeExport.status).label }}
                  </Badge>
                  <Badge variant="rose">활성</Badge>
                </div>
                <div class="export-title">
                  {{ sceneTitle ? `${sceneTitle} 병합 영상` : '씬 병합 영상' }}
                </div>
                <div class="export-meta">
                  <span>생성: {{ formatCreatedAt(activeExport.createdAt) }}</span>
                  <span>길이: {{ formatDurationMs(activeExport.durationMs) }}</span>
                </div>
              </div>
              <div class="export-actions">
                <Button
                  variant="secondary"
                  size="sm"
                  class="export-btn export-btn-preview"
                  :disabled="!canPreviewExport(activeExport)"
                  @click="handlePreviewExport(activeExport)"
                >
                  미리보기
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  class="export-btn export-btn-download"
                  :disabled="!canDownloadExport(activeExport)"
                  @click="handleDownloadExport(activeExport)"
                >
                  다운로드
                </Button>
              </div>
            </Card>
            <Card v-for="item in filteredExports" :key="item.sceneVideoId" class="export-card">
              <div class="export-thumb">
                <img
                  v-if="item.thumbnailUrl"
                  :src="item.thumbnailUrl"
                  alt="merge thumbnail"
                />
                <div v-else class="export-thumb-placeholder">No Preview</div>
              </div>
              <div class="export-info">
                <div class="export-badges">
                  <Badge
                    v-if="getStatusMeta(item.status).visible"
                    :variant="getStatusMeta(item.status).variant"
                  >
                    {{ getStatusMeta(item.status).label }}
                  </Badge>
                  <Badge v-if="item.active" variant="rose">활성</Badge>
                </div>
                <div class="export-title">
                  {{ sceneTitle ? `${sceneTitle} 병합 영상` : '씬 병합 영상' }}
                </div>
                <div class="export-meta">
                  <span>생성: {{ formatCreatedAt(item.createdAt) }}</span>
                  <span>길이: {{ formatDurationMs(item.durationMs) }}</span>
                </div>
              </div>
              <div class="export-actions">
                <Button
                  variant="secondary"
                  size="sm"
                  class="export-btn export-btn-preview"
                  :disabled="!canPreviewExport(item)"
                  @click="handlePreviewExport(item)"
                >
                  미리보기
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  class="export-btn export-btn-download"
                  :disabled="!canDownloadExport(item)"
                  @click="handleDownloadExport(item)"
                >
                  다운로드
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  class="export-btn export-btn-activate"
                  :disabled="!canActivateExport(item)"
                  :loading="activatingExportId === item.sceneVideoId"
                  @click="handleActivateExport(item)"
                >
                  활성화
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  class="export-btn export-btn-delete"
                  :disabled="item.active || isDeletingExport"
                  @click="requestDeleteExport(item)"
                >
                  삭제
                </Button>
              </div>
            </Card>
          </div>
          <div v-if="exportTotalPages > 1" class="export-pagination">
            <Button
              variant="ghost"
              size="sm"
              :disabled="!canGoPrev || isExportLoading"
              @click="goToExportPage(exportPage - 1)"
            >
              {{ exportPrevLabel }}
            </Button>
            <template v-for="(item, index) in exportPageItems" :key="`export-page-${item}-${index}`">
              <span v-if="item === exportEllipsis" class="export-ellipsis">{{ exportEllipsis }}</span>
              <Button
                v-else
                :variant="item === exportPage ? 'primary' : 'ghost'"
                size="sm"
                class="export-page-button"
                :disabled="isExportLoading"
                @click="goToExportPage(item)"
              >
                {{ item }}
              </Button>
            </template>
            <Button
              variant="ghost"
              size="sm"
              :disabled="!canGoNext || isExportLoading"
              @click="goToExportPage(exportPage + 1)"
            >
              {{ exportNextLabel }}
            </Button>
          </div>
          <div v-if="exportError" class="export-error">{{ exportError }}</div>
        </section>
      </template>
    </div>
  </TimelineLayout>

  <SceneVideoPreviewModal />
  <ConfirmModal
    :is-open="!!deleteTarget"
    title="병합 영상 삭제"
    :message="deleteTarget ? '병합 영상을 삭제하시겠습니까\? 활성 영상은 삭제할 수 없습니다.' : ''"
    confirm-text="삭제"
    cancel-text="취소"
    :is-dangerous="true"
    @confirm="confirmDeleteExport"
    @cancel="closeDeleteExportModal"
  />
</template>

<style scoped>
.header-buttons {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}
.timeline-content > section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 3rem;
  color: var(--gray-500);
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--rose-100);
  border-top-color: var(--rose-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.section-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
  letter-spacing: 0;
  text-transform: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}
.section-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--gray-500);
}
.section-subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: var(--gray-500);
  line-height: 1.4;
}
.section-header .section-subtitle {
  margin-top: 0.25rem;
}
.section-header + .section-hint,
.section-header + .section-subtitle {
  margin-top: 0.1rem;
  margin-bottom: 0.75rem;
}
.project-scenes-body > .section-subtitle {
  margin-bottom: 0.75rem;
}
.preview-card,
.track-card {
  padding: 1.5rem;
  border-color: var(--rose-200);
  box-shadow: 0 12px 24px -18px rgba(15, 23, 42, 0.18);
}
.track-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  border: 1px dashed var(--rose-200);
  background: linear-gradient(135deg, var(--rose-50), white);
}
.project-clip-empty {
  text-align: center;
  color: var(--gray-500);
  padding: 1.5rem;
  border: 1px dashed var(--rose-200);
  border-radius: 16px;
  background: linear-gradient(135deg, var(--rose-50), white);
}
.project-clip-groups {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.project-clip-group {
  padding: 1rem;
  border: 1px solid var(--rose-100);
  border-radius: 16px;
  background: white;
}
.project-clip-group-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.project-clip-group-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  color: var(--gray-900);
}
.project-clip-group-name {
  font-size: 0.95rem;
  font-weight: 700;
}
.project-clip-group-meta {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--gray-500);
}
.project-clip-group-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.project-clip-toggle {
  border: 1px solid var(--rose-200);
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.25rem 0.65rem;
  cursor: pointer;
}
.project-clip-toggle:hover {
  background: var(--rose-100);
}
.project-clip-strip {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
}
.project-clip-thumb {
  position: relative;
  width: 104px;
  height: 58px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--rose-100);
  background: var(--rose-50);
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
}
.project-clip-thumb:focus-visible {
  outline: 2px solid var(--rose-400);
  outline-offset: 2px;
}
.project-clip-thumb.active {
  border-color: var(--rose-400);
  box-shadow: 0 0 0 2px rgba(255, 133, 161, 0.3);
}
.project-clip-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.project-clip-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  color: var(--gray-400);
}
.project-clip-duration {
  position: absolute;
  right: 6px;
  bottom: 6px;
  background: rgba(15, 23, 42, 0.7);
  color: white;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: 999px;
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
.timeline-scroll-container {
  overflow-x: auto;
  border: 1px solid var(--rose-200);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--rose-50), white);
  /* Custom scrollbar styling */
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
  transition: scrollbar-color 0.3s;
}
.timeline-scroll-container:hover {
  scrollbar-color: var(--rose-300) transparent;
}
.timeline-scroll-container::-webkit-scrollbar {
  height: 40px; /* Horizontal scrollbar height increased */
  background: transparent;
}
.timeline-scroll-container::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
}
.timeline-scroll-container:hover::-webkit-scrollbar-thumb {
  background: var(--rose-300);
}
.timeline-scroll-container::-webkit-scrollbar-thumb:hover {
  background: var(--rose-400);
}
.timeline-inner-wrapper {
  padding: 0;
  position: relative;
}
.timeline-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding-top: 0.5rem;
}


.project-overview-card {
  padding: 1.5rem;
  border: 1px solid var(--rose-200);
  border-radius: 18px;
  background: linear-gradient(135deg, var(--rose-100), white);
  box-shadow: none;
}

.project-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
}

.project-metric {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  border: 1px solid var(--rose-200);
  background: linear-gradient(135deg, var(--rose-50), white);
}

.metric-label {
  font-size: 0.7rem;
  color: var(--gray-600);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.metric-value {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--gray-900);
}

.project-scenes-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.project-scenes-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.project-scenes-empty {
  text-align: center;
  color: var(--gray-500);
  padding: 1.25rem;
  border: 1px dashed var(--rose-200);
  background: linear-gradient(135deg, var(--rose-50), white);
}

.project-scene-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.project-scene-card {
  padding: 1.1rem 1.35rem;
  border: 1px solid var(--rose-100);
  border-radius: 18px;
  background: white;
  box-shadow: none;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.project-scene-card:hover {
  border-color: var(--rose-200);
  box-shadow: 0 16px 36px -20px rgba(255, 133, 161, 0.35);
  transform: translateY(-1px);
}

.project-scene-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.project-scene-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.project-scene-info {
  flex: 1;
  min-width: 0;
}

.project-scene-title {
  margin-top: 0.35rem;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--gray-900);
}

.project-scene-description {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: var(--gray-500);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-scene-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  flex-shrink: 0;
}

.project-scene-clips {
  margin-top: 0.85rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--rose-100);
}

.project-scene-clip-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.project-scene-clip-meta-items {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.project-scene-clip-count,
.project-scene-clip-duration {
  font-weight: 600;
}

.project-scene-clip-empty {
  margin-top: 0.85rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  border: 1px dashed var(--rose-200);
  background: linear-gradient(135deg, var(--rose-50), white);
  color: var(--gray-500);
  font-size: 0.8rem;
}

@media (max-width: 960px) {
  .project-metrics {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }

  .project-clip-group-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .project-scene-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .project-scene-actions {
    justify-content: flex-start;
  }
}


.export-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-header.export-header {
  border-bottom: none;
  padding-bottom: 0;
  margin-bottom: 0.25rem;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 0.5rem;
  border-radius: 999px;
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.65rem;
  font-weight: 700;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-toggle {
  border-radius: 999px;
  padding: 0.2rem 0.75rem;
  font-weight: 600;
  color: var(--gray-600);
  border: 1px solid var(--gray-200);
  background: white;
}

.section-toggle:hover {
  color: var(--gray-800);
  border-color: var(--rose-200);
  background: var(--rose-50);
}

.section-hint.export-hint {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--gray-500);
}

.export-loading {
  font-size: 0.8125rem;
  color: var(--gray-500);
}

.export-empty {
  text-align: center;
  color: var(--gray-500);
}

.export-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.export-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  border: 1px solid var(--rose-100);
  border-radius: 18px;
  background: white;
  box-shadow: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.export-card:hover {
  border-color: var(--rose-200);
  box-shadow: 0 16px 36px -20px rgba(255, 133, 161, 0.35);
  transform: translateY(-1px);
}

.export-card-active {
  border: 1px solid var(--rose-200);
  background: linear-gradient(90deg, rgba(255, 239, 247, 0.85), rgba(255, 255, 255, 1) 45%);
  box-shadow: 0 16px 36px -22px rgba(255, 133, 161, 0.35);
}

.export-card-active:hover {
  transform: none;
  border-color: var(--rose-200);
  box-shadow: 0 16px 36px -22px rgba(255, 133, 161, 0.4);
}
.export-card-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: linear-gradient(180deg, var(--rose-400), var(--rose-500));
  border-radius: 16px 0 0 16px;
}

.export-thumb {
  position: relative;
  width: 160px;
  height: 90px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--rose-50);
  border: 1px solid var(--rose-100);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.65);
}

.export-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.export-thumb-placeholder {
  font-size: 0.75rem;
  color: var(--gray-400);
}

.export-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}

.export-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.export-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--gray-900);
}

.export-meta {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.export-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  margin-top: 0;
  flex-shrink: 0;
}

.export-actions :deep(.export-btn) {
  min-width: 0;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.export-actions :deep(.export-btn-preview) {
  background: var(--gray-50);
  color: var(--gray-700);
  border: 1px solid var(--gray-200);
}

.export-actions :deep(.export-btn-preview:hover) {
  background: white;
  border-color: var(--rose-200);
  color: var(--rose-600);
}

.export-actions :deep(.export-btn-activate) {
  background: var(--rose-50);
  border: 1px solid var(--rose-200);
  color: var(--rose-700);
}

.export-actions :deep(.export-btn-activate:hover) {
  background: var(--rose-100);
  border-color: var(--rose-300);
  color: var(--rose-700);
}

.export-actions :deep(.export-btn-download) {
  box-shadow: var(--shadow-sm);
}

.export-actions :deep(.export-btn-download:hover) {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.export-actions :deep(.export-btn-delete) {
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--error);
}

.export-actions :deep(.export-btn-delete:hover) {
  background: rgba(239, 68, 68, 0.08);
  color: var(--error-600);
}

.export-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding-top: 0.75rem;
  margin-top: 0.25rem;
  border-top: 1px solid var(--rose-100);
}

.export-ellipsis {
  font-size: 0.75rem;
  color: var(--gray-400);
  padding: 0 0.25rem;
}

.export-page-button {
  min-width: 2.25rem;
}

.export-error {
  font-size: 0.75rem;
  color: var(--error-600);
  margin-top: 0.25rem;
  padding: 0.5rem 0.75rem;
  border-radius: 10px;
  border: 1px solid var(--error-border);
  background: var(--error-bg);
}

@media (max-width: 960px) {
  .export-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .export-thumb {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }

  .export-actions {
    justify-content: flex-start;
    margin-top: 0.5rem;
  }
}
/* Uses global .icon-sm from base.css */
</style>
