<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { MoreVertical, Trash2, Pencil, Share2 } from 'lucide-vue-next'
import type { Project } from '../../types/api/projects'
import Badge from '../common/Badge.vue'
import AvatarGroup from '../common/AvatarGroup.vue'
import TimeAgo from '../common/TimeAgo.vue'
import { getProjectProgress } from '../../services/mock/projects'
import { useProjectStore } from '../../stores/project'

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

const handleCardClick = () => {
  projectStore.touchProject(props.project.projectId)
}

const isMenuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const progress = computed(() => getProjectProgress(props.project.projectId))

const progressPercent = computed(() => {
  if (progress.value.total === 0) return 0
  return Math.round((progress.value.completed / progress.value.total) * 100)
})


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

// Mock member avatars based on member count
const memberAvatars = computed(() => {
  const avatars = []
  for (let i = 0; i < Math.min(props.project.memberCount, 3); i++) {
    avatars.push({
      src: `https://i.pravatar.cc/150?u=${props.project.projectId}-${i}`,
      alt: `Member ${i + 1}`,
    })
  }
  return avatars
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
    :class="['project-card', { 'project-card--list': viewMode === 'list' }]"
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
        >
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
          <circle r="2" cy="50" cx="50" class="particle"></circle>
        </svg>
      </div>
    </label>

    <!-- More Menu (Top-Right) -->
    <div class="more-menu-container" ref="menuRef">
      <button class="more-btn" @click="toggleMenu">
        <MoreVertical class="icon-sm" />
      </button>
      <div v-if="isMenuOpen" class="dropdown-menu">
        <button class="menu-item" @click="closeMenu">
          <Pencil class="icon-sm" />
          수정
        </button>
        <button class="menu-item" @click="closeMenu">
          <Share2 class="icon-sm" />
          공유
        </button>
        <div class="menu-divider"></div>
        <button class="menu-item delete" @click="handleDeleteRequest">
          <Trash2 class="icon-sm" />
          삭제
        </button>
      </div>
    </div>

    <!-- Thumbnail -->
    <div class="card-thumbnail">
      <img
        v-if="project.thumbnailUrl"
        :src="project.thumbnailUrl"
        :alt="project.title"
        class="thumbnail-image"
      />
      <div v-else class="thumbnail-placeholder">
        <img src="/icon.png" alt="No Preview" class="preview-icon" />
        <span>No Preview</span>
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
          {{ progress.total === 0 ? '진행 전' : `${progress.completed}/${progress.total}` }}
        </span>
      </div>

      <!-- Footer -->
      <div class="card-footer">
        <AvatarGroup :avatars="memberAvatars" :max="2" size="sm" />
        <span class="card-time">Edited <TimeAgo :date="project.updatedAt" /></span>
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
  --star-color: #FFDAF6;
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

.star .svg-filled {
  opacity: 0;
  transform: scale(0);
}

.star .svg-celebrate {
  position: absolute;
  width: 100%;
  height: 100%;
  display: none;
  stroke: var(--star-color);
  fill: var(--star-color);
  stroke-width: 2px;
}

.star .particle {
  position: absolute;
  animation-fill-mode: forwards;
  display: none;
}

.star .checkbox:checked ~ .svg-container .svg-outline {
  opacity: 0;
}

.star .checkbox:checked ~ .svg-container .svg-filled {
  opacity: 1;
  transform: scale(1);
  animation: keyframes-svg-filled 0.9s;
}

.star .checkbox:not(:checked) ~ .svg-container .svg-filled {
  animation: keyframes-svg-unfilled 0.3s forwards;
}

.star .checkbox:checked ~ .svg-container .svg-celebrate {
  display: block;
}

.star .checkbox:checked ~ .svg-container .particle {
  display: block;
}

.star:hover {
  transform: scale(1.1);
}

.star .particle:nth-child(1) {
  animation: particle-1 1s cubic-bezier(0.25, 0.1, 0.25, 1);
}
.star .particle:nth-child(2) {
  animation: particle-2 1s ease-out;
}
.star .particle:nth-child(3) {
  animation: particle-3 1s ease-out;
}
.star .particle:nth-child(4) {
  animation: particle-4 1s ease-out;
}
.star .particle:nth-child(5) {
  animation: particle-5 1s ease-out;
}
.star .particle:nth-child(6) {
  animation: particle-6 1s ease-out;
}
.star .particle:nth-child(7) {
  animation: particle-7 1s ease-out;
}
.star .particle:nth-child(8) {
  animation: particle-8 1s ease-out;
}

@keyframes keyframes-svg-filled {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  25% {
    transform: scale(1.2);
    opacity: 1;
  }
  50% {
    transform: scale(1);
    filter: brightness(1.5);
  }
}

@keyframes keyframes-svg-unfilled {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(0);
    opacity: 0;
  }
}

@keyframes particle-1 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  40% {
    transform: translate(-9px, -12px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(-18px, 18px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-2 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  40% {
    transform: translate(9px, -12px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(18px, 18px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-3 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  40% {
    transform: translate(-13px, -9px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(-21px, 20px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-4 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  40% {
    transform: translate(13px, -9px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(21px, 20px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-5 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  45% {
    transform: translate(0, -13px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(0, 18px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-6 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  35% {
    transform: translate(-15px, -7px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(-26px, 22px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-7 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  35% {
    transform: translate(15px, -7px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(26px, 22px) scale(0);
    opacity: 0;
  }
}

@keyframes particle-8 {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  45% {
    transform: translate(0, -16px) scale(0.6);
    opacity: 0.6;
  }
  100% {
    transform: translate(0, 20px) scale(0);
    opacity: 0;
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
  color: var(--red-500);
}

.menu-item.delete:hover {
  background: var(--red-50);
}

.menu-divider {
  height: 1px;
  background: var(--gray-100);
  margin: 0.25rem 0;
}
</style>
