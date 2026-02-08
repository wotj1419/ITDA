import { ref } from 'vue'
import { gsap } from 'gsap'

export const useLandingInteractions = () => {
  const navBar = ref<HTMLElement | null>(null)
  const mobileMenuOpen = ref(false)
  const mockupRef = ref<HTMLElement | null>(null)
  const heroVideoRef = ref<HTMLVideoElement | null>(null)
  const heroVideoMuted = ref(true)

  const toggleMobileMenu = () => {
    mobileMenuOpen.value = !mobileMenuOpen.value
  }

  const closeMobileMenu = () => {
    mobileMenuOpen.value = false
  }

  const playHeroVideo = (videoElement: HTMLVideoElement | null) => {
    if (!videoElement) return
    videoElement.currentTime = 0
    videoElement.muted = heroVideoMuted.value
    videoElement.playsInline = true
    void videoElement.play().catch(() => {})
  }

  const toggleHeroVideoSound = () => {
    heroVideoMuted.value = !heroVideoMuted.value
    const videoElement = heroVideoRef.value
    if (!videoElement) return

    videoElement.muted = heroVideoMuted.value
    if (!heroVideoMuted.value) {
      videoElement.volume = 1
    }
    void videoElement.play().catch(() => {})
  }

  const scrollTo = (id: string) => {
    closeMobileMenu()
    const target = document.getElementById(id)
    if (!target) return

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const top = Math.max(0, target.getBoundingClientRect().top + window.scrollY - 24)
    window.scrollTo({ top, behavior: reducedMotion ? 'auto' : 'smooth' })
  }

  const onScroll = () => {
    if (!navBar.value) return

    if (window.scrollY > 50) {
      navBar.value.classList.add('nav-scrolled')
      return
    }
    navBar.value.classList.remove('nav-scrolled')
  }

  const onMockupMouseMove = (event: PointerEvent) => {
    if (!mockupRef.value) return
    if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return

    const rect = mockupRef.value.getBoundingClientRect()
    const x = (event.clientX - rect.left) / rect.width - 0.5
    const y = (event.clientY - rect.top) / rect.height - 0.5

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

  return {
    navBar,
    mobileMenuOpen,
    mockupRef,
    heroVideoRef,
    heroVideoMuted,
    toggleMobileMenu,
    closeMobileMenu,
    playHeroVideo,
    toggleHeroVideoSound,
    scrollTo,
    onScroll,
    onMockupMouseMove,
    onMockupMouseLeave,
  }
}
