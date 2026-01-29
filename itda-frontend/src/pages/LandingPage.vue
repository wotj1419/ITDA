<script setup lang="ts">
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  PlayCircle,
} from 'lucide-vue-next'

// GSAP Animation
import { onMounted, onUnmounted, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const mainContainer = ref<HTMLElement | null>(null)
const heroSection = ref<HTMLElement | null>(null)
const heroContent = ref<HTMLElement | null>(null)
const heroVideo = ref<HTMLElement | null>(null)
const heroVideoElement = ref<HTMLVideoElement | null>(null)
// const feature1Section = ref<HTMLElement | null>(null)
// const feature2Section = ref<HTMLElement | null>(null)

let ctx: gsap.Context
let isVideoPinned = false
let pinOffset = { top: 0, left: 0 }
let scaleTarget = { x: 1, y: 1 }

onMounted(() => {
  ctx = gsap.context(() => {
    const updatePinOffset = () => {
      const videoWrap = heroVideo.value
      const sectionEl = heroSection.value
      if (!videoWrap || !sectionEl) return
      const rect = videoWrap.getBoundingClientRect()
      const sectionRect = sectionEl.getBoundingClientRect()
      pinOffset = {
        top: rect.top - sectionRect.top,
        left: rect.left - sectionRect.left,
      }
      scaleTarget = {
        x: rect.width ? Math.max(1, rect.right / rect.width) : 1,
        y: rect.height ? Math.max(1, (window.innerHeight - rect.top) / rect.height) : 1,
      }
    }

    updatePinOffset()

    const clearPinnedStyles = () => {
      const videoWrap = heroVideo.value
      if (!videoWrap) return
      videoWrap.classList.remove('is-pinned')
      videoWrap.style.removeProperty('top')
      videoWrap.style.removeProperty('left')
      videoWrap.style.removeProperty('right')
    }

    const applyPinnedStyles = () => {
      const videoWrap = heroVideo.value
      if (!videoWrap) return
      updatePinOffset()
      videoWrap.style.top = `${pinOffset.top}px`
      videoWrap.style.left = `${pinOffset.left}px`
      videoWrap.style.right = 'auto'
      videoWrap.classList.add('is-pinned')
    }

    // 1. Hero Section Animation (Pin logic & Scale)
    const tlHero = gsap.timeline({
      scrollTrigger: {
        trigger: heroSection.value,
        start: 'top top',
        end: '+=150%',
        scrub: 1,
        pin: true,
        invalidateOnRefresh: true,
        onUpdate: (self) => {
          const videoEl = heroVideoElement.value
          const videoWrap = heroVideo.value
          const sectionEl = heroSection.value
          if (!videoEl || !videoWrap || !sectionEl) return

          const shouldPin = self.progress > 0.001

          if (shouldPin && !isVideoPinned) {
            applyPinnedStyles()
            isVideoPinned = true
          } else if (!shouldPin && isVideoPinned) {
            clearPinnedStyles()
            isVideoPinned = false
          }

          const currentScaleX = Number(gsap.getProperty(videoWrap, 'scaleX'))
          const currentScaleY = Number(gsap.getProperty(videoWrap, 'scaleY'))
          const epsilon = 0.02
          const shouldPlay =
            currentScaleX >= scaleTarget.x - epsilon &&
            currentScaleY >= scaleTarget.y - epsilon
          if (shouldPlay) {
            if (videoEl.paused && !videoEl.ended) {
              void videoEl.play().catch(() => {})
            }
            return
          }

          if (!videoEl.paused) {
            videoEl.pause()
          }
        },
        onRefresh: () => {
          updatePinOffset()
          const videoWrap = heroVideo.value
          if (isVideoPinned && videoWrap) {
            videoWrap.style.top = `${pinOffset.top}px`
            videoWrap.style.left = `${pinOffset.left}px`
          }
        },
        onLeave: () => {
          const videoEl = heroVideoElement.value
          if (!videoEl || videoEl.paused) return
          videoEl.pause()
        },
      },
    })

    // Init state for video
    // Initial state: small, positioned on the right
    gsap.set(heroVideo.value, { 
      width: '35vw', 
      height: 'auto', 
      aspectRatio: '16/9',
      borderRadius: '20px',
      x: 0,
      y: 0,
      xPercent: 0,
      yPercent: 0,
      scaleX: 1,
      scaleY: 1,
      transformOrigin: 'right top',
      opacity: 1,
      maxWidth: '800px'
    })

    // Animation sequences
    tlHero
      .to(
        heroContent.value,
        { x: -100, opacity: 0, duration: 1 }, 
        0,
      )
      .to(
        heroVideo.value,
        {
          scaleX: () => scaleTarget.x,
          scaleY: () => scaleTarget.y,
          borderRadius: '0px',
          opacity: 1,
          duration: 2.5,
        },
        '<',
      )

    // Feature Sections Animation
    const sections = gsap.utils.toArray('.feature-section') as HTMLElement[]
    sections.forEach((section) => {
      gsap.from(section.querySelectorAll('.fade-up'), {
        scrollTrigger: {
          trigger: section,
          start: 'top 80%',
          toggleActions: 'play none none reverse',
        },
        y: 50,
        opacity: 0,
        duration: 0.8,
        stagger: 0.2,
        ease: 'power2.out',
      })
    })

  }, mainContainer.value as Element) // Scope
})

onUnmounted(() => {
  ctx.revert() // Cleanup
})
</script>

<template>
  <div class="landing" ref="mainContainer">
    <!-- Header -->
    <header class="landing-header">
      <RouterLink to="/" class="landing-logo">
        <img src="/icon.png" alt="Logo" class="logo-icon" />
        <span class="logo-text">잇다</span>
      </RouterLink>
      <nav class="landing-nav">
        <!-- <RouterLink to="/auth" class="nav-link">로그인</RouterLink> -->
        <!-- <RouterLink to="/auth" class="btn btn-primary">무료로 시작하기</RouterLink> -->
      </nav>
    </header>

    <!-- Hero Section -->
    <section class="hero-section" ref="heroSection">
      <!-- Decorative Background -->
      <!-- <div class="hero-bg-blob hero-bg-blob-1"></div> -->
      <!-- <div class="hero-bg-blob hero-bg-blob-2"></div> -->

      <div class="hero-container">
        <!-- Left Text Content -->
        <div class="hero-content" ref="heroContent">
          <div class="badge badge-rose hero-badge mb-4">NEW VERSION 2.0 AVAILABLE</div>
          <h1 class="hero-title">
            Itda에서<br />
            모든 영화 제작을<br />
            <span class="text-highlight">한 번에.</span>
          </h1>
          <p class="hero-description">
            당신이 상상하는 시나리오 - Itda가 영상, 오디오, 3D 캐릭터까지 완벽하게 구현합니다. 복잡한 툴 없이, 오직 아이디어만으로 나만의 영화를 완성하세요.
          </p>
          <div class="hero-actions">
            <RouterLink to="/auth" class="btn btn-primary btn-lg">
              지금 시작하기 <ArrowRight class="w-5 h-5 ml-2" />
            </RouterLink>
            <button class="btn btn-outline btn-lg">
              <PlayCircle class="w-5 h-5 mr-2" /> 데모 영상 보기
            </button>
          </div>
        </div>

        <!-- Right Video Content -->
        <div class="hero-video-wrapper" ref="heroVideo">
           <video 
            class="hero-video-content"
            src="/web.firstpage.video.mp4" 
            muted 
            loop
            playsinline
            ref="heroVideoElement"
          ></video>
        </div>
      </div>
    </section>

    <!-- Section 2: Connect Ideas -->
    <section class="feature-section section-connect">
      <div class="container feature-container">
        <div class="feature-image-wrapper fade-up">
          <img src="/images/landing/node_feature.png" alt="Connect Ideas" class="feature-image" />
        </div>
        <div class="feature-content fade-up">
          <h2 class="h1 mb-6">아이디어를<br/>노드로 잇다</h2>
          <p class="feature-text">
            시나리오부터 영상까지, 영화 제작의 모든 단계를 하나의 캔버스에 펼칩니다.<br/><br/>
            잇다는 복잡한 과정을 눈에 보이는 노드 흐름으로 자연스럽게 연결합니다.
          </p>
        </div>
      </div>
    </section>

    <!-- Section 3: Collaborate -->
    <section class="feature-section section-collab bg-gray-50">
      <div class="container feature-container reverse">
        <div class="feature-content fade-up">
          <h2 class="h1 mb-6">같은 캔버스에서<br/>함께 만들다</h2>
          <p class="feature-text">
            작업 화면 위에서 바로 대화하고, 보고, 함께 결정하세요.<br/><br/>
            잇다는 협업을 노드 흐름 속에 자연스럽게 녹여냅니다.
          </p>
        </div>
        <div class="feature-image-wrapper fade-up">
          <img src="/images/landing/collab_feature.png" alt="Collaborate" class="feature-image" />
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="footer-logo">🎬 ITDA</div>
      <div class="footer-links">
        <a href="#">이용약관</a>
        <a href="#">개인정보처리방침</a>
        <a href="#">문의하기</a>
      </div>
      <p class="footer-copyright">© 2026 Itda. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  font-family: 'Pretendard', sans-serif; /* Setup standard font */
}

/* Header */
.landing-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0,0,0,0.05);
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
  object-fit: contain;
}

.logo-text {
  font-weight: 800;
  font-size: 1.25rem;
}

/* Hero Section */
.hero-section {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 0;
  background: #fff;
}

.hero-container {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
  max-width: 1400px;
  padding: 120px 4rem 0;
  gap: 4rem;
  height: 100vh;
}

.hero-content {
  flex: 1;
  text-align: left;
  z-index: 2;
  min-width: 0;
}

.hero-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--rose-600);
  background: var(--rose-50);
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
  border: 1px solid var(--rose-200);
  margin-bottom: 1rem;
}

.hero-title {
  font-size: 4.5rem; /* Large scale */
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 1.5rem;
  color: #111827;
  letter-spacing: -0.02em;
}

.text-highlight {
  color: var(--rose-500);
}

.hero-description {
  font-size: 1.125rem;
  color: #6B7280;
  line-height: 1.6;
  margin-bottom: 2.5rem;
  max-width: 500px;
}

.hero-actions {
  display: flex;
  gap: 1rem;
}

.btn-primary {
  background: var(--rose-500);
  color: white;
  border: none;
  font-weight: 700;
}
.btn-primary:hover {
  background: var(--rose-600);
}

.btn-outline {
  background: white;
  color: #374151;
  border: 1px solid #E5E7EB;
  font-weight: 600;
}
.btn-outline:hover {
  background: #F9FAFB;
}

/* Hero Video */
.hero-video-wrapper {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  z-index: 2;
  position: relative;
  max-width: 50%;
  margin-left: auto;
}

.hero-video-wrapper.is-pinned {
  position: absolute;
}


.hero-video-content {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* Connect / Collab Sections */
.feature-section {
  padding: 8rem 2rem;
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.feature-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  gap: 6rem;
}

.feature-container.reverse {
  /* No special grid reverse needed if we swap DOM order, but let's keep it flexible */
}

.feature-image-wrapper {
  position: relative;
}

.feature-image {
  width: 100%;
  height: auto;
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0,0,0,0.05);
}

.feature-content h2 {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: #111827;
}

.feature-text {
  font-size: 1.25rem;
  color: #4B5563;
  line-height: 1.7;
  white-space: pre-line;
}

/* Footer */
.landing-footer {
  padding: 3rem 2rem;
  background: #111827;
  color: white;
  text-align: center;
}

.footer-logo {
  font-weight: 700;
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
}

.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.footer-links a {
  color: #9CA3AF;
  text-decoration: none;
  font-size: 0.875rem;
}

.footer-copyright {
  color: #6B7280;
  font-size: 0.75rem;
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-container,
  .feature-container {
    grid-template-columns: 1fr;
    text-align: center;
    gap: 3rem;
    align-items: center;
  }

  .hero-content {
    text-align: center;
    order: 1;
  }
  
  .hero-video-wrapper {
    order: 2;
    margin-top: 2rem;
    justify-content: center;
    align-items: center;
    margin-left: 0;
    max-width: 100%;
  }

  .hero-actions {
    justify-content: center;
  }
  
  .hero-title {
    font-size: 3rem;
  }
  
  .feature-content h2 {
    font-size: 2.5rem;
  }
}
</style>
