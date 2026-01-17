<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useTimelineStore } from '../stores/timeline'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'

import TimelineLayout from '../layouts/TimelineLayout.vue'
import VideoPreview from '../components/timeline/VideoPreview.vue'
import VideoTrack from '../components/timeline/VideoTrack.vue'
import TimeRuler from '../components/timeline/TimeRuler.vue'
import MergeProgress from '../components/timeline/MergeProgress.vue'
import Button from '../components/common/Button.vue'
import Card from '../components/common/Card.vue'
import { GitMerge, Download, RefreshCw } from 'lucide-vue-next'

const route = useRoute()
const projectStore = useProjectStore()
const timelineStore = useTimelineStore()
const uiStore = useUIStore()
const collabStore = useCollabStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

onMounted(async () => {
  if (projectId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      timelineStore.loadClips(projectId.value),
    ])
    
    // 협업 방 입장
    collabStore.joinRoom(projectId.value)
    collabStore.updateLocation('Timeline 편집 중')
  }
})

onUnmounted(() => {
  // 페이지 이탈 시 협업 방 퇴장
  collabStore.leaveRoom()
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
</script>

<template>
  <TimelineLayout
    :project-title="project?.title || 'Project'"
    :clip-count="timelineStore.clipCount"
    :total-duration="timelineStore.totalDuration"
  >
    <div class="timeline-content">
      <!-- Loading -->
      <div v-if="timelineStore.isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>타임라인 로딩 중...</span>
      </div>

      <template v-else>
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

        <!-- Actions -->
        <div class="timeline-actions">
          <Button
            v-if="timelineStore.mergeStatus === 'done'"
            variant="ghost"
            @click="handleReset"
          >
            <RefreshCw class="icon-sm" />
            다시 병합
          </Button>
          <Button
            variant="primary"
            :disabled="!timelineStore.canMerge"
            @click="handleMerge"
          >
            <GitMerge class="icon-sm" />
            영상 병합하기
          </Button>
          <Button
            variant="secondary"
            :disabled="!timelineStore.canDownload"
            @click="handleDownload"
          >
            <Download class="icon-sm" />
            다운로드 (MP4)
          </Button>
        </div>
      </template>
    </div>
  </TimelineLayout>
</template>

<style scoped>
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

.timeline-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

/* Uses global .icon-sm from base.css */
</style>
