# Phase 3 Analysis: Hero Video Lazy Loading 적용

## 1. 개선 목표

Phase 3의 목표는 랜딩 페이지 초기 로딩 시점에서 `/scene-1.mp4` 요청 자체를 제거하는 것입니다.

Phase 1에서는 `preload="metadata"`를 적용했고, Phase 2에서는 `poster` 이미지를 추가했습니다. 하지만 Phase 2까지는 `video src`가 초기부터 연결되어 있었기 때문에 브라우저가 여전히 7.8MiB hero video를 다운로드했습니다.

Phase 3에서는 다음 방향으로 개선했습니다.

```txt
초기 렌더링 시 video src를 연결하지 않음
poster 이미지만 먼저 표시
mockup 영역이 화면에 들어오면 video src 연결
그 후 play() 호출
```

## 2. 개선이 필요했던 이유

Phase 2 측정 결과, poster 이미지는 정상 적용되었지만 핵심 병목인 video 전송량은 줄지 않았습니다.

```txt
Phase 2 Mobile Performance: 71
Phase 2 총 전송량: 8,794KiB
Phase 2 scene-1.mp4 전송량: 7.8MiB
```

즉, poster를 추가해도 `autoplay`와 초기 `src` 연결 구조 때문에 브라우저는 poster와 video를 모두 요청했습니다.

문제는 다음과 같았습니다.

```txt
초기 로딩 단계에서 사용자가 아직 영상 영역을 보기 전 7.8MiB 다운로드 발생
Mobile 네트워크 환경에서 전체 전송량 대부분이 hero video에 집중
poster 추가만으로는 네트워크 병목 제거 불가
```

따라서 실제 전송량을 줄이려면 `preload` 조정이 아니라 `src` 연결 시점 자체를 늦춰야 했습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
```

핵심 변경:

```txt
shouldLoadHeroVideo 상태 추가
IntersectionObserver로 mockup 영역 감지
초기 video src는 undefined
mockup 영역이 보이면 src 연결 후 playHeroVideo 호출
데모 보기 버튼/소리 버튼 클릭 시 즉시 로드
preload="none" 적용
```

변경 전:

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

변경 후:

```vue
<video
  ref="heroVideoRef"
  :src="shouldLoadHeroVideo ? selectedHeroVideo : undefined"
  poster="/scene-1-poster.webp"
  :muted="heroVideoMuted"
  :autoplay="shouldLoadHeroVideo"
  loop
  playsinline
  preload="none"
  class="mockup-video"
></video>
```

추가한 로딩 제어:

```ts
const shouldLoadHeroVideo = ref(false)

const loadHeroVideo = async () => {
  if (shouldLoadHeroVideo.value) return

  shouldLoadHeroVideo.value = true
  await nextTick()
  playHeroVideo(heroVideoRef.value)
  heroVideoObserver?.disconnect()
  heroVideoObserver = null
}
```

IntersectionObserver 적용:

```ts
heroVideoObserver = new IntersectionObserver(
  (entries) => {
    const entry = entries[0]
    if (!entry?.isIntersecting) return
    void loadHeroVideo()
  },
  { threshold: 0.35 }
)
```

이 방법을 선택한 이유:

```txt
기존 UI 구조를 유지할 수 있음
poster가 먼저 보이기 때문에 빈 영역이 생기지 않음
초기 네트워크 요청에서 7.8MiB video를 제거할 수 있음
사용자가 데모 보기 또는 소리 버튼을 누르면 즉시 video를 로드해 기능 동작을 유지할 수 있음
```

## 4. 측정 파일

개선 전:

```txt
performance-reports/measurements/before/lighthouse_mobile_before.json
performance-reports/measurements/before/lighthouse_desktop_before.json
```

Phase 2 후:

```txt
performance-reports/measurements/after/phase2/lighthouse_mobile_after_phase2.json
performance-reports/measurements/after/phase2/lighthouse_desktop_after_phase2.json
```

Phase 3 후:

```txt
performance-reports/measurements/after/phase3/lighthouse_mobile_after_phase3.json
performance-reports/measurements/after/phase3/lighthouse_desktop_after_phase3.json
```

측정 URL:

```txt
개선 전 Production: https://itda-pi.vercel.app/
Phase 3 Preview: https://itda-ffojfy7cu-wotj.vercel.app/
```

## 5. 개선 전후 결과

### Lighthouse Mobile

| 항목 | 개선 전 | Phase 2 후 | Phase 3 후 | 개선 전 대비 |
| --- | ---: | ---: | ---: | ---: |
| Performance | 69 | 71 | 63 | -6 |
| FCP | 4.9s | 3.0s | 3.3s | -1.6s |
| LCP | 5.1s | 6.1s | 6.1s | +1.0s |
| TBT | 0ms | 120ms | 350ms | +350ms |
| CLS | 0.020 | 0.026 | 0.028 | +0.008 |
| Speed Index | 4.9s | 3.0s | 3.3s | -1.6s |
| TTI | 5.1s | 6.1s | 6.2s | +1.1s |
| 총 요청 수 | 63 | 65 | 64 | +1 |
| 총 전송량 | 8.48MiB | 8.59MiB | 0.79MiB | -7.69MiB |
| Video 전송량 | 7.8MiB | 7.8MiB | 0 | -7.8MiB |
| Poster 전송량 | 없음 | 109.9KiB | 109.9KiB | +109.9KiB |
| Font 전송량 | 471.5KiB | 471.5KiB | 471.5KiB | 동일 |
| JS 전송량 | 172.7KiB | 172.8KiB | 172.8KiB | 거의 동일 |

### Lighthouse Desktop

| 항목 | 개선 전 | Phase 2 후 | Phase 3 후 | 개선 전 대비 |
| --- | ---: | ---: | ---: | ---: |
| Performance | 98 | 95 | 96 | -2 |
| FCP | 0.7s | 1.0s | 0.8s | +0.1s |
| LCP | 1.1s | 1.3s | 1.3s | +0.2s |
| TBT | 10ms | 0ms | 0ms | -10ms |
| CLS | 0.008 | 0.010 | 0.011 | +0.003 |
| Speed Index | 0.9s | 1.1s | 1.2s | +0.3s |
| TTI | 1.1s | 1.3s | 1.3s | +0.2s |
| 총 요청 수 | 64 | 70 | 69 | +5 |
| 총 전송량 | 8.50MiB | 8.65MiB | 0.85MiB | -7.65MiB |
| Video 전송량 | 7.8MiB | 7.8MiB | 0 | -7.8MiB |
| Poster 전송량 | 없음 | 109.9KiB | 109.9KiB | +109.9KiB |

## 6. 상세 수치

### 전체 전송량

| 항목 | Mobile 개선 전 | Mobile Phase 2 | Mobile Phase 3 |
| --- | ---: | ---: | ---: |
| 총 전송량(bytes) | 8,892,376 | 9,004,900 | 825,048 |
| 요청 수 | 63 | 65 | 64 |
| Lighthouse total byte weight | 8,684KiB | 8,794KiB | 806KiB |

개선 전 대비 감소량:

```txt
8,892,376 - 825,048 = 8,067,328 bytes
약 7.69MiB 감소
```

비율:

```txt
0.79MiB / 8.48MiB = 약 9.3%
즉, 초기 전송량 약 90.7% 감소
```

### Hero video 전송량

| 항목 | 개선 전 | Phase 2 후 | Phase 3 후 |
| --- | ---: | ---: | ---: |
| scene-1.mp4 transferSize | 8,180,044 bytes | 8,180,038 bytes | 0 bytes |
| scene-1.mp4 resourceSize | 8,175,293 bytes | 8,175,293 bytes | 0 bytes |
| 초기 요청 여부 | 요청됨 | 요청됨 | 요청되지 않음 |

`scene-1.mp4` 초기 요청은 Phase 3에서 제거되었습니다.

감소량:

```txt
8,180,044 - 0 = 8,180,044 bytes
약 7.8MiB 감소
```

### Poster 이미지 전송량

| 항목 | Phase 2 후 | Phase 3 후 |
| --- | ---: | ---: |
| scene-1-poster.webp transferSize | 112,533 bytes | 112,581 bytes |
| scene-1-poster.webp resourceSize | 112,304 bytes | 약 112,304 bytes |

poster는 Phase 3에서도 정상적으로 로드됩니다.

즉, Phase 3의 구조는 다음과 같습니다.

```txt
초기 로딩: poster만 요청
mockup 진입 후: scene-1.mp4 요청
```

## 7. 브라우저 동작 검증

로컬 빌드 결과를 브라우저에서 확인했습니다.

검증 결과:

```txt
초기 로딩 scene-1.mp4 요청: 0회
초기 로딩 scene-1-poster.webp 요청: 1회
mockup 영역 스크롤 후 scene-1.mp4 요청: 1회
```

따라서 코드 의도대로 초기 네트워크에서 video 요청이 제거되었습니다.

## 8. 결과 해석

Phase 3에서 명확히 좋아진 지표:

```txt
Mobile 총 전송량: 8.48MiB -> 0.79MiB
Desktop 총 전송량: 8.50MiB -> 0.85MiB
scene-1.mp4 초기 요청: 7.8MiB -> 0
Mobile FCP: 4.9s -> 3.3s
Mobile Speed Index: 4.9s -> 3.3s
```

핵심 병목이었던 hero video 전송량은 해결되었습니다.

하지만 Mobile Performance 점수는 하락했습니다.

```txt
Mobile Performance: 69 -> 63
Mobile TBT: 0ms -> 350ms
Mobile LCP: 5.1s -> 6.1s
```

즉, 네트워크 병목을 제거한 뒤 다음 병목이 드러났습니다.

Phase 3 Mobile에서 확인된 long task:

```txt
LandingPage JS: 257ms
index JS: 196ms
Unattributable long task: 115ms
Main-thread work: 4.7s
```

LCP 대상은 video가 아니라 hero headline 텍스트입니다.

```txt
selector: div.landing > section.hero-section > div.hero-content > h1.hero-headline
nodeLabel: 끊어지는 맥락은 잊다, 영상의 흐름을 잇다
```

따라서 Phase 3의 성능 해석은 다음과 같습니다.

```txt
네트워크 전송량 최적화는 성공
Lighthouse Mobile 점수 개선은 실패
다음 병목은 JS 실행 시간과 render-blocking font/css
```

## 9. Render-blocking 리소스

Phase 3 Mobile에서도 render-blocking 리소스가 남아 있습니다.

```txt
assets/index-ysD8JUcj.css
https://static.toss.im/tps/others.css
https://static.toss.im/tps/main.css
```

특히 Toss 외부 폰트 CSS는 hero headline 렌더링에 영향을 줄 가능성이 있습니다.

```txt
LCP 대상: hero headline 텍스트
외부 폰트 CSS: render-blocking
font 요청 수: 47개
font 전송량: 471.5KiB
```

따라서 LCP 개선을 위해서는 hero video보다 font/css 최적화가 더 직접적인 다음 과제입니다.

## 10. SEO 점수 주의 사항

Phase 3 SEO 점수는 개선 전보다 낮습니다.

```txt
개선 전 Mobile SEO: 91
Phase 3 Mobile SEO: 54
```

하지만 이는 성능 개선 때문이 아니라 Vercel Preview Deployment 측정 URL 차이의 영향입니다.

Phase 3 측정 URL:

```txt
https://itda-ffojfy7cu-wotj.vercel.app/
```

Vercel Preview Deployment는 검색 엔진 인덱싱이 차단될 수 있으므로 SEO 점수는 성능 비교 지표로 사용하지 않습니다.

## 11. 결론

Phase 3의 결론은 다음과 같습니다.

```txt
video src 지연 연결을 적용해 초기 로딩에서 scene-1.mp4 요청을 제거했고,
Mobile 기준 총 전송량을 8.48MiB에서 0.79MiB로 약 90.7% 줄였다.
```

수치 근거:

```txt
scene-1.mp4 개선 전: 8,180,044 bytes
scene-1.mp4 Phase 3 후: 0 bytes
총 전송량 개선 전: 8,892,376 bytes
총 전송량 Phase 3 후: 825,048 bytes
```

다만 Lighthouse Mobile Performance 점수는 69에서 63으로 하락했습니다.

```txt
원인: TBT 350ms 증가
주요 병목: LandingPage JS long task, index JS long task, Toss font/css render-blocking
```

따라서 Phase 3는 전송량 최적화 관점에서는 성공이고, Lighthouse 점수 개선 관점에서는 추가 작업이 필요합니다.

## 12. 다음 개선 방향

다음 개선 후보는 두 가지입니다.

### 12.1 JS 실행 비용 개선

목표:

```txt
Mobile TBT 350ms 감소
LandingPage JS long task 감소
초기 애니메이션 실행 비용 점검
```

검토 대상:

```txt
itda-frontend/src/pages/landing/useLandingAnimations.ts
GSAP 초기 애니메이션
랜딩 페이지 mount 직후 실행되는 DOM 조작
```

### 12.2 Toss 폰트 최적화

목표:

```txt
hero headline LCP 개선
render-blocking CSS 감소
외부 font 요청 수 감소
```

검토 대상:

```txt
itda-frontend/src/assets/styles/base.css
@import url('https://static.toss.im/tps/main.css')
@import url('https://static.toss.im/tps/others.css')
```

권장 순서:

```txt
1. Mobile Performance 하락 원인인 JS/TBT 먼저 분석
2. 이후 Toss font/css 최적화로 LCP 개선 검토
```

## 13. 포트폴리오 기록 문장

Phase 3는 다음처럼 기록할 수 있습니다.

```txt
랜딩 페이지 초기 로딩에서 7.8MiB hero video가 즉시 다운로드되는 병목을 확인하고,
poster 기반 lazy loading 구조로 변경했습니다.
초기에는 WebP poster만 렌더링하고, mockup 영역이 viewport에 진입할 때 IntersectionObserver로 video src를 연결하도록 구현해
Mobile 기준 총 전송량을 8.48MiB에서 0.79MiB로 약 90.7% 절감했습니다.
이 과정에서 scene-1.mp4 초기 요청이 7.8MiB에서 0으로 감소했음을 Lighthouse와 브라우저 Network 검증으로 확인했습니다.
```

주의해서 함께 적을 문장:

```txt
전송량 병목은 해결했지만 Mobile Lighthouse 점수는 TBT 증가로 하락했으며,
이후 JS long task와 외부 폰트 render-blocking 최적화를 다음 개선 과제로 도출했습니다.
```

## 14. 체크리스트

```txt
[x] 초기 video src 제거
[x] poster 기반 초기 화면 유지
[x] IntersectionObserver로 mockup 진입 감지
[x] mockup 진입 후 video src 연결
[x] 데모 보기 클릭 시 video 즉시 로드
[x] 소리 버튼 클릭 시 video 즉시 로드
[x] preload="none" 적용
[x] 빌드 성공 확인
[x] 브라우저 Network 검증
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] scene-1.mp4 초기 요청 제거 확인
[x] 총 전송량 약 90.7% 감소 확인
[x] Mobile Performance 점수 하락 원인 분석
[ ] JS/TBT 병목 개선
[ ] Toss font/css render-blocking 개선
```
