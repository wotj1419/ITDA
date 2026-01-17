<script setup lang="ts">
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  PlayCircle,
  Play,
  Pencil,
  Palette,
  Film,
  GitBranch,
  Users,
  Layers,
} from 'lucide-vue-next'

const stats = [
  { value: '1,200+', label: '크리에이터' },
  { value: '5,000+', label: '생성된 영상' },
  { value: '50K+', label: 'AI 이미지' },
]

const features = [
  {
    icon: Pencil,
    title: 'AI 시나리오 생성',
    description: '아이디어만 입력하면 AI가 씬별 시나리오를 자동으로 작성합니다.',
  },
  {
    icon: Palette,
    title: '스토리보드 생성',
    description: 'Gemini로 마스터 이미지와 다양한 앵글의 샷을 생성합니다.',
  },
  {
    icon: Film,
    title: 'AI 영상 변환',
    description: 'Veo 3.1로 스토리보드 이미지를 시네마틱 영상으로 변환합니다.',
  },
  {
    icon: GitBranch,
    title: '노드 기반 워크플로우',
    description: '마스터 → 그리드 → 샷 → 영상 흐름을 시각적으로 관리합니다.',
  },
  {
    icon: Users,
    title: '실시간 협업',
    description: 'WebRTC 화상통화로 팀원과 아이디어를 실시간으로 공유합니다.',
  },
  {
    icon: Layers,
    title: '타임라인 편집',
    description: '확정된 영상 클립을 조합하여 최종 영화를 완성합니다.',
  },
]

const workflowSteps = [
  { number: 1, title: '기획', description: '시나리오 작성 및\n캐릭터 설정' },
  { number: 2, title: '스토리보드', description: 'AI 이미지 생성 및\n샷 구성' },
  { number: 3, title: '영상 생성', description: 'Image-to-Video\nAI 변환' },
  { number: 4, title: '편집 & 완성', description: '타임라인 편집 및\n최종 병합' },
]

function scrollToDemo() {
  document.querySelector('.demo-video')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <div class="landing">
    <!-- Header -->
    <header class="landing-header">
      <RouterLink to="/" class="landing-logo">
        <div class="logo-icon"></div>
        <span class="logo-text">AI Movie Studio</span>
      </RouterLink>
      <nav class="landing-nav">
        <RouterLink to="/auth" class="nav-link">로그인</RouterLink>
        <RouterLink to="/auth" class="btn btn-primary">무료로 시작하기</RouterLink>
      </nav>
    </header>

    <!-- Hero Section -->
    <section class="hero-section">
      <!-- Decorative Background -->
      <div class="hero-bg-blob hero-bg-blob-1"></div>
      <div class="hero-bg-blob hero-bg-blob-2"></div>

      <div class="badge badge-rose hero-badge">🎬 v2.7 Rose - Now Available</div>

      <h1 class="hero-title">
        Turn your ideas into<br />AI-generated films
      </h1>

      <p class="hero-description">
        From script to screen — create stunning short films with the power of AI.
        No filming. No editing skills required.
      </p>

      <div class="cta-group">
        <RouterLink to="/auth" class="btn btn-primary btn-lg">
          무료로 시작하기
          <ArrowRight class="w-5 h-5" />
        </RouterLink>
        <button class="btn btn-secondary btn-lg" @click="scrollToDemo">
          <PlayCircle class="w-5 h-5" />
          데모 보기
        </button>
      </div>

      <!-- Demo Video -->
      <div class="demo-video">
        <img
          src="https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=1200&auto=format"
          alt="Demo"
        />
        <div class="demo-play-btn">
          <Play class="w-8 h-8 play-icon" />
        </div>
      </div>

      <!-- Social Proof -->
      <div class="social-proof">
        <div v-for="stat in stats" :key="stat.label" class="stat">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div class="section-header">
        <h2 class="h1">All-in-One AI Filmmaking</h2>
        <p class="text-muted section-subtitle">기획부터 완성까지, 하나의 플랫폼에서</p>
      </div>

      <div class="features-grid">
        <div v-for="feature in features" :key="feature.title" class="feature-card">
          <div class="feature-icon">
            <component :is="feature.icon" class="w-8 h-8 icon" />
          </div>
          <h3 class="h3 feature-title">{{ feature.title }}</h3>
          <p class="text-muted text-sm">{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <!-- Workflow Section -->
    <section class="workflow-section">
      <h2 class="h1">Simple 4-Step Workflow</h2>
      <p class="text-muted section-subtitle">아이디어에서 영화까지, 단 4단계</p>

      <div class="workflow-steps">
        <div v-for="step in workflowSteps" :key="step.number" class="workflow-step">
          <div class="workflow-step-number">{{ step.number }}</div>
          <h4 class="workflow-step-title">{{ step.title }}</h4>
          <p class="text-muted text-sm workflow-step-desc" v-html="step.description.replace('\n', '<br>')"></p>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <h2 class="h1">Ready to Create Your AI Film?</h2>
      <p class="text-muted cta-subtitle">지금 무료로 시작하고 첫 번째 AI 영화를 만들어보세요.</p>
      <RouterLink to="/auth" class="btn btn-primary btn-lg">
        무료로 시작하기
        <ArrowRight class="w-5 h-5" />
      </RouterLink>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="footer-logo">🎬 AI Movie Studio</div>
      <div class="footer-links">
        <a href="#">이용약관</a>
        <a href="#">개인정보처리방침</a>
        <a href="#">문의하기</a>
      </div>
      <p class="footer-copyright">© 2026 AI Movie Studio. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
}

/* Header */
.landing-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  z-index: 50;
}

.landing-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  color: var(--gray-900);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--rose-400), var(--rose-500));
  border-radius: 8px;
}

.logo-text {
  font-weight: 700;
}

.landing-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-link {
  color: var(--gray-600);
  text-decoration: none;
  font-size: 0.875rem;
}

.nav-link:hover {
  color: var(--gray-900);
}

/* Hero Section */
.hero-section {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 6rem 2rem 4rem;
  position: relative;
  overflow: hidden;
}

.hero-bg-blob {
  position: fixed;
  border-radius: 50%;
  filter: blur(100px);
  z-index: -1;
}

.hero-bg-blob-1 {
  top: -20%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: var(--rose-200);
  opacity: 0.5;
}

.hero-bg-blob-2 {
  bottom: -20%;
  right: -10%;
  width: 600px;
  height: 600px;
  background: var(--rose-300);
  opacity: 0.4;
  filter: blur(120px);
}

.hero-badge {
  margin-bottom: 1rem;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, var(--gray-900), var(--rose-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 1.25rem;
  color: var(--gray-500);
  max-width: 600px;
  margin-bottom: 2.5rem;
}

.cta-group {
  display: flex;
  gap: 1rem;
  margin-bottom: 3rem;
}

/* Demo Video */
.demo-video {
  max-width: 900px;
  width: 100%;
  aspect-ratio: 16/9;
  background: var(--gray-900);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  position: relative;
}

.demo-video img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.8;
}

.demo-play-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  transition: all 0.3s;
}

.demo-play-btn:hover {
  transform: translate(-50%, -50%) scale(1.1);
}

.play-icon {
  color: var(--rose-500);
}

/* Social Proof */
.social-proof {
  display: flex;
  gap: 4rem;
  margin-top: 4rem;
  padding: 2rem;
}

.stat {
  text-align: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--rose-500);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

/* Features Section */
.features-section {
  padding: 6rem 2rem;
  background: white;
}

.section-header {
  text-align: center;
  margin-bottom: 3rem;
}

.section-subtitle {
  font-size: 1.125rem;
  margin-top: 1rem;
}

.features-grid {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
}

.feature-card {
  text-align: center;
  padding: 2rem;
}

.feature-icon {
  width: 64px;
  height: 64px;
  background: var(--rose-50);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.feature-icon .icon {
  color: var(--rose-500);
}

.feature-title {
  margin-bottom: 0.75rem;
}

/* Workflow Section */
.workflow-section {
  padding: 6rem 2rem;
  text-align: center;
}

.workflow-steps {
  max-width: 900px;
  margin: 3rem auto 0;
  display: flex;
  justify-content: space-between;
  position: relative;
}

.workflow-steps::before {
  content: '';
  position: absolute;
  top: 30px;
  left: 15%;
  right: 15%;
  height: 2px;
  background: var(--rose-200);
}

.workflow-step {
  flex: 1;
  position: relative;
  z-index: 1;
}

.workflow-step-number {
  width: 60px;
  height: 60px;
  background: var(--rose-500);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 auto 1rem;
}

.workflow-step-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.workflow-step-desc {
  line-height: 1.4;
}

/* CTA Section */
.cta-section {
  padding: 6rem 2rem;
  text-align: center;
  background: linear-gradient(135deg, var(--rose-50), var(--rose-100));
}

.cta-subtitle {
  font-size: 1.125rem;
  margin: 1rem 0 2rem;
}

/* Footer */
.landing-footer {
  padding: 3rem 2rem;
  background: var(--gray-900);
  color: white;
  text-align: center;
}

.footer-logo {
  font-weight: 700;
  font-size: 1.25rem;
  margin-bottom: 1.5rem;
}

.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.footer-links a {
  color: var(--gray-400);
  text-decoration: none;
  font-size: 0.875rem;
}

.footer-links a:hover {
  color: white;
}

.footer-copyright {
  color: var(--gray-500);
  font-size: 0.75rem;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }

  .cta-group {
    flex-direction: column;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .workflow-steps {
    flex-direction: column;
    gap: 2rem;
  }

  .workflow-steps::before {
    display: none;
  }

  .social-proof {
    flex-direction: column;
    gap: 1.5rem;
  }
}
</style>
