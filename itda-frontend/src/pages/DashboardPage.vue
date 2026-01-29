<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'
import { useAuthStore } from '../stores/auth'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import ProjectCard from '../components/project/ProjectCard.vue'
import NewProjectModal from '../components/project/NewProjectModal.vue'
import StartCollabModal from '../components/project/StartCollabModal.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'

const router = useRouter()
const projectStore = useProjectStore()
const uiStore = useUIStore()
const collabStore = useCollabStore()
const authStore = useAuthStore()
const isCreatingProject = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const unreadNotifications = ref(0)
const isNotificationOpen = ref(false)
const notificationRef = ref<HTMLElement | null>(null)
const greetingText = '\ubc18\uac00\uc6cc\uc694'
const greetingEmoji = '\u2728'
const greetingSuffix = '\uB2D8'
const notificationTitle = '\uc54c\ub9bc'
const notificationFilterLabel = '\uc77d\uc9c0 \uc54a\uc740 \ud56d\ubaa9\ub9cc \ud45c\uc2dc'
const notificationEmptyTitle = '\uc54c\ub9bc\uc774 \uc5c6\uc2b5\ub2c8\ub2e4'
const notificationEmptyMeta = '\uc0c8 \uc54c\ub9bc\uc774 \uc624\uba74 \uc5ec\uae30\uc5d0 \ud45c\uc2dc\ub429\ub2c8\ub2e4.'
const notificationAvatar = '\ud83d\ude42'
const notificationCloseSymbol = '\u00d7'

// Delete Confirmation State
const showDeleteModal = ref(false)
const projectToDelete = ref<{ projectId: number; title: string } | null>(null)

// Load projects on mount
onMounted(async () => {
  await projectStore.loadProjects()
  document.addEventListener('click', handleNotificationClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleNotificationClickOutside)
})

// Quick access projects removed


// Check if project is favorite
const isFavorite = (projectId: number) => {
  return projectStore.favoriteProjects.some((p) => p.projectId === projectId)
}

const handleToggleFavorite = (projectId: number) => {
  projectStore.toggleFavorite(projectId)
}

const createEmptyProject = async () => {
  if (isCreatingProject.value) return
  isCreatingProject.value = true
  const newProject = await projectStore.addProject({
    title: '새 프로젝트',
    description: '',
    genre: '',
  })
  isCreatingProject.value = false

  if (newProject) {
    router.push({ name: 'project-detail', params: { id: newProject.projectId } })
    return
  }

  uiStore.showToast({
    type: 'error',
    title: '프로젝트 생성 실패',
    message: '잠시 후 다시 시도해주세요.',
  })
}

const toggleNotifications = () => {
  isNotificationOpen.value = !isNotificationOpen.value
}

const closeNotifications = () => {
  isNotificationOpen.value = false
}

const handleNotificationClickOutside = (e: MouseEvent) => {
  if (notificationRef.value && !notificationRef.value.contains(e.target as Node)) {
    closeNotifications()
  }
}

const openStartCollabModal = () => {
  uiStore.openModal('start-collab')
}

const handleStartCollab = async (projectId: number) => {
  await collabStore.joinRoom(projectId)
  collabStore.showFloatingBar(true)
}

// Delete Handlers
const handleRequestDelete = (projectId: number) => {
  const project = projectStore.projects.find(p => p.projectId === projectId)
  if (project) {
    projectToDelete.value = { projectId, title: project.title }
    showDeleteModal.value = true
  }
}

const confirmDelete = async () => {
  if (projectToDelete.value) {
    await projectStore.moveToTrash(projectToDelete.value.projectId)
    // No toast message as requested by user
    showDeleteModal.value = false
    projectToDelete.value = null
  }
}

const cancelDelete = () => {
  showDeleteModal.value = false
  projectToDelete.value = null
}

</script>

<template>
  <DefaultLayout
    :show-collaborators="false"
    @start-collab="openStartCollabModal"
  >
    <template #header-left-after-divider>
      <div class="header-greeting">
        <h2 class="welcome-title">
          {{ greetingText }}<span v-if="authStore.user?.name">, {{ authStore.user.name }}{{ greetingSuffix }}</span> {{ greetingEmoji }}
        </h2>
        <div class="notification-wrap" ref="notificationRef">
          <button
            class="notification"
            type="button"
            aria-label="Notifications"
            @click.stop="toggleNotifications"
          >
            <span class="bell-container" aria-hidden="true">
              <span class="bell"></span>
            </span>
            <span v-if="unreadNotifications > 0" class="notification-badge">
              {{ unreadNotifications }}
            </span>
          </button>
          <div v-if="isNotificationOpen" class="notification-panel">
            <div class="notification-panel__header">
              <h3>{{ notificationTitle }}</h3>
              <button class="panel-close" type="button" @click="closeNotifications">{{ notificationCloseSymbol }}</button>
            </div>
            <div class="notification-panel__filter">
              <span class="filter-label">{{ notificationFilterLabel }}</span>
              <span class="filter-pill">OFF</span>
            </div>
            <div class="notification-panel__list">
              <div class="notification-item">
                <div class="notification-avatar">{{ notificationAvatar }}</div>
                <div class="notification-content">
                  <div class="notification-title">{{ notificationEmptyTitle }}</div>
                  <div class="notification-meta">{{ notificationEmptyMeta }}</div>
                </div>
                <span class="notification-dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <template #header-actions>
      <button
        class="button"
        type="button"
        :disabled="isCreatingProject"
        @click="createEmptyProject"
      >
        <span class="button__text">새 프로젝트</span>
        <span class="button__icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            class="svg"
            aria-hidden="true"
          >
            <path d="M11 5h2v14h-2zM5 11h14v2H5z"></path>
          </svg>
        </span>
      </button>
    </template>
    <div class="dashboard-container">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="toolbar-left">
          <h1 class="page-title">내 프로젝트</h1>
          <p class="project-count">{{ projectStore.projectCount }}개의 프로젝트</p>
        </div>
        <div class="toolbar-right">
          <div class="cyber-signboard">
            <div class="cyber-switch">
              <input
                id="cyber-opt-1"
                type="radio"
                name="cyber-mode"
                value="grid"
                :checked="viewMode === 'grid'"
                @change="viewMode = 'grid'"
              />
              <label for="cyber-opt-1" class="cyber-label" aria-label="Grid view">
                <svg
                  class="icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <rect x="3" y="3" width="7" height="9"></rect>
                  <rect x="14" y="3" width="7" height="5"></rect>
                  <rect x="14" y="12" width="7" height="9"></rect>
                  <rect x="3" y="16" width="7" height="5"></rect>
                </svg>
                <span class="glare"></span>
              </label>

              <input
                id="cyber-opt-2"
                type="radio"
                name="cyber-mode"
                value="list"
                :checked="viewMode === 'list'"
                @change="viewMode = 'list'"
              />
              <label for="cyber-opt-2" class="cyber-label" aria-label="List view">
                <svg
                  class="icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="10" y1="6" x2="20" y2="6"></line>
                  <line x1="10" y1="12" x2="20" y2="12"></line>
                  <line x1="10" y1="18" x2="20" y2="18"></line>
                  <circle cx="5" cy="6" r="1.4"></circle>
                  <circle cx="5" cy="12" r="1.4"></circle>
                  <circle cx="5" cy="18" r="1.4"></circle>
                </svg>
                <span class="glare"></span>
              </label>

              <div class="cyber-highlight">
                <div class="highlight-inner"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Access Section Removed -->


      <!-- All Projects Section -->
      <section class="section">
        <h3 class="section-title">모든 프로젝트</h3>
        <div :class="['projects-grid', { 'list-view': viewMode === 'list' }]">
          <!-- Project Cards -->
          <ProjectCard
            v-for="project in projectStore.sortedProjects"
            :key="project.projectId"
            :project="project"
            :is-favorite="isFavorite(project.projectId)"
            :view-mode="viewMode"
            @toggle-favorite="handleToggleFavorite"
            @delete="handleRequestDelete"
          />

          <!-- Add New Project Card -->
          <button class="add-project-card" :disabled="isCreatingProject" @click="createEmptyProject">
            <div class="add-project-icon">
              <Plus class="icon-lg" />
            </div>
            <span class="add-project-text">새로운 프로젝트 생성</span>
          </button>
        </div>
      </section>
    </div>

    <!-- New Project Modal -->
    <NewProjectModal />
    <StartCollabModal @start="handleStartCollab" />

    <!-- Confirm Modal -->
    <ConfirmModal
      :is-open="showDeleteModal"
      title="잠깐! 휴지통으로 보낼까요? 🗑️"
      :message="`'${projectToDelete?.title}' 프로젝트를 정말 삭제하시겠어요? 🥺\n30일 동안은 보관되니까 너무 걱정 마세요!`"
      confirm-text="네, 보낼래요"
      :is-dangerous="true"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />
  </DefaultLayout>
</template>

<style scoped>
.dashboard-container {
  max-width: 1600px;
  margin: 0 auto;
}

.welcome-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-800);
  margin: 0;
  white-space: nowrap;
}

.header-greeting {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.toolbar-left {
  display: flex;
  flex-direction: column;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.cyber-signboard {
  --primary-glow: rgba(255, 133, 161, 0.9);
  --secondary-glow: rgba(255, 197, 210, 0.9);
  --inactive-color: var(--gray-400);
  --bg-dark: var(--rose-50);
  --switch-width: 84px;
  --switch-height: 42px;
  --padding: 5px;
  --item-width: calc((var(--switch-width) - (var(--padding) * 2)) / 2);

  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2px;
  font-family: inherit;
}

.cyber-switch {
  position: relative;
  width: var(--switch-width);
  height: var(--switch-height);
  background: var(--bg-dark);
  border-radius: 14px;
  box-shadow: none;
  display: flex;
  align-items: center;
  padding: var(--padding);
  box-sizing: border-box;
  overflow: hidden;
  border: 1px solid var(--rose-100);
}

.cyber-switch input[type="radio"] {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.cyber-label {
  flex: 1;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  z-index: 2;
  position: relative;
  border-radius: 14px;
  transition: all 0.3s ease;
  -webkit-tap-highlight-color: transparent;
}

.cyber-label .icon {
  width: 18px;
  height: 18px;
  color: var(--inactive-color);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 1px 2px rgba(255, 133, 161, 0.2));
}

.cyber-highlight {
  position: absolute;
  top: var(--padding);
  left: var(--padding);
  width: var(--item-width);
  height: calc(var(--switch-height) - (var(--padding) * 2));
  background: transparent;
  z-index: 1;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: none;
}

.highlight-inner {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 245, 249, 0.7) 100%
  );
  border: 1px solid rgba(255, 133, 161, 0.25);
  box-shadow: none;
  backdrop-filter: blur(4px);
  position: relative;
}

.highlight-inner::after {
  content: "";
  position: absolute;
  top: 0;
  left: 10%;
  width: 80%;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.8),
    transparent
  );
  opacity: 0.8;
}

#cyber-opt-1:checked ~ .cyber-highlight {
  transform: translateX(0%);
}

#cyber-opt-1:checked ~ [for="cyber-opt-1"] .icon {
  color: var(--rose-600);
  filter: drop-shadow(0 0 8px var(--primary-glow));
  transform: scale(1.08);
}

#cyber-opt-2:checked ~ .cyber-highlight {
  transform: translateX(100%);
}

#cyber-opt-2:checked ~ [for="cyber-opt-2"] .icon {
  color: var(--rose-600);
  filter: drop-shadow(0 0 8px var(--primary-glow));
  transform: scale(1.08);
}

.cyber-switch input:focus-visible ~ .cyber-highlight .highlight-inner {
  border: 1px solid rgba(255, 133, 161, 0.6);
  box-shadow: 0 0 20px var(--primary-glow);
}

.cyber-label:hover .icon {
  color: var(--rose-400);
}

.cyber-label:active .icon {
  transform: scale(0.95);
}

.glare {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background: radial-gradient(
    circle at 50% -20%,
    rgba(255, 255, 255, 0.35),
    transparent 60%
  );
  opacity: 0;
  transition: opacity 0.3s;
}

.cyber-label:hover .glare {
  opacity: 1;
}

@keyframes neon-pulse {
  0%,
  100% {
    box-shadow: none;
  }
  50% {
    box-shadow: none;
  }
}

.highlight-inner {
  animation: neon-pulse 3s infinite ease-in-out;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0 0 0.25rem;
}

.project-count {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0;
}

/* Sections */
.section {
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}



/* Projects Grid */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.projects-grid.list-view {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.projects-grid.list-view .add-project-card {
  min-height: 96px;
  flex-direction: row;
  justify-content: flex-start;
  gap: 1rem;
  padding: 1.25rem;
}

.projects-grid.list-view .add-project-icon {
  margin-bottom: 0;
}

@media (max-width: 640px) {
  .projects-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .projects-grid.list-view {
    grid-template-columns: 1fr;
  }
}

/* Add Project Card */
.add-project-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  background: white;
  border: 2px dashed var(--rose-200);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-project-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.add-project-card:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.add-project-icon {
  width: 64px;
  height: 64px;
  background: var(--rose-50);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  color: var(--rose-400);
}

.add-project-text {
  font-weight: 500;
  color: var(--gray-600);
}

/* Icons */
.icon-sm {
  width: 16px;
  height: 16px;
}

.icon-lg {
  width: 32px;
  height: 32px;
}

/* Notification bell */
.notification-wrap {
  position: relative;
  margin-left: -6px;
}

.notification {
  color: var(--gray-600);
  background: transparent;
  border: none;
  padding: 15px 15px;
  border-radius: 50px;
  cursor: pointer;
  transition: 300ms;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}



.notification-badge {
  color: white;
  font-size: 10px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ef4444;
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1;
}

.notification:hover {
  background: rgba(170, 170, 170, 0.062);
}

.notification:hover > .bell-container {
  animation: bell-animation 650ms ease-out 0s 1 normal both;
}

.bell-container {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell {
  border: 2.17px solid currentColor;
  border-radius: 10px 10px 0 0;
  width: 15px;
  height: 17px;
  background: transparent;
  display: block;
  position: relative;
  top: -3px;
}

.bell::before,
.bell::after {
  content: "";
  background: currentColor;
  display: block;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  height: 2.17px;
}

.bell::before {
  top: 100%;
  width: 20px;
}

.bell::after {
  top: calc(100% + 4px);
  width: 7px;
}


.notification-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  width: min(360px, 80vw);
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  z-index: 40;
  padding: 0.75rem 0;
}

.notification-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1rem 0.5rem;
  border-bottom: 1px solid var(--rose-100);
}

.notification-panel__header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--gray-900);
}

.panel-close {
  border: none;
  background: var(--rose-50);
  color: var(--gray-500);
  width: 28px;
  height: 28px;
  border-radius: 8px;
  cursor: pointer;
}

.notification-panel__filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--rose-100);
  font-size: 0.75rem;
  color: var(--gray-500);
}

.filter-pill {
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  background: var(--gray-100);
  color: var(--gray-600);
  font-weight: 600;
}

.notification-panel__list {
  max-height: 360px;
  overflow-y: auto;
  padding: 0.5rem 1rem;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--gray-100);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--rose-50);
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-900);
}

.notification-meta {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-top: 0.25rem;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--rose-400);
  margin-top: 0.35rem;
}

@keyframes bell-animation {
  20% {
    transform: rotate(15deg);
  }

  40% {
    transform: rotate(-15deg);
    scale: 1.1;
  }
  60% {
    transform: rotate(10deg);
    scale: 1.1;
  }
  80% {
    transform: rotate(-10deg);
  }
  0%,
  100% {
    transform: rotate(0deg);
  }
}

/* New Project Button Animation */
.button {
  position: relative;
  width: 150px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  border: 1px solid var(--rose-500);
  background-color: var(--rose-500);
  overflow: hidden;
  border-radius: 12px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  box-shadow: var(--shadow-md);
}

.button,
.button__icon,
.button__text {
  transition: all 0.3s;
}

.button__text {
  transform: translateX(20px);
  color: #fff;
  font-weight: 600;
}

.button__icon {
  position: absolute;
  transform: translateX(105px);
  height: 100%;
  width: 38px;
  background-color: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.button .svg {
  width: 22px;
  height: 22px;
  fill: #fff;
}

.button:hover {
  background: var(--rose-600);
}

.button:hover .button__text {
  color: transparent;
}

.button:hover .button__icon {
  width: 148px;
  transform: translateX(0);
}

.button:active {
  transform: scale(0.95);
}

.button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}
</style>
