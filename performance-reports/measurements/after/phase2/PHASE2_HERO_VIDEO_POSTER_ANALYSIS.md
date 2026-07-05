# Phase 2 Analysis: Hero Video Poster 적용

## 1. 개선 목표

Phase 2의 목표는 랜딩 페이지 UI를 유지하면서 hero video가 실제로 재생되기 전 보여줄 가벼운 대체 이미지를 제공하는 것입니다.

Phase 1에서 `preload="metadata"`로 변경했지만, `autoplay`와 mount 직후 `play()` 호출 때문에 `/scene-1.mp4` 전송량은 줄지 않았습니다.

따라서 Phase 2에서는 다음 개선을 진행했습니다.

```txt
scene-1.mp4 첫 프레임 기반 poster 이미지 생성
video 태그에 poster 속성 추가
향후 video src 지연 연결을 위한 시각적 안전장치 마련
```

## 2. 개선이 필요했던 이유

Phase 1 측정 결과, preload 전략만으로는 가장 큰 병목인 hero video 다운로드를 줄이지 못했습니다.

```txt
Phase 1 Mobile Performance: 70
Phase 1 총 전송량: 8,748KiB
Phase 1 scene-1.mp4 전송량: 7.8MiB
```

즉, 전체 초기 전송량의 대부분이 여전히 `/scene-1.mp4`에서 발생했습니다.

하지만 바로 video `src`를 지연 연결하면 영상 영역이 비어 보이거나 첫 화면이 달라질 수 있습니다. 사용자는 UI 변화가 없기를 원했기 때문에, 먼저 기존 영상 첫 프레임과 동일한 poster 이미지를 추가해 시각적 안정성을 확보했습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
itda-frontend/public/scene-1-poster.webp
```

생성한 poster 파일:

```txt
itda-frontend/public/scene-1-poster.webp
파일 크기: 112,304 bytes
형식: WebP
원본: scene-1.mp4 첫 프레임
```

변경 내용:

```diff
<video
  ref="heroVideoRef"
  :src="selectedHeroVideo"
+ poster="/scene-1-poster.webp"
  :muted="heroVideoMuted"
  autoplay
  loop
  playsinline
  preload="metadata"
  class="mockup-video"
></video>
```

이 방법을 선택한 이유:

```txt
기존 UI와 거의 동일한 첫 화면 유지 가능
영상이 로드되기 전에도 mockup 영역이 비어 보이지 않음
Phase 3에서 video src를 지연 연결해도 시각적 이질감을 줄일 수 있음
7.8MiB 영상 대신 약 110KiB 이미지를 먼저 보여줄 수 있는 기반 확보
```

## 4. 측정 파일

개선 전:

```txt
performance-reports/measurements/before/lighthouse_mobile_before.json
performance-reports/measurements/before/lighthouse_desktop_before.json
```

Phase 1 후:

```txt
performance-reports/measurements/after/phase1/lighthouse_mobile_after_phase1.json
```

Phase 2 후:

```txt
performance-reports/measurements/after/phase2/lighthouse_mobile_after_phase2.json
performance-reports/measurements/after/phase2/lighthouse_desktop_after_phase2.json
```

측정 URL:

```txt
개선 전 Production: https://itda-pi.vercel.app/
Phase 2 Preview: https://itda-4pfg8z464-wotj.vercel.app/
```

주의:

```txt
Phase 2는 Vercel Preview Deployment에서 측정되었습니다.
Preview URL은 검색 엔진 인덱싱이 차단되어 SEO 점수가 낮게 나올 수 있으므로 성능 비교 지표로 사용하지 않습니다.
```

## 5. 개선 전후 결과

### Lighthouse Mobile

| 항목 | 개선 전 | Phase 1 후 | Phase 2 후 | 개선 전 대비 |
| --- | ---: | ---: | ---: | ---: |
| Performance | 69 | 70 | 71 | +2 |
| FCP | 4.9s | 3.8s | 3.0s | -1.9s |
| LCP | 5.1s | 5.7s | 6.1s | +1.0s |
| TBT | 0ms | 20ms | 120ms | +120ms |
| CLS | 0.020 | 0.027 | 0.026 | +0.006 |
| Speed Index | 4.9s | 3.8s | 3.0s | -1.9s |
| TTI | 5.1s | 5.7s | 6.1s | +1.0s |
| 총 요청 수 | 63 | 64 | 65 | +2 |
| 총 전송량 | 8,684KiB | 8,748KiB | 8,794KiB | +110KiB |
| Video 전송량 | 7.8MiB | 7.8MiB | 7.8MiB | 변화 없음 |
| Poster 전송량 | 없음 | 없음 | 약 110KiB | +110KiB |
| Font 전송량 | 471.5KiB | 535.3KiB | 471.5KiB | 거의 동일 |
| JS 전송량 | 172.7KiB | 172.8KiB | 172.8KiB | 거의 동일 |

### Lighthouse Desktop

| 항목 | 개선 전 | Phase 2 후 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 98 | 95 | -3 |
| FCP | 0.7s | 1.0s | +0.3s |
| LCP | 1.1s | 1.3s | +0.2s |
| TBT | 10ms | 0ms | -10ms |
| CLS | 0.008 | 0.010 | +0.002 |
| Speed Index | 0.9s | 1.1s | +0.2s |
| 총 요청 수 | 64 | 70 | +6 |
| 총 전송량 | 8,709KiB | 8,860KiB | +151KiB |
| Video 전송량 | 7.8MiB | 7.8MiB | 변화 없음 |
| Poster 전송량 | 없음 | 약 110KiB | +110KiB |

## 6. 상세 수치

### 전체 전송량

| 항목 | Mobile 개선 전 | Mobile Phase 2 |
| --- | ---: | ---: |
| 총 전송량(bytes) | 8,892,376 | 9,004,900 |
| 총 리소스 크기(bytes) | 9,597,283 | 9,709,790 |
| 요청 수 | 63 | 65 |

증가량:

```txt
9,004,900 - 8,892,376 = 112,524 bytes
약 110KiB 증가
```

증가 원인:

```txt
scene-1-poster.webp가 새로 로드됨
poster transferSize: 112,533 bytes
```

### Hero video 전송량

| 항목 | 개선 전 | Phase 1 후 | Phase 2 후 |
| --- | ---: | ---: | ---: |
| scene-1.mp4 transferSize | 8,180,044 bytes | 8,180,096 bytes | 8,180,038 bytes |
| scene-1.mp4 resourceSize | 8,175,293 bytes | 8,175,293 bytes | 8,175,293 bytes |
| statusCode | 206 | 206 | 206 |
| priority | Low | Low | Low |

`scene-1.mp4` 전송량은 Phase 2 후에도 사실상 동일합니다.

차이:

```txt
8,180,038 - 8,180,044 = -6 bytes
```

즉, poster 추가만으로는 hero video 다운로드 자체가 줄어들지 않았습니다.

### Poster 이미지 전송량

| 항목 | Phase 2 후 |
| --- | ---: |
| scene-1-poster.webp transferSize | 112,533 bytes |
| scene-1-poster.webp resourceSize | 112,304 bytes |
| statusCode | 200 |
| priority | High |
| mimeType | image/webp |

poster는 정상적으로 로드되었습니다.

다만 video `src`가 여전히 초기부터 연결되어 있고 `autoplay`도 유지되므로, 브라우저는 poster와 video를 모두 요청합니다.

## 7. 결과 해석

Phase 2에서 좋아진 지표:

```txt
Mobile Performance: 69 -> 71
Mobile FCP: 4.9s -> 3.0s
Mobile Speed Index: 4.9s -> 3.0s
```

화면에 첫 콘텐츠가 보이는 속도는 개선되었습니다. poster가 영상 영역의 초기 시각 상태를 빠르게 제공하면서 체감 렌더링 지표에는 긍정적인 영향을 준 것으로 볼 수 있습니다.

하지만 나빠지거나 개선되지 않은 지표도 있습니다.

```txt
Mobile LCP: 5.1s -> 6.1s
Mobile TBT: 0ms -> 120ms
총 전송량: 8,684KiB -> 8,794KiB
scene-1.mp4: 7.8MiB 유지
```

핵심 판단:

```txt
Phase 2는 실제 네트워크 병목을 제거한 단계가 아니라,
다음 단계에서 video src를 지연 연결하기 위한 준비 단계입니다.
```

LCP 요소는 영상이 아니라 hero headline 텍스트였습니다.

```txt
selector: div.landing > section.hero-section > div.hero-content > h1.hero-headline
nodeLabel: 끊어지는 맥락은 잊다, 영상의 흐름을 잇다
```

따라서 현재 LCP 지연은 다음 요인이 함께 작용한 결과로 해석해야 합니다.

```txt
Toss 외부 폰트 CSS가 render-blocking 리소스로 동작
hero headline 텍스트 렌더링이 LCP 대상
scene-1.mp4 7.8MiB 요청이 초기 네트워크 대역폭을 계속 사용
poster 이미지가 추가되었지만 video 요청은 제거되지 않음
```

## 8. SEO 점수 주의 사항

Phase 2 SEO 점수는 개선 전보다 낮습니다.

```txt
개선 전 Mobile SEO: 91
Phase 2 Mobile SEO: 54
```

하지만 이는 성능 개선 때문에 발생한 문제가 아니라 측정 URL 차이의 영향입니다.

Phase 2 측정 URL:

```txt
https://itda-4pfg8z464-wotj.vercel.app/
```

Vercel Preview Deployment는 검색 엔진 인덱싱이 차단될 수 있어 Lighthouse SEO에서 다음 항목이 실패합니다.

```txt
Page is blocked from indexing
```

따라서 Phase 2 비교에서도 SEO 점수는 성능 비교 지표로 사용하지 않습니다.

## 9. 결론

Phase 2의 결론은 다음과 같습니다.

```txt
hero video에 poster 이미지를 적용해 초기 시각 상태를 안정화했고,
Mobile FCP와 Speed Index는 4.9s에서 3.0s로 개선되었다.
```

하지만 핵심 병목인 영상 전송량은 줄지 않았습니다.

수치 근거:

```txt
scene-1.mp4 개선 전: 8,180,044 bytes
scene-1.mp4 Phase 2 후: 8,180,038 bytes
```

따라서 Phase 2는 최종 성능 개선이라기보다 다음 개선을 위한 기반 작업입니다.

```txt
poster 적용만으로는 영상 다운로드 병목을 해결할 수 없음
실제 전송량 개선을 위해서는 video src 지연 연결 또는 lazy loading이 필요함
```

## 10. 다음 개선 방향

다음 단계는 Phase 3입니다.

목표:

```txt
초기 페이지 로딩 시점에 scene-1.mp4 요청 자체를 발생시키지 않음
poster 이미지만 먼저 보여줌
mockup 영역이 화면에 가까워졌을 때 video src 연결
그 후 play() 호출
```

권장 구현 방향:

```txt
1. selectedHeroVideo를 바로 video src에 연결하지 않음
2. 초기에는 poster만 표시
3. IntersectionObserver로 mockup 영역 감지
4. 화면에 가까워지면 video src 연결
5. src 연결 후 playHeroVideo 호출
```

예상 효과:

```txt
초기 Network에서 scene-1.mp4 요청 제거 또는 지연
초기 전송량 8.5MiB 수준에서 큰 폭 감소 가능
Mobile LCP/FCP 개선 가능성 증가
사용자에게는 기존 poster가 보이므로 UI 변화 최소화
```

추가로 검토할 병목:

```txt
Toss 외부 폰트 CSS render-blocking 해소
font-display 전략 또는 필요한 font subset만 로드
icon.png 최적화
```

## 11. 포트폴리오 기록 문장

Phase 2는 다음처럼 기록하는 것이 정확합니다.

```txt
랜딩 페이지의 7.8MiB hero video가 초기 로딩 성능 병목으로 확인되어,
영상 첫 프레임 기반 WebP poster 이미지를 생성하고 video 태그에 적용했습니다.
이를 통해 영상 로딩 전에도 동일한 UI를 유지하면서 Mobile FCP와 Speed Index를 4.9s에서 3.0s로 개선했습니다.
다만 autoplay 구조로 인해 scene-1.mp4 전송량은 7.8MiB로 유지되어,
poster 적용만으로는 네트워크 병목이 해결되지 않음을 수치로 검증했고,
이후 video src 지연 연결 방식으로 개선 방향을 전환했습니다.
```

## 12. 체크리스트

```txt
[x] scene-1.mp4 첫 프레임 기반 poster 이미지 생성
[x] scene-1-poster.webp 파일 추가
[x] video 태그에 poster 속성 적용
[x] 빌드 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] poster 이미지 로드 확인
[x] scene-1.mp4 전송량 변화 없음 확인
[x] Phase 2가 다음 단계 준비 작업이라는 결론 도출
[ ] Phase 3 video src 지연 연결 구현
[ ] Phase 3 측정 후 전송량 감소 여부 확인
```
