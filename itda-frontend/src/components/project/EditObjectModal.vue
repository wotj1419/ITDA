<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from 'vue'
import { Save } from 'lucide-vue-next'
import type { ObjectSheet, ObjectType } from '../../types/api/objects'
import { OBJECT_STYLE_OPTIONS, OBJECT_TYPE_OPTIONS } from '../../constants/objects'
import { resolveApiUrl } from '../../services/api/urls'
import { fetchProtectedBlobUrl } from '../../services/api/media'
import { useUIStore } from '../../stores/ui'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'

const MODAL_ID = 'edit-object-modal'

interface Props {
  object?: ObjectSheet | null
  isUpdating?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'submit', data: {
    objectId: number
    name: string
    type: ObjectType
    description: string
    style: string
    file?: File | null
  }): void
  (e: 'close'): void
}>()

const uiStore = useUIStore()

const name = ref('')
const description = ref('')
const selectedStyle = ref(OBJECT_STYLE_OPTIONS[0])
const selectedType = ref<ObjectType>(OBJECT_TYPE_OPTIONS[0].value)
const file = ref<File | null>(null)
const previewUrl = ref<string | null>(null)

const styleOptions = OBJECT_STYLE_OPTIONS
const typeOptions = OBJECT_TYPE_OPTIONS

const resolvedSheetImageUrl = computed(() => resolveApiUrl(props.object?.sheetImageUrl) ?? null)
const objectImageUrl = ref<string | null>(null)
let imageRequestId = 0

const isDirectUrl = (url: string) => url.startsWith('data:') || url.startsWith('blob:')

const revokeObjectUrl = (url: string | null) => {
  if (url && url.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

const loadObjectImage = async (url: string | null) => {
  const requestId = ++imageRequestId
  revokeObjectUrl(objectImageUrl.value)
  objectImageUrl.value = null
  if (!url) return
  if (isDirectUrl(url)) {
    objectImageUrl.value = url
    return
  }
  try {
    const blobUrl = await fetchProtectedBlobUrl(url)
    if (requestId !== imageRequestId) {
      revokeObjectUrl(blobUrl)
      return
    }
    objectImageUrl.value = blobUrl
  } catch (error) {
    console.error('Failed to load object image:', error)
  }
}

const currentPreview = computed(() => previewUrl.value || objectImageUrl.value || null)

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

const resetForm = () => {
  name.value = props.object?.name || ''
  description.value = props.object?.description || ''
  selectedStyle.value = props.object?.style || OBJECT_STYLE_OPTIONS[0]
  selectedType.value = props.object?.type || OBJECT_TYPE_OPTIONS[0].value
  file.value = null
  setPreview(null)
}

const handleSubmit = () => {
  if (!props.object || !name.value.trim()) return
  emit('submit', {
    objectId: props.object.objectId,
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

watch(
  () => [props.object, uiStore.activeModal],
  ([, modalId]) => {
    if (modalId === MODAL_ID) {
      resetForm()
    }
  },
  { immediate: true }
)

watch(resolvedSheetImageUrl, (nextUrl) => {
  void loadObjectImage(nextUrl)
}, { immediate: true })

onUnmounted(() => {
  setPreview(null)
  revokeObjectUrl(objectImageUrl.value)
})

defineExpose({ MODAL_ID })
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="오브젝트 수정" @close="handleClose">
    <div class="form-group">
      <label class="form-label required">오브젝트 이름</label>
      <input v-model="name" type="text" class="form-input" placeholder="예: 민준" />
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
      <label class="form-label">이미지 교체</label>
      <input
        type="file"
        accept="image/*"
        class="form-input"
        @change="handleFileChange"
      />
      <div v-if="currentPreview" class="image-preview">
        <img :src="currentPreview" alt="preview" />
      </div>
    </div>

    <template #footer>
      <Button variant="secondary" @click="uiStore.closeModal()">취소</Button>
      <Button
        variant="primary"
        :loading="isUpdating"
        :disabled="isUpdating || !name.trim()"
        @click="handleSubmit"
      >
        <Save class="icon-sm" />
        수정 저장
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
</style>
