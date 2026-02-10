<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Avatar from '../components/common/Avatar.vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const user = computed(() => authStore.user)
const isLoading = computed(() => authStore.isAuthenticated && !authStore.user)

const displayName = computed(() => user.value?.name || '이름 미등록')
const displayEmail = computed(() => user.value?.email || '이메일 미등록')
const displayAvatarUrl = computed(() => user.value?.profileImageUrl ?? undefined)

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (authStore.isAuthenticated && !authStore.user) {
    authStore.fetchMe()
  }
})
</script>

<template>
  <DefaultLayout>
    <div class="account-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">계정/설정</h1>
          <p class="page-description">계정 정보와 보안 설정을 관리합니다.</p>
        </div>
        <RouterLink to="/profile/edit" class="btn btn-primary">
          계정 정보 수정
        </RouterLink>
      </div>

      <div v-if="isLoading" class="loading-state">계정 정보를 불러오는 중...</div>

      <div v-else class="settings-grid">
        <section class="card settings-card">
          <div class="section-header">
            <h2 class="section-title">계정 정보</h2>
            <p class="section-desc">로그인에 사용되는 기본 정보입니다.</p>
          </div>

          <div class="account-summary">
            <Avatar
              :src="displayAvatarUrl"
              :alt="displayName"
              size="lg"
              :user-id="user?.id"
            />
            <div class="account-text">
              <div class="account-name">{{ displayName }}</div>
              <div class="account-email">{{ displayEmail }}</div>
            </div>
          </div>

          <div class="info-list">
            <div class="info-item">
              <span class="info-label">이메일</span>
              <span class="info-value">{{ displayEmail }}</span>
            </div>
          </div>
        </section>

        <section class="card settings-card">
          <div class="section-header">
            <h2 class="section-title">보안</h2>
            <p class="section-desc">비밀번호 재설정 및 세션을 관리합니다.</p>
          </div>

          <div class="security-actions">
            <RouterLink to="/auth/forgot" class="btn btn-secondary">
              비밀번호 재설정
            </RouterLink>
            <button type="button" class="btn btn-ghost" @click="handleLogout">
              로그아웃
            </button>
          </div>
          <p class="hint-text">
            비밀번호 재설정은 이메일 인증을 통해 진행됩니다.
          </p>
        </section>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.account-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 2rem;
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
  margin: 0.5rem 0 0;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.settings-card {
  padding: 1.5rem;
}

.section-header {
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.section-desc {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-top: 0.35rem;
}

.account-summary {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 12px;
  background: var(--gray-50);
  margin-bottom: 1.25rem;
}

.account-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.account-name {
  font-weight: 600;
  color: var(--gray-900);
}

.account-email {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.info-list {
  display: grid;
  gap: 0.75rem;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.875rem;
}

.info-label {
  color: var(--gray-500);
}

.info-value {
  color: var(--gray-900);
  font-weight: 500;
}

.security-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.hint-text {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

.loading-state {
  padding: 2rem;
  text-align: center;
  color: var(--gray-500);
}
</style>
