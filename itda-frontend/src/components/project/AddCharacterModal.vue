<script setup lang="ts">
import { ref, watch } from 'vue'
import { Sparkles } from 'lucide-vue-next'
import { useUIStore } from '../../stores/ui'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'

const MODAL_ID = 'add-character-modal'

interface Props {
  isGenerating?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'submit', data: {
    name: string
    description: string
    style: string
  }): void
  (e: 'close'): void
}>()

const uiStore = useUIStore()

// Form state
const name = ref('')
const description = ref('')
const selectedStyle = ref('실사')

const styleOptions = ['실사', '만화', '애니메이션', '사이버펑크']

const handleSubmit = () => {
  if (!name.value.trim()) return

  emit('submit', {
    name: name.value.trim(),
    description: description.value.trim(),
    style: selectedStyle.value,
  })
}

const handleClose = () => {
  emit('close')
}

const resetForm = () => {
  name.value = ''
  description.value = ''
  selectedStyle.value = '실사'
}

// Reset form when modal opens
watch(() => uiStore.activeModal, (modalId) => {
  if (modalId === MODAL_ID) {
    resetForm()
  }
})

// Expose modal ID for parent to use
defineExpose({
  MODAL_ID,
})
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="캐릭터 추가" @close="handleClose">
    <div class="form-group">
      <label class="form-label required">캐릭터 이름</label>
      <input
        v-model="name"
        type="text"
        class="form-input"
        placeholder="예: 민준"
      />
    </div>

    <div class="form-group">
      <label class="form-label">캐릭터 설명</label>
      <textarea
        v-model="description"
        class="form-input form-textarea"
        placeholder="성격, 외형, 의상, 배경 등을 입력하세요"
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

    <template #footer>
      <Button variant="secondary" @click="uiStore.closeModal()">
        취소
      </Button>
      <Button
        variant="primary"
        :loading="isGenerating"
        :disabled="isGenerating || !name.trim()"
        @click="handleSubmit"
      >
        <Sparkles class="btn-icon" />
        캐릭터 생성
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
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.form-input::placeholder {
  color: var(--gray-400);
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
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

.btn-icon {
  width: 18px;
  height: 18px;
}
</style>
