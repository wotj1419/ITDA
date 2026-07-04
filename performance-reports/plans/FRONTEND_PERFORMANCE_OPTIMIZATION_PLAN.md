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
[ ] 폰트 최적화 방식 결정
```
