# Frontend Performance Optimization Plan

이 문서는 현재 Vercel 배포 기준 Lighthouse 결과를 바탕으로, UI 변화를 최소화하면서 프론트엔드 성능을 개선하기 위한 작업 계획입니다.

목표는 현재 UI와 사용자 경험을 유지하면서 초기 네트워크 비용, 렌더 차단 리소스, 정적 리소스 전송량을 줄이는 것입니다.

## 1. 현재 기준선

측정 환경:

```txt
배포 환경: Vercel
측정 URL: https://itda-pi.vercel.app/
측정 대상: 랜딩 페이지 /
측정 도구: Chrome DevTools Lighthouse
측정 방식: Navigation
```

현재 측정 결과:

| 항목 | Desktop | Mobile |
| --- | ---: | ---: |
| Performance | 98 | 69 |
| FCP | 0.7s | 4.9s |
| LCP | 1.1s | 5.1s |
| TBT | 10ms | 0ms |
| CLS | 0.008 | 0.02 |
| Speed Index | 0.9s | 4.9s |
| 총 요청 수 | 64 | 63 |
| 총 전송량 | 8.5MiB | 8.5MiB |

핵심 해석:

```txt
Desktop 기준 렌더링 성능은 우수함
JS 실행 병목은 거의 없음
레이아웃 안정성도 좋음
하지만 전체 전송량이 큼
전송량 대부분이 랜딩 hero video에서 발생함
```

## 2. 주요 병목

### 2.1 Hero Video

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

측정 결과:

```txt
scene-1.mp4 전송량: 7.8MiB
전체 전송량: 8.5MiB
```

문제:

```txt
preload="auto"로 인해 영상이 초기 로딩 단계에서 크게 다운로드될 수 있음
전체 전송량의 대부분을 hero video가 차지함
Mobile 환경에서 FCP/LCP 저하에 영향을 줄 수 있음
```

### 2.2 Toss 외부 폰트

파일:

```txt
itda-frontend/src/assets/styles/base.css
```

현재 구현:

```css
@import url('https://static.toss.im/tps/main.css');
@import url('https://static.toss.im/tps/others.css');
```

측정 결과:

```txt
폰트 요청 수: 45~46개
폰트 전송량: 약 443~453KiB
외부 CSS가 render-blocking request로 잡힘
Best Practices에서 third-party cookies 이슈 발생
```

문제:

```txt
외부 CSS @import가 렌더링을 막을 수 있음
폰트 요청 수가 많음
외부 도메인 의존성으로 네트워크 변수가 커짐
폰트 변경은 UI 인상 변화 가능성이 있어 신중히 접근 필요
```

### 2.3 Icon Image

파일:

```txt
itda-frontend/public/icon.png
```

문제:

```txt
실제 표시 크기보다 원본 이미지가 큼
Mobile Lighthouse에서 약 33KiB 절감 가능성이 잡힘
```

우선순위는 hero video와 font보다 낮습니다.

## 3. 개선 원칙

이번 최적화에서는 다음 원칙을 지킵니다.

```txt
UI 레이아웃 변경 금지
문구 변경 금지
색상/브랜드 톤 변경 금지
사용자가 보는 화면 변화 최소화
기능 동작 유지
성능 지표는 같은 URL, 같은 Lighthouse 조건으로 재측정
```

즉, 시각적 디자인을 바꾸는 것이 아니라 로딩 전략과 리소스 전달 방식을 개선합니다.

## 4. 작업 계획

## Phase 1. Hero Video preload 전략 변경

목표:

```txt
UI 변화 없이 hero video의 초기 선다운로드 비용을 줄임
```

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
```

변경 내용:

```diff
- preload="auto"
+ preload="metadata"
```

이유:

```txt
preload="auto"는 브라우저가 영상 데이터를 적극적으로 미리 다운로드할 수 있음
preload="metadata"는 영상 길이/메타데이터 중심으로 로드를 유도함
autoplay, loop, muted, playsinline은 유지하므로 UI 변화 가능성이 낮음
```

검증 항목:

```txt
랜딩 페이지 UI가 기존과 동일하게 보이는지
hero video가 정상 재생되는지
sound toggle이 정상 동작하는지
scene-1.mp4 초기 전송량이 줄었는지
총 전송량이 줄었는지
Desktop/Mobile Lighthouse 변화
```

측정 파일명 예시:

```txt
performance-reports/measurements/after/phase1/lighthouse_desktop_after_phase1.json
performance-reports/measurements/after/phase1/lighthouse_mobile_after_phase1.json
```

예상 효과:

```txt
초기 네트워크 점유 감소
Mobile Performance/FCP/LCP 개선 가능
Desktop 성능 유지
```

주의:

```txt
브라우저 정책상 autoplay 영상은 metadata 설정 후에도 일부 데이터를 다운로드할 수 있음
따라서 Network 탭에서 실제 scene-1.mp4 전송량을 반드시 확인해야 함
```

## Phase 2. Hero Video poster 추가

목표:

```txt
영상이 로드되기 전에도 동일한 시각 품질 유지
preload를 더 낮출 수 있는 기반 마련
```

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
itda-frontend/public/scene-1-poster.webp
```

작업 내용:

```txt
scene-1.mp4의 첫 프레임 또는 대표 프레임을 이미지로 추출
WebP 또는 압축 PNG로 저장
video 태그에 poster 속성 추가
```

예상 코드:

```vue
<video
  ref="heroVideoRef"
  :src="selectedHeroVideo"
  poster="/scene-1-poster.webp"
  :muted="heroVideoMuted"
  autoplay
  loop
  playsinline
  preload="metadata"
  class="mockup-video"
></video>
```

이유:

```txt
영상 다운로드 전에도 mockup 영역이 비어 보이지 않음
preload="none"까지 검토할 수 있는 시각적 안전장치가 됨
사용자 체감 UI 변화를 줄일 수 있음
```

검증 항목:

```txt
poster와 기존 영상 첫 화면의 시각 차이가 거의 없는지
영상 재생 전 깜빡임이 없는지
LCP/FCP에 부정적 영향이 없는지
poster 파일 크기가 과도하지 않은지
```

예상 효과:

```txt
영상 로딩 전 시각적 안정성 유지
추후 preload="none" 적용 가능성 확보
```

## Phase 3. Hero Video lazy loading 검토

목표:

```txt
초기 페이지 진입 시 scene-1.mp4 요청 자체를 지연
```

적용 조건:

```txt
Phase 1, Phase 2 후에도 Mobile 전송량/LCP 개선이 부족할 때 진행
```

방법:

```txt
IntersectionObserver로 app mockup 영역이 화면 근처에 들어왔을 때 video src 연결
또는 기존 LazyVideo 구조를 랜딩 hero video에 맞게 재사용
```

주의:

```txt
hero video가 첫 화면 하단에 걸쳐 있으므로 lazy loading 적용 시 영상 재생 타이밍이 달라질 수 있음
UI 변화 가능성이 Phase 1보다 큼
반드시 스크린샷과 실제 재생 타이밍을 비교해야 함
```

검증 항목:

```txt
초기 Network에서 scene-1.mp4 요청이 사라지는지
mockup 영역 진입 시 영상이 자연스럽게 로드되는지
사용자 체감상 빈 영역이 생기지 않는지
```

## Phase 3.5. Mobile TBT / JS 실행 비용 개선

목표:

```txt
Phase 3 이후 Mobile Lighthouse에서 증가한 TBT와 long task를 줄임
초기 렌더링에 반드시 필요하지 않은 랜딩 애니메이션 초기화를 지연
UI 레이아웃과 디자인은 유지하면서 JavaScript 실행 시점만 조정
```

Phase 3.5 추가 이유:

```txt
기존 계획은 Phase 3 hero video lazy loading 이후 Phase 4 Toss 폰트 최적화로 넘어가는 흐름이었습니다.
하지만 Phase 3 측정 결과, 원래 가장 큰 병목이었던 scene-1.mp4 초기 전송 문제는 해결되었고 새로운 병목이 드러났습니다.

해결된 병목:
scene-1.mp4 초기 요청: 7.8MiB -> 0
Mobile 총 전송량: 8.48MiB -> 0.79MiB

새로 드러난 병목:
Mobile Performance: 69 -> 63
Mobile TBT: 0ms -> 350ms
LandingPage JS long task: 257ms
index JS long task: 196ms

즉, 병목이 네트워크 전송량에서 초기 JavaScript 실행 비용으로 이동했습니다.
Phase 4의 Toss 폰트 최적화는 LCP와 render-blocking 개선에는 도움이 될 수 있지만,
현재 점수 하락의 직접 원인인 TBT와 long task를 먼저 해결하는 작업은 아닙니다.

따라서 측정 결과에 따라 우선순위를 조정해 Phase 3과 Phase 4 사이에 Phase 3.5를 추가했습니다.
번호를 3.5로 둔 이유는 기존 Phase 4, 5, 6 계획을 유지하면서 Phase 3 결과로 새로 발견된 병목을 중간에 반영하기 위해서입니다.
```

포트폴리오 기록 관점:

```txt
정해진 최적화 순서를 그대로 진행한 것이 아니라,
Lighthouse와 Network 측정 결과를 기반으로 병목이 video 전송량에서 JavaScript 실행 비용으로 이동했음을 확인했습니다.
그 결과 다음 개선 우선순위를 Toss 폰트 최적화가 아니라 Mobile TBT와 long task 개선으로 재조정했습니다.
```

적용 조건:

```txt
Phase 3 측정 후 scene-1.mp4 초기 요청은 제거되었지만 Mobile Performance 점수가 하락할 때 진행
Mobile TBT가 의미 있게 증가하거나 long task가 LandingPage JS에서 발생할 때 진행
```

Phase 3 측정에서 확인된 문제:

```txt
Mobile Performance: 69 -> 63
Mobile TBT: 0ms -> 350ms
LandingPage JS long task: 257ms
index JS long task: 196ms
Main-thread work: 4.7s
```

작업 파일:

```txt
itda-frontend/src/pages/landing/useLandingAnimations.ts
itda-frontend/src/pages/LandingPage.vue
```

우선 분석할 내용:

```txt
setupLandingAnimations가 mount 직후 어떤 DOM query와 GSAP timeline을 실행하는지 확인
첫 화면 hero 영역에 필요한 애니메이션과 아래쪽 섹션 애니메이션을 분리할 수 있는지 확인
features/workflow/node-edge/cta 등 화면 아래 섹션의 애니메이션을 지연 초기화할 수 있는지 확인
```

개선 방법:

```txt
1. hero 영역에 필요한 애니메이션만 mount 직후 실행
2. below-the-fold 섹션 애니메이션은 IntersectionObserver 또는 ScrollTrigger 진입 시점에 초기화
3. 초기 렌더링에 필요 없는 무거운 DOM query/GSAP timeline 생성을 지연
4. requestIdleCallback을 사용할 수 있으면 idle 시점에 보조 애니메이션 초기화
5. requestIdleCallback 미지원 브라우저는 setTimeout fallback 사용
```

주의:

```txt
애니메이션 모양, 순서, 사용자 체감 UI는 유지해야 함
초기 렌더링 점수 개선을 위해 애니메이션 자체를 삭제하지 않음
prefers-reduced-motion 동작을 유지해야 함
ScrollTrigger/observer 정리 함수가 누락되지 않도록 unmount cleanup 확인
```

검증 항목:

```txt
Mobile TBT가 감소했는지
LandingPage JS long task 시간이 줄었는지
FCP/LCP/Speed Index가 악화되지 않았는지
랜딩 페이지 애니메이션이 기존처럼 동작하는지
스크롤 후 features/workflow/node-edge 섹션 애니메이션이 정상 실행되는지
Desktop 성능이 유지되는지
```

측정 파일명 예시:

```txt
performance-reports/measurements/after/phase4/lighthouse_desktop_after_phase4.json
performance-reports/measurements/after/phase4/lighthouse_mobile_after_phase4.json
```

## Phase 4. Toss 폰트 최적화

목표:

```txt
외부 폰트 CSS와 과도한 폰트 요청을 줄임
렌더 차단 시간을 줄임
```

단, 폰트는 UI 인상에 직접 영향을 주므로 신중하게 진행합니다.

### 4.1 현재 폰트 유지 상태에서 분석

먼저 실제로 어떤 font-weight가 사용되는지 확인합니다.

확인 대상:

```txt
font-weight: 400
font-weight: 500
font-weight: 600
font-weight: 700
font-weight: 800 이상
```

확인 이유:

```txt
현재 Toss 폰트 CSS는 많은 weight/문자 subset을 로드함
실제 필요한 weight만 줄이면 UI 변화 없이 요청 수를 줄일 수 있음
```

### 4.2 자체 호스팅 WOFF2 검토

방법:

```txt
실제 사용하는 weight만 WOFF2로 준비
public/fonts 아래에 배치
@font-face 직접 선언
font-display: swap 적용
```

예시:

```css
@font-face {
  font-family: 'ITDA Sans';
  src: url('/fonts/itda-sans-regular.woff2') format('woff2');
  font-weight: 400;
  font-display: swap;
}

@font-face {
  font-family: 'ITDA Sans';
  src: url('/fonts/itda-sans-bold.woff2') format('woff2');
  font-weight: 700;
  font-display: swap;
}
```

주의:

```txt
폰트 라이선스 확인 필요
Toss 폰트와 다르면 UI 인상이 달라질 수 있음
스크린샷 비교 필수
```

### 4.3 시스템 폰트 전환은 후순위

시스템 폰트 전환은 성능 효과가 크지만 UI 변화 가능성이 큽니다.

따라서 현재 목표가 "UI 변화 최소화"인 동안에는 후순위로 둡니다.

## Phase 4.5. Hero Video Poster 초기 요청 최적화

추가 이유:

```txt
Phase 4 이후 실제 Lighthouse 결과를 확인한 결과,
다음 계획이었던 icon.png보다 scene-1-poster.webp의 초기 전송량이 더 컸습니다.

Phase 4 Mobile 기준:
scene-1-poster.webp: 약 112KB
icon.png: 약 34KB
unused JS 예상 절감: 약 73KB

따라서 기존 계획의 Phase 5 icon 최적화보다,
hero video poster 초기 요청을 먼저 점검하는 것이 더 효과적인 순서입니다.
```

목표:

```txt
scene-1.mp4 lazy loading은 유지하면서,
초기 화면에서 반드시 필요하지 않은 poster 이미지 요청도 지연할 수 있는지 검토합니다.
UI 시각 변화는 최소화하고, mockup 영역 근처 진입 시점에 poster/video가 자연스럽게 준비되도록 합니다.
```

현재 상태:

```vue
<video
  :src="shouldLoadHeroVideo ? selectedHeroVideo : undefined"
  poster="/scene-1-poster.webp"
  preload="none"
/>
```

문제:

```txt
video src는 shouldLoadHeroVideo가 true가 될 때까지 연결되지 않지만,
poster 속성은 초기 렌더링 시점부터 연결되어 scene-1-poster.webp가 초기 요청됩니다.
이 poster는 약 112KB로, 현재 남은 앱 자체 이미지 리소스 중 가장 큽니다.
```

개선 후보:

```txt
1. poster 속성도 shouldLoadHeroVideo 또는 별도 shouldLoadHeroPoster 상태와 연결
2. mockup 영역이 viewport 근처에 들어오기 전까지 poster 요청 지연
3. poster 지연 중에는 기존 mockup-screen 배경색과 layout을 유지해 레이아웃 변화 방지
4. IntersectionObserver rootMargin을 사용해 사용자가 mockup에 도달하기 전 미리 poster/video 준비
```

예상 코드 방향:

```vue
<video
  :src="shouldLoadHeroVideo ? selectedHeroVideo : undefined"
  :poster="shouldLoadHeroPoster ? '/scene-1-poster.webp' : undefined"
  preload="none"
/>
```

주의:

```txt
poster는 mockup 영역의 시각 안정성을 위해 추가한 리소스입니다.
너무 늦게 로드하면 사용자가 mockup 영역을 볼 때 검은 배경이 먼저 보일 수 있습니다.
따라서 UI 변화를 최소화하려면 rootMargin을 두고 화면 진입 전에 poster를 먼저 연결해야 합니다.
```

검증 항목:

```txt
초기 Network에서 scene-1-poster.webp 요청이 사라지는지
mockup 영역 근처 진입 시 poster 또는 video가 자연스럽게 로드되는지
scene-1.mp4 초기 요청 제거가 유지되는지
FCP/LCP/Speed Index가 악화되지 않는지
CLS가 증가하지 않는지
스크린샷 또는 녹화 기준으로 mockup 영역 시각 변화가 허용 가능한지
```

측정 파일명:

```txt
performance-reports/measurements/after/phase4_5/lighthouse_desktop_after_phase4_5.json
performance-reports/measurements/after/phase4_5/lighthouse_mobile_after_phase4_5.json
```

예상 효과:

```txt
초기 전송량 약 112KB 감소 가능
scene-1.mp4 0 bytes 유지
Mobile total transfer 감소 가능
```

다만 poster는 LCP 대상은 아니므로 Lighthouse 점수 상승 폭은 크지 않을 수 있습니다.

## Phase 5. Icon image 최적화

목표:

```txt
시각 변화 없이 icon.png 전송량 감소
```

방법:

```txt
현재 icon.png의 표시 크기 확인
표시 크기에 맞는 작은 PNG/WebP 생성
또는 SVG 로고가 가능하면 SVG 사용
```

검증 항목:

```txt
로고가 흐려지지 않는지
색감이 달라지지 않는지
Mobile Lighthouse image delivery 경고가 줄어드는지
```

우선순위:

```txt
낮음
```

이유:

```txt
절감 가능량이 약 33KiB 수준으로, hero video나 font 대비 효과가 작음
```

## Phase 6. 전역 CSS / Unused JS 점검

Phase 4 이후 계획 수정:

```txt
기존 계획에서는 전역 CSS와 unused JS를 함께 점검하는 것으로 작성했지만,
Phase 4 Mobile Lighthouse 결과에서는 unused-css-rules 점수가 1로 문제 없음이 확인됐습니다.

반면 unused-javascript에서는 약 73KiB 절감 가능성이 표시됐습니다.
따라서 Phase 6은 전역 CSS 분리보다 초기 entry JS에 불필요한 모듈이 포함되는지 확인하는 작업을 우선합니다.
```

목표:

```txt
초기 공통 번들에 불필요한 스타일/코드가 들어가는지 확인
```

확인 대상:

```txt
itda-frontend/src/main.ts
```

현재 전역 import:

```ts
import './assets/styles/variables.css'
import './assets/styles/base.css'
import './assets/styles/utilities.css'
import '@vue-flow/node-resizer/dist/style.css'
import './assets/styles/node-canvas.css'
```

검토 포인트:

```txt
랜딩 페이지에서 Vue Flow 관련 CSS가 필요한지
node-canvas.css를 에디터 라우트로 분리할 수 있는지
CSS 분리 시 UI 회귀가 발생하지 않는지
```

주의:

```txt
전역 CSS 분리는 UI 회귀 가능성이 있음
현재 Desktop TBT가 10ms로 매우 낮으므로 우선순위는 높지 않음
```

## 5. 최종 작업 순서

UI 변화를 최소화하기 위해 다음 순서로 진행합니다.

```txt
1. Phase 1: hero video preload="metadata" 변경
2. Vercel 재배포
3. Lighthouse Desktop/Mobile 재측정
4. Network에서 scene-1.mp4 전송량 확인
5. Phase 2: poster 추가
6. 재측정
7. 필요 시 Phase 3: hero video lazy loading 검토
8. Phase 4: Toss 폰트 최적화 검토
9. Phase 5: icon 이미지 최적화
10. Phase 6: 전역 CSS/unused JS 점검
```

Phase 3 측정 이후 업데이트된 실제 다음 순서:

```txt
1. Phase 3 결과 확인: scene-1.mp4 초기 요청 제거 여부 확인
2. Mobile TBT / long task 증가 여부 확인
3. Phase 3.5: 랜딩 애니메이션 초기화 지연 및 JS 실행 비용 개선
4. Vercel Preview 재배포
5. Lighthouse Desktop/Mobile 재측정
6. TBT, long task, Main-thread work 개선 여부 확인
7. 이후 Phase 4: Toss 폰트 최적화 검토
```

가장 먼저 진행할 작업:

```txt
LandingPage.vue의 preload="auto"를 preload="metadata"로 변경
```

이 작업은 다음 이유로 첫 번째에 배치합니다.

```txt
코드 변경이 작음
UI 변화 가능성이 낮음
가장 큰 병목인 scene-1.mp4와 직접 연결됨
개선 전/후 비교 효과가 명확할 가능성이 큼
문제가 생겨도 되돌리기 쉬움
```

## 6. 재측정 방법

각 Phase 후 동일 조건으로 Lighthouse를 다시 측정합니다.

측정 조건:

```txt
URL: https://itda-pi.vercel.app/
Mode: Navigation
Device: Desktop, Mobile 각각 측정
Categories: Performance, Accessibility, Best Practices, SEO
```

저장 파일명:

```txt
performance-reports/measurements/after/phase1/lighthouse_desktop_after_phase1.json
performance-reports/measurements/after/phase1/lighthouse_mobile_after_phase1.json
performance-reports/measurements/after/phase2/lighthouse_desktop_after_phase2.json
performance-reports/measurements/after/phase2/lighthouse_mobile_after_phase2.json
performance-reports/measurements/after/phase3/lighthouse_desktop_after_phase3.json
performance-reports/measurements/after/phase3/lighthouse_mobile_after_phase3.json
performance-reports/measurements/after/phase4/lighthouse_desktop_after_phase4.json
performance-reports/measurements/after/phase4/lighthouse_mobile_after_phase4.json
```

반드시 기록할 항목:

```txt
Performance
FCP
LCP
TBT
CLS
Speed Index
총 요청 수
총 전송량
Video 전송량
Font 전송량
JS 전송량
scene-1.mp4 초기 다운로드 여부
```

## 7. 개선 전/후 비교 표

### Desktop

| 항목 | 개선 전 | Phase 1 후 | Phase 2 후 |
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

| 항목 | 개선 전 | Phase 1 후 | Phase 2 후 |
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

## 8. 포트폴리오 작성 방향

개선 전 기준으로는 다음처럼 작성할 수 있습니다.

```txt
Vercel 정적 배포 환경에서 Lighthouse를 측정한 결과,
Desktop Performance 98, FCP 0.7s, LCP 1.1s, CLS 0.008을 기록했습니다.
Route-level code splitting과 미디어 lazy loading 구조로 JS 실행 비용과 레이아웃 이동은 낮게 유지되고 있었지만,
전체 전송량 8.5MiB 중 7.8MiB가 hero video에서 발생하는 병목을 확인했습니다.
```

개선 후에는 다음 문장을 실제 수치로 보강합니다.

```txt
UI는 유지한 상태에서 hero video의 preload 전략을 조정하고 poster 기반 로딩을 적용해,
초기 전송량을 __MiB에서 __MiB로 줄이고 Mobile LCP를 __s에서 __s로 개선했습니다.
```

## 9. 작업 체크리스트

```txt
[ ] Phase 1: LandingPage.vue preload="metadata" 변경
[ ] Phase 1 빌드 확인
[ ] GitHub push
[ ] Vercel 재배포 확인
[ ] Desktop Lighthouse 재측정
[ ] Mobile Lighthouse 재측정
[ ] Network 전송량 비교
[ ] 결과를 performance-reports/baselines/FRONTEND_PERFORMANCE_BASELINE.md에 반영
[ ] Phase 2 poster 이미지 생성
[ ] poster 적용
[ ] 재측정
[ ] Phase 3 lazy loading 필요 여부 판단
[ ] Phase 3 결과에서 Mobile TBT / long task 확인
[ ] Phase 3.5: useLandingAnimations.ts 초기화 비용 분석
[ ] Phase 3.5: below-the-fold 애니메이션 초기화 지연
[ ] Phase 3.5 재측정
[ ] 폰트 최적화 방식 결정
```

## 10. Phase 4 이후 실제 우선순위 업데이트

업데이트 기준:

```txt
Phase 1~4까지 실제 Vercel Preview Lighthouse 측정을 완료한 뒤,
남은 병목과 리소스 크기를 기준으로 이후 작업 순서를 재조정했습니다.
```

완료된 작업:

```txt
[x] Phase 1: hero video preload 전략 개선
[x] Phase 2: hero video poster 적용
[x] Phase 3: hero video lazy loading 적용
[x] Phase 3.5: landing animation lazy initialization 적용
[x] Phase 4: Toss font CSS loading 최적화
```

Phase 4 이후 확인된 상태:

```txt
scene-1.mp4 초기 전송: 0KiB 유지
Mobile TBT: 0ms 유지
Toss main.css / others.css render-blocking 항목 제거
Mobile CSS transfer: 123.93KiB -> 45.71KiB

남은 주요 후보:
scene-1-poster.webp: 약 112KB
unused JS 예상 절감: 약 73KB
icon.png: 약 34KB
font transfer: 약 443KB
```

수정된 진행 순서:

```txt
1. Phase 4.5: hero video poster 초기 요청 최적화
2. Vercel Preview 배포
3. Lighthouse Desktop/Mobile 재측정
4. Phase 4.5 결과 문서화
5. Phase 6: unused JS / 초기 entry chunk 분석
6. Phase 5: icon.png 최적화
7. 필요 시 font-weight 축소 또는 self-hosting 검토
```

계획 수정 이유:

```txt
기존 계획에서는 Phase 4 다음 작업이 icon.png 최적화였지만,
실제 Phase 4 측정 결과 icon.png보다 scene-1-poster.webp의 초기 전송량이 더 컸습니다.

또한 unused-css-rules는 문제가 없었고 unused-javascript에서 약 73KiB 절감 가능성이 잡혔습니다.
따라서 다음 작업은 icon보다 poster 초기 요청 최적화를 먼저 진행하고,
이후 Phase 6은 CSS보다 JS chunk 분석 중심으로 진행하는 것이 더 합리적입니다.
```

Phase 4.5 이후 추가 수정:

```txt
Phase 4.5에서 poster 속성을 조건부로 분리했지만,
mockup 영역이 초기 viewport와 가까워 scene-1-poster.webp 요청은 초기 로딩에 그대로 포함됐습니다.

즉, UI 안정성을 유지하는 범위에서는 poster 지연 로딩의 실측 효과가 작았습니다.
반면 Phase 4.5 Lighthouse에서도 unused-javascript 절감 가능성은 약 65KiB 이상으로 유지됐습니다.

따라서 다음 개발은 Phase 5 icon 최적화가 아니라
Phase 6 unused JS / 초기 entry chunk 분석을 먼저 진행합니다.
```

보류할 작업:

```txt
Toss font weight 축소 또는 system font 전환은 성능 효과가 클 수 있지만 UI 글자 굵기 변화 가능성이 큽니다.
현재 목표가 UI 변화 최소화이므로 바로 진행하지 않고, 스크린샷 비교와 사용자 확인 이후에만 검토합니다.
```

다음에 바로 진행할 작업:

```txt
Phase 6: 초기 entry JS에 불필요한 모듈이 포함되는 원인을 분석합니다.
router lazy loading, services/index.ts barrel export, stores/project.ts import 구조,
Vite build warning의 dynamic/static import 혼재 여부를 우선 확인합니다.
```
