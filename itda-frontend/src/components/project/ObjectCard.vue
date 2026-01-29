<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { ObjectSheet } from '../../types/api/objects'
import { OBJECT_TYPE_LABELS } from '../../constants/objects'
import { resolveApiUrl } from '../../services/api/urls'
import { fetchProtectedBlobUrl } from '../../services/api/media'
import Button from '../common/Button.vue'
import Card from '../common/Card.vue'
import { Download, MoreVertical, Pencil, Trash2 } from 'lucide-vue-next'

interface Props {
  object: ObjectSheet
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'edit', object: ObjectSheet): void
  (e: 'delete', object: ObjectSheet): void
  (e: 'download', object: ObjectSheet): void
}>()

const typeLabelMap = OBJECT_TYPE_LABELS
const resolvedSheetImageUrl = computed(() => resolveApiUrl(props.object.sheetImageUrl) ?? null)
const objectImageUrl = ref<string | null>(null)
let imageRequestId = 0

const isDirectUrl = (url: string) => url.startsWith('data:') || url.startsWith('blob:')

const revokeObjectUrl = (url: string | null) => {
  if (url && url.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

const loadObjectImage = async (url: string | null) => {
  const requestId = ++imageRequestId
  revokeObjectUrl(objectImageUrl.value)
  objectImageUrl.value = null
  if (!url) return
  if (isDirectUrl(url)) {
    objectImageUrl.value = url
    return
  }
  try {
    const blobUrl = await fetchProtectedBlobUrl(url)
    if (requestId !== imageRequestId) {
      revokeObjectUrl(blobUrl)
      return
    }
    objectImageUrl.value = blobUrl
  } catch (error) {
    console.error('Failed to load object image:', error)
  }
}

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

const requestEdit = () => {
  showMenu.value = false
  emit('edit', props.object)
}

const requestDownload = () => {
  showMenu.value = false
  emit('download', props.object)
}

const requestDelete = () => {
  showMenu.value = false
  emit('delete', props.object)
}

onMounted(() => {
  document.addEventListener('click', closeMenu)
})

watch(resolvedSheetImageUrl, (nextUrl) => {
  void loadObjectImage(nextUrl)
}, { immediate: true })

onUnmounted(() => {
  document.removeEventListener('click', closeMenu)
  revokeObjectUrl(objectImageUrl.value)
})
</script>

<template>
  <Card class="object-card">
    <div class="object-menu" ref="menuRef">
      <Button
        variant="ghost"
        size="sm"
        icon
        type="button"
        class="object-menu-trigger"
        aria-label="오브젝트 메뉴"
        @click.stop="toggleMenu"
      >
        <MoreVertical class="icon-sm" />
      </Button>
      <transition name="fade">
        <div v-if="showMenu" class="object-menu-dropdown">
          <button
            type="button"
            class="object-menu-item"
            @click.stop="requestEdit"
          >
            <Pencil class="icon-sm" />
            수정
          </button>
          <button
            type="button"
            class="object-menu-item"
            @click.stop="requestDownload"
          >
            <Download class="icon-sm" />
            다운로드
          </button>
          <div class="object-menu-divider"></div>
          <button
            type="button"
            class="object-menu-item text-danger"
            @click.stop="requestDelete"
          >
            <Trash2 class="icon-sm" />
            삭제
          </button>
        </div>
      </transition>
    </div>

    <div class="object-avatar">
      <img
        v-if="objectImageUrl"
        :src="objectImageUrl"
        :alt="object.name"
        class="avatar-image"
      />
      <span v-else class="avatar-placeholder">
        {{ object.name.charAt(0).toUpperCase() }}
      </span>
    </div>

    <div class="object-meta">
      <span class="object-type">{{ typeLabelMap[object.type] || object.type }}</span>
    </div>

    <h4 class="object-name">{{ object.name }}</h4>
    <p class="object-description">{{ object.description }}</p>
  </Card>
</template>

<style scoped>
.object-card {
  position: relative;
  text-align: center;
  padding: 1.5rem;
}

.object-avatar {
  width: 80px;
  height: 80px;
  margin: 0 auto 0.75rem;
  border-radius: 50%;
  overflow: hidden;
  background: var(--rose-100);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--rose-500);
}

.object-meta {
  display: flex;
  justify-content: center;
  margin-bottom: 0.35rem;
}

.object-type {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: var(--rose-50);
  color: var(--rose-500);
}

.object-name {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--gray-900);
  margin: 0 0 0.25rem;
}

.object-description {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0 0 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  min-height: 2.8em;
}

.object-menu {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  display: inline-flex;
}

.object-menu-trigger {
  box-shadow: none;
}

.object-menu-dropdown {
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

.object-menu-item {
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

.object-menu-item:hover {
  background: var(--rose-50);
}

.object-menu-item.text-danger {
  color: var(--error);
}

.object-menu-item.text-danger:hover {
  background: rgba(239, 68, 68, 0.1);
}

.object-menu-divider {
  height: 1px;
  background: var(--gray-100);
  margin: 0.25rem 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
