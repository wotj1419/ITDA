<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useTimelineStore } from '../stores/timeline'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'
import { TIMELINE_PLAYBACK_MODAL_ID } from '../constants/ui'

import TimelineLayout from '../layouts/TimelineLayout.vue'
import VideoPreview from '../components/timeline/VideoPreview.vue'
import VideoTrack from '../components/timeline/VideoTrack.vue'
import TimeRuler from '../components/timeline/TimeRuler.vue'
import MergeProgress from '../components/timeline/MergeProgress.vue'
import TimelinePlaybackModal from '../components/timeline/TimelinePlaybackModal.vue'
import Button from '../components/common/Button.vue'
import { GitMerge, RefreshCw, Play } from 'lucide-vue-next'

const route = useRoute()
const projectStore = useProjectStore()
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

onMounted(async () => {
  if (projectId.value) {
    // 협업 방 입장 (데이터 로딩과 병렬, 실패 영향 최소화)
    collabStore.joinRoom(projectId.value)
    collabStore.updateLocation('Timeline 편집 중')

    await Promise.all([
      projectStore.loadProject(projectId.value),
      timelineStore.loadClips(projectId.value, sceneId.value ?? undefined),
    ])
  }
})

watch([projectId, sceneId], async ([nextProjectId, nextSceneId]) => {
  if (!nextProjectId) return

  // 프로젝트 변경 시 협업 방 재입장
  collabStore.joinRoom(nextProjectId)
  collabStore.updateLocation('Timeline 편집 중')

  await timelineStore.loadClips(nextProjectId, nextSceneId ?? undefined)
})

async function handleReorder(clipIds: string[]) {
  const success = await timelineStore.reorderClips(clipIds)
  if (success) {
    uiStore.showToast({
      type: 'success',
      title: '순서 변경',
      message: '클립 순서가 변경되었습니다.',
    })
  }
}

async function handleRemove(clipId: string) {
  const success = await timelineStore.removeClip(clipId)
  if (success) {
    uiStore.showToast({
      type: 'success',
      title: '삭제',
      message: '클립이 제거되었습니다.',
    })
  }
}

async function handleMerge() {
  const success = await timelineStore.startMerge()
  if (success) {
    uiStore.showToast({
      type: 'success',
      title: '병합 완료',
      message: '영상이 준비되었습니다.',
    })
  }
}

function handleDownload() {
  if (timelineStore.downloadUrl) {
    window.open(timelineStore.downloadUrl, '_blank')
  }
}

function handleReset() {
  timelineStore.resetMerge()
}

function handlePlay() {
  uiStore.openModal(TIMELINE_PLAYBACK_MODAL_ID)
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
  >
    <template #actions>
      <div class="header-buttons">
        <Button
          v-if="timelineStore.mergeStatus === 'done'"
          variant="ghost"
          @click="handleReset"
        >
          <RefreshCw class="icon-md" />
          다시 병합
        </Button>
        <Button
          variant="primary"
          :disabled="!timelineStore.canMerge"
          @click="handleMerge"
        >
          <GitMerge class="icon-md" />
          영상 병합하기
        </Button>
      </div>
    </template>

    <div class="timeline-content">
      <!-- Loading -->
      <div v-if="timelineStore.isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>타임라인 로딩 중...</span>
      </div>

      <template v-else>
        <!-- Preview -->
        <section>
          <div class="section-header">
            <h3 class="section-title">미리보기</h3>
            <Button
              variant="ghost"
              :disabled="timelineStore.orderedClips.length === 0"
              @click="handlePlay"
            >
              <Play class="icon-md" />
              재생
            </Button>
          </div>
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
                  :clips="timelineStore.orderedClips"
                  @reorder="handleReorder"
                  @remove="handleRemove"
                />
              </div>
            </div>
            <div class="track-info">
              <span class="clip-count">{{ timelineStore.clipCount }}개 클립</span>
              <span class="duration-text">
                총 길이: {{ timelineStore.totalDuration }}초
              </span>
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
      </template>
    </div>
  </TimelineLayout>

  <TimelinePlaybackModal :clips="timelineStore.orderedClips" />
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

/* Uses global .icon-sm from base.css */
</style>
