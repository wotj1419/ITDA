<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { Users } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useProjectStore } from '../stores/project'
import { useUIStore } from '../stores/ui'
import ProjectCard from '../components/project/ProjectCard.vue'
import UserWelcomeTitle from '../components/common/UserWelcomeTitle.vue'

const router = useRouter()
const authStore = useAuthStore()
const projectStore = useProjectStore()
const uiStore = useUIStore()
const isCreatingProject = ref(false)

onMounted(async () => {
  if (projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

const sharedProjects = computed(() =>
  projectStore.projects.filter((project) => project.role !== 'OWNER')
)

const isFavorite = (projectId: number) => projectStore.isFavorite(projectId)

const handleToggleFavorite = (projectId: number) => {
  projectStore.toggleFavorite(projectId)
}

const createEmptyProject = async () => {
  if (isCreatingProject.value) return
  isCreatingProject.value = true
  const newProject = await projectStore.addProject({
    title: '새프로젝트',
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
    <template #header-left-after-divider>
      <UserWelcomeTitle :name="authStore.user?.name" />
    </template>
    <template #header-actions>
      <button
        class="button"
        type="button"
        :disabled="isCreatingProject"
        @click="createEmptyProject"
      >
        <span class="button__text">새프로젝트</span>
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
    <div class="shared-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">공유 프로젝트</h1>
          <p class="page-description">{{ sharedProjects.length }}개의 공유 프로젝트</p>
        </div>
      </div>

      <div v-if="sharedProjects.length > 0" class="projects-grid">
        <ProjectCard
          v-for="project in sharedProjects"
          :key="project.projectId"
          :project="project"
          :is-favorite="isFavorite(project.projectId)"
          @toggle-favorite="handleToggleFavorite"
        />
      </div>
      <div v-else class="empty-state">
        <div class="icon-wrapper">
          <Users class="icon-lg" />
        </div>
        <h3 class="empty-title">아직 공유 받은 프로젝트가 없습니다</h3>
        <p class="empty-description">공유받은 프로젝트가 이곳에 표시됩니다.</p>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.page-header {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--gray-900);
  margin-bottom: 0.5rem;
}

.page-description {
  color: var(--gray-500);
  font-size: 1rem;
}


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

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 1rem;
  background: var(--rose-50);
  border: 1px dashed var(--rose-200);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.icon-wrapper {
  width: 64px;
  height: 64px;
  background: var(--gray-100);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  color: var(--gray-400);
}

.icon-lg {
  width: 32px;
  height: 32px;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--gray-900);
  margin-bottom: 0.5rem;
}

.empty-description {
  color: var(--gray-500);
}
</style>
