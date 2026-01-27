<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'
import { useUIStore } from '../../stores/ui'
import { useProjectStore } from '../../stores/project'

const MODAL_ID = 'start-collab'

const uiStore = useUIStore()
const projectStore = useProjectStore()

const selectedId = ref<number | null>(null)
const projects = computed(() => projectStore.projects)
const isOpen = computed(() => uiStore.activeModal === MODAL_ID)

const emit = defineEmits<{
  (e: 'start', projectId: number): void
}>()

watch(isOpen, (open) => {
  if (!open) return
  selectedId.value = projects.value[0]?.projectId ?? null
})

const handleStart = () => {
  if (selectedId.value == null) return
  emit('start', selectedId.value)
  uiStore.closeModal()
}
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="Start Collaboration" size="sm">
    <div class="modal-content">
      <div v-if="projects.length === 0" class="empty-state">
        No projects available.
      </div>

      <div v-else class="project-list">
        <label
          v-for="project in projects"
          :key="project.projectId"
          class="project-option"
        >
          <input
            v-model="selectedId"
            type="radio"
            name="collab-project"
            :value="project.projectId"
          />
          <span class="project-title">{{ project.title }}</span>
        </label>
      </div>
    </div>

    <template #footer>
      <Button variant="secondary" size="sm" @click="uiStore.closeModal">
        Cancel
      </Button>
      <Button
        variant="primary"
        size="sm"
        :disabled="selectedId == null"
        @click="handleStart"
      >
        Start
      </Button>
    </template>
  </ModalBase>
</template>

<style scoped>
.modal-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.empty-state {
  padding: 1rem;
  border: 1px dashed var(--rose-200);
  border-radius: 12px;
  text-align: center;
  color: var(--gray-500);
  background: var(--rose-50);
  font-size: 0.875rem;
}

.project-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 320px;
  overflow: auto;
  padding-right: 0.25rem;
}

.project-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid var(--rose-100);
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.project-option:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.project-option input {
  accent-color: var(--rose-500);
}

.project-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
