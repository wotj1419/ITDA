<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { Trash2 } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import ProjectCard from '../components/project/ProjectCard.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'

const projectStore = useProjectStore()

// State
const deletedProjects = ref<any[]>([]) 
const isLoading = ref(true)

// Modal State
const showModal = ref(false)
const modalType = ref<'restore' | 'delete'>('restore')
const selectedProject = ref<{ id: number; title: string } | null>(null)

// Modal Content Computed
const modalTitle = computed(() => {
  return modalType.value === 'restore' 
    ? '다시 함께 해볼까요? ✨' 
    : '정말 이별인가요? 😢'
})

const modalMessage = computed(() => {
  if (!selectedProject.value) return ''
  return modalType.value === 'restore'
    ? `'${selectedProject.value.title}' 프로젝트를 복구할게요!\n다시 멋진 이야기를 만들어봐요.`
    : `'${selectedProject.value.title}' 프로젝트를 영구적으로 삭제합니다.\n삭제 후에는 절대 되돌릴 수 없어요!`
})

const modalConfirmText = computed(() => {
  return modalType.value === 'restore' ? '네, 복구할래요' : '잘 가..'
})

const modalIsDangerous = computed(() => {
  return modalType.value === 'delete'
})

onMounted(async () => {
  await loadDeletedProjects()
})

const loadDeletedProjects = async () => {
  isLoading.value = true
  deletedProjects.value = await projectStore.getDeletedProjects()
  isLoading.value = false
}

// Helper to calculate remaining days
const getDaysRemaining = (deletedAt?: string) => {
  if (!deletedAt) return 30 // Default to 30 if unknown
  
  const deleteDate = new Date(deletedAt)
  const expirationDate = new Date(deleteDate.getTime() + 30 * 24 * 60 * 60 * 1000)
  const now = new Date()
  
  const diffTime = expirationDate.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays > 0 ? diffDays : 0
}

// Action Handlers
const requestRestore = (project: any) => {
  selectedProject.value = { id: project.projectId, title: project.title }
  modalType.value = 'restore'
  showModal.value = true
}

const requestDelete = (project: any) => {
  selectedProject.value = { id: project.projectId, title: project.title }
  modalType.value = 'delete'
  showModal.value = true
}

const handleConfirm = async () => {
  if (!selectedProject.value) return

  if (modalType.value === 'restore') {
    await projectStore.restoreProject(selectedProject.value.id)
  } else {
    await projectStore.permanentDeleteProject(selectedProject.value.id)
  }
  
  await loadDeletedProjects()
  closeModal()
}

const closeModal = () => {
  showModal.value = false
  selectedProject.value = null
}
</script>

<template>
  <DefaultLayout>
    <div class="trash-page">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">휴지통</h1>
          <p class="page-description">삭제된 프로젝트는 30일 후 영구 삭제됩니다.</p>
        </div>
      </div>

      <!-- Content -->
      <div v-if="isLoading" class="loading-state">
        Loading...
      </div>
      
      <div v-else-if="deletedProjects.length > 0" class="project-grid">
        <div 
          v-for="project in deletedProjects" 
          :key="project.projectId"
          class="trash-card-wrapper"
        >
          <ProjectCard
            :project="project"
            :is-favorite="false"
            class="trash-card"
          />
          
          <!-- Days Remaining Badge -->
          <div class="days-remaining">
            {{ getDaysRemaining(project.deletedAt) }}일
          </div>

          <div class="trash-actions">
            <button class="action-btn restore" @click="requestRestore(project)">
              복구
            </button>
            <button class="action-btn delete" @click="requestDelete(project)">
              영구 삭제
            </button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="icon-wrapper">
          <Trash2 class="icon-lg" />
        </div>
        <h3 class="empty-title">휴지통이 비어있습니다</h3>
        <p class="empty-description">삭제된 프로젝트가 없습니다.</p>
      </div>

      <!-- Confirm Modal -->
      <ConfirmModal
        :is-open="showModal"
        :title="modalTitle"
        :message="modalMessage"
        :confirm-text="modalConfirmText"
        :is-dangerous="modalIsDangerous"
        @confirm="handleConfirm"
        @cancel="closeModal"
      />
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
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 1rem;
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

.trash-card-wrapper {
  position: relative;
  /* Ensure wrapper has stacking context */
  z-index: 1; 
}

.trash-card {
  opacity: 0.7;
  filter: grayscale(0.5);
  transition: all 0.2s;
}

.trash-card-wrapper:hover .trash-card {
  opacity: 0.3;
}

.days-remaining {
  position: absolute;
  bottom: 0.75rem;
  left: 50%;
  transform: translateX(-50%);
  color: #1f2937; /* Dark Gray (Gray 800) */
  font-size: 1rem;
  font-weight: 800;
  z-index: 5;
  text-shadow: 
    -1px -1px 0 #fff,  
    1px -1px 0 #fff,
    -1px 1px 0 #fff,
    1px 1px 0 #fff,
    0 2px 4px rgba(0,0,0,0.15); /* White outline + Soft Shadow */
  width: max-content;
  pointer-events: none;
}

.trash-actions {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem; /* Increased gap for better visibility */
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 10;
  pointer-events: none; /* Initially disable pointer events */
}

.trash-card-wrapper:hover .trash-actions {
  opacity: 1;
  pointer-events: auto; /* Enable on hover */
}

.action-btn {
  padding: 0.75rem 1.5rem; /* Larger padding */
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-size: 0.9rem;
  transition: all 0.2s;
  width: 140px; /* Wider buttons */
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.action-btn.restore {
  background: white;
  color: var(--gray-900);
  border: 1px solid var(--gray-200);
}

.action-btn.restore:hover {
  background: var(--gray-50);
  transform: translateY(-2px);
}

.action-btn.delete {
  background: var(--red-100);
  color: var(--gray-900);
}

.action-btn.delete:hover {
  background: var(--red-200);
  transform: translateY(-2px);
}
</style>
