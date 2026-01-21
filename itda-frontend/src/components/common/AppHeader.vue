<script setup lang="ts">
import { ref } from 'vue'
import { Search, Share2, Users } from 'lucide-vue-next'
import AvatarGroup from './AvatarGroup.vue'

interface Props {
  showCollaborators?: boolean
  showShareButton?: boolean
  showCollabButton?: boolean
}

withDefaults(defineProps<Props>(), {
  showCollaborators: true,
  showShareButton: true,
  showCollabButton: true,
})

const emit = defineEmits<{
  (e: 'search', query: string): void
  (e: 'share'): void
  (e: 'startCollab'): void
}>()

const searchQuery = ref('')

const collaborators = [
  { src: '', fallback: 'MK' },
  { src: '', fallback: 'SJ' },
  { src: '', fallback: 'YH' },
  { src: '', fallback: 'JW' },
  { src: '', fallback: 'EJ' },
]

const handleSearch = () => {
  emit('search', searchQuery.value)
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <!-- Collaborator Avatars -->
      <AvatarGroup
        v-if="showCollaborators"
        :avatars="collaborators"
        :max="3"
        size="sm"
      />

      <div v-if="showCollaborators && showShareButton" class="divider"></div>

      <button v-if="showShareButton" class="btn btn-ghost" @click="emit('share')">
        <Share2 class="icon-sm" />
        <span>프로젝트 공유</span>
      </button>
    </div>

    <div class="header-center">
      <div class="search-wrapper">
        <Search class="search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          class="header-search"
          placeholder="씬, 프롬프트, 에셋 검색..."
          @keyup.enter="handleSearch"
        />
      </div>
    </div>

    <div class="header-right">
      <slot name="actions" />
      <button v-if="showCollabButton" class="btn btn-primary" @click="emit('startCollab')">
        <Users class="icon-sm" />
        <span>실시간 협업 시작</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 64px;
  padding: 0 1.5rem;
  background: white;
  border-bottom: 1px solid var(--rose-100);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-shrink: 0;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-center {
  flex: 1;
  max-width: 400px;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--rose-200);
}

/* Search */
.search-wrapper {
  position: relative;
  width: 100%;
}

.search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: var(--gray-400);
}

.header-search {
  width: 100%;
  padding: 0.625rem 1rem 0.625rem 2.5rem;
  background: var(--rose-50);
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-700);
  transition: all 0.2s ease;
}

.header-search::placeholder {
  color: var(--gray-400);
}

.header-search:focus {
  outline: none;
  background: white;
  border-color: var(--rose-200);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

/* Icons */
.icon-sm {
  width: 16px;
  height: 16px;
}
</style>
