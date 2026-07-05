# Phase 3.5 Analysis: Landing Animation Lazy Initialization 적용

## 1. 개선 목표

Phase 3.5의 목표는 Phase 3 이후 Mobile Lighthouse에서 증가한 TBT와 long task를 줄이는 것입니다.

Phase 3에서 hero video lazy loading을 적용해 `/scene-1.mp4` 초기 요청은 제거했지만, Mobile Performance 점수는 오히려 하락했습니다.

Phase 3 측정 결과:

```txt
Mobile Performance: 69 -> 63
Mobile TBT: 0ms -> 350ms
LandingPage JS long task: 257ms
index JS long task: 196ms
Main-thread work: 4.7s
```

따라서 Phase 3.5에서는 네트워크가 아니라 초기 JavaScript 실행 비용을 줄이는 방향으로 개선했습니다.

## 2. 개선이 필요했던 이유

Phase 3까지의 개선으로 가장 큰 네트워크 병목은 해결되었습니다.

```txt
scene-1.mp4 초기 요청: 7.8MiB -> 0
Mobile 총 전송량: 8.48MiB -> 0.79MiB
```

하지만 성능 병목이 사라진 것이 아니라 병목의 위치가 바뀌었습니다.

```txt
기존 병목: hero video 네트워크 전송량
새 병목: 랜딩 페이지 초기 JavaScript 실행 비용
```

`setupLandingAnimations()`는 mount 직후 hero 영역뿐 아니라 features, workflow, node-edge, cta 섹션의 GSAP animation, ScrollTrigger, DOM query, workflow title text split, node cursor animation까지 한 번에 초기화하고 있었습니다.

이 구조는 첫 화면 렌더링에 필요하지 않은 below-the-fold 애니메이션 작업까지 초기 JavaScript 실행 구간에 포함시킵니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/pages/landing/useLandingAnimations.ts
```

핵심 변경:

```txt
hero 영역 애니메이션은 mount 직후 즉시 실행
features/workflow/node-edge/cta 섹션 애니메이션은 IntersectionObserver로 지연 초기화
workflow title text split은 workflow 섹션 진입 후 실행
node cursor 반복 애니메이션은 node-edge 섹션 진입 후 실행
cta orb 반복 애니메이션은 cta 섹션 진입 후 실행
각 섹션별 gsap.context cleanup 분리
```

변경 전 구조:

```txt
setupLandingAnimations()
  hero animation 초기화
  hero orb 반복 animation 초기화
  section header ScrollTrigger 전체 초기화
  bento card animation 초기화
  workflow step animation 초기화
  workflow title text split 즉시 실행
  node cursor animation 즉시 초기화
  node flow 반복 timeline 즉시 생성
  workflow progress ScrollTrigger 초기화
  cta animation 초기화
  cta orb 반복 animation 초기화
```

변경 후 구조:

```txt
setupLandingAnimations()
  hero animation만 즉시 초기화
  observeOnce('.features-section') 진입 후 features animation 초기화
  observeOnce('.workflow-section') 진입 후 workflow animation/text split 초기화
  observeOnce('.node-edge-section') 진입 후 node cursor/node flow animation 초기화
  observeOnce('.cta-section') 진입 후 cta animation 초기화
```

이 방법을 선택한 이유:

```txt
UI 디자인과 애니메이션 자체는 유지할 수 있음
첫 화면에 필요하지 않은 JS 작업만 뒤로 미룰 수 있음
Phase 3에서 드러난 TBT/long task 문제를 직접 겨냥함
아래 섹션은 사용자가 스크롤하기 전까지 초기화하지 않아도 사용자 경험 영향이 작음
```

## 4. 측정 파일

개선 전:

```txt
performance-reports/measurements/before/lighthouse_mobile_before.json
performance-reports/measurements/before/lighthouse_desktop_before.json
```

Phase 3 후:

```txt
performance-reports/measurements/after/phase3/lighthouse_mobile_after_phase3.json
performance-reports/measurements/after/phase3/lighthouse_desktop_after_phase3.json
```

Phase 3.5 후:

```txt
performance-reports/measurements/after/phase3_5/lighthouse_mobile_after_phase3_5.json
performance-reports/measurements/after/phase3_5/lighthouse_desktop_after_phase3_5.json
```

측정 URL:

```txt
Phase 3 Preview: https://itda-ffojfy7cu-wotj.vercel.app/
Phase 3.5 Preview: https://itda-ctk4qlhha-wotj.vercel.app/
```

## 5. 개선 전후 결과

### Lighthouse Mobile

| 항목 | 개선 전 | Phase 3 후 | Phase 3.5 후 | Phase 3 대비 |
| --- | ---: | ---: | ---: | ---: |
| Performance | 69 | 63 | 67 | +4 |
| FCP | 4.9s | 3.3s | 4.2s | +0.9s |
| LCP | 5.1s | 6.1s | 6.4s | +0.3s |
| TBT | 0ms | 350ms | 0ms | -350ms |
| CLS | 0.020 | 0.028 | 0.019 | -0.009 |
| Speed Index | 4.9s | 3.3s | 4.2s | +0.9s |
| TTI | 5.1s | 6.2s | 6.4s | +0.2s |
| Main-thread work | 4.6s | 4.7s | 3.8s | -0.9s |
| Bootup time | 0.8s | 0.8s | 0.7s | -0.1s |
| 총 요청 수 | 63 | 64 | 64 | 동일 |
| 총 전송량 | 8.48MiB | 0.79MiB | 0.86MiB | +0.07MiB |
| Video 전송량 | 7.8MiB | 0 | 0 | 유지 |
| Font 전송량 | 471.5KiB | 471.5KiB | 549.7KiB | +78.2KiB |
| JS 전송량 | 172.7KiB | 172.8KiB | 173.2KiB | +0.4KiB |

### Lighthouse Desktop

| 항목 | 개선 전 | Phase 3 후 | Phase 3.5 후 | Phase 3 대비 |
| --- | ---: | ---: | ---: | ---: |
| Performance | 98 | 96 | 96 | 동일 |
| FCP | 0.7s | 0.8s | 0.7s | -0.1s |
| LCP | 1.1s | 1.3s | 1.3s | 동일 |
| TBT | 10ms | 0ms | 0ms | 동일 |
| CLS | 0.008 | 0.011 | 0.009 | -0.002 |
| Speed Index | 0.9s | 1.2s | 1.1s | -0.1s |
| TTI | 1.1s | 1.3s | 1.3s | 동일 |
| Main-thread work | 1.7s | 1.7s | 2.0s | +0.3s |
| 총 전송량 | 8.50MiB | 0.85MiB | 0.93MiB | +0.08MiB |
| Video 전송량 | 7.8MiB | 0 | 0 | 유지 |

## 6. 상세 수치

### Mobile TBT / long task

| 항목 | Phase 3 후 | Phase 3.5 후 |
| --- | ---: | ---: |
| TBT | 350ms | 0ms |
| Main-thread work | 4.7s | 3.8s |
| Bootup time | 0.8s | 0.7s |
| LandingPage JS top long task | 257ms | 180ms |
| index JS top long task | 196ms | 71ms |

Phase 3 top long tasks:

```txt
LandingPage JS: 257ms
index JS: 196ms
Unattributable: 115ms
Document: 103ms
```

Phase 3.5 top long tasks:

```txt
LandingPage JS: 180ms
Document: 83ms
index JS: 71ms
Unattributable: 63ms
```

해석:

```txt
below-the-fold 애니메이션 초기화를 지연하면서 초기 JavaScript 작업량이 줄었습니다.
Lighthouse TBT는 350ms에서 0ms로 개선되었습니다.
남아 있는 long task는 있지만 TBT 집계 구간에서의 blocking time은 제거된 것으로 측정되었습니다.
```

### Hero video 전송량

| 항목 | Phase 3 후 | Phase 3.5 후 |
| --- | ---: | ---: |
| scene-1.mp4 transferSize | 0 bytes | 0 bytes |
| 초기 요청 여부 | 요청되지 않음 | 요청되지 않음 |

Phase 3.5에서도 Phase 3의 핵심 개선인 hero video 초기 요청 제거는 유지되었습니다.

```txt
scene-1.mp4 초기 요청: 0
```

### 전체 전송량

| 항목 | Phase 3 후 | Phase 3.5 후 |
| --- | ---: | ---: |
| Mobile 총 전송량(bytes) | 825,048 | 905,248 |
| Mobile 총 전송량(MiB) | 0.79MiB | 0.86MiB |
| Desktop 총 전송량(MiB) | 0.85MiB | 0.93MiB |

전송량은 Phase 3보다 약간 증가했습니다.

주요 원인:

```txt
Toss font 전송량: 471.5KiB -> 549.7KiB
JS 전송량 변화는 0.4KiB 수준으로 거의 없음
scene-1.mp4는 계속 0 bytes
```

따라서 전송량 증가는 Phase 3.5 코드 변경보다 외부 Toss font 응답/측정 조건 차이 영향이 큽니다.

## 7. 결과 해석

Phase 3.5에서 좋아진 지표:

```txt
Mobile Performance: 63 -> 67
Mobile TBT: 350ms -> 0ms
Mobile CLS: 0.028 -> 0.019
Main-thread work: 4.7s -> 3.8s
LandingPage JS top long task: 257ms -> 180ms
index JS top long task: 196ms -> 71ms
```

즉, Phase 3.5의 목표였던 TBT와 JS 실행 비용 개선은 성공했습니다.

하지만 나빠진 지표도 있습니다.

```txt
Mobile FCP: 3.3s -> 4.2s
Mobile LCP: 6.1s -> 6.4s
Mobile Speed Index: 3.3s -> 4.2s
```

LCP 대상은 Phase 3과 동일하게 hero headline 텍스트입니다.

```txt
selector: div.landing > section.hero-section > div.hero-content > h1.hero-headline
nodeLabel: 끊어지는 맥락은 잊다, 영상의 흐름을 잇다
```

Phase 3.5에서도 render-blocking 리소스는 남아 있습니다.

```txt
https://static.toss.im/tps/others.css
https://static.toss.im/tps/main.css
assets/index-ysD8JUcj.css
```

특히 Phase 3.5 측정에서 Toss font 관련 전송량과 render-blocking 시간이 더 크게 잡혔습니다.

```txt
tps/others.css wastedMs: 600ms
tps/main.css wastedMs: 927ms
Toss font transfer: 549.7KiB
```

따라서 Phase 3.5 후 남은 병목은 JavaScript보다 font/css 쪽으로 이동했다고 볼 수 있습니다.

## 8. SEO 점수 주의 사항

Phase 3.5 SEO 점수는 개선 전보다 낮습니다.

```txt
개선 전 SEO: 91
Phase 3.5 SEO: 54
```

하지만 이는 Vercel Preview Deployment URL의 인덱싱 차단 영향입니다.

Phase 3.5 측정 URL:

```txt
https://itda-ctk4qlhha-wotj.vercel.app/
```

따라서 SEO 점수는 이번 성능 비교 지표로 사용하지 않습니다.

## 9. 결론

Phase 3.5의 결론은 다음과 같습니다.

```txt
랜딩 페이지의 below-the-fold 애니메이션 초기화를 지연해
Mobile TBT를 350ms에서 0ms로 줄이고,
Main-thread work를 4.7s에서 3.8s로 줄였습니다.
```

수치 근거:

```txt
Mobile Performance: 63 -> 67
Mobile TBT: 350ms -> 0ms
LandingPage JS top long task: 257ms -> 180ms
index JS top long task: 196ms -> 71ms
```

다만 FCP/LCP는 악화되었습니다.

```txt
Mobile FCP: 3.3s -> 4.2s
Mobile LCP: 6.1s -> 6.4s
```

이 결과는 Phase 3.5가 JS/TBT 병목에는 효과적이었지만, hero headline 렌더링과 외부 폰트 CSS 병목은 아직 해결하지 못했다는 의미입니다.

## 10. 다음 개선 방향

다음 단계는 Phase 4 Toss font/css 최적화입니다.

목표:

```txt
hero headline LCP 개선
Toss 외부 CSS render-blocking 감소
font 요청 수와 font 전송량 감소
```

검토 대상:

```txt
itda-frontend/src/assets/styles/base.css
@import url('https://static.toss.im/tps/main.css')
@import url('https://static.toss.im/tps/others.css')
```

Phase 4에서 확인할 항목:

```txt
실제로 사용 중인 font-weight
Toss font CSS를 유지할지, 필요한 font만 self-hosting할지
font-display 전략
UI 인상 변화 여부
Mobile FCP/LCP 개선 여부
```

## 11. 포트폴리오 기록 문장

Phase 3.5는 다음처럼 기록할 수 있습니다.

```txt
Hero video lazy loading 이후 Mobile Lighthouse에서 TBT가 350ms까지 증가한 것을 확인하고,
랜딩 페이지의 GSAP 애니메이션 초기화 구조를 분석했습니다.
기존에는 mount 직후 모든 섹션의 ScrollTrigger, DOM query, workflow title split, 반복 애니메이션이 한 번에 실행되고 있어
초기 JavaScript 실행 비용이 커졌습니다.
이를 hero 영역 즉시 초기화와 below-the-fold 섹션 지연 초기화 구조로 분리해,
Mobile TBT를 350ms에서 0ms로 줄이고 Main-thread work를 4.7s에서 3.8s로 개선했습니다.
```

주의해서 함께 적을 문장:

```txt
다만 FCP/LCP는 외부 Toss font CSS의 render-blocking 영향으로 여전히 악화되어,
다음 개선 과제로 font/css 최적화를 도출했습니다.
```

## 12. 체크리스트

```txt
[x] useLandingAnimations.ts 초기화 구조 분석
[x] hero animation 즉시 실행 유지
[x] features section animation 지연 초기화
[x] workflow section animation/text split 지연 초기화
[x] node-edge cursor/flow animation 지연 초기화
[x] cta section animation 지연 초기화
[x] 빌드 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] Mobile TBT 350ms -> 0ms 개선 확인
[x] scene-1.mp4 초기 요청 제거 유지 확인
[x] FCP/LCP 악화 원인 분석
[ ] Phase 4 Toss font/css 최적화
```
