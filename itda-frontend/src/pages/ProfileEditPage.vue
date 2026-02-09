<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Avatar from '../components/common/Avatar.vue'
import { useAuthStore } from '../stores/auth'
import { useUIStore } from '../stores/ui'
import type { UpdateProfileRequest } from '../types/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUIStore()

const form = ref({
  name: '',
})

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const filePreviewUrl = ref<string | null>(null)

const isSubmitting = ref(false)
const isLoading = computed(() => authStore.isAuthenticated && !authStore.user)

const currentName = computed(() => authStore.user?.name ?? '')
const currentProfileImageUrl = computed(() => authStore.user?.profileImageUrl ?? '')

const normalizedName = computed(() => form.value.name.trim())

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
const MAX_IMAGE_SIZE = 5 * 1024 * 1024

const nameError = computed(() => {
  if (!normalizedName.value) return '이름을 입력해주세요.'
  if (normalizedName.value.length < 2) return '이름은 2자 이상이어야 합니다.'
  if (normalizedName.value.length > 100) return '이름은 100자 이하로 입력해주세요.'
  return ''
})

const fileError = computed(() => {
  if (!selectedFile.value) return ''
  const type = selectedFile.value.type.toLowerCase()
  if (!ALLOWED_IMAGE_TYPES.includes(type)) {
    return '지원하지 않는 이미지 형식입니다.'
  }
  if (selectedFile.value.size > MAX_IMAGE_SIZE) {
    return '이미지 파일은 5MB 이하로 업로드해주세요.'
  }
  return ''
})

const hasNameChange = computed(() => normalizedName.value !== currentName.value)
const hasImageChange = computed(() => !!selectedFile.value)

const canSubmit = computed(() => {
  return !!authStore.user
    && !isSubmitting.value
    && !nameError.value
    && !fileError.value
    && (hasNameChange.value || hasImageChange.value)
})

const previewImageUrl = computed(() => {
  if (filePreviewUrl.value) return filePreviewUrl.value
  if (currentProfileImageUrl.value) return currentProfileImageUrl.value
  return undefined
})

const syncForm = () => {
  if (!authStore.user) return
  form.value.name = authStore.user.name ?? ''
}

watch(() => authStore.user, () => syncForm(), { immediate: true })

watch(selectedFile, (file) => {
  if (filePreviewUrl.value) {
    URL.revokeObjectURL(filePreviewUrl.value)
    filePreviewUrl.value = null
  }
  if (file) {
    filePreviewUrl.value = URL.createObjectURL(file)
  }
})

onBeforeUnmount(() => {
  if (filePreviewUrl.value) {
    URL.revokeObjectURL(filePreviewUrl.value)
  }
})

const resetFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0] ?? null
  selectedFile.value = file
}

const handleSave = async () => {
  if (!canSubmit.value) return
  isSubmitting.value = true

  try {
    if (selectedFile.value) {
      await authStore.uploadProfileImage(selectedFile.value)
      selectedFile.value = null
      resetFileInput()
    }

    if (hasNameChange.value) {
      const payload: UpdateProfileRequest = { name: normalizedName.value }
      await authStore.updateProfile(payload)
    }

    uiStore.showToast({
      type: 'success',
      title: '저장 완료',
      message: '계정 정보가 업데이트되었습니다.',
    })
    router.push('/profile')
  } catch (error) {
    console.error(error)
    uiStore.showToast({
      type: 'error',
      title: '저장 실패',
      message: '계정 정보를 저장하는 중 오류가 발생했습니다.',
    })
  } finally {
    isSubmitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  if (authStore.isAuthenticated && !authStore.user) {
    authStore.fetchMe()
  }
})
</script>

<template>
  <DefaultLayout>
    <div class="account-edit">
      <div class="page-header">
        <button class="btn btn-ghost" type="button" @click="handleCancel">
          <ArrowLeft class="w-4 h-4" />
          뒤로
        </button>
        <div>
          <h1 class="page-title">계정 정보 수정</h1>
          <p class="page-description">이름과 프로필 이미지를 업데이트할 수 있습니다.</p>
        </div>
      </div>

      <div v-if="isLoading" class="loading-state">계정 정보를 불러오는 중...</div>

      <div v-else class="card settings-card">
        <form @submit.prevent="handleSave">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label required">이름</label>
              <input
                type="text"
                class="form-input"
                v-model="form.name"
                required
                maxlength="100"
              />
              <p v-if="nameError" class="form-error">{{ nameError }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">프로필 이미지</label>
              <input
                ref="fileInputRef"
                type="file"
                class="form-input"
                accept="image/png, image/jpeg, image/webp"
                @change="handleFileChange"
              />
              <p v-if="fileError" class="form-error">{{ fileError }}</p>
              <p class="form-hint">JPG/PNG/WebP, 최대 5MB</p>
            </div>
          </div>

          <div class="preview-row">
            <Avatar
              :src="previewImageUrl"
              :alt="normalizedName || '사용자'"
              size="lg"
              :user-id="authStore.user?.id"
            />
            <div class="preview-text">
              <div class="preview-title">미리보기</div>
              <div class="preview-desc">사이드바와 프로젝트 협업 영역에 표시됩니다.</div>
            </div>
          </div>

          <div class="readonly-grid">
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input type="email" class="form-input" :value="authStore.user?.email || ''" readonly />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="handleCancel">취소</button>
            <button type="submit" class="btn btn-primary" :disabled="!canSubmit">
              {{ isSubmitting ? '저장 중...' : '저장' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.account-edit {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0;
}

.page-description {
  color: var(--gray-500);
  margin: 0.35rem 0 0;
}

.settings-card {
  padding: 1.5rem;
}

.form-grid {
  display: grid;
  gap: 1rem;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  margin-top: 1rem;
  border-radius: 12px;
  background: var(--gray-50);
}

.preview-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.preview-title {
  font-weight: 600;
  color: var(--gray-900);
}

.preview-desc {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.readonly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.form-error {
  font-size: 0.75rem;
  color: var(--error);
  margin-top: 0.5rem;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-top: 0.35rem;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 1.5rem;
  margin-top: 1.5rem;
  border-top: 1px solid var(--rose-100);
}

.loading-state {
  padding: 2rem;
  text-align: center;
  color: var(--gray-500);
}
</style>
