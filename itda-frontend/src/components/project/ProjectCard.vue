<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { Star, MoreVertical, Trash2, Pencil, Share2 } from 'lucide-vue-next'
import type { Project } from '../../types/api/projects'
import Badge from '../common/Badge.vue'
import AvatarGroup from '../common/AvatarGroup.vue'
import TimeAgo from '../common/TimeAgo.vue'
import { getProjectProgress } from '../../services/mock/projects'

interface Props {
  project: Project
  isFavorite?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isFavorite: false,
})

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
    class="project-card"
  >
    <!-- Favorite Icon (Top-Left) -->
    <Star
      v-if="project"
      class="favorite-icon"
      :fill="isFavorite ? 'currentColor' : 'none'"
      :class="{ active: isFavorite }"
      @click.prevent.stop="$emit('toggle-favorite', project.projectId)"
    />

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

/* Thumbnail */
.card-thumbnail {
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
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
  color: var(--gray-400);
  font-size: 0.875rem;
}

/* Content */
.card-content {
  padding: 1rem;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
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

/* Progress */
.card-progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
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

.card-time {
  font-size: 0.75rem;
  color: var(--gray-500);
}

/* Favorite */
.favorite-icon {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  width: 20px;
  height: 20px;
  color: var(--rose-400);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
  cursor: pointer;
  z-index: 10;
}

.favorite-icon:hover {
  transform: scale(1.1);
}

/* More Menu */
.more-menu-container {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  z-index: 20;
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
