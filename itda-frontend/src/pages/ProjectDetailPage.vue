<script setup lang="ts">
import ProjectLayout from '../layouts/ProjectLayout.vue'
import StoryTab from './project/sections/StoryTab.vue'
import ScenesTab from './project/sections/ScenesTab.vue'
import ObjectsTab from './project/sections/ObjectsTab.vue'
import TimelinePlaybackModal from '../components/timeline/TimelinePlaybackModal.vue'
import SceneVideoPreviewModal from '../components/scene-editor/SceneVideoPreviewModal.vue'
import { useProjectDetail } from './project/composables/useProjectDetail'
import { SCENE_VIDEO_PREVIEW_MODAL_ID, TIMELINE_PLAYBACK_MODAL_ID } from '../constants/ui'

const {
  activeTab,
  tabItems,
  projectId,
  project,
  scenes,
  sceneProgress,
  objects,
  isSavingObject,
  isUpdatingObject,
  editingObject,
  scenarioStore,
  resolveSceneStatusConfig,
  handleTabChange,
  getSceneEditLink,
  getScenePreview,
  isPreviewLoading,
  openPreview,
  closePreview,
  activePreviewClip,
  activePreviewScene,
  setPreviewTrackRef,
  scrollPreview,
  handlePreviewWheel,
  getClipWidth,
  getOverflowCount,
  toggleStoryboard,
  handleStoryboardWheel,
  storyboardOpenMap,
  handleDragStart,
  handleDragEnd,
  handleDragOver,
  handleAddScene,
  handleDeleteScene,
  openAddObjectModal,
  openEditObjectModal,
  handleAddObject,
  handleUpdateObject,
  handleDeleteObject,
  handleDownloadObjectImage,
} = useProjectDetail()

import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useCollabStore } from '../stores/collab'
import { useTimelineStore } from '../stores/timeline'
import { useUIStore } from '../stores/ui'

const collabStore = useCollabStore()
const timelineStore = useTimelineStore()
const uiStore = useUIStore()
const isProjectPreviewLoading = ref(false)
const projectPreviewClips = computed(() => timelineStore.orderedClips)
// Hide Scenes tab UI (page disabled for now).
const isScenesTabHidden = true
const visibleTabs = computed(() =>
  tabItems.filter((tab) => (isScenesTabHidden ? tab.key !== 'scenes' : true))
)

// ... existing code ...

onMounted(() => {
  if (projectId.value) {
    collabStore.joinRoom(Number(projectId.value))
    collabStore.updateLocation('SCENE_LIST')
  }
})

// Watch for ID changes (e.g. reload or route update)
watch(projectId, (newId) => {
    if (newId) {
        collabStore.joinRoom(Number(newId))
        collabStore.updateLocation('SCENE_LIST')
    }
})

onUnmounted(() => {
  timelineStore.clearTimeline()
})

function openProjectMergedPreview(): boolean {
  if (!project.value?.previewVideoUrl) return false
  uiStore.openModal(SCENE_VIDEO_PREVIEW_MODAL_ID, {
    title: project.value.title || '프로젝트 미리보기',
    videoUrl: project.value.previewVideoUrl,
    posterUrl: project.value.previewThumbnailUrl || project.value.thumbnailUrl || null,
  })
  return true
}

async function openTimelineFallbackPreview(): Promise<boolean> {
  if (!projectId.value) return false

  await timelineStore.loadClips(projectId.value, undefined, {
    hydrateDurations: false,
  })

  if (timelineStore.error) {
    console.error('Failed to load timeline clips for preview fallback:', timelineStore.error)
    return false
  }

  const startClip = timelineStore.orderedClips.find((clip) => Boolean(clip.videoUrl))
  if (!startClip) return false

  uiStore.openModal(TIMELINE_PLAYBACK_MODAL_ID, { startClipId: startClip.clipId })
  return true
}

async function handleProjectPreview(): Promise<void> {
  if (!projectId.value || !project.value || isProjectPreviewLoading.value) return

  isProjectPreviewLoading.value = true
  try {
    if (openProjectMergedPreview()) {
      return
    }

    const openedFallback = await openTimelineFallbackPreview()
    if (!openedFallback) {
      uiStore.showToast({
        type: 'error',
        title: 'Preview unavailable',
        message: 'No playable videos are available.',
      })
    }
  } catch (error) {
    console.error('Failed to open project preview modal:', error)
    uiStore.showToast({
      type: 'error',
      title: 'Preview unavailable',
      message: 'Could not open preview. Please try again shortly.',
    })
  } finally {
    isProjectPreviewLoading.value = false
  }
}


const openScenarioDrawer = () => scenarioStore.openDrawer()
</script>

<template>
  <ProjectLayout
    :project="project"
    :active-tab="activeTab"
    :scene-count="scenes.length"
    :progress="sceneProgress"
    :hide-scenes="isScenesTabHidden"
    :preview-loading="isProjectPreviewLoading"
    @tab-change="handleTabChange"
    @preview="handleProjectPreview"
  >
    <div class="project-content">
      <!-- Tabs -->
      <div class="tabs">
        <button
        v-for="tab in visibleTabs"
          :key="tab.key"
          :class="['tab', { active: activeTab === tab.key }]"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <StoryTab
        v-if="activeTab === 'story'"
        :scenes="scenes"
        :project="project"
        :project-id="projectId"
        :storyboard-open-map="storyboardOpenMap"
        :get-scene-preview="getScenePreview"
        :is-preview-loading="isPreviewLoading"
        :open-preview="openPreview"
        :close-preview="closePreview"
        :active-preview-clip="activePreviewClip"
        :active-preview-scene="activePreviewScene"
        :toggle-storyboard="toggleStoryboard"
        :handle-storyboard-wheel="handleStoryboardWheel"
        :handle-add-scene="handleAddScene"
        :handle-delete-scene="handleDeleteScene"
        :handle-drag-start="handleDragStart"
        :handle-drag-end="handleDragEnd"
        :handle-drag-over="handleDragOver"
        :on-open-scenario="openScenarioDrawer"
      />

      <ScenesTab
        v-if="activeTab === 'scenes' && !isScenesTabHidden"
        :scenes="scenes"
        :project-id="projectId"
        :resolve-scene-status-config="resolveSceneStatusConfig"
        :get-scene-preview="getScenePreview"
        :is-preview-loading="isPreviewLoading"
        :open-preview="openPreview"
        :close-preview="closePreview"
        :active-preview-clip="activePreviewClip"
        :active-preview-scene="activePreviewScene"
        :set-preview-track-ref="setPreviewTrackRef"
        :scroll-preview="scrollPreview"
        :handle-preview-wheel="handlePreviewWheel"
        :get-clip-width="getClipWidth"
        :get-overflow-count="getOverflowCount"
        :get-scene-edit-link="getSceneEditLink"
      />

      <ObjectsTab
        v-if="activeTab === 'objects'"
        :objects="objects"
        :editing-object="editingObject"
        :is-saving-object="isSavingObject"
        :is-updating-object="isUpdatingObject"
        :open-add-object-modal="openAddObjectModal"
        :open-edit-object-modal="openEditObjectModal"
        :handle-add-object="handleAddObject"
        :handle-update-object="handleUpdateObject"
        :handle-delete-object="handleDeleteObject"
        :handle-download-object-image="handleDownloadObjectImage"
      />
    </div>
  </ProjectLayout>
  <TimelinePlaybackModal :clips="projectPreviewClips" />
  <SceneVideoPreviewModal />
</template>

<style>
/* Project Content */
.project-content {
  max-width: 980px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box; /* Maintain padding within width */
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--rose-100);
  padding-bottom: 0.75rem;
  overflow-x: auto; /* Enable horizontal scrolling */
  white-space: nowrap; /* Prevent wrapping */
  -webkit-overflow-scrolling: touch; /* Smooth scroll on iOS */
  padding-right: 1rem; /* Padding for scroll end */
}

/* Hide scrollbar for cleaner UI */
.tabs::-webkit-scrollbar {
  height: 0;
  width: 0;
  display: none;
}

.tab {
  padding: 0.25rem 0.25rem 0.75rem;
  border: none;
  background: transparent;
  color: var(--gray-400);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: 0;
  transition: all 0.2s ease;
  flex-shrink: 0; /* Don't shrink tabs */
  position: relative;
}

.tab:hover {
  color: var(--gray-900);
}

.tab.active {
  color: var(--rose-600);
}

.tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background: var(--rose-500);
  border-radius: 999px;
  opacity: 0;
  transform: scaleX(0.6);
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.tab.active::after {
  opacity: 1;
  transform: scaleX(1);
}

/* Section Header */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap; /* Allow wrapping on small screens */
  gap: 0.5rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto; /* Push to right */
}

/* Scene List */
.scene-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.storyboard-wrap {
  margin-top: 0.75rem;
}

.storyboard-strip {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 0.25rem;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.storyboard-item {
  flex: 0 0 auto;
  width: 120px;
  border: 1px solid var(--rose-100);
  border-radius: 10px;
  overflow: hidden;
  background: white;
  padding: 0;
  cursor: pointer;
  scroll-snap-align: start;
}

.storyboard-thumb {
  width: 100%;
  height: 72px;
  object-fit: cover;
  display: block;
}

.storyboard-label {
  display: block;
  padding: 0.375rem 0.5rem;
  font-size: 0.6875rem;
  color: var(--gray-600);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.storyboard-empty {
  font-size: 0.75rem;
  color: var(--gray-500);
  padding: 0.5rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.add-scene-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  color: var(--gray-500);
  font-size: 0.875rem;
}

.add-icon {
  width: 20px;
  height: 20px;
  color: var(--rose-400);
}

/* Scene Preview */
.scene-preview-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.scene-preview-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.scene-preview-row {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 1fr;
  gap: 1.5rem;
  align-items: center;
}

.preview-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0; /* Allow flex shrinking for ellipses */
}

.preview-media {
  position: relative;
  background: var(--rose-50);
  border-radius: 12px;
  padding: 0.75rem 1.25rem;
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  overflow: hidden;
}

.preview-strip {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  overflow-x: auto;
  padding-bottom: 0.25rem;
  width: 100%;
  scroll-behavior: smooth;
}

.preview-thumb {
  position: relative;
  height: 54px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.preview-thumb .preview-image,
.preview-thumb .preview-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.preview-thumb .preview-video {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.preview-thumb.has-video:hover .preview-video,
.preview-thumb.has-video:focus-visible .preview-video {
  opacity: 1;
}

.preview-thumb.has-video:hover .preview-image,
.preview-thumb.has-video:focus-visible .preview-image {
  opacity: 0;
}

.preview-thumb.force-video .preview-video {
  opacity: 1;
}

.preview-thumb.force-video .preview-image {
  opacity: 0;
}

.preview-duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  font-size: 0.625rem;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.75);
  color: white;
  padding: 1px 4px;
  border-radius: 4px;
}

.preview-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: rgba(15, 23, 42, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.preview-thumb:hover .preview-play {
  opacity: 1;
}

.preview-scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: white;
  color: var(--gray-600);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  cursor: pointer;
}

.preview-scroll-btn.left {
  left: 8px;
}

.preview-scroll-btn.right {
  right: 8px;
}

.preview-media:hover .preview-scroll-btn {
  opacity: 1;
}

.preview-overflow {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--rose-500);
  background: white;
  border: 1px dashed var(--rose-200);
  border-radius: 999px;
  padding: 0.125rem 0.5rem;
}

.preview-empty {
  width: 100%;
  height: 72px;
  border-radius: 10px;
  border: 1px dashed var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.5rem;
  color: var(--gray-500);
  font-size: 0.75rem;
  background: white;
}

.preview-empty img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.preview-empty img.preview-empty-icon {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.preview-empty-icon {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.preview-loading {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.preview-body {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap; /* Allow wrapping */
}

.preview-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--gray-900);
}

.preview-description {
  margin: 0;
  color: var(--gray-500);
  font-size: 0.8125rem;
  line-height: 1.4;
}

.preview-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.preview-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.empty-state {
  border: 1px dashed var(--rose-200);
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
  color: var(--gray-500);
  background: var(--rose-50);
  margin-bottom: 1rem;
}

@media (max-width: 960px) {
  .scene-preview-row {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .preview-media {
    min-height: 80px;
    padding: 0.5rem;
  }
}

/* Ensure images/videos are responsive */
img, video {
    max-width: 100%;
    height: auto;
}

/* Preview Modal */
.preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.preview-modal-content {
  width: min(880px, 100%);
  background: white;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  max-height: 90vh; /* Don't overflow screen */
  overflow-y: auto;
}

.preview-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.preview-modal-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
}

.preview-modal-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--gray-500);
}

.preview-modal-close {
  border: none;
  background: var(--rose-50);
  color: var(--gray-600);
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.preview-modal-close:hover {
  background: var(--rose-100);
  color: var(--rose-500);
}

.preview-modal-body {
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-modal-body video {
  width: 100%;
  max-height: 50vh; /* limit height more on mobile */
  border-radius: 12px;
  background: black;
}

.preview-modal-empty {
  width: 100%;
  min-height: 240px;
  border-radius: 12px;
  border: 1px dashed var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.75rem;
  color: var(--gray-500);
}

.preview-modal-empty img {
  max-width: 100%;
  border-radius: 12px;
}

.preview-modal-empty-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.storyboard-empty-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

/* Object Grid */
.object-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); /* smaller min size for mobile */
  gap: 1rem;
}

.add-object-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  text-align: center;
  min-height: 200px;
}

.add-object-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--rose-50);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-object-icon .add-icon {
  width: 24px;
  height: 24px;
}

.add-object-text {
  color: var(--gray-500);
  font-size: 0.875rem;
}

/* Scenario Trigger */
.scenario-trigger {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1.25rem;
  background: linear-gradient(135deg, var(--rose-50) 0%, white 100%);
  border: 1px solid var(--rose-100);
  border-radius: 12px;
}

.scenario-hint {
  font-size: 0.8125rem;
  color: var(--gray-500);
  margin: 0;
}

/* Utilities */
.mb-6 {
  margin-bottom: 1.5rem;
}

/* Uses global .icon-sm from base.css */

.text-muted {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.tab-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
