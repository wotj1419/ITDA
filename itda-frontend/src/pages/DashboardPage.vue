<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, LayoutGrid, List } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'
import { useAuthStore } from '../stores/auth'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import ProjectCard from '../components/project/ProjectCard.vue'
import NewProjectModal from '../components/project/NewProjectModal.vue'
import StartCollabModal from '../components/project/StartCollabModal.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'
import TimeAgo from '../components/common/TimeAgo.vue'

const router = useRouter()
const projectStore = useProjectStore()
const uiStore = useUIStore()
const collabStore = useCollabStore()
const authStore = useAuthStore()
const isCreatingProject = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')

// Delete Confirmation State
const showDeleteModal = ref(false)
const projectToDelete = ref<{ projectId: number; title: string } | null>(null)

// Load projects on mount
onMounted(async () => {
  await projectStore.loadProjects()
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

const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid'
}
</script>

<template>
  <DefaultLayout
    :show-collaborators="false"
    @start-collab="openStartCollabModal"
  >
    <template #header-left-after-divider>
      <h2 class="welcome-title">
        반가워요<span v-if="authStore.user?.name">, {{ authStore.user.name }}님</span> ✨
      </h2>
    </template>
    <template #header-actions>
      <button class="btn btn-primary" :disabled="isCreatingProject" @click="createEmptyProject">
        <Plus class="icon-sm" />
        New Project
      </button>
    </template>
    <div class="dashboard-container">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="toolbar-left">
          <h1 class="page-title">My Projects</h1>
          <p class="project-count">{{ projectStore.projectCount }} projects</p>
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
        <h3 class="section-title">All Projects</h3>
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
            <span class="add-project-text">Create New Project</span>
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
  --switch-width: 100px;
  --switch-height: 52px;
  --padding: 6px;
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
  border-radius: 18px;
  box-shadow:
    inset 0 2px 4px rgba(255, 133, 161, 0.12),
    inset 0 -1px 2px rgba(255, 255, 255, 0.6),
    0 12px 24px -8px rgba(255, 133, 161, 0.25);
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
  width: 22px;
  height: 22px;
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
  border-radius: 14px;
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 245, 249, 0.7) 100%
  );
  border: 1px solid rgba(255, 133, 161, 0.25);
  box-shadow:
    0 0 16px var(--primary-glow),
    inset 0 0 12px rgba(255, 133, 161, 0.15);
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
  border-radius: 14px;
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
    box-shadow:
      0 0 16px var(--primary-glow),
      inset 0 0 12px rgba(255, 133, 161, 0.15);
  }
  50% {
    box-shadow:
      0 0 22px var(--secondary-glow),
      inset 0 0 16px rgba(255, 133, 161, 0.2);
  }
}

.highlight-inner {
  animation: neon-pulse 3s infinite ease-in-out;
}

.page-title {
  font-size: 1.5rem;
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
</style>
