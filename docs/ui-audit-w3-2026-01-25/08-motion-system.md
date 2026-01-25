# 08) 전환/모션 시스템 제안 (공통)

해커톤 데모에서 “완성도”는 기능만이 아니라 **전환/모션의 일관성**에서 크게 체감됩니다.

## 모션 원칙

- 빠르되(민첩), 튀지 않게(성숙)
- 반복/무한 애니메이션은 최소화(시선 피로/싼티 방지)
- `prefers-reduced-motion` 대응(최소한의 접근성)

## 권장 타이밍/이징(기본 세트)

- `fast`: 120ms (hover/pressed)
- `ui`: 180ms (탭/토글/짧은 전환)
- `page`: 240~320ms (라우트 전환/모달)
- 이징:
  - 기본: `cubic-bezier(0.2, 0.8, 0.2, 1)` (부드러운 스냅)
  - 강조: `cubic-bezier(0.16, 1, 0.3, 1)` (더 탄력)

## 라우트 전환(페이지 단위)

### 추천 스타일

- 기본: 페이드 + 위로 12px 이동
- “프로젝트 상세 → 씬 편집/타임라인” 같은 핵심 전환:
  - 공유 요소(썸네일/제목/미니 타임라인)를 활용한 “확대/이동” 연출

### 선택(임팩트 강화): View Transition API

- 데모(Chrome)에서 “프로덕트 완성도” 체감이 큰 편.
- 적용 우선순위:
  1) 프로젝트 상세 → 씬 편집 (씬 카드 썸네일/제목 공유 요소)
  2) 씬 편집 → 타임라인 (미니 타임라인 공유 요소)
  3) 랜딩 → 로그인 (CTA 공유 요소)

### Vue 구현 힌트(예시)

```vue
<!-- App.vue 또는 레이아웃에서 router-view 전환 -->
<router-view v-slot="{ Component }">
  <Transition name="page" mode="out-in">
    <component :is="Component" />
  </Transition>
</router-view>
```

```css
.page-enter-active,
.page-leave-active {
  transition: opacity 240ms cubic-bezier(0.2, 0.8, 0.2, 1),
              transform 240ms cubic-bezier(0.2, 0.8, 0.2, 1);
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
```

## 모달/패널

- 오픈: `opacity 0 → 1` + `scale 0.98 → 1`
- 백드롭: `blur 0 → 6px`, 어둡게(0.2~0.35)
- 사이드 패널(씬 편집 오른쪽):
  - `translateX(16px) + opacity`로 “툴 패널” 느낌

## 테마 전환(Studio Mode)

- 전환은 “배경만 부드럽게”가 핵심(텍스트/레이아웃이 흔들리면 퀄리티가 급락)
- 추천: 150~200ms 동안 `background-color`, `color`, `border-color`만 크로스페이드
- `prefers-reduced-motion`에서는 애니메이션 없이 즉시 전환

## 탭 전환(Story/Scenes/Objects)

- 인디케이터 바는 “슬라이드”
- 콘텐츠는 “좌/우 12px 이동 + 페이드”
- 높이 변화는 레이아웃 튐이 생기기 쉬우니:
  - `clip-path` 또는 `transform` 기반으로 처리 권장

## 노드 편집(캔버스)

- 노드 생성: scale 0.96 → 1.0 + fade (180ms)
- 엣지(연결선) 생성: stroke-dashoffset 애니메이션(선이 그려짐)
- 상태 변화:
  - `RUNNING`: 얇은 진행 바/스캔 라인
  - `SUCCEEDED`: 썸네일 blur → sharp (300~450ms)
  - `FAILED`: 붉은 플래시 + “재시도” CTA(흔들림 남발 금지)

## 토스트/알림

- 성공: 2~3초 자동 닫힘 + 가벼운 “글로우”
- 실패: 사용자가 닫을 때까지 유지(또는 6초) + 바로 “다시 시도” 액션 제공
