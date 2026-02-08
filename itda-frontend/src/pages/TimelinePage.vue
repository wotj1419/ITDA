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
import { fetchSceneExports, deleteSceneExport } from '../services/api/timeline'
import { SCENE_VIDEO_PREVIEW_MODAL_ID } from '../constants/ui'
import type { SceneExportItem } from '../types/api/timeline'

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

const orderedClips = computed(() => timelineStore.orderedClips)
const selectedClipId = ref<string | null>(null)

const sceneExports = ref<SceneExportItem[]>([])
const isExportLoading = ref(false)
const exportError = ref<string | null>(null)
const deleteTarget = ref<SceneExportItem | null>(null)
const isDeletingExport = ref(false)
const exportPage = ref(1)
const exportPageSize = 8
const exportTotalCount = ref(0)
const exportPrevLabel = '이전'
const exportNextLabel = '다음'
const exportEllipsis = '...'

const exportCountLabel = computed(() => (
  exportTotalCount.value > 0 ? exportTotalCount.value : sceneExports.value.length
))

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

async function loadSceneExports(page = 1): Promise<void> {
  if (sceneId.value === null) return
  isExportLoading.value = true
  exportError.value = null
  exportPage.value = page
  exportTotalCount.value = 0
  try {
    const response = await fetchSceneExports(sceneId.value, {
      page,
      size: exportPageSize,
    })
    sceneExports.value = response.items
    exportTotalCount.value = response.totalCount
    exportPage.value = response.page
  } catch (error) {
    console.error('Failed to load scene exports', error)
    exportError.value = '병합 영상 목록을 불러오지 못했습니다.'
    sceneExports.value = []
    exportTotalCount.value = 0
  } finally {
    isExportLoading.value = false
  }
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

function getStatusMeta(status?: string | null): { label: string; variant: 'default' | 'success' | 'warning' | 'error' | 'info' } {
  switch (status) {
    case 'COMPLETED':
      return { label: '완료', variant: 'success' }
    case 'FAILED':
      return { label: '실패', variant: 'error' }
    case 'GENERATING':
      return { label: '생성 중', variant: 'warning' }
    case 'QUEUED':
      return { label: '대기', variant: 'info' }
    default:
      return { label: status || '알 수 없음', variant: 'default' }
  }
}

function canPreviewExport(item: SceneExportItem): boolean {
  return item.status === 'COMPLETED' && Boolean(item.previewUrl)
}

function canDownloadExport(item: SceneExportItem): boolean {
  return item.status === 'COMPLETED' && Boolean(item.downloadUrl)
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
        <section>
          <div class="section-header">
            <h3 class="section-title">타임라인 클립</h3>
            <span class="section-hint">드래그로 순서를 변경할 수 있습니다.</span>
          </div>
          <Card class="track-card">
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
          </Card>
        </section>
        <!-- Merge Progress -->
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
        <section v-if="isSceneTimeline" class="export-section">
          <div class="project-section-header">
            <div class="project-section-title-wrap">
              <h3 class="project-section-title">씬 병합 영상</h3>
              <span class="section-count">{{ exportCountLabel }}</span>
            </div>
            <div class="project-section-actions">
              <Button
                variant="secondary"
                size="sm"
                :disabled="isExportLoading"
                @click="loadSceneExports"
              >
                새로고침
              </Button>
            </div>
          </div>
          <p class="project-section-hint">
            씬에서 생성한 병합 영상을 미리보기/다운로드/삭제할 수 있습니다.
          </p>

          <div v-if="isExportLoading" class="export-loading">병합 영상 목록을 불러오는 중...</div>
          <Card v-else-if="sceneExports.length === 0" dashed class="export-empty">
            아직 생성된 병합 영상이 없습니다.
          </Card>
          <div v-else class="export-list">
            <Card v-for="item in sceneExports" :key="item.sceneVideoId" class="export-card">
              <div class="export-thumb">
                <img
                  v-if="item.thumbnailUrl"
                  :src="item.thumbnailUrl"
                  alt="merge thumbnail"
                />
                <div v-else class="export-thumb-placeholder">No Preview</div>
                <span v-if="item.active" class="export-active">활성</span>
              </div>
              <div class="export-info">
                <div class="export-badges">
                  <Badge :variant="getStatusMeta(item.status).variant">
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
                  :disabled="!canPreviewExport(item)"
                  @click="handlePreviewExport(item)"
                >
                  미리보기
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  :disabled="!canDownloadExport(item)"
                  @click="handleDownloadExport(item)"
                >
                  다운로드
                </Button>
                <Button
                  variant="danger"
                  size="sm"
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
  gap: 1.5rem;
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
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
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
  padding-top: 0.75rem;
  border-top: 1px solid var(--rose-100);
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
  border-radius: 8px;
  background: var(--rose-50);
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


.export-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.project-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.project-section-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.project-section-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
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

.project-section-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.project-section-hint {
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
  display: grid;
  grid-template-columns: 160px 1fr auto;
  gap: 1rem;
  align-items: center;
}

.export-thumb {
  position: relative;
  width: 160px;
  height: 90px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--rose-50);
  border: 1px solid var(--rose-100);
  display: flex;
  align-items: center;
  justify-content: center;
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

.export-active {
  position: absolute;
  top: 6px;
  left: 6px;
  background: var(--rose-500);
  color: white;
  font-size: 0.625rem;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 999px;
}

.export-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.export-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.export-title {
  font-size: 0.9rem;
  font-weight: 600;
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
}

.export-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding-top: 0.5rem;
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
}

@media (max-width: 960px) {
  .export-card {
    grid-template-columns: 1fr;
  }

  .export-thumb {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }

  .export-actions {
    justify-content: flex-start;
  }
}
/* Uses global .icon-sm from base.css */
</style>
