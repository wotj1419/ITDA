<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
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
      <!-- Logo -->
      <RouterLink to="/dashboard" class="header-logo">
        <img src="/icon.png" alt="Logo" class="logo-icon" />
        <span class="logo-text">잇다</span>
      </RouterLink>

      <div class="divider"></div>

      <button v-if="showShareButton" class="btn btn-ghost" @click="emit('share')">
        <Share2 class="icon-sm" />
        <span>프로젝트 공유</span>
      </button>
    </div>

    <div class="header-center">
      <!-- Collaborator Avatars -->
      <AvatarGroup
        v-if="showCollaborators"
        :avatars="collaborators"
        :max="3"
        size="sm"
      />
      <div v-if="showCollaborators" class="divider"></div>
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

/* Logo */
.header-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  color: var(--gray-900);
  margin-right: 0.5rem;
}

.logo-icon {
  margin-top: -2px;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: contain; /* 투명 배경이면 contain 추천 */
  background: transparent;
  flex-shrink: 0;
}


.logo-text {
  font-weight: 700;
  font-size: 1rem;
  white-space: nowrap;
}

/* Header Layout Refinement */
.header-center {
  flex: 0 1 400px; /* Grow 0 to prevent bounce, Shrink 1, Basis 400px */
  margin: 0 auto;
  min-width: 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--rose-200);
}

/* Search */
.search-wrapper {
  position: relative;
  width: 50%;
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

/* Prevent right section from being crushed */
.header-right {
  flex-shrink: 0;
}

/* Responsive Header */
@media (max-width: 1100px) {
  /* No special flex change needed if we keep base logic consistent */
}

@media (max-width: 800px) {
  /* Hide text labels delayed to 800px */
  .btn span {
    display: none;
  }
  
  .btn .icon-sm {
    margin: 0;
  }
}

@media (max-width: 768px) {
  /* Reuse mobile/tablet logic */
  .header-left .divider,
  .header-center .divider,
  .header-center :deep(.avatar-group) {
    display: none;
  }
}

@media (max-width: 640px) {
  .logo-text {
    display: none;
  }

  /* On mobile, let search take more space since other items are hidden */
  .header-center {
    max-width: none; /* Uncap width on very small screens */
    margin: 0 0.5rem;
  }
  
  .header-search {
    padding: 0.5rem 0.5rem 0.5rem 2rem;
    min-width: 0;
  }
  
  .search-wrapper {
    width: 100%;
  }

  .header {
    padding: 0 0.75rem;
    gap: 0.5rem;
  }
}

/* Icons */
.icon-sm {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* Ensure buttons don't wrap text weirdly before disappearing */
.btn {
  white-space: nowrap;
}
</style>
