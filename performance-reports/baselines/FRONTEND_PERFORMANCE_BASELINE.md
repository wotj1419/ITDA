# Frontend Performance Baseline

이 문서는 Vercel에 배포한 `itda-frontend`의 현재 프론트엔드 성능 측정 결과와, 성능이 잘 나온 부분의 코드 근거를 정리한 기준선 문서입니다.

추후 최적화 작업 후 같은 형식으로 개선 후 수치를 추가해 개선 전/후 비교 자료로 확장합니다.

## 1. 측정 환경

```txt
배포 환경: Vercel 정적 배포
측정 URL: https://itda-pi.vercel.app/
측정 도구: Chrome DevTools Lighthouse
측정 방식: Navigation
측정 대상: 랜딩 페이지 /
백엔드 연결: 없음
```

현재 배포는 프론트엔드 정적 파일만 Vercel에 배포한 상태입니다.

따라서 이번 측정은 다음 항목에 초점을 둡니다.

```txt
랜딩 페이지 초기 로딩
정적 리소스 전송량
JS/CSS 번들 로딩
폰트 로딩
hero video 로딩
Lighthouse Performance
Web Vitals Lab 지표
```

회원가입, 로그인, 대시보드, 프로젝트 상세, 에디터 등 백엔드 API가 필요한 플로우는 이번 측정 범위에서 제외했습니다.

## 2. Lighthouse 결과 요약

### Desktop

측정 파일:

```txt
performance-reports/measurements/before/lighthouse_desktop_before.json
```

| 항목 | 결과 |
| --- | ---: |
| Performance | 98 |
| Accessibility | 93 |
| Best Practices | 58 |
| SEO | 91 |
| FCP | 0.7s |
| LCP | 1.1s |
| TBT | 10ms |
| CLS | 0.008 |
| Speed Index | 0.9s |
| TTI | 1.1s |
| 총 요청 수 | 64 |
| 총 전송량 | 8.5MiB |

Desktop 기준으로 초기 렌더링 성능은 매우 우수합니다.

특히 다음 지표가 좋게 나왔습니다.

```txt
FCP 0.7s
LCP 1.1s
TBT 10ms
CLS 0.008
Performance 98
```

이는 데스크톱 중심의 AI 영상 제작 툴이라는 서비스 성격을 고려했을 때 포트폴리오의 주 지표로 사용하기 적합합니다.

### Mobile

측정 파일:

```txt
performance-reports/measurements/before/lighthouse_mobile_before.json
```

| 항목 | 결과 |
| --- | ---: |
| Performance | 69 |
| Accessibility | 93 |
| Best Practices | 58 |
| SEO | 91 |
| FCP | 4.9s |
| LCP | 5.1s |
| TBT | 0ms |
| CLS | 0.02 |
| Speed Index | 4.9s |
| TTI | 5.1s |
| 총 요청 수 | 63 |
| 총 전송량 | 8.5MiB |

Mobile 기준에서는 Performance가 69로 낮아졌습니다.

주요 원인은 JS 실행 병목이 아니라 네트워크 전송량과 렌더 차단 리소스입니다.

```txt
TBT: 0ms
CLS: 0.02
```

위 수치가 양호하므로, 모바일 성능 하락의 핵심은 무거운 JavaScript 실행보다 초기 리소스 로딩 비용으로 판단합니다.

## 3. Desktop / Mobile 비교

| 항목 | Mobile | Desktop |
| --- | ---: | ---: |
| Performance | 69 | 98 |
| FCP | 4.9s | 0.7s |
| LCP | 5.1s | 1.1s |
| TBT | 0ms | 10ms |
| CLS | 0.02 | 0.008 |
| Speed Index | 4.9s | 0.9s |
| 총 요청 수 | 63 | 64 |
| 총 전송량 | 8.5MiB | 8.5MiB |

정리하면 다음과 같습니다.

```txt
Desktop 기준 렌더링 성능은 우수하다.
JS 실행 차단 시간은 매우 낮다.
레이아웃 안정성도 좋다.
하지만 Desktop/Mobile 모두 총 전송량은 크다.
총 전송량의 대부분은 hero video인 scene-1.mp4에서 발생한다.
```

## 4. 리소스 전송량 분석

### Desktop 리소스 요약

| 유형 | 요청 수 | 전송량 |
| --- | ---: | ---: |
| Media | 1 | 7.8MiB |
| Font | 46 | 453.3KiB |
| Script | 9 | 172.7KiB |
| Stylesheet | 4 | 60.2KiB |
| Image / Other | 3 | 약 33.6KiB |
| Document | 1 | 559B |

### Mobile 리소스 요약

| 유형 | 요청 수 | 전송량 |
| --- | ---: | ---: |
| Media | 1 | 7.8MiB |
| Font | 45 | 442.9KiB |
| Script | 9 | 172.7KiB |
| Stylesheet | 4 | 45.7KiB |
| Image | 2 | 33.6KiB |
| Document | 1 | 709B |

### 가장 큰 리소스

| 순위 | 리소스 | 전송량 |
| ---: | --- | ---: |
| 1 | `/scene-1.mp4` | 7.8MiB |
| 2 | `/assets/index-BOT_VMMU.js` | 113.0KiB |
| 3 | `/icon.png` | 약 33.5KiB |
| 4 | `/assets/index-EluARvBU.js` | 28.9KiB |
| 5 | `/assets/LandingPage-D728y6iD.js` | 27.4KiB |
| 6 | `https://static.toss.im/tps/main.css` | 26.9KiB |
| 7 | `https://static.toss.im/tps/others.css` | 16.2KiB |

가장 큰 병목은 `scene-1.mp4`입니다.

현재 전체 전송량 8.5MiB 중 7.8MiB가 hero video 하나에서 발생합니다.

## 5. 성능이 잘 나온 부분

### 5.1 초기 JavaScript 실행 비용이 낮음

Desktop 기준:

```txt
TBT: 10ms
JavaScript execution time: 0.2s
```

Mobile 기준:

```txt
TBT: 0ms
JavaScript execution time: 0.8s
```

초기 로딩 시 JS가 메인 스레드를 오래 점유하지 않고 있습니다.

이는 route-level code splitting과 Vite production build의 영향으로 볼 수 있습니다.

### 5.2 레이아웃 안정성이 좋음

Desktop 기준:

```txt
CLS: 0.008
```

Mobile 기준:

```txt
CLS: 0.02
```

두 환경 모두 CLS가 낮습니다.

이미지, 영상, 카드 영역 등에 비율과 크기를 미리 잡아두는 방식이 화면 밀림을 줄이는 데 기여했습니다.

### 5.3 Desktop 초기 렌더링이 빠름

Desktop 기준:

```txt
FCP: 0.7s
LCP: 1.1s
Speed Index: 0.9s
```

LCP 대상은 랜딩 페이지의 hero headline입니다.

```txt
h1.hero-headline
"끊어지는 맥락은 잊다, 영상의 흐름을 잇다"
```

즉, 데스크톱 환경에서는 핵심 텍스트 콘텐츠가 빠르게 표시되고 있습니다.

## 6. 좋은 성능을 만든 코드 근거

### 6.1 Vue Router 기반 Route-Level Code Splitting

파일:

```txt
itda-frontend/src/router/index.ts
```

주요 페이지 컴포넌트를 동적 import로 분리했습니다.

```ts
component: () => import('../pages/LandingPage.vue')
component: () => import('../pages/AuthPage.vue')
component: () => import('../pages/DashboardPage.vue')
component: () => import('../pages/ProjectDetailPage.vue')
component: () => import('../pages/SceneEditPage.vue')
component: () => import('../pages/TimelinePage.vue')
```

효과:

```txt
랜딩 페이지 진입 시 에디터/타임라인 화면 코드를 함께 다운로드하지 않음
무거운 페이지를 route 단위 chunk로 분리
초기 JS 실행 비용 감소
TBT를 낮게 유지
```

현재 빌드 산출물 기준 큰 chunk는 다음과 같습니다.

```txt
SceneEditPage-BoTVbdtR.js      417,778 bytes
index-BOT_VMMU.js              318,604 bytes
ProjectDetailPage-DsKWMY8a.js   85,331 bytes
LandingPage-D728y6iD.js         70,704 bytes
TimelinePage-Co1aWkEG.js        33,564 bytes
```

`SceneEditPage`처럼 무거운 에디터 화면이 별도 chunk로 분리되어 있으므로, 랜딩 페이지 초기 로딩에 직접 포함되지 않습니다.

포트폴리오 작성 예시:

```txt
Vue Router의 route-level code splitting을 적용해 에디터, 타임라인 등 무거운 작업 화면을 초기 번들에서 분리했습니다.
그 결과 랜딩 페이지 초기 로딩에서 JS 실행 비용을 낮게 유지했고, Desktop 기준 TBT 10ms, Lighthouse Performance 98을 기록했습니다.
```

### 6.2 Vite Production Build 사용

파일:

```txt
itda-frontend/package.json
```

빌드 스크립트:

```json
"build": "vue-tsc -b && vite build"
```

효과:

```txt
TypeScript 타입 체크 후 production build 수행
정적 assets 해시 파일명 생성
페이지별 chunk 분리
배포 환경에서 JS/CSS 압축 전송
```

Lighthouse Desktop 기준:

```txt
index-BOT_VMMU.js
resource size: 311.1KiB
transfer size: 113.0KiB
```

즉, 실제 네트워크 전송량은 압축을 통해 줄어든 상태입니다.

포트폴리오 작성 예시:

```txt
Vite production build를 통해 페이지 단위 chunk와 압축 가능한 정적 assets 구조를 구성했고, 배포 환경에서 초기 JS 전송량을 줄였습니다.
```

### 6.3 IntersectionObserver 기반 LazyVideo 컴포넌트

파일:

```txt
itda-frontend/src/components/media/LazyVideo.vue
```

핵심 구현:

```ts
useIntersectionObserver(
  videoRef,
  (entries) => {
    if (!props.lazy) return
    const entry = entries[0]
    if (!entry) return
    isVisible.value = entry.isIntersecting
  },
  { rootMargin: '150px' }
)

const resolvedSrc = computed(() => {
  if (!props.src) return undefined
  if (!props.lazy) return props.src
  return isVisible.value ? props.src : undefined
})
```

효과:

```txt
영상이 화면 근처에 들어오기 전까지 src를 연결하지 않음
초기 진입 시 불필요한 영상 다운로드 방지
목록/타임라인/미리보기 화면에서 미디어 로딩 비용 절감
```

사용 예시:

```txt
itda-frontend/src/pages/project/sections/ScenesTab.vue
itda-frontend/src/components/timeline/ClipItem.vue
itda-frontend/src/components/editor/MiniTimeline.vue
itda-frontend/src/pages/project/sections/StoryTab.vue
```

`ScenesTab.vue`에서는 영상 미리보기에 `LazyVideo`와 `preload="none"`을 사용합니다.

```vue
<LazyVideo
  v-if="clip.contentUrl"
  class="preview-video"
  :src="clip.contentUrl"
  :poster="getClipThumbnail(scene, clip, index) || undefined"
  :play-on-hover="true"
  preload="none"
/>
```

포트폴리오 작성 예시:

```txt
영상 제작 서비스 특성상 미디어 리소스가 큰 문제를 고려해, IntersectionObserver 기반 LazyVideo 컴포넌트를 구현했습니다.
영상이 화면 근처에 들어오기 전까지 src를 연결하지 않고 preload를 none/metadata로 제어하여 목록 화면의 불필요한 미디어 다운로드를 줄였습니다.
```

### 6.4 영상 preload 전략 적용

파일:

```txt
itda-frontend/src/components/project/ProjectCard.vue
```

프로젝트 카드의 미리보기 영상은 썸네일이 있으면 즉시 preload하지 않습니다.

```vue
:preload="previewImageUrl ? 'none' : 'metadata'"
```

효과:

```txt
썸네일 이미지가 있는 경우 영상 파일 다운로드 지연
hover/focus 등 사용자 의도가 있을 때만 영상 재생
프로젝트 목록 화면에서 네트워크 낭비 감소
```

포트폴리오 작성 예시:

```txt
프로젝트 카드 미리보기 영상은 썸네일 존재 여부에 따라 preload를 none/metadata로 분기해, 목록 화면에서 영상 파일이 불필요하게 선로딩되지 않도록 제어했습니다.
```

### 6.5 영상 길이 메타데이터 캐싱

파일:

```txt
itda-frontend/src/stores/timeline.ts
```

핵심 구현:

```ts
const durationCache = new Map<string, number>()
const durationInflight = new Map<string, Promise<number | null>>()
```

영상 길이를 읽을 때 `preload = 'metadata'`만 사용합니다.

```ts
video.preload = 'metadata'
```

그리고 같은 URL에 대한 중복 요청을 방지합니다.

```ts
const cached = durationCache.get(url)
if (cached) return Promise.resolve(cached)

const inflight = durationInflight.get(url)
if (inflight) return inflight
```

효과:

```txt
영상 전체 파일을 다운로드하지 않고 metadata만 읽음
동일 URL에 대한 중복 duration 계산 방지
타임라인 화면에서 반복적인 미디어 메타데이터 요청 감소
```

포트폴리오 작성 예시:

```txt
타임라인 영상 길이 계산 시 preload=metadata만 사용하고, durationCache와 inflight Map으로 동일 URL의 중복 메타데이터 요청을 방지했습니다.
```

### 6.6 레이아웃 크기 사전 확보

파일:

```txt
itda-frontend/src/pages/LandingPage.vue
itda-frontend/src/components/project/ProjectCard.vue
```

랜딩 페이지의 영상 mockup 영역에는 비율을 미리 지정했습니다.

```css
aspect-ratio: 16 / 9;
```

프로젝트 카드 썸네일에도 비율을 지정했습니다.

```css
.card-thumbnail {
  aspect-ratio: 16 / 9;
}
```

효과:

```txt
이미지/영상 로딩 전에도 영역 높이를 미리 확보
리소스 로딩 후 레이아웃 밀림 감소
CLS를 낮게 유지
```

측정 결과:

```txt
Desktop CLS: 0.008
Mobile CLS: 0.02
```

포트폴리오 작성 예시:

```txt
영상과 썸네일 영역에 aspect-ratio를 적용해 리소스 로딩 전후의 레이아웃 이동을 줄였고, Desktop CLS 0.008 수준의 안정적인 레이아웃을 확보했습니다.
```

## 7. 현재 남아 있는 성능 개선 후보

현재 코드에서 성능 개선 여지가 가장 큰 부분은 랜딩 페이지 hero video입니다.

파일:

```txt
itda-frontend/src/pages/LandingPage.vue
```

현재 구현:

```ts
const selectedHeroVideo = '/scene-1.mp4'
```

```vue
<video
  ref="heroVideoRef"
  :src="selectedHeroVideo"
  :muted="heroVideoMuted"
  autoplay
  loop
  playsinline
  preload="auto"
  class="mockup-video"
></video>
```

문제:

```txt
scene-1.mp4 전송량: 7.8MiB
전체 전송량: 8.5MiB
```

즉, 랜딩 페이지 전체 전송량의 대부분이 hero video에서 발생합니다.

개선 후보:

```txt
preload="auto" 제거 또는 metadata/none으로 변경
poster 이미지 추가
사용자 스크롤/클릭 이후 영상 로딩
모바일에서는 정적 poster만 표시
영상 압축 또는 WebM/AV1 등 대체 포맷 검토
```

두 번째 개선 후보는 외부 Toss 폰트입니다.

파일:

```txt
itda-frontend/src/assets/styles/base.css
```

현재 구현:

```css
@import url('https://static.toss.im/tps/main.css');
@import url('https://static.toss.im/tps/others.css');
```

문제:

```txt
폰트 요청 수: 45~46개
폰트 전송량: 약 443~453KiB
외부 CSS가 render-blocking request로 잡힘
Best Practices에서 third-party cookies 이슈 발생
```

개선 후보:

```txt
사용하는 font-weight만 제한
자체 호스팅 WOFF2로 전환
font-display: swap 적용
시스템 폰트 fallback 사용 범위 확대
외부 @import 제거
```

## 8. 현재 포트폴리오 문장 초안

현재 측정 결과만 기준으로 작성할 수 있는 문장입니다.

```txt
Vercel 정적 배포 환경에서 데스크톱 중심 사용 시나리오를 기준으로 Lighthouse를 측정한 결과,
Performance 98, FCP 0.7s, LCP 1.1s, CLS 0.008을 기록했습니다.

Vue Router 기반 route-level code splitting으로 에디터와 타임라인 등 무거운 작업 화면을 초기 번들에서 분리했고,
Vite production build를 통해 페이지 단위 chunk와 압축 가능한 정적 assets 구조를 구성했습니다.

또한 IntersectionObserver 기반 LazyVideo 컴포넌트와 preload 제어를 통해 서비스 내부의 영상 미리보기 리소스가
초기 진입 시 불필요하게 다운로드되지 않도록 설계했습니다.
```

개선 후에는 아래 문장에 실제 개선 수치를 추가합니다.

```txt
초기 측정 결과 전체 전송량 8.5MiB 중 7.8MiB가 hero video에서 발생하는 병목을 확인했고,
preload 전략과 미디어 로딩 방식을 개선하여 전송량과 LCP를 개선했습니다.
```

## 9. 개선 전/후 비교 표

아직 개선 후 측정 전입니다.

최적화 후 같은 URL과 같은 Lighthouse 조건으로 다시 측정해 아래 표를 채웁니다.

### Desktop

| 항목 | 개선 전 | 개선 후 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 98 | - | - |
| FCP | 0.7s | - | - |
| LCP | 1.1s | - | - |
| TBT | 10ms | - | - |
| CLS | 0.008 | - | - |
| Speed Index | 0.9s | - | - |
| 총 요청 수 | 64 | - | - |
| 총 전송량 | 8.5MiB | - | - |
| Video 전송량 | 7.8MiB | - | - |
| Font 전송량 | 453.3KiB | - | - |
| JS 전송량 | 172.7KiB | - | - |

### Mobile

| 항목 | 개선 전 | 개선 후 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 69 | - | - |
| FCP | 4.9s | - | - |
| LCP | 5.1s | - | - |
| TBT | 0ms | - | - |
| CLS | 0.02 | - | - |
| Speed Index | 4.9s | - | - |
| 총 요청 수 | 63 | - | - |
| 총 전송량 | 8.5MiB | - | - |
| Video 전송량 | 7.8MiB | - | - |
| Font 전송량 | 442.9KiB | - | - |
| JS 전송량 | 172.7KiB | - | - |

## 10. 주의할 점

현재 문서의 수치는 Vercel 정적 배포 환경 기준입니다.

따라서 포트폴리오에는 다음처럼 표현하는 것이 정확합니다.

```txt
Vercel 정적 배포 환경에서 프론트엔드 랜딩 페이지 성능을 측정했다.
```

다음 표현은 현재 측정 근거와 맞지 않습니다.

```txt
Nginx 운영 서버 환경에서 측정했다.
백엔드 API 포함 실제 서비스 전체 플로우를 측정했다.
```

또한 "성능을 올렸다"라고 쓰려면 개선 전/후 수치가 필요합니다.

현재 단계에서는 다음 표현이 적절합니다.

```txt
현재 코드 구조가 Desktop 기준 Lighthouse Performance 98을 기록했으며,
그 원인을 route-level code splitting, 미디어 lazy loading, Vite build, layout stability 관점에서 분석했다.
```

최적화 후에는 개선 전/후 수치를 추가해 다음처럼 확장할 수 있습니다.

```txt
개선 전 전체 전송량 8.5MiB에서 개선 후 __MiB로 감소시켰고,
Mobile LCP를 5.1s에서 __s로 개선했다.
```
