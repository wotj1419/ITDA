<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { GripVertical, Image, Pencil, Check, Loader2, MoreVertical, Trash2 } from 'lucide-vue-next'
import type { Scene, SceneStatus } from '../../types/api/scenes'
import Badge from '../common/Badge.vue'
import Button from '../common/Button.vue'

interface Props {
  scene: Scene
  projectId: number
  draggable?: boolean
  showThumbnail?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  draggable: true,
  showThumbnail: true,
})

const emit = defineEmits<{
  (e: 'delete', scene: Scene): void
}>()

const statusConfig = computed(() => {
  const configs: Record<SceneStatus, { label: string; variant: 'success' | 'info' | 'default'; showIcon: boolean }> = {
    COMPLETED: { label: '완료', variant: 'success', showIcon: true },
    IN_PROGRESS: { label: '진행 중', variant: 'info', showIcon: true },
    DRAFT: { label: '초안', variant: 'default', showIcon: false },
  }
  return configs[props.scene?.status] ?? configs.DRAFT
})

const showMenu = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

const closeMenu = (event: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    showMenu.value = false
  }
}

const requestDelete = () => {
  showMenu.value = false
  emit('delete', props.scene)
}

onMounted(() => {
  document.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenu)
})

const editLink = computed(() => ({
  name: 'scene-edit',
  params: {
    projectId: props.projectId,
    sceneId: props.scene.sceneId,
  },
}))
</script>

<template>
  <div
    class="scene-card"
    :class="{
      'scene-card-draggable': draggable,
      'scene-card-compact': !showThumbnail,
    }"
  >
    <div class="scene-card-content">
      <!-- Drag Handle -->
      <div v-if="draggable" class="drag-handle" draggable="true" aria-label="씬 순서 이동">
        <GripVertical class="drag-handle-icon" />
      </div>

      <!-- Thumbnail -->
      <div v-if="showThumbnail" class="scene-thumbnail">
        <img
          v-if="scene.thumbnailUrl"
          :src="scene.thumbnailUrl"
          :alt="scene.title"
          class="thumbnail-image"
        />
        <div v-else class="thumbnail-placeholder">
          <Image class="thumbnail-icon" />
        </div>
      </div>

      <!-- Info -->
      <div class="scene-info">
        <div class="scene-header">
          <Badge variant="default" size="sm">씬 {{ scene.order }}</Badge>
          <h4 class="scene-title">{{ scene.title }}</h4>
          <Badge :variant="statusConfig.variant" size="sm" class="status-badge">
            <Check v-if="statusConfig.showIcon && scene.status === 'COMPLETED'" class="status-icon" />
            <Loader2 v-if="statusConfig.showIcon && scene.status === 'IN_PROGRESS'" class="status-icon spin" />
            {{ statusConfig.label }}
          </Badge>
        </div>
        <p v-if="scene.description" class="scene-description">{{ scene.description }}</p>
      </div>

      <!-- Actions -->
      <div class="scene-actions">
        <slot name="actions-left" />
        <RouterLink :to="editLink" custom v-slot="{ navigate }">
          <Button
            :variant="scene.status === 'IN_PROGRESS' ? 'primary' : 'secondary'"
            size="sm"
            @click="navigate"
          >
            <Pencil class="icon-sm" />
            편집
          </Button>
        </RouterLink>
        <div class="scene-menu" ref="menuRef">
          <Button
            variant="ghost"
            size="sm"
            icon
            type="button"
            class="scene-menu-trigger"
            aria-label="씬 메뉴"
            @click.stop="toggleMenu"
          >
            <MoreVertical class="icon-sm" />
          </Button>
          <transition name="fade">
            <div v-if="showMenu" class="scene-menu-dropdown">
              <button
                type="button"
                class="scene-menu-item text-danger"
                @click.stop="requestDelete"
              >
                <Trash2 class="icon-sm" />
                삭제
              </button>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <slot name="extra-inside" />
  </div>

  <slot name="extra-content" />
</template>

<style scoped>
.scene-card {
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 12px;
  padding: 1rem;
  transition: all 0.2s ease;
}

.scene-card-draggable {
  cursor: default;
}

.scene-card-draggable:active {
  cursor: grabbing;
}

.scene-card:hover {
  border-color: var(--rose-200);
  box-shadow: 0 4px 12px rgba(255, 133, 161, 0.08);
}

.scene-card-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.scene-card-compact .scene-card-content {
  gap: 0.75rem;
}

.drag-handle {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  flex-shrink: 0;
}

.drag-handle-icon {
  width: 16px;
  height: 16px;
  color: var(--gray-300);
}

.scene-thumbnail {
  width: 80px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
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
}

.thumbnail-icon {
  width: 20px;
  height: 20px;
  color: var(--rose-300);
}

.scene-info {
  flex: 1;
  min-width: 0;
}

.scene-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.scene-card-compact .scene-header {
  flex-wrap: wrap;
}

.scene-title {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--gray-900);
  margin: 0;
}

.status-badge {
  margin-left: 0.5rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.status-icon {
  width: 12px;
  height: 12px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.scene-description {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-card-compact .scene-description {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.scene-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.scene-menu {
  position: relative;
  display: inline-flex;
}

.scene-menu-trigger {
  box-shadow: none;
}

.scene-menu-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  min-width: 140px;
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 10px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  padding: 0.35rem;
  z-index: 20;
}

.scene-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.625rem;
  border: none;
  background: transparent;
  color: var(--gray-700);
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 600;
}

.scene-menu-item:hover {
  background: var(--rose-50);
}

.scene-menu-item.text-danger {
  color: var(--error);
}

/* Uses global .icon-sm from base.css */

/* Drag states */
.scene-card.dragging {
  opacity: 0.6;
  transform: scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}
</style>
