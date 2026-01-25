<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ShieldAlert, ArrowLeft, LayoutDashboard } from 'lucide-vue-next'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/common/Button.vue'

const route = useRoute()
const router = useRouter()

const redirectPath = computed(() => {
  const raw = route.query.redirect
  if (typeof raw === 'string' && raw.startsWith('/')) {
    return raw
  }
  return '/dashboard'
})

const handleBack = () => {
  router.push(redirectPath.value)
}
</script>

<template>
  <DefaultLayout :show-collaborators="false" :show-share-button="false" :show-collab-button="false">
    <div class="access-denied">
      <div class="access-card">
        <div class="access-icon">
          <ShieldAlert />
        </div>
        <h1 class="access-title">접근 권한이 없습니다</h1>
        <p class="access-subtitle">
          이 페이지는 프로젝트 멤버만 접근할 수 있어요.
          초대를 받았는지 확인하거나 소유자에게 권한을 요청해 주세요.
        </p>

        <div class="access-actions">
          <Button variant="secondary" size="sm" @click="handleBack">
            <ArrowLeft class="icon-sm" />
            이전 페이지
          </Button>
          <RouterLink to="/dashboard">
            <Button variant="primary" size="sm">
              <LayoutDashboard class="icon-sm" />
              대시보드로
            </Button>
          </RouterLink>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.access-denied {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.access-card {
  width: min(560px, 100%);
  background: white;
  border-radius: 20px;
  border: 1px solid var(--rose-100);
  padding: 2.5rem;
  text-align: center;
  box-shadow: 0 12px 30px rgba(255, 133, 161, 0.12);
}

.access-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  margin: 0 auto 1.25rem;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffe1e8, #fff3f6);
  color: var(--rose-500);
}

.access-icon :deep(svg) {
  width: 28px;
  height: 28px;
}

.access-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gray-900);
  margin-bottom: 0.75rem;
}

.access-subtitle {
  color: var(--gray-600);
  line-height: 1.6;
  margin-bottom: 2rem;
}

.access-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
