# Phase 4.5 Analysis: Hero Poster Lazy Loading 적용

## 1. 개선 목표

Phase 4.5의 목표는 Phase 4 이후에도 초기 로딩에 포함되던 `scene-1-poster.webp` 요청을 지연하는 것이었습니다.

Phase 4 Mobile 기준:

```txt
scene-1.mp4 초기 전송량: 0KiB
scene-1-poster.webp 전송량: 112,518 bytes
Mobile total transfer: 805.69KiB
```

`scene-1.mp4`는 이미 lazy loading으로 초기 요청에서 제거됐지만, `poster` 속성은 초기 렌더링부터 연결되어 있어 약 112KB 이미지 요청이 남아 있었습니다.

## 2. 개선 방법

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
```

변경 전:

```vue
<video
  :src="shouldLoadHeroVideo ? selectedHeroVideo : undefined"
  poster="/scene-1-poster.webp"
  preload="none"
/>
```

변경 후:

```vue
<video
  :src="shouldLoadHeroVideo ? selectedHeroVideo : undefined"
  :poster="shouldLoadHeroPoster ? '/scene-1-poster.webp' : undefined"
  preload="none"
/>
```

추가한 구조:

```txt
shouldLoadHeroPoster 상태 추가
heroPosterObserver 추가
mockup 영역 근처 진입 시 poster 연결
video 로드 시 loadHeroPoster() 먼저 호출
unmount 시 poster observer cleanup 추가
```

## 3. 측정 파일

Phase 4:

```txt
performance-reports/measurements/after/phase4/lighthouse_mobile_after_phase4.json
performance-reports/measurements/after/phase4/lighthouse_desktop_after_phase4.json
```

Phase 4.5:

```txt
performance-reports/measurements/after/phase4_5/lighthouse_moblie_after_phase4_5.json
performance-reports/measurements/after/phase4_5/lighthouse_desktop_after_phase4_5.json
```

측정 URL:

```txt
Phase 4: https://itda-4yc4arge3-wotj.vercel.app/
Phase 4.5: https://itda-pfsv47rdv-wotj.vercel.app/
```

## 4. 개선 전후 결과

### Lighthouse Mobile

| 항목 | Phase 4 | Phase 4.5 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 67 | 66 | -1 |
| FCP | 4.57s | 4.88s | +0.31s |
| LCP | 6.07s | 6.08s | +0.01s |
| TBT | 0ms | 0ms | 동일 |
| CLS | 0.020 | 0.020 | 동일 |
| Speed Index | 4.57s | 4.88s | +0.31s |
| Main-thread work | 4.42s | 4.17s | -0.25s |
| 총 요청 수 | 64 | 64 | 동일 |
| 총 전송량 | 805.69KiB | 805.97KiB | +0.28KiB |
| Image 전송량 | 109.95KiB | 109.96KiB | 거의 동일 |
| scene-1-poster.webp | 112,518 bytes | 112,541 bytes | +23 bytes |
| scene-1.mp4 | 0 | 0 | 유지 |

### Lighthouse Desktop

| 항목 | Phase 4 | Phase 4.5 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 95 | 95 | 동일 |
| FCP | 1.01s | 1.06s | +0.05s |
| LCP | 1.27s | 1.22s | -0.05s |
| TBT | 0ms | 0ms | 동일 |
| CLS | 0.009 | 0.009 | 동일 |
| Speed Index | 1.09s | 1.06s | -0.03s |
| Main-thread work | 1.88s | 1.74s | -0.14s |
| 총 요청 수 | 69 | 69 | 동일 |
| 총 전송량 | 871.85KiB | 872.04KiB | +0.19KiB |
| scene-1-poster.webp | 112,523 bytes | 112,566 bytes | +43 bytes |
| scene-1.mp4 | 0 | 0 | 유지 |

## 5. 결과 해석

기대했던 결과:

```txt
초기 page load에서 scene-1-poster.webp 요청 제거
Mobile total transfer 약 112KB 감소
```

실제 결과:

```txt
scene-1-poster.webp 요청은 Phase 4.5에서도 유지됐습니다.
Mobile total transfer도 805.69KiB에서 805.97KiB로 거의 동일했습니다.
```

원인:

```txt
IntersectionObserver가 page load 직후 바로 발동한 것으로 판단됩니다.
랜딩 첫 화면에서 mockup 영역이 viewport 하단 또는 rootMargin 범위에 이미 들어와 있어,
shouldLoadHeroPoster가 초기 로딩 중 true로 바뀌고 poster가 바로 연결됐습니다.
```

즉, 코드 구조는 조건부 로딩으로 바뀌었지만 현재 랜딩 페이지 배치에서는 poster 로딩 시점이 충분히 늦춰지지 않았습니다.

## 6. Lighthouse 수치 변화가 작은 이유

```txt
1. 실제로 poster 요청이 제거되지 않아 전송량 절감이 발생하지 않았습니다.
2. poster는 현재 LCP 대상이 아닙니다.
3. 현재 LCP는 hero headline 텍스트 쪽이라 font/css와 초기 렌더링 타이밍의 영향을 더 많이 받습니다.
4. Phase 3에서 이미 가장 큰 병목인 scene-1.mp4 7.8MiB 초기 전송을 제거했습니다.
5. TBT는 이미 0ms라 JavaScript 실행 측면에서 추가 점수 개선 여지가 작습니다.
```

따라서 Phase 4.5의 결과는 성능 점수 상승보다, “현재 UI 배치에서는 poster 지연 로딩 효과가 제한적이다”는 검증 결과로 보는 것이 정확합니다.

## 7. 결론

Phase 4.5 결론:

```txt
Hero poster를 상태 기반으로 지연 연결하도록 구조를 개선했지만,
현재 랜딩 페이지에서 mockup 영역이 초기 viewport와 가까워 observer가 바로 발동했습니다.

그 결과 scene-1-poster.webp 요청은 초기 로딩에 그대로 포함됐고,
Mobile Performance는 67점에서 66점으로 거의 동일했습니다.
```

유지된 성과:

```txt
scene-1.mp4 초기 요청 0 유지
Mobile TBT 0ms 유지
CLS 안정성 유지
```

개선되지 않은 부분:

```txt
scene-1-poster.webp 초기 요청 제거 실패
총 전송량 감소 없음
Mobile Performance 점수 개선 없음
```

## 8. 다음 판단

선택지는 두 가지입니다.

```txt
1. poster 로딩을 더 늦춘다
   - rootMargin 제거
   - threshold 상향
   - 실제 mockup 노출 또는 클릭 시점까지 지연
   - 단점: 검은 화면 노출 가능성 증가

2. Phase 4.5는 실험 결과로 기록하고 다음 병목으로 이동한다
   - 추천 방향
   - 이유: UI 변화 최소화 조건에서는 poster를 더 늦추는 것이 사용자 체감 품질을 해칠 수 있음
```

추천:

```txt
Phase 4.5는 실험 결과로 마무리하고,
다음 단계에서는 Lighthouse에서 약 65~73KiB 절감 가능성이 표시된 unused JS / 초기 entry chunk 분석으로 이동하는 것이 좋습니다.
```

## 9. 포트폴리오 기록 문장

```txt
Hero video lazy loading 이후에도 약 112KB의 poster 이미지가 초기 요청에 포함되는 것을 확인하고,
poster 속성을 상태 기반으로 분리해 IntersectionObserver 진입 시점에 연결되도록 개선했습니다.

다만 Vercel Preview 환경에서 Lighthouse를 재측정한 결과,
mockup 영역이 초기 viewport와 가까워 observer가 page load 직후 발동했고
scene-1-poster.webp 요청은 112,518 bytes에서 112,541 bytes로 유지됐습니다.

이를 통해 단순 조건부 바인딩만으로는 현재 레이아웃에서 초기 poster 전송을 제거하기 어렵다는 점을 확인했고,
UI 안정성을 해치지 않는 범위에서는 추가 지연보다 unused JS 분석으로 우선순위를 전환하는 것이 합리적이라고 판단했습니다.
```

## 10. 체크리스트

```txt
[x] scene-1-poster.webp 초기 전송량 확인
[x] shouldLoadHeroPoster 상태 추가
[x] poster 속성 조건부 바인딩 적용
[x] poster observer 추가
[x] video lazy loading 유지
[x] build 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] scene-1.mp4 초기 요청 0 유지 확인
[x] scene-1-poster.webp 요청 유지 확인
[x] Lighthouse 점수 개선 없음 확인
[ ] 다음 단계 unused JS / 초기 entry chunk 분석 진행
```
