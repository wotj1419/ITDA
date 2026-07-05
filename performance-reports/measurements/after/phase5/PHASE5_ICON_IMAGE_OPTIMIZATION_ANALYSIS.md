# Phase 5 Analysis: icon.png 이미지 용량 최적화

## 1. 개선 목표

Phase 5의 목표는 여러 화면에서 공통으로 사용하는 `icon.png`의 전송량을 줄이는 것입니다.

Phase 6 기준 `icon.png` 상태:

```txt
원본 크기: 439x388
파일 크기: 34,190 bytes
주요 표시 크기: 24px ~ 64px
```

실제 UI에서는 아이콘이 대부분 작은 크기로 표시되고 있었습니다.

```txt
랜딩 헤더 로고: 34px
랜딩 footer 로고: 28px
앱 헤더 로고: 24px
인증 페이지 로고: 64px
프로젝트/타임라인 placeholder: 32px ~ 48px
```

따라서 원본 `439x388` 이미지를 그대로 전송할 필요가 크지 않았습니다.

## 2. 개선이 필요했던 이유

Phase 1~4.5에서 큰 네트워크 병목은 대부분 제거했습니다.

```txt
scene-1.mp4 초기 요청: 7.8MiB -> 0
scene-1-poster.webp 추가 지연 실험 완료
Toss font CSS render-blocking 개선
초기 entry JS code splitting 완료
```

남은 정적 리소스 중 `icon.png`는 크기는 크지 않지만, 랜딩 페이지 첫 진입에서 favicon과 로고 이미지로 요청됩니다.

Phase 6 Mobile에서 확인된 `icon.png` 요청:

```txt
icon.png transferSize: 34,342 bytes
icon.png resourceSize: 34,190 bytes
```

Phase 6 Desktop에서 확인된 `icon.png` 요청:

```txt
icon.png transferSize: 34,390 bytes
icon.png resourceSize: 34,190 bytes
```

절대 크기는 작지만, UI 변화 없이 줄일 수 있는 정적 리소스였기 때문에 마무리 최적화 대상으로 적합했습니다.

## 3. 개선 방법

작업 파일:

```txt
itda-frontend/public/icon.png
```

변경 전:

```txt
439x388
34,190 bytes
```

변경 후:

```txt
128x113
4,637 bytes
```

적용 방식:

```txt
기존 파일 경로 /icon.png 유지
코드 참조 변경 없음
이미지 비율 유지
투명 배경 유지
고해상도 화면에서 64px 표시까지 대응 가능한 128px 폭으로 리사이즈
```

이 방법을 선택한 이유:

```txt
UI 코드 변경 없이 정적 리소스 용량만 줄일 수 있음
기존 /icon.png 참조를 유지해 favicon, 로고, placeholder 사용처를 모두 그대로 유지할 수 있음
64px 표시 영역에서도 2x 해상도 대응이 가능해 흐림 가능성이 낮음
```

## 4. 측정 파일

Phase 6:

```txt
performance-reports/measurements/after/phase6/lighthouse_mobile_after_phase6.json
performance-reports/measurements/after/phase6/lighthouse_desktop_after_phase6.json
```

Phase 5:

```txt
performance-reports/measurements/after/phase5/lighthouse_mobile_after_phase5.json
performance-reports/measurements/after/phase5/lighthouse_desktop_after_phase5.json
```

측정 URL:

```txt
Phase 6: https://itda-iirc6tu6d-wotj.vercel.app/
Phase 5: https://itda-4owgn95f4-wotj.vercel.app/
```

## 5. 빌드 결과

빌드 명령:

```txt
npm run build
```

빌드 결과:

```txt
build 성공
dist/icon.png: 4,637 bytes
```

파일 크기 변화:

| 항목 | Phase 6 | Phase 5 | 변화 |
| --- | ---: | ---: | ---: |
| icon.png width | 439px | 128px | -311px |
| icon.png height | 388px | 113px | -275px |
| icon.png file size | 34,190 bytes | 4,637 bytes | -29,553 bytes |
| 감소율 | - | - | 약 86.4% |

## 6. 개선 전후 결과

### Lighthouse Mobile

| 항목 | Phase 6 | Phase 5 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 62 | 73 | +11 |
| FCP | 2.75s | 3.22s | +0.47s |
| LCP | 5.61s | 5.47s | -0.14s |
| TBT | 485ms | 29ms | -456ms |
| CLS | 0.023 | 0.026 | +0.003 |
| Speed Index | 3.01s | 3.22s | +0.21s |
| TTI | 5.62s | 5.49s | -0.13s |
| Main-thread work | 4.82s | 4.98s | +0.16s |
| Bootup time | 0.66s | 0.78s | +0.12s |
| 총 요청 수 | 63 | 63 | 동일 |
| 총 전송량 | 759,272 bytes | 729,811 bytes | -29,461 bytes |
| Script 전송량 | 111,482 bytes | 111,442 bytes | -40 bytes |
| Image 전송량 | 112,619 bytes | 112,629 bytes | +10 bytes |
| Font 전송량 | 453,487 bytes | 453,513 bytes | +26 bytes |
| Unused JavaScript | 0 bytes | 0 bytes | 동일 |

### Lighthouse Desktop

| 항목 | Phase 6 | Phase 5 | 변화 |
| --- | ---: | ---: | ---: |
| Performance | 97 | 95 | -2 |
| FCP | 0.72s | 0.80s | +0.08s |
| LCP | 1.18s | 1.24s | +0.06s |
| TBT | 43ms | 85ms | +42ms |
| CLS | 0.010 | 0.010 | 동일 |
| Speed Index | 1.04s | 1.18s | +0.14s |
| TTI | 1.18s | 1.24s | +0.06s |
| Main-thread work | 2.15s | 2.18s | +0.03s |
| Bootup time | 0.23s | 0.23s | 동일 |
| 총 요청 수 | 68 | 68 | 동일 |
| 총 전송량 | 826,887 bytes | 797,294 bytes | -29,593 bytes |
| Script 전송량 | 134,532 bytes | 134,539 bytes | +7 bytes |
| Image 전송량 | 146,954 bytes | 117,336 bytes | -29,618 bytes |
| Font 전송량 | 464,142 bytes | 464,144 bytes | +2 bytes |
| Unused JavaScript | 0 bytes | 0 bytes | 동일 |

## 7. 상세 수치

### icon.png 네트워크 요청

| 항목 | Phase 6 Mobile | Phase 5 Mobile | 변화 |
| --- | ---: | ---: | ---: |
| icon.png transferSize | 34,342 bytes | 4,780 bytes | -29,562 bytes |
| icon.png resourceSize | 34,190 bytes | 4,637 bytes | -29,553 bytes |

| 항목 | Phase 6 Desktop | Phase 5 Desktop | 변화 |
| --- | ---: | ---: | ---: |
| icon.png transferSize | 34,390 bytes | 4,779 bytes | -29,611 bytes |
| icon.png resourceSize | 34,190 bytes | 4,637 bytes | -29,553 bytes |

Phase 5의 목표였던 `icon.png` 전송량 감소는 달성됐습니다.

```txt
Mobile 기준 icon.png transferSize 약 29.6KB 감소
Desktop 기준 icon.png transferSize 약 29.6KB 감소
원본 파일 크기 약 86.4% 감소
```

### 총 전송량

| 항목 | Phase 6 | Phase 5 | 변화 |
| --- | ---: | ---: | ---: |
| Mobile total transfer | 759,272 bytes | 729,811 bytes | -29,461 bytes |
| Desktop total transfer | 826,887 bytes | 797,294 bytes | -29,593 bytes |

총 전송량 감소량은 `icon.png` 감소량과 거의 일치합니다.

```txt
icon.png 자체 최적화가 전체 전송량 감소로 그대로 반영됨
요청 수는 동일하지만 각 요청의 payload가 줄어듦
```

### ImageBytes 분류 주의

Mobile에서는 `Image 전송량` 합계가 거의 변하지 않았습니다.

```txt
Phase 6 Mobile Image transfer: 112,619 bytes
Phase 5 Mobile Image transfer: 112,629 bytes
```

하지만 이는 최적화가 실패했다는 의미가 아닙니다.

Mobile Lighthouse 네트워크 상세에서 `icon.png` 첫 요청이 `Image`가 아니라 `Other`로 분류됐기 때문입니다.

```txt
Phase 6 Mobile icon.png Other transferSize: 34,342 bytes
Phase 5 Mobile icon.png Other transferSize: 4,780 bytes
```

따라서 Mobile에서는 이미지 카테고리 합계보다 `icon.png` 개별 요청과 전체 전송량을 기준으로 보는 것이 정확합니다.

## 8. 결과 해석

Phase 5에서 좋아진 지표:

```txt
Mobile Performance: 62 -> 73
Mobile TBT: 485ms -> 29ms
Mobile LCP: 5.61s -> 5.47s
Mobile total transfer: 759,272 bytes -> 729,811 bytes
Mobile icon.png transferSize: 34,342 bytes -> 4,780 bytes
Desktop total transfer: 826,887 bytes -> 797,294 bytes
Desktop icon.png transferSize: 34,390 bytes -> 4,779 bytes
```

Phase 5에서 나빠진 지표:

```txt
Mobile FCP: 2.75s -> 3.22s
Mobile Speed Index: 3.01s -> 3.22s
Desktop Performance: 97 -> 95
Desktop TBT: 43ms -> 85ms
Desktop LCP: 1.18s -> 1.24s
```

해석:

```txt
icon.png 최적화 자체는 명확하게 성공했습니다.
전체 전송량도 약 29.5KB 줄었습니다.

다만 Lighthouse Performance 점수 변화는 icon.png 하나만의 영향으로 보기 어렵습니다.
Mobile 점수 상승은 TBT가 485ms에서 29ms로 크게 줄어든 영향이 더 큽니다.
반대로 Desktop 점수 하락도 icon.png 때문이라기보다 측정 시점의 TBT/FCP/LCP 편차 영향으로 보는 것이 적절합니다.
```

## 9. Lighthouse 점수 변화가 icon 최적화와 1:1로 연결되지 않는 이유

```txt
1. icon.png 절감량은 약 29.5KB로, 전체 Lighthouse 점수를 크게 좌우할 정도의 크기는 아닙니다.
2. icon.png는 현재 LCP 대상이 아닙니다.
3. Mobile Performance 상승의 가장 큰 직접 원인은 TBT 감소입니다.
4. Phase 5에서도 LandingPage long task는 약 515ms로 유지됐습니다.
5. Vercel Preview + Lighthouse Mobile 환경은 실행 타이밍 편차가 있어 TBT가 흔들릴 수 있습니다.
```

Phase 6과 Phase 5의 Mobile long task:

```txt
Phase 6 LandingPage JS top long task: 519ms
Phase 5 LandingPage JS top long task: 515ms
```

long task 자체는 거의 그대로였지만, Lighthouse의 TBT 집계 구간 차이로 총 TBT는 크게 달라졌습니다.

```txt
Phase 6 Mobile TBT: 485ms
Phase 5 Mobile TBT: 29ms
```

따라서 Phase 5 결과는 다음처럼 보는 것이 정확합니다.

```txt
icon.png 용량 최적화는 성공
전체 전송량 약 29.5KB 감소
Performance 점수 변화는 이미지 최적화 효과와 측정 편차가 함께 반영된 결과
```

## 10. 결론

Phase 5 결론:

```txt
실제 표시 크기보다 큰 439x388 icon.png를 128x113으로 리사이즈해,
파일 크기를 34,190 bytes에서 4,637 bytes로 줄였습니다.
```

수치 근거:

```txt
icon.png file size: 34,190 bytes -> 4,637 bytes
Mobile icon.png transferSize: 34,342 bytes -> 4,780 bytes
Desktop icon.png transferSize: 34,390 bytes -> 4,779 bytes
Mobile total transfer: 759,272 bytes -> 729,811 bytes
Desktop total transfer: 826,887 bytes -> 797,294 bytes
```

즉, UI 코드 변경 없이 정적 이미지 payload를 약 86.4% 줄였고, 전체 전송량도 약 29.5KB 감소했습니다.

## 11. 다음 판단

Phase 5까지 완료하면서 최초 계획에 있던 주요 최적화 작업은 모두 수행했습니다.

완료된 작업:

```txt
[x] Phase 1: hero video preload 전략 변경
[x] Phase 2: hero video poster 추가
[x] Phase 3: hero video lazy loading
[x] Phase 3.5: 랜딩 애니메이션 초기화 지연
[x] Phase 4: Toss font CSS 로딩 최적화
[x] Phase 4.5: hero poster lazy loading 실험
[x] Phase 6: unused JS / 초기 entry chunk 분리
[x] Phase 5: icon.png 이미지 용량 최적화
```

추가로 더 개선한다면 다음 후보가 남아 있습니다.

```txt
LandingPage long task / Style/Layout 비용 분석
Toss font self-hosting 또는 font-weight 축소
scene-1-poster.webp 추가 압축 또는 더 작은 poster 생성
```

다만 위 작업들은 UI 변화 가능성이나 작업 범위가 더 크기 때문에, 현재 포트폴리오용 성능 개선 사례는 Phase 5까지로 마무리해도 충분합니다.

## 12. 포트폴리오 기록 문장

Phase 5는 다음처럼 기록할 수 있습니다.

```txt
Lighthouse와 Network 분석을 통해 공통 로고 이미지인 icon.png가 실제 표시 크기보다 큰 439x388 원본으로 전송되고 있음을 확인했습니다.
랜딩 헤더, footer, 인증 페이지, placeholder 등 대부분의 사용처가 24~64px 표시 크기였기 때문에,
고해상도 화면 대응을 유지할 수 있는 128px 폭 이미지로 리사이즈했습니다.

그 결과 icon.png 파일 크기를 34,190 bytes에서 4,637 bytes로 약 86.4% 줄였고,
Mobile 기준 icon.png transferSize를 34,342 bytes에서 4,780 bytes로,
전체 전송량을 759,272 bytes에서 729,811 bytes로 줄였습니다.
```

주의해서 함께 적을 문장:

```txt
Mobile Lighthouse Performance는 62점에서 73점으로 상승했지만,
이는 icon.png 최적화만의 직접 효과라기보다 TBT 측정 편차가 함께 반영된 결과입니다.
따라서 Phase 5의 핵심 성과는 점수 상승보다 정적 이미지 payload 감소와 전체 전송량 감소로 보는 것이 정확합니다.
```

## 13. 체크리스트

```txt
[x] icon.png 실제 파일 크기 확인
[x] icon.png 사용처와 표시 크기 확인
[x] 128px 폭 리사이즈 적용
[x] 기존 /icon.png 경로 유지
[x] build 성공 확인
[x] Vercel Preview Deployment 측정
[x] Lighthouse Mobile 결과 비교
[x] Lighthouse Desktop 결과 비교
[x] icon.png transferSize 감소 확인
[x] 전체 전송량 감소 확인
[x] Performance 점수 변화 원인 주의 사항 기록
```
