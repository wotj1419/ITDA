<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import ProjectCard from '../components/project/ProjectCard.vue'

const router = useRouter()
const projectStore = useProjectStore()
const uiStore = useUIStore()
const isCreatingProject = ref(false)

onMounted(async () => {
  if (projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

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
</script>

<template>
  <DefaultLayout>
    <div class="dashboard-container">
      <div class="toolbar">
        <div class="toolbar-left">
          <h1 class="page-title">즐겨찾기</h1>
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
          <button
            class="button"
            type="button"
            :disabled="isCreatingProject"
            @click="createEmptyProject"
          >
            <span class="button__text">? ????</span>
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
  margin-bottom: 1.5rem;
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
