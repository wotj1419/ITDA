<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Star } from 'lucide-vue-next'
import type { Project } from '../../types'
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

defineEmits<{
  (e: 'toggle-favorite', projectId: number): void
}>()
</script>

<template>
  <RouterLink
    :to="`/projects/${project.projectId}`"
    class="project-card"
  >
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
        <span class="progress-text">{{ progress.completed }}/{{ progress.total }}</span>
      </div>

      <!-- Footer -->
      <div class="card-footer">
        <AvatarGroup :avatars="memberAvatars" :max="2" size="sm" />
        <span class="card-time">Edited <TimeAgo :date="project.updatedAt" /></span>
      </div>
    </div>

    <!-- Favorite Icon -->
    <Star
      v-if="project"
      class="favorite-icon"
      :fill="isFavorite ? 'currentColor' : 'none'"
      :class="{ active: isFavorite }"
      @click.prevent.stop="$emit('toggle-favorite', project.projectId)"
    />
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
  background: var(--rose-100);
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
/* Favorite */
.favorite-icon {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
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
</style>
