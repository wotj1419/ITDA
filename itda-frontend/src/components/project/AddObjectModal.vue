<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { Sparkles } from 'lucide-vue-next'
import type { ObjectType } from '../../types/api/objects'
import { OBJECT_STYLE_OPTIONS, OBJECT_TYPE_OPTIONS } from '../../constants/objects'
import { useUIStore } from '../../stores/ui'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'

const MODAL_ID = 'add-object-modal'

interface Props {
  isSaving?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'submit', data: {
    name: string
    type: ObjectType
    description: string
    style: string
    file: File
  }): void
  (e: 'close'): void
}>()

const uiStore = useUIStore()

const defaultStyle = OBJECT_STYLE_OPTIONS[0] ?? '실사'
const defaultType: ObjectType = OBJECT_TYPE_OPTIONS[0]?.value ?? 'CHARACTER'

const name = ref('')
const description = ref('')
const selectedStyle = ref<string>(defaultStyle)
const selectedType = ref<ObjectType>(defaultType)
const file = ref<File | null>(null)
const previewUrl = ref<string | null>(null)

const styleOptions = OBJECT_STYLE_OPTIONS
const typeOptions = OBJECT_TYPE_OPTIONS

const setPreview = (nextFile: File | null) => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = nextFile ? URL.createObjectURL(nextFile) : null
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement | null
  const nextFile = target?.files?.[0] ?? null
  file.value = nextFile
  setPreview(nextFile)
}

const handleSubmit = () => {
  if (!name.value.trim() || !description.value.trim() || !file.value) return

  emit('submit', {
    name: name.value.trim(),
    type: selectedType.value,
    description: description.value.trim(),
    style: selectedStyle.value,
    file: file.value,
  })
}

const handleClose = () => {
  emit('close')
}

const resetForm = () => {
  name.value = ''
  description.value = ''
  selectedStyle.value = defaultStyle
  selectedType.value = defaultType
  file.value = null
  setPreview(null)
}

watch(() => uiStore.activeModal, (modalId) => {
  if (modalId === MODAL_ID) {
    resetForm()
  }
})

onUnmounted(() => {
  setPreview(null)
})

defineExpose({
  MODAL_ID,
})
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="오브젝트 추가" @close="handleClose">
    <div class="form-group">
      <label class="form-label required">오브젝트 이름</label>
      <input
        v-model="name"
        type="text"
        class="form-input"
        placeholder="예: 민준"
      />
    </div>

    <div class="form-group">
      <label class="form-label required">오브젝트 유형</label>
      <select v-model="selectedType" class="form-input">
        <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="form-label required">오브젝트 설명</label>
      <textarea
        v-model="description"
        class="form-input form-textarea"
        placeholder="외형, 특징, 소품 정보 등을 입력하세요"
        rows="3"
      ></textarea>
    </div>

    <div class="form-group">
      <label class="form-label">아트 스타일</label>
      <div class="chip-group">
        <button
          v-for="style in styleOptions"
          :key="style"
          type="button"
          :class="['chip', { selected: selectedStyle === style }]"
          @click="selectedStyle = style"
        >
          {{ style }}
        </button>
      </div>
    </div>

    <div class="form-group">
      <label class="form-label required">오브젝트 이미지</label>
      <input
        type="file"
        accept="image/*"
        class="form-input"
        @change="handleFileChange"
      />
      <div v-if="previewUrl" class="image-preview">
        <img :src="previewUrl" alt="preview" />
      </div>
    </div>

    <template #footer>
      <Button variant="secondary" @click="uiStore.closeModal()">
        취소
      </Button>
      <Button
        variant="primary"
        :loading="isSaving"
        :disabled="isSaving || !name.trim() || !description.trim() || !file"
        @click="handleSubmit"
      >
        <Sparkles class="icon-sm" />
        오브젝트 생성
      </Button>
    </template>
  </ModalBase>
</template>

<style scoped>
.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
  margin-bottom: 0.375rem;
}

.form-label.required::after {
  content: '*';
  color: var(--rose-500);
  margin-left: 0.25rem;
}

.form-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-900);
  background: white;
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: var(--rose-400);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

.form-input::placeholder {
  color: var(--gray-400);
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
}

.image-preview {
  margin-top: 0.75rem;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--rose-100);
  background: var(--rose-50);
}

.image-preview img {
  width: 100%;
  display: block;
  object-fit: cover;
  max-height: 220px;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chip {
  padding: 0.5rem 1rem;
  border: 1px solid var(--rose-200);
  border-radius: 9999px;
  background: white;
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.chip.selected {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

/* Uses global .icon-sm from base.css */
</style>
