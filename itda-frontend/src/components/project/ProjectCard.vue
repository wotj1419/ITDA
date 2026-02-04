<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { MoreVertical, Trash2 } from 'lucide-vue-next'
import type { Project } from '../../types/api/projects'
import Badge from '../common/Badge.vue'
import TimeAgo from '../common/TimeAgo.vue'
import { useProjectStore } from '../../stores/project'
import { useSceneStore } from '../../stores/scene'

interface Props {
  project: Project
  isFavorite?: boolean
  viewMode?: 'grid' | 'list'
}

const props = withDefaults(defineProps<Props>(), {
  isFavorite: false,
  viewMode: 'grid',
})

const projectStore = useProjectStore()
const sceneStore = useSceneStore()

const handleCardClick = () => {
  projectStore.touchProject(props.project.projectId)
}

const isMenuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const progress = computed(() => {
  const completed = sceneStore.getCompletedSceneCount(props.project.projectId)
  const total = Math.max(props.project.sceneCount ?? 0, completed)
  return { completed, total }
})
const isHighlighted = computed(() => projectStore.highlightedProjectId === props.project.projectId)

const progressPercent = computed(() => {
  if (progress.value.total === 0) return 0
  return Math.round((progress.value.completed / progress.value.total) * 100)
})

const previewImageUrl = computed(() => props.project.previewThumbnailUrl || props.project.thumbnailUrl || '')
const previewVideoUrl = computed(() => props.project.previewVideoUrl || '')
const hasPreviewVideo = computed(() => Boolean(previewVideoUrl.value))
const showPlaceholder = computed(() => !previewImageUrl.value && !hasPreviewVideo.value)

const previewVideoRef = ref<HTMLVideoElement | null>(null)
const isPreviewPlaying = ref(false)

const playPreview = async () => {
  if (!hasPreviewVideo.value) return
  const videoEl = previewVideoRef.value
  if (!videoEl) return
  try {
    videoEl.preload = 'metadata'
    videoEl.currentTime = 0
    await videoEl.play()
    isPreviewPlaying.value = true
  } catch {
    isPreviewPlaying.value = false
  }
}

const stopPreview = () => {
  const videoEl = previewVideoRef.value
  if (!videoEl) return
  videoEl.pause()
  videoEl.currentTime = 0
  isPreviewPlaying.value = false
}


const badgeVariant = computed(() => {
  switch (props.project.genre?.toLowerCase()) {
    case 'sci-fi':
    case 'sf':
      return 'rose'
    case 'draft':
      return 'default'
    default:
      return 'default'
  }
})

const toggleMenu = (e: Event) => {
  e.preventDefault()
  e.stopPropagation()
  isMenuOpen.value = !isMenuOpen.value
}

const closeMenu = () => {
  isMenuOpen.value = false
}

// Click outside handler
const handleClickOutside = (e: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const emit = defineEmits<{
  (e: 'toggle-favorite', projectId: number): void
  (e: 'delete', projectId: number): void
}>()

const handleDeleteRequest = (e: Event) => {
  e.preventDefault()
  e.stopPropagation()
  closeMenu()
  emit('delete', props.project.projectId)
}
</script>

<template>
  <RouterLink
    :to="`/projects/${project.projectId}`"
    :class="['project-card', { 'project-card--list': viewMode === 'list', 'project-card--flash': isHighlighted }]"
    :data-project-id="project.projectId"
    @click="handleCardClick"
  >
    <!-- Favorite Icon (Top-Left) -->
    <label
      v-if="project"
      title="Star"
      class="star"
      @click.stop
    >
      <input
        :id="`star-checkbox-${project.projectId}`"
        class="checkbox"
        type="checkbox"
        :checked="isFavorite"
        @click.stop
        @change.stop="$emit('toggle-favorite', project.projectId)"
      />
      <div class="svg-container">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="svg-outline"
          viewBox="0 0 24 24"
        >
          <path
            d="M12 2.5L9.45 8.5L3 9.06L7.725 13.39L6.25 19.82L12 16.5L17.75 19.82L16.275 13.39L21 9.06L14.55 8.5L12 2.5ZM12 4.75L14 9.33L18.7 9.75L15 13.07L16.18 17.75L12 15.16L7.82 17.75L9 13.07L5.3 9.75L10 9.33L12 4.75Z"
          ></path>
        </svg>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="svg-filled"
          viewBox="0 0 24 24"
        >
          <path
            d="M12 2.5L9.45 8.5L3 9.06L7.725 13.39L6.25 19.82L12 16.5L17.75 19.82L16.275 13.39L21 9.06L14.55 8.5L12 2.5Z"
          ></path>
        </svg>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="svg-celebrate"
          viewBox="0 0 100 100"
          width="100"
          height="100"
        >
          <polygon points="10,10 20,20"></polygon>
          <polygon points="10,50 20,50"></polygon>
          <polygon points="20,80 30,70"></polygon>
          <polygon points="90,10 80,20"></polygon>
          <polygon points="90,50 80,50"></polygon>
          <polygon points="80,80 70,70"></polygon>
        </svg>
      </div>
    </label>

    <!-- More Menu (Top-Right) -->
    <div class="more-menu-container" ref="menuRef">
      <button class="more-btn" @click="toggleMenu">
        <MoreVertical class="icon-sm" />
      </button>
      <div v-if="isMenuOpen" class="dropdown-menu">
        <button class="menu-item delete" @click="handleDeleteRequest">
          <Trash2 class="icon-sm" />
          ??젣
        </button>
      </div>
    </div>

    <!-- Thumbnail -->
    <div
      class="card-thumbnail"
      @mouseenter="playPreview"
      @mouseleave="stopPreview"
      @focusin="playPreview"
      @focusout="stopPreview"
    >
      <video
        v-if="hasPreviewVideo"
        ref="previewVideoRef"
        class="thumbnail-video"
        :class="{ 'is-visible': isPreviewPlaying || !previewImageUrl }"
        :src="previewVideoUrl"
        :poster="previewImageUrl || undefined"
        muted
        loop
        playsinline
        preload="none"
      />
      <img
        v-if="previewImageUrl"
        :src="previewImageUrl"
        :alt="project.title"
        class="thumbnail-image"
      />
      <div
        v-if="showPlaceholder"
        class="thumbnail-placeholder"
      >
        <img src="/icon.png" alt="아직 미리보기가 없어요" class="preview-icon" />
        <span>아직 미리보기가 없어요</span>
      </div>
    </div>

    <!-- Content -->
    <div class="card-content">
      <!-- Title & Badge -->
      <div class="card-header">
        <h3 class="card-title">{{ project.title }}</h3>
        <Badge v-if="project.genre" :variant="badgeVariant">
          {{ project.genre }}
        </Badge>
      </div>

      <!-- Description -->
      <p v-if="project.description" class="card-description">
        {{ project.description }}
      </p>

      <!-- Progress -->
      <div class="card-progress">
        <div class="progress-bar">
          <div
            class="progress-bar-fill"
            :style="{ width: `${progressPercent}%` }"
          ></div>
        </div>
        <span class="progress-text">
          {{ progress.total === 0 ? '진행 중' : `${progress.completed}/${progress.total}` }}
        </span>
      </div>

      <!-- Footer -->
      <div class="card-footer">
        <span class="card-time"><TimeAgo :date="project.updatedAt" /></span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.project-card {
  display: block;
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 16px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s ease;
  position: relative;
}

.project-card:hover {
  border-color: var(--rose-200);
  box-shadow: 0 8px 24px -4px rgba(255, 133, 161, 0.12);
  transform: translateY(-2px);
}

.project-card--flash {
  border-color: var(--rose-400);
  box-shadow:
    0 0 0 3px rgba(255, 133, 161, 0.2),
    0 12px 30px -6px rgba(255, 133, 161, 0.35);
  animation: project-flash 1.2s ease;
}

.project-card--list {
  display: flex;
  align-items: stretch;
  gap: 1rem;
  padding: 1rem;
}

/* Thumbnail */
.card-thumbnail {
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
}

.project-card--list .card-thumbnail {
  flex: 0 0 160px;
  aspect-ratio: auto;
  width: 160px;
  height: 96px;
  border-radius: 12px;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.thumbnail-video.is-visible {
  opacity: 1;
}

.thumbnail-placeholder {
  width: 100%;
  height: 100%;
  background: var(--rose-50);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.5rem;
  color: var(--gray-400);
  font-size: 0.875rem;
}

.preview-icon {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

/* Content */
.card-content {
  padding: 1rem;
}

.project-card--list .card-content {
  flex: 1;
  padding: 0.25rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.project-card--list .card-header {
  margin-bottom: 0;
}

.card-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.card-description {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0 0 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card--list .card-description {
  margin: 0;
}

/* Progress */
.card-progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.project-card--list .card-progress {
  margin-bottom: 0;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: var(--gray-100);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
}

/* Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.project-card--list .card-footer {
  margin-top: auto;
}

.card-time {
  font-size: 0.75rem;
  color: var(--gray-500);
}

/* Favorite */
.star {
  --star-color: #FF5B89;
  display: inline-flex;
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  width: 28px;
  height: 28px;
  cursor: pointer;
  z-index: 10;
  transition: transform 0.3s ease;
}

.star .checkbox {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  z-index: 20;
  cursor: pointer;
}

.star .svg-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.star .svg-outline,
.star .svg-filled {
  fill: var(--star-color);
  position: absolute;
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
}

.star .svg-outline {
  z-index: 2;
}

.star .svg-filled {
  z-index: 1;
  display: none;
  animation: keyframes-svg-filled 2s;
}

.star .svg-celebrate {
  position: absolute;
  animation: keyframes-svg-celebrate 0.5s;
  animation-fill-mode: forwards;
  width: 100%;
  height: 100%;
  display: none;
  stroke: var(--star-color);
  fill: var(--star-color);
  stroke-width: 2px;
  z-index: 3;
  pointer-events: none;
}

.star .checkbox:checked ~ .svg-container .svg-filled {
  display: block;
}

.star .checkbox:checked ~ .svg-container .svg-celebrate {
  display: block;
}

.star:hover {
  transform: scale(1.1);
}

@keyframes keyframes-svg-filled {
  0% {
    transform: scale(0);
  }
  25% {
    transform: scale(1.2);
  }
  50% {
    transform: scale(1);
    filter: brightness(1.5);
  }
}

@keyframes keyframes-svg-celebrate {
  0% {
    transform: scale(0);
  }
  50% {
    opacity: 1;
    filter: brightness(1.5);
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
    display: none;
  }
}

@keyframes project-flash {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 133, 161, 0.35);
  }
  50% {
    box-shadow:
      0 0 0 6px rgba(255, 133, 161, 0.25),
      0 12px 30px -6px rgba(255, 133, 161, 0.35);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 133, 161, 0.2);
  }
}

/* More Menu */
.more-menu-container {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  z-index: 20;
}

.project-card--list .more-menu-container {
  top: 0.75rem;
  right: 0.75rem;
}

.more-btn {
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rose-400); 
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px; /* Optional slight radius for hover effect */
}

.more-btn:hover {
  background: var(--rose-50);
  color: var(--rose-600);
  box-shadow: 0 2px 8px rgba(255, 133, 161, 0.2);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0.25rem;
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 140px;
  padding: 0.5rem;
  overflow: hidden;
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: none;
  background: transparent;
  color: var(--gray-700);
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.menu-item:hover {
  background: var(--rose-50);
  color: var(--gray-900);
}


.menu-item.delete {
  color: #ef4444;
}

.menu-item.delete:hover {
  background: #fef2f2;
  color: #dc2626;
}
</style>


