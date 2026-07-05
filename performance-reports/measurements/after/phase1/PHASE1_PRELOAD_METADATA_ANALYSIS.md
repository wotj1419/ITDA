# Phase 1 Analysis: Hero Video Preload Metadata

## 1. 개선 목표

Phase 1의 목표는 랜딩 페이지 UI를 유지하면서 hero video의 초기 네트워크 비용을 줄이는 것이었습니다.

현재 랜딩 페이지에서 가장 큰 리소스는 `/scene-1.mp4`입니다.

개선 전 측정 결과:

```txt
전체 전송량: 8.5MiB
scene-1.mp4 전송량: 7.8MiB
```

즉, 전체 초기 전송량의 대부분이 hero video에서 발생했습니다.

## 2. 개선이 필요했던 이유

Lighthouse Mobile 기준으로 다음 문제가 확인되었습니다.

```txt
Performance: 69
FCP: 4.9s
LCP: 5.1s
Speed Index: 4.9s
총 전송량: 8.5MiB
Video 전송량: 7.8MiB
```

특히 `/scene-1.mp4`는 단일 파일로 7.8MiB를 차지했습니다.

이 리소스가 초기 로딩에 포함되면 다음 문제가 발생할 수 있습니다.

```txt
느린 네트워크에서 초기 렌더링 지연
Mobile Lighthouse Performance 하락
사용자가 영상을 보기 전에도 큰 데이터 다운로드
랜딩 페이지 초기 전송량 증가
```

따라서 가장 먼저, UI 변경 가능성이 낮은 `preload` 전략 변경을 실험했습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/src/pages/LandingPage.vue
```

변경 내용:

```diff
- preload="auto"
+ preload="metadata"
```

변경 전:

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

변경 후:

```vue
<video
  ref="heroVideoRef"
  :src="selectedHeroVideo"
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
코드 변경 범위가 작음
UI 변경 가능성이 낮음
autoplay, loop, muted, playsinline 동작은 유지됨
브라우저에 영상 전체 선다운로드 대신 metadata 중심 로드를 유도할 수 있음
```

## 4. 측정 파일

개선 전:

```txt
performance-reports/measurements/before/lighthouse_mobile_before.json
```

Phase 1 후:

```txt
performance-reports/measurements/after/phase1/lighthouse_mobile_after_phase1.json
```

주의:

```txt
Phase 1 측정 파일은 관리 편의를 위해 `lighthouse_mobile_after_phase1.json` 이름으로 정리했습니다.
```

## 5. 개선 전/후 결과

### Lighthouse Mobile

| 항목 | 개선 전 | Phase 1 후 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 69 | 70 | +1 |
| FCP | 4.9s | 3.8s | -1.1s |
| LCP | 5.1s | 5.7s | +0.6s |
| TBT | 0ms | 20ms | +20ms |
| CLS | 0.02 | 0.027 | +0.007 |
| Speed Index | 4.9s | 3.8s | -1.1s |
| TTI | 5.1s | 5.7s | +0.6s |
| 총 요청 수 | 63 | 64 | +1 |
| 총 전송량 | 8.5MiB | 8.5MiB | 거의 동일 |
| Video 전송량 | 7.8MiB | 7.8MiB | 변화 없음 |
| Font 전송량 | 442.9KiB | 453.3KiB | 거의 동일 |
| JS 전송량 | 172.7KiB | 172.8KiB | 거의 동일 |

## 6. 상세 수치

### 전체 전송량

| 항목 | 개선 전 | Phase 1 후 |
| --- | ---: | ---: |
| 총 전송량(bytes) | 8,892,376 | 8,957,890 |
| 총 리소스 크기(bytes) | 9,597,283 | 9,597,450 |
| 요청 수 | 63 | 64 |

총 전송량은 오히려 약간 증가했습니다.

증가량:

```txt
8,957,890 - 8,892,376 = 65,514 bytes
약 64KiB 증가
```

### Hero video 전송량

| 항목 | 개선 전 | Phase 1 후 |
| --- | ---: | ---: |
| scene-1.mp4 transferSize | 8,180,044 bytes | 8,180,096 bytes |
| scene-1.mp4 resourceSize | 8,175,293 bytes | 8,175,293 bytes |
| statusCode | 206 | 206 |
| priority | Low | Low |

`scene-1.mp4` 전송량은 사실상 동일합니다.

차이:

```txt
8,180,096 - 8,180,044 = 52 bytes
```

즉, `preload="metadata"` 변경만으로는 hero video 전송량이 줄지 않았습니다.

## 7. 결과 해석

Phase 1은 일부 지표를 개선했지만, 핵심 병목인 video 전송량은 해결하지 못했습니다.

좋아진 지표:

```txt
FCP: 4.9s -> 3.8s
Speed Index: 4.9s -> 3.8s
Performance: 69 -> 70
```

나빠진 지표:

```txt
LCP: 5.1s -> 5.7s
TTI: 5.1s -> 5.7s
TBT: 0ms -> 20ms
CLS: 0.02 -> 0.027
```

단, TBT와 CLS는 여전히 양호한 범위입니다.

핵심 판단:

```txt
preload 속성만 metadata로 변경하는 방식은 충분하지 않음
autoplay와 즉시 play 호출 구조 때문에 브라우저가 결국 영상을 다운로드함
```

관련 코드:

```ts
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  playHeroVideo(heroVideoRef.value)
})
```

그리고 `playHeroVideo` 내부에서 mount 직후 영상 재생을 시도합니다.

```ts
const playHeroVideo = (videoElement: HTMLVideoElement | null) => {
  if (!videoElement) return
  videoElement.currentTime = 0
  videoElement.muted = heroVideoMuted.value
  videoElement.playsInline = true
  void videoElement.play().catch(() => {})
}
```

따라서 `preload="metadata"`를 적용해도, `autoplay`와 `play()` 호출로 인해 영상 데이터가 계속 다운로드됩니다.

## 8. SEO 점수 주의 사항

Phase 1 후 SEO 점수는 크게 낮아졌습니다.

```txt
개선 전 SEO: 91
Phase 1 후 SEO: 54
```

하지만 이는 성능 변경 때문이 아니라 측정 URL 차이의 영향입니다.

Phase 1 측정 URL:

```txt
https://itda-git-fe-refactorlanding-performance-wotj.vercel.app/
```

Vercel Preview Deployment는 검색 엔진 인덱싱이 차단될 수 있어 Lighthouse SEO에서 다음 항목이 실패했습니다.

```txt
Page is blocked from indexing
Document does not have a meta description
```

따라서 Phase 1 비교에서는 SEO 점수를 핵심 비교 지표로 사용하지 않습니다.

## 9. 결론

Phase 1의 결론은 다음과 같습니다.

```txt
preload="auto"를 preload="metadata"로 변경했지만,
hero video의 실제 초기 전송량은 줄지 않았다.
```

수치 근거:

```txt
scene-1.mp4 개선 전: 8,180,044 bytes
scene-1.mp4 Phase 1 후: 8,180,096 bytes
```

따라서 Phase 1은 "가벼운 preload 조정만으로는 병목을 해결할 수 없다"는 것을 확인한 실험입니다.

## 10. 다음 개선 방향

다음 단계는 단순 preload 조정이 아니라, 영상 요청 자체를 초기 로딩에서 분리하는 방향이어야 합니다.

추천 방향:

```txt
1. scene-1.mp4의 첫 프레임 또는 대표 프레임을 poster 이미지로 생성
2. video 태그에 poster 적용
3. 초기에는 video src를 연결하지 않음
4. mockup 영역이 화면에 가까워지거나 사용자가 데모를 볼 때 src 연결
5. 이후 play() 호출
```

즉, Phase 2는 단순 poster 추가에서 끝나면 안 되고, 가능하면 poster 기반 지연 로딩까지 이어져야 합니다.

권장 구현 방향:

```txt
poster + src 지연 연결
또는 기존 LazyVideo 구조를 랜딩 hero video에 맞게 확장
```

예상 효과:

```txt
초기 Network에서 scene-1.mp4 요청 제거 또는 지연
총 초기 전송량 8.5MiB에서 대폭 감소 가능
Mobile FCP/LCP 개선 가능
```

## 11. 포트폴리오 기록 문장

Phase 1은 성공적인 최적화라기보다 실험 결과로 기록하는 것이 정확합니다.

작성 예시:

```txt
랜딩 페이지 전체 전송량 8.5MiB 중 7.8MiB가 hero video에서 발생하는 병목을 확인하고,
우선 UI 변경이 거의 없는 preload 전략 변경을 실험했습니다.
preload를 auto에서 metadata로 변경했지만 autoplay 및 mount 직후 play 호출 구조로 인해
영상 전체 다운로드가 유지되는 것을 확인했고, 이후 poster 기반 지연 로딩으로 개선 방향을 전환했습니다.
```

## 12. 체크리스트

```txt
[x] preload="auto" -> preload="metadata" 변경
[x] 빌드 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] scene-1.mp4 전송량 변화 확인
[x] preload 단독 변경으로는 병목 해결이 어렵다는 결론 도출
[ ] Phase 2 poster 이미지 생성
[ ] video src 지연 연결 방식 설계
[ ] Phase 2 구현 후 재측정
```
