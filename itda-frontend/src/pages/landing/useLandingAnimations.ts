import type { Ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import type { NodeCollabCursor, NodeCursorAnchorKey, NodeCursorAnchors } from './nodeCursor'

gsap.registerPlugin(ScrollTrigger)

interface SetupLandingAnimationsOptions {
  mainContainer: Ref<HTMLElement | null>
  nodeCollabCursors: NodeCollabCursor[]
  nodeCursorAnchors: NodeCursorAnchors
  pickNextNodeCursorAnchorKey: (currentKey: NodeCursorAnchorKey) => NodeCursorAnchorKey
}

type CleanupFn = () => void

const observeOnce = (
  container: HTMLElement,
  selector: string,
  init: () => CleanupFn | void,
  rootMargin = '160px',
): CleanupFn => {
  const target = container.querySelector<HTMLElement>(selector)
  if (!target) return () => {}

  let cleanup: CleanupFn | void
  let observer: IntersectionObserver | null = null
  let initialized = false

  const run = () => {
    if (initialized) return
    initialized = true
    observer?.disconnect()
    observer = null
    cleanup = init()
  }

  if (!('IntersectionObserver' in window)) {
    run()
    return () => cleanup?.()
  }

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (!entry?.isIntersecting) return
      run()
    },
    { rootMargin, threshold: 0.01 },
  )

  observer.observe(target)

  return () => {
    observer?.disconnect()
    observer = null
    cleanup?.()
  }
}

const animateSectionHeader = (header: HTMLElement) => {
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
}

export const setupLandingAnimations = ({
  mainContainer,
  nodeCollabCursors,
  nodeCursorAnchors,
  pickNextNodeCursorAnchorKey,
}: SetupLandingAnimationsOptions) => {
  const container = mainContainer.value
  if (!container) return () => {}

  const cleanupFns: CleanupFn[] = []

  const heroCtx = gsap.context(() => {
    const heroTimeline = gsap.timeline({ defaults: { ease: 'power3.out' } })
    heroTimeline
      .from('.hero-badge', { y: 30, opacity: 0, duration: 0.8, scale: 0.9 }, 0.3)
      .from('.hero-headline', { y: 50, opacity: 0, duration: 1 }, 0.5)
      .from('.hero-sub', { y: 30, opacity: 0, duration: 0.8 }, 0.8)
      .from('.hero-cta-group', { y: 30, opacity: 0, duration: 0.8 }, 1.0)
      .from('.hero-stats', { y: 20, opacity: 0, duration: 0.7 }, 1.2)
      .from('.app-mockup-wrapper', { y: 80, opacity: 0, scale: 0.95, duration: 1.2 }, 1.3)

    gsap.utils.toArray<HTMLElement>('.hero-orb').forEach((orb, index) => {
      gsap.to(orb, {
        y: index % 2 === 0 ? -35 : 35,
        x: index % 2 === 0 ? 20 : -20,
        scale: 1 + (index % 3) * 0.08,
        duration: 4 + index * 0.7,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1,
      })
    })

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
  }, container)

  cleanupFns.push(() => heroCtx.revert())

  cleanupFns.push(
    observeOnce(container, '.features-section', () => {
      const featuresCtx = gsap.context(() => {
        const header = container.querySelector<HTMLElement>('.features-section .section-header')
        if (header) animateSectionHeader(header)

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
      }, container)

      return () => featuresCtx.revert()
    }),
  )

  cleanupFns.push(
    observeOnce(container, '.workflow-section', () => {
      const workflowTitleCleanupFns: CleanupFn[] = []

      const workflowCtx = gsap.context(() => {
        const header = container.querySelector<HTMLElement>('.workflow-section .section-header')
        if (header) animateSectionHeader(header)

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

        const workflowTitles = gsap.utils.toArray<HTMLElement>('.wf-step-title')
        const workflowTitleWordGroups: HTMLElement[][] = []
        workflowTitles.forEach((title) => {
          const originalText = title.textContent?.trim()
          if (!originalText) return

          const fragment = document.createDocumentFragment()
          const parts = originalText.split(/(\s+)/)

          parts.forEach((part) => {
            if (/^\s+$/.test(part)) {
              fragment.appendChild(document.createTextNode(part))
              return
            }

            const word = document.createElement('span')
            word.className = 'wf-title-word'
            word.textContent = part
            fragment.appendChild(word)
          })

          title.textContent = ''
          title.appendChild(fragment)

          const words = title.querySelectorAll<HTMLElement>('.wf-title-word')
          if (!words.length) return

          const wordArray = Array.from(words)
          workflowTitleWordGroups.push(wordArray)
          gsap.set(wordArray, { yPercent: 100, opacity: 0 })

          workflowTitleCleanupFns.push(() => {
            title.textContent = originalText
          })
        })

        if (workflowTitleWordGroups.length > 0) {
          const workflowTitleTimeline = gsap.timeline({
            scrollTrigger: {
              trigger: '.workflow-section',
              start: 'top 72%',
              end: 'bottom 45%',
              scrub: 1,
            },
          })

          workflowTitleWordGroups.forEach((words, index) => {
            workflowTitleTimeline.to(
              words,
              {
                yPercent: 0,
                opacity: 1,
                duration: 0.62,
                stagger: 0.08,
                ease: 'power2.out',
              },
              index * 0.58,
            )
          })
        }

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
      }, container)

      return () => {
        workflowCtx.revert()
        workflowTitleCleanupFns.forEach((cleanup) => cleanup())
        workflowTitleCleanupFns.length = 0
      }
    }),
  )

  cleanupFns.push(
    observeOnce(container, '.node-edge-section', () => {
      let nodeFlowCleanup: CleanupFn | null = null

      const nodeCtx = gsap.context(() => {
        const header = container.querySelector<HTMLElement>('.node-edge-section .section-header')
        if (header) animateSectionHeader(header)

        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
        if (!reducedMotion) {
          const cursorAngles = [-7, -4, -6]
          gsap.utils.toArray<HTMLElement>('.collab-cursor').forEach((cursor, index) => {
            const cursorConfig = nodeCollabCursors[index]
            if (!cursorConfig) return

            let currentAnchorKey = cursorConfig.start
            gsap.set(cursor, {
              top: nodeCursorAnchors[currentAnchorKey].top,
              left: nodeCursorAnchors[currentAnchorKey].left,
              x: 0,
              y: 0,
              rotation: cursorAngles[index % cursorAngles.length],
            })

            const clickPulse = cursor.querySelector<HTMLElement>('.collab-cursor-click')
            if (clickPulse) gsap.set(clickPulse, { scale: 0.2, opacity: 0 })

            const pulseClick = () => {
              if (!clickPulse) return
              gsap.set(clickPulse, { scale: 0.2, opacity: 0.34 })
              gsap.to(clickPulse, {
                scale: 1.42,
                opacity: 0,
                duration: 0.62,
                ease: 'power1.out',
              })
            }

            const moveToNextAnchor = () => {
              const nextAnchorKey = pickNextNodeCursorAnchorKey(currentAnchorKey)
              const nextAnchor = nodeCursorAnchors[nextAnchorKey]
              const moveDuration = gsap.utils.random(2.8, 3.9)
              const pauseDuration = gsap.utils.random(1.8, 2.7)

              gsap.to(cursor, {
                top: nextAnchor.top,
                left: nextAnchor.left,
                duration: moveDuration,
                ease: 'sine.inOut',
                onComplete: () => {
                  currentAnchorKey = nextAnchorKey
                  pulseClick()
                  gsap.delayedCall(pauseDuration, moveToNextAnchor)
                },
              })
            }

            gsap.delayedCall(0.8 + index * 0.35, moveToNextAnchor)
          })

          const nodeFlowCards = ['.node-card--master', '.node-card--grid-a', '.node-card--shot-a', '.node-card--video-a']
            .map((selector) => document.querySelector<HTMLElement>(selector))
            .filter((card): card is HTMLElement => card !== null)

          const nodeFlowEdges = ['.edge-flow--master-grid', '.edge-flow--grid-shot', '.edge-flow--shot-video']
            .map((selector) => document.querySelector<SVGGElement>(selector))
            .filter((edge): edge is SVGGElement => edge !== null)

          if (nodeFlowCards.length === 4) {
            const nodeFlowSteps = [
              { cardIndex: 0, edgeIndex: 0 },
              { cardIndex: 1, edgeIndex: 1 },
              { cardIndex: 2, edgeIndex: 2 },
              { cardIndex: 3, edgeIndex: 2 },
            ]

            const clearNodeFlowState = () => {
              nodeFlowCards.forEach((card) => card.classList.remove('node-card--flow-active'))
              nodeFlowEdges.forEach((edge) => edge.classList.remove('edge-flow--active'))
            }

            const nodeImpulseTimeline = gsap.timeline({
              repeat: -1,
              repeatDelay: 1.25,
            })

            nodeFlowSteps.forEach(({ cardIndex, edgeIndex }) => {
              nodeImpulseTimeline.call(() => {
                clearNodeFlowState()
                nodeFlowCards[cardIndex]?.classList.add('node-card--flow-active')
                nodeFlowEdges[edgeIndex]?.classList.add('edge-flow--active')
              })
              nodeImpulseTimeline.to({}, { duration: 1.45 })
            })

            nodeImpulseTimeline.call(() => {
              clearNodeFlowState()
            })

            nodeFlowCleanup = () => {
              nodeImpulseTimeline.kill()
              clearNodeFlowState()
            }
          }
        }
      }, container)

      return () => {
        nodeCtx.revert()
        nodeFlowCleanup?.()
        nodeFlowCleanup = null
      }
    }),
  )

  cleanupFns.push(
    observeOnce(container, '.cta-section', () => {
      const ctaCtx = gsap.context(() => {
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

        gsap.utils.toArray<HTMLElement>('.cta-orb').forEach((orb, index) => {
          gsap.to(orb, {
            y: index % 2 === 0 ? -25 : 25,
            x: index % 2 === 0 ? 15 : -15,
            scale: 1 + (index % 2) * 0.1,
            duration: 4 + index * 1.5,
            ease: 'sine.inOut',
            yoyo: true,
            repeat: -1,
          })
        })
      }, container)

      return () => ctaCtx.revert()
    }),
  )

  return () => {
    cleanupFns.forEach((cleanup) => cleanup())
    cleanupFns.length = 0
  }
}
