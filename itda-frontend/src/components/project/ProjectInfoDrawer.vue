<script setup lang="ts">
import { ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import type { ProjectDetail } from '../../types/api/projects'
import { useProjectStore } from '../../stores/project'
import { useUIStore } from '../../stores/ui'
import Button from '../common/Button.vue'

interface Props {
  project: ProjectDetail | null
  isOpen: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

const projectStore = useProjectStore()
const uiStore = useUIStore()

const localTitle = ref('')
const localDescription = ref('')
const localGenre = ref('')
const isSaving = ref(false)

const genreOptions = [
  { value: '', label: '선택하세요' },
  { value: 'fantasy', label: '판타지' },
  { value: 'sf', label: 'SF' },
  { value: 'romance', label: '로맨스' },
  { value: 'action', label: '액션' },
  { value: 'thriller', label: '스릴러' },
  { value: 'comedy', label: '코미디' },
  { value: 'documentary', label: '다큐멘터리' },
]

const syncFromProject = () => {
  if (!props.project) return
  localTitle.value = props.project.title || ''
  localDescription.value = props.project.description || ''
  localGenre.value = props.project.genre || ''
}

watch(() => props.project, syncFromProject, { immediate: true })
watch(() => props.isOpen, (open) => {
  if (open) syncFromProject()
})

const handleClose = () => {
  emit('close')
}

const handleSave = async () => {
  if (!props.project || isSaving.value) return
  const title = localTitle.value.trim()
  const genre = localGenre.value.trim()

  if (!title) {
    uiStore.showToast({
      type: 'error',
      title: '프로젝트 제목을 입력해주세요.',
    })
    return
  }

  if (!genre) {
    uiStore.showToast({
      type: 'error',
      title: '장르를 선택해주세요.',
    })
    return
  }

  isSaving.value = true
  const updated = await projectStore.updateProject(props.project.projectId, {
    title,
    description: localDescription.value.trim(),
    genre,
  })
  isSaving.value = false

  if (!updated) {
    uiStore.showToast({
      type: 'error',
      title: '프로젝트 저장 실패',
      message: '잠시 후 다시 시도해주세요.',
    })
    return
  }

  uiStore.showToast({
    type: 'success',
    title: '프로젝트 정보가 저장되었습니다.',
  })
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="isOpen" class="drawer-backdrop" @click="handleClose" />
    </Transition>

    <Transition name="slide">
      <aside
        v-if="isOpen"
        class="project-info-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="project-info-title"
      >
        <header class="drawer-header">
          <h2 id="project-info-title" class="drawer-title">프로젝트 정보</h2>
          <button class="close-btn" type="button" aria-label="닫기" @click="handleClose">
            <X class="icon-close" />
          </button>
        </header>

        <main class="drawer-content">
          <div class="form-group">
            <label class="form-label required">프로젝트 제목</label>
            <input
              v-model="localTitle"
              type="text"
              class="form-input"
              placeholder="프로젝트 제목을 입력하세요"
            />
          </div>

          <div class="form-group">
            <label class="form-label required">장르</label>
            <select v-model="localGenre" class="form-input form-select">
              <option v-for="option in genreOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">설명</label>
            <textarea
              v-model="localDescription"
              class="panel-textarea panel-textarea--prompt project-description-textarea"
              placeholder="프로젝트에 대한 간단한 설명을 입력하세요"
              rows="4"
            ></textarea>
          </div>
        </main>

        <footer class="drawer-footer">
          <Button variant="secondary" @click="handleClose">닫기</Button>
          <Button variant="primary" :loading="isSaving" @click="handleSave">저장</Button>
        </footer>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 1000;
}

.project-info-drawer {
  position: fixed;
  z-index: 1001;
  background: white;
  display: flex;
  flex-direction: column;
  box-shadow: -6px 0 24px rgba(15, 23, 42, 0.12);
}

@media (min-width: 769px) {
  .project-info-drawer {
    top: 0;
    right: 0;
    bottom: 0;
    width: 360px;
    max-width: 90vw;
  }
}

@media (max-width: 768px) {
  .project-info-drawer {
    inset: 0;
  }
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--rose-100);
  background: var(--rose-50);
}

.drawer-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--gray-900);
}

.close-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--gray-200);
  border-radius: 10px;
  background: white;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--gray-50);
  color: var(--gray-700);
}

.icon-close {
  width: 18px;
  height: 18px;
}

.drawer-content {
  flex: 1;
  padding: 1.25rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--rose-100);
  background: var(--gray-50);
}

.project-description-textarea {
  min-height: 140px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (min-width: 769px) {
  .slide-enter-active,
  .slide-leave-active {
    transition: transform 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateX(100%);
  }
}

@media (max-width: 768px) {
  .slide-enter-active,
  .slide-leave-active {
    transition: transform 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateY(100%);
  }
}
</style>
