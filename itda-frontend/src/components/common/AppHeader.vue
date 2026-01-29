<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useProjectStore } from '../../stores/project'
import type { Project } from '../../types/api/projects'

interface Props {
  showCollaborators?: boolean
}

withDefaults(defineProps<Props>(), {
  showCollaborators: true,
})

const emit = defineEmits<{
  (e: 'search', query: string): void
}>()

const route = useRoute()
const projectStore = useProjectStore()
const searchQuery = ref('')
const searchWrapperRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)
const isResultsOpen = ref(false)

const normalizedQuery = computed(() => searchQuery.value.trim().toLowerCase())
const filteredProjects = computed<Project[]>(() => {
  if (!normalizedQuery.value) return []
  return projectStore.sortedProjects
    .filter((project) => project.title?.toLowerCase().includes(normalizedQuery.value))
    .slice(0, 6)
})

const showResults = computed(() => {
  return route.name === 'dashboard' && isResultsOpen.value && filteredProjects.value.length > 0
})

const openResults = () => {
  if (route.name !== 'dashboard') return
  isResultsOpen.value = normalizedQuery.value.length > 0
}

const closeResults = () => {
  isResultsOpen.value = false
  searchQuery.value = ''
}

const handleSearchInput = () => {
  openResults()
}

const findBestMatch = () => {
  if (!normalizedQuery.value) return null
  const exactMatch = filteredProjects.value.find(
    (project) => project.title?.toLowerCase() === normalizedQuery.value
  )
  return exactMatch ?? filteredProjects.value[0] ?? null
}

const performSearch = () => {
  if (route.name !== 'dashboard') return
  const match = findBestMatch()
  if (match) {
    handleSelectProject(match)
    return
  }
  openResults()
}

const scrollToProjectCard = async (projectId: number) => {
  await nextTick()
  const target = document.querySelector(`[data-project-id="${projectId}"]`) as HTMLElement | null
  if (!target) return
  const scrollContainer = target.closest('.main-content') as HTMLElement | null
  if (scrollContainer) {
    const containerRect = scrollContainer.getBoundingClientRect()
    const targetRect = target.getBoundingClientRect()
    const offset =
      targetRect.top - containerRect.top + scrollContainer.scrollTop - containerRect.height / 2 + targetRect.height / 2
    scrollContainer.scrollTo({ top: offset, behavior: 'smooth' })
    return
  }
  target.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const handleSelectProject = (project: Project) => {
  searchQuery.value = project.title
  closeResults()
  projectStore.highlightProject(project.projectId)
  scrollToProjectCard(project.projectId)
}

const handleSearch = () => {
  emit('search', searchQuery.value)
  performSearch()
  if (searchInputRef.value) {
    searchInputRef.value.focus()
  }
}

const handleClickOutside = (event: MouseEvent) => {
  if (!searchWrapperRef.value) return
  if (!searchWrapperRef.value.contains(event.target as Node)) {
    closeResults()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
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

      <div v-if="$slots['left-after-divider']" class="header-left-extra">
        <slot name="left-after-divider" />
      </div>
    </div>

    <div class="header-center">
      <div class="input-wrapper" ref="searchWrapperRef">
        <button class="icon" type="button" @click="handleSearch" aria-label="Search">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            height="25px"
            width="25px"
          >
            <path
              stroke-linejoin="round"
              stroke-linecap="round"
              stroke-width="3"
              stroke="#fff"
              d="M11.5 21C16.7467 21 21 16.7467 21 11.5C21 6.25329 16.7467 2 11.5 2C6.25329 2 2 6.25329 2 11.5C2 16.7467 6.25329 21 11.5 21Z"
            ></path>
            <path
              stroke-linejoin="round"
              stroke-linecap="round"
              stroke-width="3"
              stroke="#fff"
              d="M22 22L20 20"
            ></path>
          </svg>
        </button>
        <input
          v-model="searchQuery"
          class="input"
          name="text"
          type="text"
          placeholder="프로젝트 검색"
          ref="searchInputRef"
          @focus="openResults"
          @input="handleSearchInput"
          @keyup.enter="handleSearch"
        />
        <div v-if="showResults" class="search-results">
          <button
            v-for="project in filteredProjects"
            :key="project.projectId"
            class="search-result-item"
            type="button"
            @click.prevent="handleSelectProject(project)"
          >
            <span class="result-title">{{ project.title }}</span>
            <span v-if="project.genre" class="result-meta">{{ project.genre }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="header-actions">
      <slot name="actions" />
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
  justify-content: flex-start;
  gap: 1rem;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-left-extra {
  display: flex;
  align-items: center;
  min-width: 0;
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
  color : var(--rose-600)
}

/* Header Layout Refinement */
.header-center {
  flex: 0 1 400px; /* Grow 0 to prevent bounce, Shrink 1, Basis 400px */
  margin-left: auto;
  margin-right: 0.5rem;
  min-width: 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--rose-200);
}

/* Search */
.input-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  position: relative;
  --search-size: 30px;
}

.input {
  border-style: none;
  height: var(--search-size);
  width: var(--search-size);
  padding: 0;
  box-sizing: border-box;
  outline: none;
  border-radius: 50%;
  aspect-ratio: 1 / 1;
  min-width: var(--search-size);
  min-height: var(--search-size);
  transition: 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
  background-color: var(--rose-500);
  box-shadow: 0 0 6px var(--rose-100);
  padding-right: 0;
  color: #fff;
}

.input::placeholder,
.input {
  font-size: 17px;
}

.input::placeholder {
  color: transparent;
}

.input:focus::placeholder {
  color: var(--gray-400);
}

.icon {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  right: 0px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  width: var(--search-size);
  height: var(--search-size);
  outline: none;
  border-style: none;
  border-radius: 50%;
  pointer-events: painted;
  background-color: transparent;
  transition: 0.2s linear;
}

.icon svg {
  width: 15px;
  height: 15px;
}

.icon:focus ~ .input,
.input:focus {
  box-shadow: none;
  width: 250px;
  border-radius: 0px;
  background-color: transparent;
  border-bottom: 3px solid var(--rose-500);
  color: var(--gray-700);
  caret-color: var(--gray-700);
  padding: 0 10px;
  transition: all 500ms cubic-bezier(0.22, 0.61, 0.36, 1);
}

.search-results {
  position: absolute;
  top: calc(100% + 0.35rem);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
  padding: 0.25rem;
  z-index: 40;
  max-height: 260px;
  overflow-y: auto;
}

.search-result-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-800);
  transition: background 0.2s ease, color 0.2s ease;
}

.search-result-item:hover {
  background: var(--rose-50);
  color: var(--gray-900);
}

.result-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-meta {
  flex-shrink: 0;
  font-size: 0.75rem;
  color: var(--gray-400);
  background: var(--rose-50);
  border-radius: 999px;
  padding: 0.125rem 0.5rem;
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

  .input {
    min-width: 0;
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
