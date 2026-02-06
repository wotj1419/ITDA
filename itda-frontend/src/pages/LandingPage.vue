<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import {
  Sparkles,
  Pencil,
  Palette,
  Film,
  GitBranch,
  Users,
  Layers,
  ArrowRight,
  Menu,
  X,
  Play,
  Zap,
  Star,
} from 'lucide-vue-next'

gsap.registerPlugin(ScrollTrigger)

/* ─── Data ─── */
const features = [
  {
    icon: Pencil,
    title: 'AI 시나리오 생성',
    description: '아이디어만 입력하면 AI가 씬별 시나리오를 자동으로 작성합니다.',
    span: 'span-2',
    accent: true,
    tag: 'GPT-4o',
  },
  {
    icon: Palette,
    title: '스토리보드 생성',
    description: 'Gemini로 마스터 이미지와 다양한 앵글의 샷을 생성합니다.',
    span: '',
    accent: false,
    tag: 'Gemini',
  },
  {
    icon: GitBranch,
    title: '노드 기반 워크플로우',
    description: '마스터 → 그리드 → 샷 → 영상 흐름을 시각적으로 관리합니다.',
    span: '',
    accent: false,
    tag: 'Visual',
  },
  {
    icon: Film,
    title: 'AI 영상 변환',
    description: 'Veo 3.1로 스토리보드 이미지를 시네마틱 영상으로 변환합니다.',
    span: '',
    accent: true,
    tag: 'Veo 3.1',
  },
  {
    icon: Users,
    title: '실시간 협업',
    description: 'WebRTC 화상통화로 팀원과 아이디어를 실시간으로 공유합니다.',
    span: '',
    accent: false,
    tag: 'WebRTC',
  },
  {
    icon: Layers,
    title: '타임라인 편집',
    description: '확정된 영상 클립을 조합하여 최종 영화를 완성합니다.',
    span: '',
    accent: false,
    tag: 'Editor',
  },
]

const workflowSteps = [
  { icon: Pencil, title: '기획', description: '시나리오 작성 및 캐릭터 설정' },
  { icon: Palette, title: '스토리보드', description: 'AI 이미지 생성 및 샷 구성' },
  { icon: Film, title: '영상 생성', description: 'Image-to-Video AI 변환' },
  { icon: Layers, title: '편집 & 완성', description: '타임라인 편집 및 최종 병합' },
]

const stats = [
  { value: '10x', label: '더 빠른 제작' },
  { value: '4단계', label: '간편한 워크플로우' },
  { value: '∞', label: '무한한 상상력' },
]

/* ─── Refs ─── */
const mainContainer = ref<HTMLElement | null>(null)
const navBar = ref<HTMLElement | null>(null)
const mobileMenuOpen = ref(false)

let ctx: gsap.Context

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const closeMobileMenu = () => {
  mobileMenuOpen.value = false
}

const scrollTo = (id: string) => {
  closeMobileMenu()
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' })
  }
}

/* ─── Scroll-based navbar ─── */
const onScroll = () => {
  if (!navBar.value) return
  if (window.scrollY > 50) {
    navBar.value.classList.add('nav-scrolled')
  } else {
    navBar.value.classList.remove('nav-scrolled')
  }
}

/* ─── Mouse tilt for mockup ─── */
const mockupRef = ref<HTMLElement | null>(null)
const onMockupMouseMove = (e: MouseEvent) => {
  if (!mockupRef.value) return
  const rect = mockupRef.value.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5
  gsap.to(mockupRef.value, {
    rotateY: x * 6,
    rotateX: -y * 4,
    duration: 0.6,
    ease: 'power2.out',
  })
}
const onMockupMouseLeave = () => {
  if (!mockupRef.value) return
  gsap.to(mockupRef.value, {
    rotateY: 0,
    rotateX: 2,
    duration: 0.8,
    ease: 'elastic.out(1, 0.5)',
  })
}

/* ─── GSAP ─── */
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })

  ctx = gsap.context(() => {
    /* Hero entrance timeline */
    const heroTl = gsap.timeline({ defaults: { ease: 'power3.out' } })
    heroTl
      .from('.hero-badge', { y: 30, opacity: 0, duration: 0.8, scale: 0.9 }, 0.3)
      .from('.hero-headline', { y: 50, opacity: 0, duration: 1 }, 0.5)
      .from('.hero-sub', { y: 30, opacity: 0, duration: 0.8 }, 0.8)
      .from('.hero-cta-group', { y: 30, opacity: 0, duration: 0.8 }, 1.0)
      .from('.hero-stats', { y: 20, opacity: 0, duration: 0.7 }, 1.2)
      .from('.app-mockup-wrapper', { y: 80, opacity: 0, scale: 0.95, duration: 1.2 }, 1.3)

    /* Hero aurora orbs floating — more dramatic movement */
    gsap.utils.toArray<HTMLElement>('.hero-orb').forEach((orb, i) => {
      gsap.to(orb, {
        y: i % 2 === 0 ? -35 : 35,
        x: i % 2 === 0 ? 20 : -20,
        scale: 1 + (i % 3) * 0.08,
        duration: 4 + i * 0.7,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1,
      })
    })

    /* Floating sparkle particles */
    gsap.utils.toArray<HTMLElement>('.sparkle-particle').forEach((p, i) => {
      gsap.to(p, {
        y: -30 - i * 10,
        x: (i % 2 === 0 ? 1 : -1) * (10 + i * 5),
        opacity: 0,
        duration: 2.5 + i * 0.5,
        ease: 'power1.out',
        repeat: -1,
        delay: i * 0.6,
      })
    })

    /* Hero mockup parallax scrub */
    gsap.to('.app-mockup-wrapper', {
      y: -80,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero-section',
        start: 'top top',
        end: 'bottom top',
        scrub: 1.5,
      },
    })

    /* Section headers reveal */
    gsap.utils.toArray<HTMLElement>('.section-header').forEach((header) => {
      gsap.from(header, {
        scrollTrigger: {
          trigger: header,
          start: 'top 85%',
          toggleActions: 'play none none reverse',
        },
        y: 40,
        opacity: 0,
        duration: 0.9,
        ease: 'power3.out',
      })
    })

    /* Features stagger with scale */
    const bentoCards = gsap.utils.toArray<HTMLElement>('.bento-card')
    gsap.set(bentoCards, { autoAlpha: 1 })
    gsap.from(bentoCards, {
      scrollTrigger: {
        trigger: '.features-section',
        start: 'top 75%',
        once: true,
        invalidateOnRefresh: true,
      },
      y: 70,
      autoAlpha: 0,
      scale: 0.95,
      duration: 0.9,
      stagger: 0.1,
      ease: 'power3.out',
      immediateRender: false,
    })

    /* Workflow steps stagger */
    gsap.from('.wf-step', {
      scrollTrigger: {
        trigger: '.workflow-section',
        start: 'top 75%',
        toggleActions: 'play none none reverse',
      },
      y: 50,
      opacity: 0,
      scale: 0.9,
      duration: 0.9,
      stagger: 0.15,
      ease: 'back.out(1.7)',
    })

    /* Workflow connector line scrub */
    gsap.fromTo(
      '.wf-progress-fill',
      { width: '0%' },
      {
        width: '100%',
        ease: 'none',
        scrollTrigger: {
          trigger: '.workflow-section',
          start: 'top 55%',
          end: 'bottom 55%',
          scrub: 1,
        },
      },
    )

    /* CTA section */
    gsap.from('.cta-section-inner', {
      scrollTrigger: {
        trigger: '.cta-section',
        start: 'top 85%',
        toggleActions: 'play none none reverse',
      },
      y: 50,
      opacity: 0,
      scale: 0.97,
      duration: 1,
      ease: 'power3.out',
    })

    /* CTA orbs — more dramatic */
    gsap.utils.toArray<HTMLElement>('.cta-orb').forEach((orb, i) => {
      gsap.to(orb, {
        y: i % 2 === 0 ? -25 : 25,
        x: i % 2 === 0 ? 15 : -15,
        scale: 1 + (i % 2) * 0.1,
        duration: 4 + i * 1.5,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1,
      })
    })

    /* Stat counters reveal */
    gsap.from('.stat-item', {
      scrollTrigger: {
        trigger: '.hero-stats',
        start: 'top 90%',
        toggleActions: 'play none none reverse',
      },
      y: 20,
      opacity: 0,
      duration: 0.6,
      stagger: 0.12,
      ease: 'power2.out',
    })
  }, mainContainer.value as Element)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  ctx.revert()
})
</script>

<template>
  <div class="landing" ref="mainContainer">
    <!-- ─── Navigation ─── -->
    <nav class="nav-bar" ref="navBar">
      <div class="nav-inner">
        <RouterLink to="/" class="nav-logo">
          <img src="/icon.png" alt="잇다" class="nav-logo-img" />
          <span class="nav-logo-text">잇다</span>
        </RouterLink>

        <div class="nav-links">
          <a href="#features" class="nav-anchor" @click.prevent="scrollTo('features')">기능</a>
          <a href="#workflow" class="nav-anchor" @click.prevent="scrollTo('workflow')">워크플로우</a>
          <RouterLink to="/auth" class="nav-anchor">로그인</RouterLink>
          <RouterLink to="/auth" class="btn-nav-cta">
            <Zap :size="14" />
            무료로 시작하기
          </RouterLink>
        </div>

        <button class="mobile-menu-btn" @click="toggleMobileMenu" aria-label="메뉴 열기">
          <Menu v-if="!mobileMenuOpen" :size="24" />
          <X v-else :size="24" />
        </button>
      </div>

      <Transition name="slide-down">
        <div v-if="mobileMenuOpen" class="mobile-menu">
          <a href="#features" class="mobile-link" @click.prevent="scrollTo('features')">기능</a>
          <a href="#workflow" class="mobile-link" @click.prevent="scrollTo('workflow')">워크플로우</a>
          <RouterLink to="/auth" class="mobile-link" @click="closeMobileMenu">로그인</RouterLink>
          <RouterLink to="/auth" class="btn-nav-cta mobile-cta" @click="closeMobileMenu">
            <Zap :size="14" />
            무료로 시작하기
          </RouterLink>
        </div>
      </Transition>
    </nav>

    <!-- ─── Hero Section ─── -->
    <section class="hero-section">
      <!-- Aurora mesh background -->
      <div class="hero-aurora"></div>
      <!-- Orbs -->
      <div class="hero-orb hero-orb-1"></div>
      <div class="hero-orb hero-orb-2"></div>
      <div class="hero-orb hero-orb-3"></div>
      <div class="hero-orb hero-orb-4"></div>
      <div class="hero-orb hero-orb-5"></div>
      <!-- Dot grid -->
      <div class="hero-dot-grid"></div>
      <!-- Floating sparkles -->
      <div class="sparkle-particle sp-1"><Star :size="10" /></div>
      <div class="sparkle-particle sp-2"><Star :size="8" /></div>
      <div class="sparkle-particle sp-3"><Sparkles :size="12" /></div>
      <div class="sparkle-particle sp-4"><Star :size="9" /></div>
      <div class="sparkle-particle sp-5"><Sparkles :size="10" /></div>
      <!-- Noise overlay -->
      <div class="noise-overlay"></div>

      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-glow"></span>
          <Sparkles :size="14" class="badge-icon" />
          <span>AI Movie Studio</span>
          <ArrowRight :size="12" class="badge-arrow" />
        </div>

        <h1 class="hero-headline">
          끊어지는 맥락은 잊다,<br />영상의 흐름을 <span class="brand-gradient">잇다</span>
        </h1>

        <p class="hero-sub">
          아이디어 하나로 시작하는 AI 영화 제작.<br class="mobile-br" />
          시나리오부터 최종 편집까지, 잇다가 함께합니다.
        </p>

        <div class="hero-cta-group">
          <RouterLink to="/auth" class="btn-hero-primary">
            <span class="btn-shimmer"></span>
            <span class="btn-content">
              지금 시작하기
              <ArrowRight :size="18" />
            </span>
          </RouterLink>
          <a href="#features" class="btn-hero-secondary" @click.prevent="scrollTo('features')">
            <Play :size="16" class="play-icon" />
            데모 보기
          </a>
        </div>

        <div class="hero-stats">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <div class="app-mockup-wrapper">
        <div class="mockup-glow"></div>
        <div class="mockup-ring"></div>
        <div
          class="app-mockup"
          ref="mockupRef"
          @mousemove="onMockupMouseMove"
          @mouseleave="onMockupMouseLeave"
        >
          <div class="mockup-border-gradient"></div>
          <div class="mockup-inner">
            <div class="mockup-bar">
              <div class="mockup-dots">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
              </div>
              <span class="mockup-title">잇다 — AI Film Studio</span>
              <div class="mockup-dots-spacer"></div>
            </div>
            <div class="mockup-screen">
              <video
                src="/web.firstpage.video.mp4"
                muted
                loop
                autoplay
                playsinline
                class="mockup-video"
              ></video>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── Features Section ─── -->
    <section class="features-section" id="features">
      <div class="section-container">
        <div class="section-header">
          <span class="section-label">
            <Sparkles :size="14" />
            Features
          </span>
          <h2 class="section-title">All-in-One AI Filmmaking</h2>
          <p class="section-sub">기획부터 완성까지, 하나의 플랫폼에서</p>
        </div>

        <div class="bento-grid">
          <div
            v-for="(feat, idx) in features"
            :key="idx"
            class="bento-card"
            :class="{ 'bento-span-2': feat.span === 'span-2', 'bento-accent': feat.accent }"
          >
            <div class="bento-shine"></div>
            <div class="bento-card-inner">
              <div class="bento-top">
                <div class="bento-icon">
                  <component :is="feat.icon" :size="22" />
                </div>
                <span class="bento-tag">{{ feat.tag }}</span>
              </div>
              <h3 class="bento-title">{{ feat.title }}</h3>
              <p class="bento-desc">{{ feat.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── Workflow Section ─── -->
    <section class="workflow-section" id="workflow">
      <div class="section-container">
        <div class="section-header">
          <span class="section-label">
            <Zap :size="14" />
            Workflow
          </span>
          <h2 class="section-title">아이디어에서 영화까지, 단 4단계</h2>
          <p class="section-sub">직관적인 파이프라인으로 누구나 영화 감독이 될 수 있습니다</p>
        </div>

        <div class="wf-track">
          <div class="wf-connector">
            <div class="wf-progress-fill"></div>
          </div>

          <div v-for="(step, idx) in workflowSteps" :key="idx" class="wf-step">
            <div class="wf-icon-wrapper">
              <div class="wf-icon-ring"></div>
              <div class="wf-icon-circle">
                <component :is="step.icon" :size="26" />
              </div>
            </div>
            <div class="wf-step-content">
              <span class="wf-step-label">Step {{ idx + 1 }}</span>
              <h4 class="wf-step-title">{{ step.title }}</h4>
              <p class="wf-step-desc">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── CTA Section ─── -->
    <section class="cta-section">
      <div class="cta-aurora"></div>
      <div class="cta-orb cta-orb-1"></div>
      <div class="cta-orb cta-orb-2"></div>
      <div class="cta-orb cta-orb-3"></div>
      <div class="noise-overlay"></div>

      <div class="cta-section-inner">
        <div class="cta-badge">
          <Sparkles :size="14" />
          지금 바로 시작하세요
        </div>
        <h2 class="cta-headline">
          당신의 이야기를<br /><span class="brand-gradient">AI 영화</span>로 만들어보세요
        </h2>
        <p class="cta-sub">무료로 시작하세요. 신용카드가 필요하지 않습니다.</p>
        <RouterLink to="/auth" class="btn-cta-primary">
          <span class="btn-shimmer"></span>
          <span class="btn-content">
            무료로 시작하기
            <ArrowRight :size="20" />
          </span>
        </RouterLink>
      </div>
    </section>

    <!-- ─── Footer ─── -->
    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand">
            <RouterLink to="/" class="footer-logo">
              <img src="/icon.png" alt="잇다" class="footer-logo-img" />
              <span class="footer-logo-text">잇다</span>
            </RouterLink>
            <p class="footer-tagline">AI로 만드는 새로운 영화 제작 경험</p>
          </div>

          <div class="footer-columns">
            <div class="footer-col">
              <h5 class="footer-col-title">제품</h5>
              <a href="#features" class="footer-link" @click.prevent="scrollTo('features')">기능 소개</a>
              <a href="#workflow" class="footer-link" @click.prevent="scrollTo('workflow')">워크플로우</a>
            </div>
            <div class="footer-col">
              <h5 class="footer-col-title">지원</h5>
              <a href="#" class="footer-link">이용약관</a>
              <a href="#" class="footer-link">개인정보처리방침</a>
              <a href="#" class="footer-link">문의하기</a>
            </div>
          </div>
        </div>

        <div class="footer-divider"></div>

        <div class="footer-bottom">
          <p class="footer-copyright">&copy; 2026 ITDA. All rights reserved.</p>
          <p class="footer-made">Made with AI, for filmmakers.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ─── Base ─── */
.landing {
  min-height: 100vh;
  background: #ffffff;
  font-family: 'Toss Product Sans', 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
  overflow-x: hidden;
  color: var(--gray-900);
}

/* ─── Navigation ─── */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: transparent;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-bottom: 1px solid transparent;
}

.nav-bar.nav-scrolled {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom-color: rgba(255, 133, 161, 0.15);
  box-shadow: 0 1px 24px rgba(255, 133, 161, 0.06);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-decoration: none;
  color: var(--gray-900);
}

.nav-logo-img {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

.nav-logo-text {
  font-weight: 900;
  font-size: 1.2rem;
  letter-spacing: -0.03em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.nav-anchor {
  color: var(--gray-500);
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 500;
  transition: color 0.25s ease;
  cursor: pointer;
  position: relative;
}

.nav-anchor::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--rose-500);
  border-radius: 1px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-anchor:hover {
  color: var(--gray-900);
}

.nav-anchor:hover::after {
  width: 100%;
}

.btn-nav-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 1.3rem;
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 0.85rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 12px rgba(255, 133, 161, 0.25);
}

.btn-nav-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(255, 133, 161, 0.4);
}

.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: var(--gray-700);
  cursor: pointer;
  padding: 0.25rem;
}

.mobile-menu {
  display: none;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem 2rem 1.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-bottom: 1px solid var(--rose-200);
}

.mobile-link {
  color: var(--gray-700);
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 500;
  padding: 0.6rem 0;
}

.mobile-cta {
  text-align: center;
  justify-content: center;
  margin-top: 0.5rem;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ─── Hero Section ─── */
.hero-section {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 8rem 2rem 5rem;
  overflow: hidden;
}

/* Aurora mesh gradient */
.hero-aurora {
  position: absolute;
  inset: -50%;
  z-index: 0;
  background:
    conic-gradient(from 120deg at 30% 40%, transparent 0deg, rgba(255, 133, 161, 0.06) 60deg, transparent 120deg),
    conic-gradient(from 280deg at 70% 60%, transparent 0deg, rgba(255, 200, 220, 0.08) 80deg, transparent 160deg);
  animation: aurora-rotate 20s linear infinite;
  filter: blur(60px);
}

@keyframes aurora-rotate {
  to { transform: rotate(360deg); }
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

.hero-orb-1 {
  top: -10%;
  left: -8%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, var(--rose-200), var(--rose-100));
  opacity: 0.5;
  filter: blur(100px);
}

.hero-orb-2 {
  top: 15%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--rose-300), var(--rose-200));
  opacity: 0.35;
  filter: blur(120px);
}

.hero-orb-3 {
  bottom: 15%;
  left: 5%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, var(--rose-100), transparent);
  opacity: 0.6;
  filter: blur(90px);
}

.hero-orb-4 {
  bottom: -8%;
  right: 15%;
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, var(--rose-200), transparent);
  opacity: 0.3;
  filter: blur(110px);
}

.hero-orb-5 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(255, 133, 161, 0.04), transparent 70%);
  opacity: 1;
  filter: blur(80px);
}

.hero-dot-grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image: radial-gradient(circle, var(--rose-300) 0.8px, transparent 0.8px);
  background-size: 28px 28px;
  mask-image: radial-gradient(ellipse 65% 55% at 50% 35%, black 15%, transparent 65%);
  -webkit-mask-image: radial-gradient(ellipse 65% 55% at 50% 35%, black 15%, transparent 65%);
  opacity: 0.35;
}

/* Sparkle particles */
.sparkle-particle {
  position: absolute;
  z-index: 1;
  color: var(--rose-400);
  opacity: 0.6;
  pointer-events: none;
}

.sp-1 { top: 18%; left: 12%; }
.sp-2 { top: 25%; right: 15%; }
.sp-3 { top: 45%; left: 8%; }
.sp-4 { bottom: 30%; right: 10%; }
.sp-5 { bottom: 20%; left: 20%; }

/* Noise texture */
.noise-overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 128px 128px;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  max-width: 820px;
  margin: 0 auto;
}

.hero-badge {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 1.1rem 0.45rem 0.85rem;
  background: rgba(255, 250, 252, 0.8);
  backdrop-filter: blur(12px);
  border: 1px solid var(--rose-200);
  border-radius: var(--radius-full);
  font-size: 0.78rem;
  font-weight: 650;
  color: var(--rose-600);
  margin-bottom: 2rem;
  overflow: hidden;
  cursor: default;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.hero-badge:hover {
  border-color: var(--rose-400);
  box-shadow: 0 0 20px rgba(255, 133, 161, 0.15);
}

.badge-glow {
  position: absolute;
  top: 50%;
  left: -20%;
  width: 40%;
  height: 200%;
  background: linear-gradient(90deg, transparent, rgba(255, 133, 161, 0.15), transparent);
  transform: translateY(-50%) rotate(15deg);
  animation: badge-glow-sweep 3s ease-in-out infinite;
}

@keyframes badge-glow-sweep {
  0%, 100% { left: -40%; }
  50% { left: 100%; }
}

.badge-icon {
  animation: badge-sparkle 2s ease-in-out infinite;
}

@keyframes badge-sparkle {
  0%, 100% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.15) rotate(8deg); }
}

.badge-arrow {
  opacity: 0.5;
}

.hero-headline {
  font-size: clamp(2.4rem, 5.5vw, 4.2rem);
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: -0.035em;
  color: var(--gray-900);
  margin-bottom: 1.5rem;
  word-break: keep-all;
}

.brand-gradient {
  background: linear-gradient(135deg, var(--rose-500) 0%, var(--rose-700) 50%, var(--rose-400) 100%);
  background-size: 300% 300%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradient-shift 4s ease infinite;
  position: relative;
}

@keyframes gradient-shift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-sub {
  font-size: clamp(1rem, 2vw, 1.15rem);
  color: var(--gray-500);
  line-height: 1.75;
  margin-bottom: 2.5rem;
  letter-spacing: -0.01em;
}

.hero-cta-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2.5rem;
}

/* Shimmer CTA button */
.btn-hero-primary {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 0.9rem 2.2rem;
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 1rem;
  font-weight: 700;
  text-decoration: none;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(255, 133, 161, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-hero-primary:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 32px rgba(255, 133, 161, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-shimmer {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  transform: skewX(-20deg);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { left: -100%; }
  50% { left: 150%; }
}

.btn-hero-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.9rem 2rem;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  color: var(--gray-700);
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-full);
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-hero-secondary:hover {
  border-color: var(--rose-300);
  background: rgba(255, 250, 252, 0.9);
  color: var(--rose-600);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(255, 133, 161, 0.12);
}

.btn-hero-secondary .play-icon {
  color: var(--rose-500);
}

/* Stats row */
.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
}

.stat-value {
  font-size: 1.4rem;
  font-weight: 900;
  letter-spacing: -0.03em;
  background: linear-gradient(135deg, var(--rose-500), var(--rose-700));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: 0.78rem;
  color: var(--gray-400);
  font-weight: 500;
}

/* ─── App Mockup ─── */
.app-mockup-wrapper {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 980px;
  margin: 3.5rem auto 0;
  perspective: 1400px;
}

.mockup-glow {
  position: absolute;
  inset: -60px;
  background: radial-gradient(ellipse at center, rgba(255, 133, 161, 0.12) 0%, transparent 65%);
  border-radius: var(--radius-2xl);
  z-index: -1;
  animation: glow-pulse 4s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.03); }
}

.mockup-ring {
  position: absolute;
  inset: -3px;
  border-radius: calc(var(--radius-xl) + 3px);
  background: conic-gradient(
    from 0deg,
    var(--rose-300),
    var(--rose-500),
    var(--rose-400),
    var(--rose-200),
    var(--rose-300)
  );
  z-index: -1;
  opacity: 0.5;
  animation: ring-spin 6s linear infinite;
}

@keyframes ring-spin {
  to { filter: hue-rotate(15deg); }
}

.app-mockup {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  transform: rotateX(2deg);
  transform-style: preserve-3d;
  transition: transform 0.1s;
  will-change: transform;
}

.mockup-border-gradient {
  position: absolute;
  inset: 0;
  border-radius: var(--radius-xl);
  padding: 1.5px;
  background: linear-gradient(135deg, var(--rose-300), var(--rose-500), var(--rose-300));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  z-index: 2;
  pointer-events: none;
}

.mockup-inner {
  background: #111113;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow:
    0 40px 80px rgba(0, 0, 0, 0.18),
    0 16px 40px rgba(0, 0, 0, 0.12),
    var(--shadow-xl);
}

.mockup-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #1a1a1e;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.mockup-dots {
  display: flex;
  align-items: center;
  gap: 7px;
}

.mockup-bar .dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
}

.mockup-bar .red { background: #ff5f57; }
.mockup-bar .yellow { background: #febc2e; }
.mockup-bar .green { background: #28c840; }

.mockup-title {
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.35);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.mockup-dots-spacer {
  width: 50px;
}

.mockup-screen {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #000;
}

.mockup-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ─── Features Section ─── */
.features-section {
  padding: 8rem 2rem;
  background: var(--rose-canvas, #fbfbfc);
  position: relative;
}

.section-container {
  max-width: 1100px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.section-label {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--rose-500);
  margin-bottom: 0.75rem;
  padding: 0.35rem 0.9rem;
  background: var(--rose-50);
  border: 1px solid var(--rose-200);
  border-radius: var(--radius-full);
}

.section-title {
  font-size: clamp(1.8rem, 4vw, 2.8rem);
  font-weight: 900;
  letter-spacing: -0.035em;
  color: var(--gray-900);
  margin-bottom: 0.75rem;
}

.section-sub {
  font-size: 1.05rem;
  color: var(--gray-400);
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.6;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.1rem;
}

.bento-card {
  position: relative;
  background: #fff;
  border: 1.5px solid var(--rose-200);
  border-radius: var(--radius-xl);
  padding: 2rem;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
  box-shadow: 0 2px 8px rgba(255, 133, 161, 0.06);
}

.bento-card:hover {
  transform: translateY(-8px);
  border-color: var(--rose-400);
  box-shadow: 0 20px 50px rgba(255, 133, 161, 0.18), 0 0 0 1px rgba(255, 133, 161, 0.1);
}

/* Shine sweep on hover */
.bento-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 133, 161, 0.06), transparent);
  transform: skewX(-15deg);
  transition: none;
  pointer-events: none;
}

.bento-card:hover .bento-shine {
  animation: shine-sweep 0.8s ease forwards;
}

@keyframes shine-sweep {
  to { left: 150%; }
}

.bento-card:hover .bento-icon {
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  color: #fff;
  transform: scale(1.1) rotate(-3deg);
  box-shadow: 0 6px 20px rgba(255, 133, 161, 0.3);
}

.bento-span-2 {
  grid-column: span 2;
}

.bento-accent {
  background: linear-gradient(135deg, var(--rose-50), #fff);
  border-color: var(--rose-300);
}

.bento-card-inner {
  position: relative;
  z-index: 1;
}

.bento-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.bento-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: var(--rose-50);
  color: var(--rose-500);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.bento-tag {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--rose-500);
  padding: 0.2rem 0.6rem;
  background: var(--rose-50);
  border: 1px solid var(--rose-200);
  border-radius: var(--radius-full);
  text-transform: uppercase;
}

.bento-title {
  font-size: 1.1rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--gray-900, #111);
  margin-bottom: 0.5rem;
}

.bento-desc {
  font-size: 0.88rem;
  color: var(--gray-500, #6b7280);
  line-height: 1.65;
}

/* ─── Workflow Section ─── */
.workflow-section {
  padding: 8rem 2rem;
  background: linear-gradient(180deg, var(--rose-50) 0%, #fff 100%);
  position: relative;
}

.wf-track {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-top: 4rem;
  gap: 1.5rem;
}

.wf-connector {
  position: absolute;
  top: 36px;
  left: 10%;
  right: 10%;
  height: 3px;
  background: var(--rose-100);
  border-radius: 2px;
  overflow: hidden;
  z-index: 0;
}

.wf-progress-fill {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500), var(--rose-600));
  border-radius: 2px;
  box-shadow: 0 0 12px rgba(255, 133, 161, 0.4);
}

.wf-step {
  flex: 1;
  text-align: center;
  position: relative;
  z-index: 1;
}

.wf-icon-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  margin: 0 auto 1.25rem;
}

.wf-icon-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px dashed var(--rose-200);
  opacity: 0;
  transition: all 0.4s ease;
  animation: none;
}

.wf-step:hover .wf-icon-ring {
  opacity: 1;
  animation: ring-rotate 8s linear infinite;
}

@keyframes ring-rotate {
  to { transform: rotate(360deg); }
}

.wf-icon-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--rose-200);
  color: var(--rose-500);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.wf-step:hover .wf-icon-circle {
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  border-color: var(--rose-500);
  color: #fff;
  transform: scale(1.1);
  box-shadow: 0 8px 24px rgba(255, 133, 161, 0.35);
}

.wf-step-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.wf-step-label {
  display: block;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--rose-400);
  margin-bottom: 0.35rem;
}

.wf-step-title {
  font-size: 1.05rem;
  font-weight: 750;
  letter-spacing: -0.02em;
  color: var(--gray-900);
  margin-bottom: 0.4rem;
}

.wf-step-desc {
  font-size: 0.82rem;
  color: var(--gray-400);
  line-height: 1.5;
  max-width: 160px;
  margin: 0 auto;
}

/* ─── CTA Section ─── */
.cta-section {
  position: relative;
  padding: 10rem 2rem;
  overflow: hidden;
  background: #fff;
}

.cta-aurora {
  position: absolute;
  inset: -30%;
  background:
    conic-gradient(from 200deg at 40% 50%, transparent 0deg, rgba(255, 133, 161, 0.08) 60deg, transparent 120deg),
    conic-gradient(from 40deg at 60% 50%, transparent 0deg, rgba(255, 200, 220, 0.06) 80deg, transparent 160deg);
  animation: aurora-rotate 25s linear infinite;
  filter: blur(50px);
}

.cta-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.cta-orb-1 {
  top: -15%;
  left: -8%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--rose-200), transparent);
  opacity: 0.4;
  filter: blur(100px);
}

.cta-orb-2 {
  bottom: -15%;
  right: -8%;
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, var(--rose-300), transparent);
  opacity: 0.3;
  filter: blur(120px);
}

.cta-orb-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255, 133, 161, 0.05), transparent 70%);
  filter: blur(80px);
}

.cta-section-inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 700px;
  margin: 0 auto;
}

.cta-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.9rem;
  background: var(--rose-50);
  border: 1px solid var(--rose-200);
  border-radius: var(--radius-full);
  font-size: 0.78rem;
  font-weight: 650;
  color: var(--rose-600);
  margin-bottom: 1.5rem;
}

.cta-headline {
  font-size: clamp(2rem, 4.5vw, 3.2rem);
  font-weight: 900;
  letter-spacing: -0.035em;
  color: var(--gray-900);
  margin-bottom: 1rem;
  word-break: keep-all;
  line-height: 1.2;
}

.cta-sub {
  font-size: 1.05rem;
  color: var(--gray-400);
  margin-bottom: 2.5rem;
  line-height: 1.6;
}

.btn-cta-primary {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 1.1rem 2.8rem;
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 1.15rem;
  font-weight: 700;
  text-decoration: none;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 6px 28px rgba(255, 133, 161, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-cta-primary:hover {
  transform: translateY(-3px) scale(1.03);
  box-shadow: 0 12px 40px rgba(255, 133, 161, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* ─── Footer ─── */
.site-footer {
  background: #0a0a0c;
  color: #fff;
  padding: 4rem 2rem 2rem;
  position: relative;
}

.site-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--rose-500), transparent);
  opacity: 0.3;
}

.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.footer-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 3rem;
}

.footer-brand {
  flex-shrink: 0;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  color: #fff;
  margin-bottom: 0.7rem;
}

.footer-logo-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.footer-logo-text {
  font-weight: 900;
  font-size: 1.1rem;
  letter-spacing: -0.02em;
}

.footer-tagline {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.35);
}

.footer-columns {
  display: flex;
  gap: 4rem;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.footer-col-title {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 0.3rem;
}

.footer-link {
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.25s ease;
  cursor: pointer;
}

.footer-link:hover {
  color: #fff;
}

.footer-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.07);
  margin: 2.5rem 0 1.5rem;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-copyright {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.3);
}

.footer-made {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.2);
  font-style: italic;
}

/* ─── Responsive ─── */
@media (max-width: 1024px) {
  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .bento-span-2 {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }

  .mobile-menu-btn {
    display: block;
  }

  .mobile-menu {
    display: flex;
  }

  .hero-section {
    padding: 6rem 1.5rem 3rem;
  }

  .hero-cta-group {
    flex-direction: column;
    gap: 0.75rem;
  }

  .btn-hero-primary,
  .btn-hero-secondary {
    width: 100%;
    justify-content: center;
  }

  .mobile-br {
    display: none;
  }

  .hero-stats {
    gap: 1.5rem;
  }

  .stat-value {
    font-size: 1.1rem;
  }

  .app-mockup {
    transform: none !important;
  }

  .wf-track {
    flex-direction: column;
    gap: 2rem;
  }

  .wf-connector {
    top: 0;
    bottom: 0;
    left: 36px;
    right: auto;
    width: 3px;
    height: 100%;
  }

  .wf-progress-fill {
    width: 100% !important;
    height: 0;
  }

  .wf-step {
    text-align: left;
    display: flex;
    align-items: flex-start;
    gap: 1.25rem;
  }

  .wf-icon-wrapper {
    margin: 0;
    flex-shrink: 0;
  }

  .wf-step-content {
    align-items: flex-start;
  }

  .wf-step-desc {
    margin: 0;
  }

  .footer-top {
    flex-direction: column;
    gap: 2rem;
  }

  .footer-columns {
    gap: 3rem;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 0.5rem;
    text-align: center;
  }

  .features-section,
  .workflow-section {
    padding: 5rem 1.5rem;
  }

  .cta-section {
    padding: 6rem 1.5rem;
  }
}

@media (max-width: 640px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }

  .bento-span-2 {
    grid-column: span 1;
  }

  .hero-stats {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>
