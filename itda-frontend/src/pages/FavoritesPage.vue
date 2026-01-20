<script setup lang="ts">
import { onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import ProjectCard from '../components/project/ProjectCard.vue'

const projectStore = useProjectStore()
const uiStore = useUIStore()

onMounted(async () => {
  if (projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

const handleToggleFavorite = (projectId: number) => {
  projectStore.toggleFavorite(projectId)
}

const openNewProjectModal = () => {
  uiStore.openModal('new-project')
}
</script>

<template>
  <DefaultLayout>
    <div class="dashboard-container">
      <div class="toolbar">
        <div class="toolbar-left">
          <h1 class="page-title">Favorites</h1>
          <p class="project-count">{{ projectStore.favoriteProjects.length }} saved projects</p>
        </div>
      </div>

      <section class="section">
        <div v-if="projectStore.favoriteProjects.length > 0" class="projects-grid">
          <ProjectCard
            v-for="project in projectStore.favoriteProjects"
            :key="project.projectId"
            :project="project"
            :is-favorite="true"
            @toggle-favorite="handleToggleFavorite"
          />
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">⭐</div>
          <h3 class="empty-title">즐겨찾는 프로젝트가 없습니다</h3>
          <p class="empty-desc">프로젝트 카드의 별 아이콘을 눌러 즐겨찾기에 추가해보세요.</p>
          <button class="btn btn-primary" @click="openNewProjectModal">
            <Plus class="icon-sm" />
            새 프로젝트 만들기
          </button>
        </div>
      </section>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
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

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1rem;
  text-align: center;
  background: white;
  border-radius: 16px;
  border: 1px dashed var(--rose-200);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0 0 0.5rem;
}

.empty-desc {
  color: var(--gray-500);
  margin-bottom: 1.5rem;
}

.icon-sm {
  width: 16px;
  height: 16px;
}
</style>
