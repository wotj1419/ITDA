<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import { useAuthStore } from '../stores/auth'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import ProjectCard from '../components/project/ProjectCard.vue'
import NewProjectModal from '../components/project/NewProjectModal.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'

const projectStore = useProjectStore()
const uiStore = useUIStore()
const authStore = useAuthStore()

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

const openNewProjectModal = () => {
  uiStore.openModal('new-project')
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
  <DefaultLayout>
    <template #header-left-after-divider>
      <h2 class="welcome-title">
        반가워요<span v-if="authStore.user?.name">, {{ authStore.user.name }}님</span> ✨
      </h2>
    </template>
    <template #header-actions>
      <button class="btn btn-primary" @click="openNewProjectModal">
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
      </div>

      <!-- Quick Access Section Removed -->


      <!-- All Projects Section -->
      <section class="section">
        <h3 class="section-title">All Projects</h3>
        <div class="projects-grid">
          <!-- Project Cards -->
          <ProjectCard
            v-for="project in projectStore.sortedProjects"
            :key="project.projectId"
            :project="project"
            :is-favorite="isFavorite(project.projectId)"
            @toggle-favorite="handleToggleFavorite"
            @delete="handleRequestDelete"
          />

          <!-- Add New Project Card -->
          <button class="add-project-card" @click="openNewProjectModal">
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

@media (max-width: 640px) {
  .projects-grid {
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
