# Phase 6 Analysis: 초기 Entry 번들 Code Splitting 적용

## 1. 개선 목표

Phase 6의 목표는 랜딩 페이지 초기 로딩에 포함되던 불필요한 JavaScript를 줄이는 것입니다.

Phase 4.5 Mobile 기준:

```txt
Performance: 66
Script transfer: 177,222 bytes
Unused JavaScript savings: 65,031 bytes
초기 entry JS: assets/index-CuT22di-.js
초기 entry JS resourceSize: 318,604 bytes
```

Lighthouse에서 unused JavaScript가 약 65KB로 잡혔고, 빌드 결과에서도 초기 entry chunk가 큰 편이었습니다.

분석 결과 랜딩 페이지에서 사용하지 않는 협업 관련 코드가 `App.vue`를 통해 초기 entry에 포함되고 있었습니다.

```txt
CollabContainer
collab store
WebSocket/WebRTC 협업 로직
프로젝트 협업 UI 관련 코드
```

랜딩 페이지는 `/projects` 경로가 아니므로 해당 코드는 첫 화면 렌더링에 필요하지 않습니다.

## 2. 개선이 필요했던 이유

Phase 3까지의 작업으로 가장 큰 네트워크 병목이었던 `scene-1.mp4` 초기 요청은 제거됐습니다.

```txt
scene-1.mp4 초기 전송량: 7.8MiB -> 0
```

Phase 3.5와 Phase 4를 거치면서 TBT와 font/css 병목도 일부 확인했습니다.

하지만 Phase 4.5 이후에도 Lighthouse는 초기 JS에 unused JavaScript가 남아 있다고 표시했습니다.

```txt
Mobile unused JavaScript: 65,031 bytes
Desktop unused JavaScript: 73,551 bytes
```

특히 `App.vue`는 모든 페이지에서 공통으로 실행되는 최상위 컴포넌트이기 때문에, 여기에 정적 import된 코드는 랜딩 페이지에서도 초기 entry에 포함됩니다.

즉, 실제 사용 경로는 프로젝트 편집/협업 페이지인데도 랜딩 페이지 초기 로딩 비용으로 계산되고 있었습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/App.vue
itda-frontend/src/components/collab/CollabRouteLayer.vue
itda-frontend/src/stores/project.ts
```

### App.vue 개선

변경 전 구조:

```txt
App.vue
  CollabContainer 정적 import
  useCollabStore 정적 import
  onMounted에서 collabStore.rejoinIfNeeded() 실행
  watch로 /projects 경로 이탈 시 leaveRoom() 실행
```

변경 후 구조:

```txt
App.vue
  CollabRouteLayer를 defineAsyncComponent로 동적 import
  현재 route가 /projects로 시작할 때만 CollabRouteLayer 렌더링
```

핵심 변경:

```ts
const CollabRouteLayer = defineAsyncComponent(() =>
  import('./components/collab/CollabRouteLayer.vue')
)
```

```vue
<CollabRouteLayer v-if="showCollabUI" />
```

### CollabRouteLayer 추가

새로 추가한 컴포넌트:

```txt
itda-frontend/src/components/collab/CollabRouteLayer.vue
```

역할:

```txt
CollabContainer 렌더링
collab store 연결
프로젝트 경로 진입 시 rejoinIfNeeded 실행
프로젝트 경로 이탈 시 leaveRoom 실행
```

이렇게 분리하면 협업 관련 코드는 `/projects` 경로에서만 로드됩니다.

### project.ts import 정리

`project.ts`에서는 동일한 API 모듈을 정적 import와 동적 import로 함께 사용하고 있어 Vite 경고가 발생했습니다.

```txt
동일 모듈이 static import와 dynamic import로 동시에 사용됨
dynamic import가 chunk 분리에 실질적으로 기여하지 못함
```

따라서 이미 정적으로 사용 중인 API 함수는 정적 import로 통일했습니다.

## 4. 측정 파일

Phase 4.5:

```txt
performance-reports/measurements/after/phase4_5/lighthouse_moblie_after_phase4_5.json
performance-reports/measurements/after/phase4_5/lighthouse_desktop_after_phase4_5.json
```

Phase 6:

```txt
performance-reports/measurements/after/phase6/lighthouse_mobile_after_phase6.json
performance-reports/measurements/after/phase6/lighthouse_desktop_after_phase6.json
```

측정 URL:

```txt
Phase 4.5: https://itda-pfsv47rdv-wotj.vercel.app/
Phase 6: https://itda-iirc6tu6d-wotj.vercel.app/
```

## 5. 빌드 결과

빌드 명령:

```txt
npm run build
```

빌드 결과:

```txt
build 성공
Vite dynamic/static import 경고 제거
```

초기 entry chunk 변화:

| 항목 | Phase 4.5 | Phase 6 | 변화 |
| --- | ---: | ---: | ---: |
| 초기 entry JS resourceSize | 318,604 bytes | 122,444 bytes | -196,160 bytes |
| 초기 entry JS transferSize | 115,746 bytes | 49,714 bytes | -66,032 bytes |

새로 분리된 협업 chunk:

```txt
collab-ArlL0bP9.js: 150.04 kB
gzip: 46.82 kB
CollabRouteLayer-LN1_Ccaq.js: 1.94 kB
```

해석:

```txt
랜딩 페이지에서 필요하지 않은 협업 관련 코드가 초기 entry에서 분리되었습니다.
협업 코드는 /projects 경로 진입 시 별도 chunk로 로드됩니다.
```

## 6. 개선 전후 결과

### Lighthouse Mobile

| 항목 | Phase 4.5 | Phase 6 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 66 | 62 | -4 |
| FCP | 4.88s | 2.75s | -2.13s |
| LCP | 6.08s | 5.61s | -0.47s |
| TBT | 0ms | 485ms | +485ms |
| CLS | 0.018 | 0.023 | +0.005 |
| Speed Index | 4.88s | 3.01s | -1.87s |
| TTI | 6.08s | 5.62s | -0.46s |
| Main-thread work | 4.17s | 4.82s | +0.65s |
| Bootup time | 0.71s | 0.66s | -0.05s |
| 총 요청 수 | 63 | 63 | 동일 |
| 총 전송량 | 825,310 bytes | 759,272 bytes | -66,038 bytes |
| Script 전송량 | 177,222 bytes | 111,482 bytes | -65,740 bytes |
| Font 전송량 | 453,499 bytes | 453,487 bytes | 거의 동일 |
| Image 전송량 | 112,595 bytes | 112,619 bytes | 거의 동일 |
| Unused JavaScript | 65,031 bytes | 0 bytes | -65,031 bytes |

### Lighthouse Desktop

| 항목 | Phase 4.5 | Phase 6 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 95 | 97 | +2 |
| FCP | 1.06s | 0.72s | -0.34s |
| LCP | 1.22s | 1.18s | -0.04s |
| TBT | 0ms | 43ms | +43ms |
| CLS | 0.008 | 0.010 | +0.002 |
| Speed Index | 1.06s | 1.04s | -0.02s |
| TTI | 1.22s | 1.18s | -0.04s |
| Main-thread work | 1.74s | 2.15s | +0.41s |
| Bootup time | 0.20s | 0.23s | +0.03s |
| 총 요청 수 | 68 | 68 | 동일 |
| 총 전송량 | 892,970 bytes | 826,887 bytes | -66,083 bytes |
| Script 전송량 | 200,477 bytes | 134,532 bytes | -65,945 bytes |
| Font 전송량 | 464,124 bytes | 464,142 bytes | 거의 동일 |
| Image 전송량 | 146,908 bytes | 146,954 bytes | 거의 동일 |
| Unused JavaScript | 73,551 bytes | 0 bytes | -73,551 bytes |

## 7. 상세 수치

### Script 전송량

| 항목 | Phase 4.5 | Phase 6 | 변화 |
| --- | ---: | ---: | ---: |
| Mobile Script transfer | 177,222 bytes | 111,482 bytes | -65,740 bytes |
| Desktop Script transfer | 200,477 bytes | 134,532 bytes | -65,945 bytes |
| Mobile unused JS | 65,031 bytes | 0 bytes | -65,031 bytes |
| Desktop unused JS | 73,551 bytes | 0 bytes | -73,551 bytes |

Phase 6의 목표였던 unused JavaScript 제거와 초기 JS 전송량 감소는 달성됐습니다.

```txt
Mobile 기준 약 65.7KB JS 전송량 감소
Desktop 기준 약 65.9KB JS 전송량 감소
Lighthouse unused JavaScript 항목 0 bytes로 개선
```

### 초기 entry chunk

Phase 4.5 Mobile:

```txt
assets/index-CuT22di-.js
transferSize: 115,746 bytes
resourceSize: 318,604 bytes
```

Phase 6 Mobile:

```txt
assets/index-DhvcpA_b.js
transferSize: 49,714 bytes
resourceSize: 122,444 bytes
```

변화:

```txt
transferSize: -66,032 bytes
resourceSize: -196,160 bytes
```

초기 entry chunk 자체는 크게 줄었습니다.

### Long task

Phase 4.5 Mobile top long tasks:

```txt
LandingPage JS: 215ms
Unattributable: 83ms
Document: 76ms
index JS: 62ms
```

Phase 6 Mobile top long tasks:

```txt
LandingPage JS: 519ms
Unattributable: 82ms
Unattributable: 72ms
index JS: 66ms
Document: 58ms
```

Phase 6에서 초기 JS 전송량은 줄었지만, Mobile TBT는 0ms에서 485ms로 증가했습니다.

가장 큰 원인은 Phase 6 측정에서 `LandingPage` chunk 실행이 519ms long task로 측정된 점입니다.

### Main-thread work

| 항목 | Phase 4.5 Mobile | Phase 6 Mobile | 변화 |
| --- | ---: | ---: | ---: |
| Style/Layout | 1.27s | 1.98s | +0.71s |
| Script Evaluation | 0.72s | 0.66s | -0.06s |
| Paint/Composite/Render | 0.40s | 0.38s | -0.02s |
| Other | 1.77s | 1.77s | 동일 |
| Main-thread work 전체 | 4.17s | 4.82s | +0.65s |

해석:

```txt
Script Evaluation 자체는 0.72s에서 0.66s로 감소했습니다.
하지만 Style/Layout 비용이 1.27s에서 1.98s로 증가하면서 전체 main-thread work는 늘었습니다.
```

즉, Phase 6의 코드 분리는 JS 네트워크 비용에는 효과가 있었지만, 이번 Mobile Lighthouse 점수 하락은 번들 크기보다 런타임 레이아웃/애니메이션 계산 비용의 영향을 더 크게 받은 것으로 볼 수 있습니다.

## 8. 결과 해석

Phase 6에서 좋아진 지표:

```txt
Mobile FCP: 4.88s -> 2.75s
Mobile LCP: 6.08s -> 5.61s
Mobile Speed Index: 4.88s -> 3.01s
Mobile Script transfer: 177,222 bytes -> 111,482 bytes
Mobile unused JS: 65,031 bytes -> 0 bytes
Desktop Performance: 95 -> 97
Desktop Script transfer: 200,477 bytes -> 134,532 bytes
Desktop unused JS: 73,551 bytes -> 0 bytes
```

Phase 6에서 나빠진 지표:

```txt
Mobile Performance: 66 -> 62
Mobile TBT: 0ms -> 485ms
Mobile Main-thread work: 4.17s -> 4.82s
Mobile LandingPage JS top long task: 215ms -> 519ms
Desktop TBT: 0ms -> 43ms
```

정리하면 Phase 6은 다음과 같이 해석할 수 있습니다.

```txt
초기 entry chunk 분리와 unused JS 제거는 성공했습니다.
초기 JS 전송량도 약 65KB 줄었습니다.
하지만 Mobile 측정에서는 LandingPage long task와 Style/Layout 비용이 증가해 Performance 점수는 하락했습니다.
```

따라서 Phase 6의 성과는 Lighthouse 점수 상승이 아니라, 초기 번들 구조 개선과 불필요한 JS 제거로 기록하는 것이 정확합니다.

## 9. Lighthouse 점수가 크게 오르지 않은 이유

```txt
1. Phase 3에서 이미 가장 큰 병목인 7.8MiB video 초기 요청을 제거했습니다.
2. Phase 3.5에서 TBT가 이미 0ms까지 개선된 상태였습니다.
3. Phase 6은 네트워크 JS 전송량을 줄였지만, Mobile 측정에서는 런타임 long task가 새로 크게 잡혔습니다.
4. 현재 LCP는 이미지나 영상보다 hero headline 텍스트 렌더링 영향을 더 많이 받습니다.
5. Toss font 전송량은 Phase 6에서도 약 453KB로 유지됐습니다.
6. Vercel Preview + Lighthouse Mobile 환경은 측정 편차가 있고, TBT는 특정 long task 하나에도 점수가 크게 흔들립니다.
```

특히 이번 Phase 6 Mobile에서는 `LandingPage` chunk long task가 519ms로 측정되었습니다.

```txt
Phase 4.5 LandingPage JS top long task: 215ms
Phase 6 LandingPage JS top long task: 519ms
```

이 때문에 FCP/LCP/Speed Index가 좋아졌음에도 Performance 점수는 66점에서 62점으로 하락했습니다.

## 10. 결론

Phase 6 결론:

```txt
App.vue에 정적으로 포함되어 있던 협업 UI와 collab store를 CollabRouteLayer로 분리하고,
defineAsyncComponent를 적용해 /projects 경로에서만 협업 관련 chunk가 로드되도록 개선했습니다.

그 결과 Mobile 기준 Script transfer는 177,222 bytes에서 111,482 bytes로 약 65.7KB 감소했고,
Lighthouse unused JavaScript는 65,031 bytes에서 0 bytes로 개선됐습니다.
```

수치 근거:

```txt
초기 entry JS resourceSize: 318,604 bytes -> 122,444 bytes
초기 entry JS transferSize: 115,746 bytes -> 49,714 bytes
Mobile Script transfer: 177,222 bytes -> 111,482 bytes
Mobile unused JavaScript: 65,031 bytes -> 0 bytes
Desktop Performance: 95 -> 97
```

주의할 점:

```txt
Mobile Performance는 66점에서 62점으로 하락했습니다.
이는 Phase 6의 번들 분리 실패가 아니라,
이번 측정에서 LandingPage long task와 Style/Layout 비용이 크게 잡힌 영향입니다.
```

따라서 포트폴리오에는 “점수 상승”보다 “초기 로딩에 불필요한 협업 코드를 라우트 단위로 분리해 JS 전송량과 unused JS를 줄였다”는 성과로 기록하는 것이 좋습니다.

## 11. 다음 개선 방향

다음 개선 후보:

```txt
1. LandingPage long task 분석
   - Phase 6 Mobile에서 LandingPage JS long task가 519ms로 증가
   - GSAP 초기화, ScrollTrigger, DOM query, layout 계산 흐름 재점검

2. Style/Layout 비용 분석
   - Mobile Style/Layout: 1.27s -> 1.98s
   - hero headline, section layout, animation 초기화 시 layout thrashing 가능성 확인

3. icon.png 최적화
   - 기존 계획의 Phase 5 후보
   - 단, 현재 점수 병목은 이미지보다 JS/layout 쪽 영향이 더 큼
```

추천:

```txt
Phase 6 이후에는 바로 icon.png 최적화로 넘어가기보다,
Phase 6 Mobile에서 새로 커진 LandingPage long task와 Style/Layout 비용을 먼저 점검하는 것이 좋습니다.
```

## 12. 포트폴리오 기록 문장

Phase 6은 다음처럼 기록할 수 있습니다.

```txt
Lighthouse 분석에서 랜딩 페이지 초기 로딩에 약 65KB의 unused JavaScript가 포함되는 것을 확인했습니다.
원인을 추적한 결과, 모든 페이지의 최상위 컴포넌트인 App.vue에서 프로젝트 협업 UI와 collab store를 정적으로 import하고 있어,
랜딩 페이지에서도 WebSocket/WebRTC 기반 협업 코드가 초기 entry chunk에 포함되고 있었습니다.

이를 CollabRouteLayer 컴포넌트로 분리하고 defineAsyncComponent를 적용해,
/projects 경로에서만 협업 관련 chunk가 로드되도록 라우트 단위 code splitting을 적용했습니다.

그 결과 초기 entry JS resourceSize를 318,604 bytes에서 122,444 bytes로 줄였고,
Mobile 기준 Script transfer를 177,222 bytes에서 111,482 bytes로 약 65.7KB 감소시켰으며,
Lighthouse unused JavaScript를 65,031 bytes에서 0 bytes로 개선했습니다.
```

주의해서 함께 적을 문장:

```txt
다만 Mobile Lighthouse Performance 점수는 66점에서 62점으로 하락했습니다.
이는 Phase 6 측정에서 LandingPage long task와 Style/Layout 비용이 크게 증가한 영향으로,
다음 개선 과제로 랜딩 페이지 런타임 초기화 비용 분석을 도출했습니다.
```

## 13. 체크리스트

```txt
[x] Phase 4.5 unused JavaScript 확인
[x] 초기 entry chunk 분석
[x] App.vue의 협업 코드 정적 import 확인
[x] CollabRouteLayer 컴포넌트 추가
[x] CollabRouteLayer 동적 import 적용
[x] /projects 경로에서만 협업 UI 로드되도록 변경
[x] project.ts dynamic/static import 경고 정리
[x] build 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] 초기 entry JS 감소 확인
[x] unused JavaScript 0 bytes 개선 확인
[x] Mobile TBT 악화 확인
[ ] Phase 6 이후 LandingPage long task / Style/Layout 비용 추가 분석
```
