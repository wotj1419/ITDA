# Phase 4 Analysis: Toss Font CSS Loading Optimization 적용

## 1. 개선 목표

Phase 4의 목표는 Phase 3.5 이후 남아 있던 font/css 병목을 줄이는 것이었습니다.

Phase 3.5 측정에서 hero video 초기 요청은 제거됐고, JavaScript TBT도 0ms까지 개선됐습니다. 하지만 모바일 FCP/LCP는 아직 느렸고 Lighthouse의 render-blocking insight에서 Toss font CSS가 주요 병목으로 잡혔습니다.

Phase 3.5 주요 병목:

```txt
tps/main.css render-blocking wastedMs: 927ms
tps/others.css render-blocking wastedMs: 600ms
Toss third-party transfer: 562,868 bytes
Mobile FCP: 4.19s
Mobile LCP: 6.36s
```

따라서 Phase 4에서는 UI 변경을 최소화하면서 Toss font CSS 로딩 방식을 개선하는 방향으로 작업했습니다.

## 2. 개선이 필요했던 이유

기존 구조에서는 전역 CSS인 `base.css` 최상단에서 Toss font CSS를 `@import`로 불러오고 있었습니다.

```css
@import url('https://static.toss.im/tps/main.css');
@import url('https://static.toss.im/tps/others.css');
```

CSS 내부의 `@import`는 브라우저가 CSS 파일을 파싱하는 과정에서 추가 CSS 요청을 만나게 만드는 구조입니다. 이 방식은 초기 렌더링 경로에서 외부 CSS가 늦게 발견될 수 있고, Lighthouse에서는 render-blocking resource로 잡혔습니다.

특히 랜딩 페이지의 LCP 대상은 hero headline 텍스트이기 때문에, 폰트 CSS 로딩 지연은 첫 텍스트 렌더링과 LCP에 직접 영향을 줄 수 있습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/assets/styles/base.css
itda-frontend/index.html
```

변경 전:

```txt
base.css에서 Toss font CSS를 @import로 로딩
전역 font-family에 Toss Product Sans만 지정
```

변경 후:

```txt
base.css의 Toss font CSS @import 제거
index.html head에서 static.toss.im preconnect 추가
index.html head에서 Toss main.css, others.css를 preload 후 stylesheet로 전환
noscript fallback 추가
전역 font-family에 system fallback 추가
```

적용 방식:

```html
<link rel="preconnect" href="https://static.toss.im" crossorigin />
<link
  rel="preload"
  href="https://static.toss.im/tps/main.css"
  as="style"
  onload="this.onload=null;this.rel='stylesheet'"
/>
<link
  rel="preload"
  href="https://static.toss.im/tps/others.css"
  as="style"
  onload="this.onload=null;this.rel='stylesheet'"
/>
```

전역 fallback:

```css
font-family: 'Toss Product Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

이 방법을 선택한 이유:

```txt
UI에서 사용하는 Toss Product Sans 자체는 유지
폰트 weight 구성도 유지
폰트 CSS 발견 시점을 HTML head로 앞당김
CSS @import 체인에서 외부 CSS를 제거
Toss font CSS가 늦게 로드될 경우 system font로 먼저 렌더링 가능
```

## 4. 측정 파일

Phase 3.5:

```txt
performance-reports/measurements/after/phase3_5/lighthouse_mobile_after_phase3_5.json
performance-reports/measurements/after/phase3_5/lighthouse_desktop_after_phase3_5.json
```

Phase 4:

```txt
performance-reports/measurements/after/phase4/lighthouse_mobile_after_phase4.json
performance-reports/measurements/after/phase4/lighthouse_desktop_after_phase4.json
```

측정 URL:

```txt
Phase 3.5 Preview: https://itda-ctk4qlhha-wotj.vercel.app/
Phase 4 Preview: https://itda-4yc4arge3-wotj.vercel.app/
```

## 5. 개선 전후 결과

### Lighthouse Mobile

| 항목 | Phase 3.5 | Phase 4 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 67 | 67 | 동일 |
| FCP | 4.19s | 4.57s | +0.38s |
| LCP | 6.36s | 6.07s | -0.29s |
| TBT | 0ms | 0ms | 동일 |
| CLS | 0.019 | 0.018 | -0.001 |
| Speed Index | 4.19s | 4.57s | +0.38s |
| TTI | 6.38s | 6.07s | -0.31s |
| Main-thread work | 3.81s | 4.42s | +0.61s |
| Bootup time | 0.69s | 0.79s | +0.10s |
| 총 요청 수 | 64 | 64 | 동일 |
| 총 전송량 | 905.25KiB | 805.69KiB | -99.56KiB |
| CSS 전송량 | 123.93KiB | 45.71KiB | -78.22KiB |
| Font 전송량 | 442.88KiB | 442.87KiB | 거의 동일 |
| JS 전송량 | 173.15KiB | 172.92KiB | -0.23KiB |
| Video 전송량 | 0KiB | 0KiB | 유지 |

### Lighthouse Desktop

| 항목 | Phase 3.5 | Phase 4 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 96 | 95 | -1 |
| FCP | 0.69s | 1.01s | +0.32s |
| LCP | 1.29s | 1.27s | -0.03s |
| TBT | 0ms | 0ms | 동일 |
| CLS | 0.009 | 0.009 | 동일 |
| Speed Index | 1.12s | 1.09s | -0.04s |
| TTI | 1.29s | 1.27s | -0.03s |
| Main-thread work | 2.03s | 1.88s | -0.16s |
| Bootup time | 0.22s | 0.22s | 거의 동일 |
| 총 요청 수 | 69 | 69 | 동일 |
| 총 전송량 | 950.81KiB | 871.85KiB | -78.96KiB |
| CSS 전송량 | 124.05KiB | 45.83KiB | -78.22KiB |
| Font 전송량 | 453.26KiB | 453.26KiB | 동일 |
| JS 전송량 | 195.85KiB | 195.60KiB | -0.25KiB |
| Video 전송량 | 0KiB | 0KiB | 유지 |

## 6. 상세 수치

### Toss CSS 전송량

| 항목 | Phase 3.5 Mobile | Phase 4 Mobile | 변화 |
| --- | ---: | ---: | ---: |
| main.css | 27,683 bytes | 12,767 bytes | -14,916 bytes |
| others.css | 81,678 bytes | 16,547 bytes | -65,131 bytes |
| 합계 | 109,361 bytes | 29,314 bytes | -80,047 bytes |

해석:

```txt
Toss font CSS의 실제 전송량이 약 80KB 감소했습니다.
전체 CSS 전송량도 123.93KiB에서 45.71KiB로 감소했습니다.
```

### Render-blocking insight

Phase 3.5 Mobile:

```txt
https://static.toss.im/tps/others.css
  wastedMs: 600ms
  totalBytes: 81,678

https://static.toss.im/tps/main.css
  wastedMs: 927ms
  totalBytes: 27,683

assets/index-ysD8JUcj.css
  totalBytes: 10,030

Estimated savings: 2,280ms
```

Phase 4 Mobile:

```txt
assets/index-gOuH-bkL.css
  totalBytes: 10,015
```

해석:

```txt
Phase 4 이후 Toss main.css와 others.css는 render-blocking insight 목록에서 제거됐습니다.
남은 render-blocking 항목은 앱의 기본 index CSS 하나입니다.
```

### Third-party Toss transfer

| 항목 | Phase 3.5 | Phase 4 | 변화 |
| --- | ---: | ---: | ---: |
| Mobile toss.im transfer | 562,868 bytes | 482,808 bytes | -80,060 bytes |
| Desktop toss.im transfer | 573,621 bytes | 493,450 bytes | -80,171 bytes |

해석:

```txt
Toss CSS 로딩 방식 변경 후 third-party 전송량이 약 80KB 감소했습니다.
Font woff2 전송량은 거의 동일하고, 감소분은 주로 main.css/others.css에서 발생했습니다.
```

### Mobile long task

| 항목 | Phase 3.5 | Phase 4 |
| --- | ---: | ---: |
| LandingPage JS top long task | 180ms | 277ms |
| Document long task | 83ms | 98ms |
| index JS long task | 71ms | 77ms |
| TBT | 0ms | 0ms |

해석:

```txt
Top long task는 증가했지만 Lighthouse TBT는 0ms로 유지됐습니다.
따라서 Phase 4의 핵심 성과는 JS 실행 시간 개선이 아니라 render-blocking font CSS 제거와 전송량 감소입니다.
```

## 7. 결과 해석

Phase 4에서 좋아진 지표:

```txt
Mobile LCP: 6.36s -> 6.07s
Mobile TTI: 6.38s -> 6.07s
Mobile CLS: 0.019 -> 0.018
Mobile 총 전송량: 905.25KiB -> 805.69KiB
Mobile CSS 전송량: 123.93KiB -> 45.71KiB
Mobile Toss third-party transfer: 562,868 bytes -> 482,808 bytes
Toss CSS render-blocking 항목 제거
```

유지된 지표:

```txt
Mobile Performance: 67 -> 67
Mobile TBT: 0ms -> 0ms
Video 초기 전송량: 0KiB 유지
```

나빠진 지표:

```txt
Mobile FCP: 4.19s -> 4.57s
Mobile Speed Index: 4.19s -> 4.57s
Mobile Main-thread work: 3.81s -> 4.42s
Desktop Performance: 96 -> 95
Desktop FCP: 0.69s -> 1.01s
```

종합 해석:

```txt
Phase 4는 전체 Lighthouse 점수를 올리지는 못했지만,
Phase 3.5에서 확인한 font/css render-blocking 병목을 실제로 제거했습니다.

특히 Toss CSS가 render-blocking insight에서 사라졌고,
CSS 전송량과 third-party 전송량이 약 80KB 줄었습니다.

다만 preload + onload 방식으로 바꾸면서 초기 paint 타이밍은 측정상 약간 늦어졌습니다.
이는 폰트 CSS가 렌더 차단에서는 빠졌지만, hero headline의 실제 최종 폰트 적용 타이밍과
Lighthouse의 filmstrip 기준 paint 타이밍이 측정 환경에 따라 달라졌기 때문으로 볼 수 있습니다.
```

## 8. Phase 4 결론

Phase 4의 결론은 다음과 같습니다.

```txt
Toss font CSS를 CSS @import 체인에서 제거하고 HTML preload 방식으로 분리해
Mobile 기준 CSS 전송량을 123.93KiB에서 45.71KiB로 줄였고,
Toss third-party 전송량을 562,868 bytes에서 482,808 bytes로 줄였습니다.

또한 Phase 3.5에서 render-blocking resource로 잡히던
tps/main.css와 tps/others.css를 render-blocking 목록에서 제거했습니다.
```

수치 근거:

```txt
Mobile CSS transfer: 123.93KiB -> 45.71KiB
Mobile toss.im transfer: 562,868 bytes -> 482,808 bytes
Mobile LCP: 6.36s -> 6.07s
Mobile Performance: 67 -> 67
Mobile TBT: 0ms 유지
```

주의할 점:

```txt
Performance 점수 자체는 상승하지 않았습니다.
FCP와 Speed Index는 오히려 0.38s 느려졌습니다.
따라서 Phase 4는 점수 개선보다 병목 제거와 전송량 절감 성격의 개선으로 정리하는 것이 정확합니다.
```

## 9. 포트폴리오 기록 문장

Phase 4는 다음처럼 기록할 수 있습니다.

```txt
Lighthouse 분석에서 Toss Product Sans 외부 CSS가 render-blocking resource로 잡히는 것을 확인했습니다.
기존에는 전역 CSS 내부에서 @import로 폰트 CSS를 불러와 초기 렌더링 경로에 포함됐기 때문에,
이를 index.html의 preconnect와 preload 기반 로딩으로 분리했습니다.

그 결과 모바일 기준 CSS 전송량을 123.93KiB에서 45.71KiB로 약 63% 줄였고,
Toss third-party 전송량을 562,868 bytes에서 482,808 bytes로 약 80KB 절감했습니다.
또한 tps/main.css와 tps/others.css가 Lighthouse render-blocking 목록에서 제거됐습니다.

다만 Performance 점수는 67점으로 유지됐고 FCP는 4.19s에서 4.57s로 느려져,
해당 개선은 점수 상승보다 render-blocking 병목 제거와 네트워크 전송량 절감 중심의 개선으로 판단했습니다.
```

## 10. 다음 개선 방향

Phase 4 이후 남은 개선 후보:

```txt
1. 실제로 필요한 font-weight만 유지할 수 있는지 검토
2. Toss others.css 제거 가능 여부 검토
3. 랜딩 페이지에서 사용하는 font-weight를 400/700 중심으로 단순화할 수 있는지 검토
4. self-hosting이 가능한 라이선스인지 확인 후 필요한 woff2만 직접 제공하는 방식 검토
5. 앱 index CSS를 더 줄이거나 critical CSS 범위를 나누는 방식 검토
```

주의:

```txt
others.css 제거 또는 font-weight 축소는 UI 글자 굵기 변화 가능성이 큽니다.
따라서 다음 단계는 성능 점수만 보고 진행하기보다,
랜딩 페이지 스크린샷 비교와 함께 결정해야 합니다.
```

## 11. 체크리스트

```txt
[x] base.css의 Toss font CSS @import 제거
[x] index.html에 static.toss.im preconnect 추가
[x] Toss main.css preload 적용
[x] Toss others.css preload 적용
[x] noscript fallback 추가
[x] 전역 font-family system fallback 추가
[x] build 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] Toss CSS render-blocking 제거 확인
[x] CSS 전송량 감소 확인
[x] Performance 점수 유지 확인
[x] FCP/Speed Index 악화 확인
[ ] 다음 단계 font-weight 축소 여부 결정
```
