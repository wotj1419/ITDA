<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mail, ArrowRight, Loader2 } from 'lucide-vue-next'
import { authService } from '../services'
import { useUIStore } from '../stores/ui'

const router = useRouter()
const uiStore = useUIStore()

const form = ref({
  email: '',
})

const emailError = ref('')
const isLoading = ref(false)
const emailInput = ref<HTMLInputElement | null>(null)

function validateEmail() {
  emailError.value = ''
  const input = emailInput.value
  if (input && !input.checkValidity()) {
    emailError.value = input.validationMessage
  }
  return !emailError.value
}

async function handleSubmit() {
  if (isLoading.value) return
  if (!validateEmail()) return

  isLoading.value = true
  try {
    await authService.requestPasswordReset({ email: form.value.email })
    router.push({ path: '/auth/reset', query: { email: form.value.email } })
  } catch (_error) {
    const status = (_error as { response?: { status?: number } })?.response?.status
    if (status === 404) {
      emailError.value = '가입되지 않은 이메일입니다.'
      return
    }
    uiStore.showToast({
      type: 'error',
      title: '요청 실패',
      message: '잠시 후 다시 시도해주세요.',
    })
  } finally {
    isLoading.value = false
  }
}

</script>

<template>
  <div class="auth-page">
    <div class="auth-dot-pattern"></div>

    <main class="auth-container">
      <div class="auth-logo" @click="router.push('/')" style="cursor: pointer">
        <div class="logo-container">
          <img src="/icon.png" alt="Itda Logo" class="logo-icon" />
          <h1 class="h2">잇다</h1>
        </div>
        <p class="text-muted text-sm">오늘도 잇다와 함께</p>
      </div>

      <div class="card auth-card">
        <h2 class="h3">비밀번호 찾기</h2>
        <p class="text-muted text-sm mt-2">
          가입한 이메일을 입력하면 비밀번호 변경 화면으로 진행할 수 있어요.
        </p>

        <form class="auth-form" @submit.prevent="handleSubmit" novalidate>
          <div class="form-group">
            <label class="form-label required">이메일</label>
            <div class="input-icon-wrapper">
              <Mail class="w-5 h-5 input-icon" />
              <input
                ref="emailInput"
                v-model="form.email"
                type="email"
                class="form-input with-icon"
                placeholder="your@email.com"
                required
                @input="emailError = ''"
              />
            </div>
            <div v-if="emailError" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ emailError }}</span>
            </div>
          </div>

          <button
            type="submit"
            class="btn btn-primary w-full btn-lg"
            :disabled="isLoading"
          >
            <Loader2 v-if="isLoading" class="w-5 h-5 animate-spin" />
            <span>{{ isLoading ? '처리 중...' : '비밀번호 찾기' }}</span>
            <ArrowRight v-if="!isLoading" class="w-5 h-5" />
          </button>

          <div class="text-center mt-4">
            <a class="link text-sm" @click.prevent="router.push('/auth')">로그인으로 돌아가기</a>
          </div>
        </form>
      </div>

      <p class="auth-footer">© 2026 ITDA. All rights reserved.</p>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.auth-dot-pattern {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: var(--rose-canvas);
  z-index: -1;
  pointer-events: none;
}

.auth-dot-pattern::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, #FFD6E5 1.25px, transparent 1.5px);
  background-size: 40px 40px;
  animation: dot-move 5s linear infinite;
}

@keyframes dot-move {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 30px 30px;
  }
}

.auth-container {
  width: 100%;
  max-width: 420px;
  padding: 2rem;
}

.auth-logo {
  text-align: center;
  margin-bottom: 2rem;
}

.auth-logo .logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.auth-logo .logo-container h1 {
  margin: 1rem 0 0 0 !important;
  line-height: 1;
  display: inline-block;
}

.auth-logo .logo-icon {
  width: 64px;
  height: 64px;
  object-fit: contain;
  margin: 0;
}

.auth-card {
  padding: 2rem;
}

.auth-card:hover {
  transform: none;
}

.auth-form {
  margin-top: 1.5rem;
}

.input-icon-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--gray-400);
  pointer-events: none;
}

.form-input.with-icon {
  padding-left: 2.75rem;
}

.field-tooltip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--gray-200);
  border-radius: 0.5rem;
  background: #fff;
  color: var(--gray-700);
  font-size: 0.75rem;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.field-tooltip-icon {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 0.35rem;
  background: #ffe8f2;
  color: var(--rose-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  flex: 0 0 auto;
}

.link {
  color: var(--rose-500);
  text-decoration: none;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}

.auth-footer {
  text-align: center;
  margin-top: 2rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}
</style>
