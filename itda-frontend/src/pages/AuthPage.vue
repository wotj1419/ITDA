<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUIStore } from '../stores/ui'
import { Mail, Lock, Eye, EyeOff, ArrowRight, Loader2 } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUIStore()

// Tab state
const activeTab = ref<'login' | 'register'>('login')

// Form data
const loginForm = ref({
  email: '',
  password: '',
  rememberMe: false,
})

const registerForm = ref({
  name: '',
  email: '',
  password: '',
  passwordConfirm: '',
  agreeTerms: false,
})

const loginError = ref('')
const loginErrors = ref<{ email: string; password: string }>({
  email: '',
  password: '',
})

const registerErrors = ref<{
  name: string
  email: string
  password: string
  passwordConfirm: string
}>({
  name: '',
  email: '',
  password: '',
  passwordConfirm: '',
})

const loginEmailInput = ref<HTMLInputElement | null>(null)
const loginPasswordInput = ref<HTMLInputElement | null>(null)
const registerNameInput = ref<HTMLInputElement | null>(null)
const registerEmailInput = ref<HTMLInputElement | null>(null)
const registerPasswordInput = ref<HTMLInputElement | null>(null)
const registerPasswordConfirmInput = ref<HTMLInputElement | null>(null)

// Password visibility
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)

// Loading state
const isLoading = ref(false)

// Validation
const passwordsMatch = computed(() => {
  return registerForm.value.password === registerForm.value.passwordConfirm
})

const nameErrorMessage = computed(() => {
  if (!registerForm.value.name) return ''
  if (registerForm.value.name.length < 2) return '이름은 2글자 이상이어야 합니다.'
  if (!/^[가-힣a-zA-Z]+$/.test(registerForm.value.name)) return '이름을 정확히 입력하세요.'
  return ''
})

const isNameValid = computed(() => !nameErrorMessage.value)

const isEmailValid = computed(() => {
  if (!registerForm.value.email) return true
  // User logic: Allow .kr (2 chars) OR other TLDs with 3+ chars (e.g., .com, .net)
  // Rejects 2-char TLDs that are not .kr (e.g., .io, .us) based on user request
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+(\.kr|\.[a-zA-Z]{3,})$/
  return emailRegex.test(registerForm.value.email)
})

function validateLoginFields() {
  loginErrors.value.email = ''
  loginErrors.value.password = ''

  const emailInput = loginEmailInput.value
  if (emailInput && !emailInput.checkValidity()) {
    loginErrors.value.email = emailInput.validationMessage
  }

  const passwordInput = loginPasswordInput.value
  if (passwordInput && !passwordInput.checkValidity()) {
    loginErrors.value.password = passwordInput.validationMessage
  }

  return !loginErrors.value.email && !loginErrors.value.password
}

function validateRegisterFields() {
  registerErrors.value.name = ''
  registerErrors.value.email = ''
  registerErrors.value.password = ''
  registerErrors.value.passwordConfirm = ''

  const nameInput = registerNameInput.value
  if (nameInput && !nameInput.checkValidity()) {
    registerErrors.value.name = nameInput.validationMessage
  }

  const emailInput = registerEmailInput.value
  if (emailInput && !emailInput.checkValidity()) {
    registerErrors.value.email = emailInput.validationMessage
  }

  const passwordInput = registerPasswordInput.value
  if (passwordInput && !passwordInput.checkValidity()) {
    registerErrors.value.password = passwordInput.validationMessage
  }

  const passwordConfirmInput = registerPasswordConfirmInput.value
  if (passwordConfirmInput && !passwordConfirmInput.checkValidity()) {
    registerErrors.value.passwordConfirm = passwordConfirmInput.validationMessage
  }

  return (
    !registerErrors.value.name &&
    !registerErrors.value.email &&
    !registerErrors.value.password &&
    !registerErrors.value.passwordConfirm
  )
}

async function handleLogin() {
  if (isLoading.value) return

  if (!validateLoginFields()) {
    return
  }

  isLoading.value = true
  loginError.value = '' // Reset error
  try {
    await authStore.login({
      email: loginForm.value.email,
      password: loginForm.value.password,
    })
    uiStore.showToast({
      type: 'success',
      title: '로그인 성공',
      message: '환영합니다.',
    })
    router.push('/dashboard')
  } catch (error: any) {
    const status = error.response?.status
    if (status === 404) {
      loginError.value = '존재하지 않는 아이디입니다. 다시 확인해주세요.'
    } else {
      loginError.value = '아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요.'
    }
  } finally {
    isLoading.value = false
  }
}

const registerError = ref('')
const termsError = ref(false)

// ...

async function handleRegister() {
  if (isLoading.value) return

  // Reset errors
  registerError.value = ''
  termsError.value = false

  if (!validateRegisterFields()) {
    return
  }

  if (registerForm.value.name && !isNameValid.value) {
    // Already showing inline error via (registerForm.name && !isNameValid)
    return
  }

  if (registerForm.value.email && !isEmailValid.value) {
    return
  }

  if (!passwordsMatch.value) {
    // Already showing inline error via (registerForm.passwordConfirm && !passwordsMatch)
    return
  }

  if (!registerForm.value.agreeTerms) {
    termsError.value = true
    return
  }

  isLoading.value = true
  try {
    await authStore.signup({
      name: registerForm.value.name,
      email: registerForm.value.email,
      password: registerForm.value.password,
    })
    uiStore.showToast({
      type: 'success',
      title: '회원가입 완료',
      message: '로그인 해주세요.',
    })
    activeTab.value = 'login'
  } catch (error: any) {
    if (error.response?.status === 409) {
      registerError.value = '이미 가입된 이메일입니다.'
    } else {
      const errorMessage = error.response?.data?.message || '회원가입에 실패했습니다.'
      registerError.value = errorMessage
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <!-- From Uiverse.io by themrsami -->
    <div class="auth-dot-pattern"></div>

    <main class="auth-container">
      <!-- Logo -->
      <div class="auth-logo" @click="router.push('/')" style="cursor: pointer">
        <div class="logo-container">
          <img src="/icon.png" alt="Itda Logo" class="logo-icon" />
          <h1 class="h2">잇다</h1>
        </div>
        <p class="text-muted text-sm">오늘도 잇다와 함께</p>
      </div>

      <!-- Auth Card -->
      <div class="card auth-card">
        <!-- Tabs -->
        <div class="tabs">
          <button
            class="tab"
            data-testid="auth-tab-login"
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'"
          >
            로그인
          </button>
          <button
            class="tab"
            data-testid="auth-tab-register"
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'"
          >
            회원가입
          </button>
        </div>

        <!-- Login Form -->
        <form v-if="activeTab === 'login'" class="auth-form" @submit.prevent="handleLogin" novalidate>
          <div class="form-group">
            <label class="form-label required">이메일</label>
            <div class="input-icon-wrapper">
              <Mail class="w-5 h-5 input-icon" />
              <input
                ref="loginEmailInput"
                v-model="loginForm.email"
                type="email"
                class="form-input with-icon"
                placeholder="your@email.com"
                required
                @input="loginErrors.email = ''"
              />
            </div>
            <div v-if="loginErrors.email" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ loginErrors.email }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">비밀번호</label>
            <div class="input-icon-wrapper">
              <Lock class="w-5 h-5 input-icon" />
              <input
                ref="loginPasswordInput"
                v-model="loginForm.password"
                :type="showLoginPassword ? 'text' : 'password'"
                class="form-input with-icon with-right-icon"
                :class="{ 'input-error': loginError }"
                placeholder="비밀번호"
                required
                @input="loginError = ''; loginErrors.password = ''"
              />
              <button
                type="button"
                class="input-icon-right"
                @click="showLoginPassword = !showLoginPassword"
              >
                <component :is="showLoginPassword ? EyeOff : Eye" class="w-5 h-5" />
              </button>
            </div>
            <div v-if="loginErrors.password" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ loginErrors.password }}</span>
            </div>
          </div>

          <div v-if="loginError" class="form-error mb-4" style="text-align: center;">
            {{ loginError }}
          </div>

          <div class="flex items-center justify-between mb-6">
            <label class="form-check">
              <input v-model="loginForm.rememberMe" type="checkbox" class="form-checkbox" />
              <span class="form-check-label">로그인 상태 유지</span>
            </label>
            <router-link to="/auth/forgot" class="forgot-link">비밀번호 찾기</router-link>
          </div>

          <button
            type="submit"
            class="btn btn-primary w-full btn-lg"
            :disabled="isLoading"
            data-testid="auth-submit-login"
          >
            <Loader2 v-if="isLoading" class="w-5 h-5 animate-spin" />
            <span>{{ isLoading ? '로그인 중...' : '로그인' }}</span>
            <ArrowRight v-if="!isLoading" class="w-5 h-5" />
          </button>
        </form>

        <!-- Register Form -->
        <form v-else class="auth-form" @submit.prevent="handleRegister" novalidate>
          <div class="form-group">
            <label class="form-label required">이름</label>
            <input
              ref="registerNameInput"
              v-model="registerForm.name"
              type="text"
              class="form-input"
              :class="{ 'input-error': registerForm.name && !isNameValid }"
              placeholder="이름을 입력하세요."
              required
              @input="registerErrors.name = ''"
            />
            <div v-if="registerErrors.name" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ registerErrors.name }}</span>
            </div>
            <div v-if="registerForm.name && !isNameValid" class="form-error">
              {{ nameErrorMessage }}
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">이메일</label>
            <div class="input-icon-wrapper">
              <Mail class="w-5 h-5 input-icon" />
              <input
                ref="registerEmailInput"
                v-model="registerForm.email"
                type="email"
                class="form-input with-icon"
                :class="{ 'input-error': registerForm.email && !isEmailValid }"
                placeholder="your@email.com"
                required
                @input="registerErrors.email = ''"
              />
            </div>
            <div v-if="registerErrors.email" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ registerErrors.email }}</span>
            </div>
            <div v-if="registerForm.email && !isEmailValid && !registerErrors.email" class="form-error">
              유효한 이메일 주소를 입력해주세요.
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">비밀번호</label>
            <div class="input-icon-wrapper">
              <Lock class="w-5 h-5 input-icon" />
              <input
                ref="registerPasswordInput"
                v-model="registerForm.password"
                :type="showRegisterPassword ? 'text' : 'password'"
                class="form-input with-icon with-right-icon"
                placeholder="8자 이상 입력"
                required
                minlength="8"
                @input="registerErrors.password = ''"
              />
              <button
                type="button"
                class="input-icon-right"
                @click="showRegisterPassword = !showRegisterPassword"
              >
                <component :is="showRegisterPassword ? EyeOff : Eye" class="w-5 h-5" />
              </button>
            </div>
            <div v-if="registerErrors.password" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ registerErrors.password }}</span>
            </div>
            <div class="form-helper">영문, 숫자 포함 8자 이상</div>
          </div>

          <div class="form-group">
            <label class="form-label required">비밀번호 확인</label>
            <input
              ref="registerPasswordConfirmInput"
              v-model="registerForm.passwordConfirm"
              type="password"
              class="form-input"
              :class="{ 'input-error': registerForm.passwordConfirm && !passwordsMatch }"
              placeholder="비밀번호를 다시 입력하세요"
              required
              @input="registerErrors.passwordConfirm = ''"
            />
            <div v-if="registerErrors.passwordConfirm" class="field-tooltip">
              <span class="field-tooltip-icon">!</span>
              <span class="field-tooltip-text">{{ registerErrors.passwordConfirm }}</span>
            </div>
            <div v-if="registerForm.passwordConfirm && !passwordsMatch" class="form-error">
              비밀번호가 일치하지 않습니다.
            </div>
          </div>

          <div class="form-group mb-6">
            <label class="form-check">
              <input v-model="registerForm.agreeTerms" type="checkbox" class="form-checkbox" @change="termsError = false" />
              <span class="form-check-label">
                <a href="#" class="link">이용약관</a> 및
                <a href="#" class="link">개인정보처리방침</a>에 동의합니다.
              </span>
            </label>
            <div v-if="termsError" class="form-error">
              약관에 동의해주세요.
            </div>
          </div>

          <div v-if="registerError" class="form-error mb-4" style="text-align: center;">
            {{ registerError }}
          </div>

          <button
            type="submit"
            class="btn btn-primary w-full btn-lg"
            :disabled="isLoading"
            data-testid="auth-submit-register"
          >
            <Loader2 v-if="isLoading" class="w-5 h-5 animate-spin" />
            <span>{{ isLoading ? '가입 중...' : '회원가입' }}</span>
            <ArrowRight v-if="!isLoading" class="w-5 h-5" />
          </button>
        </form>
      </div>

      <!-- Footer -->
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

/* From Uiverse.io by themrsami */
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
  margin: 1rem 0 0 0 !important; /* Move down by 1rem */
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

/* Input with icon */
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

.form-input.with-right-icon {
  padding-right: 2.75rem;
}

.input-icon-right {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--gray-400);
  padding: 0.25rem;
}

.input-icon-right:hover {
  color: var(--gray-600);
}

/* Form check */
.form-check {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  cursor: pointer;
}

.form-checkbox {
  width: 1rem;
  height: 1rem;
  accent-color: var(--rose-500);
  margin-top: 0.125rem;
}

.form-check-label {
  font-size: 0.75rem;
  color: var(--gray-600);
  line-height: 1.4;
}

.forgot-link {
  font-size: 0.75rem;
  color: var(--rose-500);
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.link {
  color: var(--rose-500);
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

/* Form divider */
.form-divider {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin: 1.5rem 0;
}

.form-divider::before,
.form-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--rose-200);
}

.form-divider span {
  font-size: 0.75rem;
  color: var(--gray-500);
}

/* Field tooltip */
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

/* Error state */
.input-error {
  border-color: var(--error);
}

.input-error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.form-error {
  font-size: 0.75rem;
  color: var(--error);
  margin-top: 0.25rem;
}

/* Footer */
.auth-footer {
  text-align: center;
  margin-top: 2rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}
</style>
