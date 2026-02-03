# 프롬프트 UX 변경 구현 계획 (Section 2 전용)

작성일: 2026-02-02  
대상 범위: `docs/as-is-node-prompt-generation-process.md`의 **2) 프론트 패널 공통 UX**에 작성된 변경안만 반영  
비범위: 3)~7)의 기존 프로세스/도메인 정책 변경

---

## 1. 목표

1. 서술 프롬프트를 **한글 단일 편집 흐름**으로 단순화한다.
2. `AI로 다듬기` 의미 혼동을 줄이기 위해 버튼 역할을 분리한다.
3. `최종 프롬프트` 생성/편집 상태를 시각적으로 명확히 한다.
4. 노드 재진입 시 `최종 프롬프트`가 안정적으로 보이도록 개선한다.

---

## 2. 요구사항 매핑

## 2-1. 서술 프롬프트 영역
- EN/KO 토글 제거
- 한글 프롬프트 textarea 1개만 제공
- `직접 편집` / `편집 완료` 토글 버튼 제공
- 편집 모드 시 textarea/버튼에 명시적 하이라이트 스타일 적용
- 기존 읽기전용 안내 툴팁 제거

## 2-2. 생성 버튼 체계
- `AI로 다듬기` → **`AI로 재생성`**으로 텍스트 변경
- 기존 내부 분기(Generate/Improve/강제 재생성)는 유지
- 신규 버튼 **`최종 프롬프트 생성`** 추가
  - 동작: `/nodes/{id}/prompt-preview`를 수동 호출하여 `promptEnFinal` 갱신
  - 목적: 사용자 입력값 기준으로 최종 프롬프트만 생성/재생성

## 2-3. 최종 프롬프트 영역
- 기본 readonly 유지
- `영문 직접 편집` 기능 유지
- 클릭/활성 상태를 `승인` 수준의 강조색으로 표시
- `한글 편집`(디테일로 스크롤) 버튼 제거
- **정책 추가:** 사용자가 한글 프롬프트를 수정한 뒤 `최종 프롬프트 생성`을 누르면,
  - `usePromptOverride=false`, `promptEnFinalOverride=''`로 override를 자동 해제한 후
  - `/nodes/{id}/prompt-preview`를 강제 호출해 최종 프롬프트를 재생성한다.
  - 목적: "내가 방금 수정한 한글 내용이 최종 프롬프트에 반영된다"는 버튼 기대 동작과 실제 동작을 일치시킴.

## 2-4. 유지 항목
- 자동 미리보기(600ms 디바운스) 유지
- `usePromptOverride=true`일 때 자동 미리보기 중단 유지
- 승인 상태 모델(`draft/generated/approved`) 유지

## 2-5. 추가 개선
- VIDEO 패널 도움말 텍스트 세로 출력 이슈 수정(가로 줄바꿈)
- 노드 재진입 시 `최종 프롬프트` 미표시 이슈 수정

## 2-6. 추가 요구사항 (2차)
1. 프롬프트 textarea(서술/최종 표시 영역) 높이를 기존 대비 약 1.2배 확대
2. 프롬프트 textarea의 스크롤 1회 이동량을 줄여 미세 스크롤 가능하게 조정
3. `AI로 재생성`, `최종 프롬프트 생성` 버튼을 축소하고 한 줄 수평 배치
4. 프롬프트/이미지/영상 생성 작업 중에는 패널 내 모든 입력/토글/버튼을 비활성화
5. 최종 프롬프트 생성 이후, 추가 수정이 발생하기 전까지
   - `최종 프롬프트 생성` 비활성화
   - 이미지/영상 생성 버튼 비활성화
   - 수정이 발생하면 자동 재활성화
   - 단, `최종 프롬프트 생성` 버튼의 재활성화 기준은 **서술 프롬프트 변경**으로 한정
     (영문 직접 편집만으로는 재활성화하지 않음)

---

## 3. 구현 전략

## 3-1. 프론트 UI/상태 전략
- 4개 패널(`MasterImagePanel`, `StoryboardGridPanel`, `ShotPanel`, `VideoPanel`)에 동일 UX 패턴 적용
- `promptLang` 상태 및 EN/KO 토글 마크업 제거
- `isPromptEditingKo`(가칭) 상태 추가:
  - `false`: 읽기 모드
  - `true`: 편집 모드(하이라이트)
- `promptKo`를 서술 프롬프트의 단일 소스로 사용

## 3-2. API 호출 전략
- `AI로 재생성`: 기존 `generatePrompt()` 재사용 (로직 변경 없음, 라벨만 변경)
- `최종 프롬프트 생성`: `refreshPromptPreview(true)`를 버튼 핸들러로 노출해 수동 호출
- 단, `최종 프롬프트 생성` 직전에 override 자동 해제 로직을 먼저 적용
  - 권장 순서: override 해제 상태 반영 → preview 호출 → `promptEnFinal` 저장

## 3-3. 최종 프롬프트 재진입 표시 전략
- `promptEnFinal`을 노드 settings에 포함해 서버 저장 가능하게 확장
  - 프론트 `buildNodeSettings()`에 `promptEnFinal` 포함
  - 패널에서 수동 `최종 프롬프트 생성` 성공 시 로컬 반영 + 저장 트리거
- 하이드레이션 시 settings의 `promptEnFinal`을 우선 복원
- 누락 시 기존 자동 미리보기로 보완

> 의도: "생성 버튼 전에는 미리보기 전용, 생성 시 저장" 정책을 깨지 않으면서도  
> 사용자가 명시적으로 생성한 최종 프롬프트는 재진입 시 그대로 보이게 함.

## 3-4. UI 잠금/Dirty 상태 전략 (2차)
- 공통 `isUiLocked` 계산값 도입
  - 기준: `isGeneratingPrompt` / `isGeneratingFinalPrompt` / `isGeneratingImage|Grid|Shot|Video` 중 하나라도 true
  - 적용: 패널 내 입력값, 선택 버튼, 승인/생성 버튼, 편집 토글 모두 disabled
- 공통 dirty 상태를 목적별로 분리
  - `isNarrativePromptDirtyForFinal`: `최종 프롬프트 생성` 버튼 전용
    - 기준: "최종 프롬프트 생성 시점의 서술 프롬프트 스냅샷" 대비 `form.prompt` 변경 여부
    - `최종 프롬프트 생성` 직후 false로 리셋, 서술 프롬프트 변경 시 true
    - 영문 직접 편집(`promptEnFinalOverride`)은 비교 대상에서 제외
  - `isFinalPromptDirty`: 이미지/그리드/샷/영상 생성 버튼 전용(기존 정책 유지)
    - 기준: "최종 프롬프트 생성 시점 스냅샷" 대비 생성 관련 입력값 변경 여부
    - nodeType별 설정 키 차이를 반영해 비교

---

## 4. 작업 단계

## Phase A. 공통 로직 정리
1. `useNodeGeneration`에 수동 최종 생성용 함수 노출 여부 검토
   - 현재 `refreshPromptPreview`가 이미 반환되고 있으므로 버튼 연결만으로 우선 처리
2. 패널 공통 텍스트 상수 정리 (`AI로 재생성`, `최종 프롬프트 생성`)

## Phase B. 패널 UI 변경 (4개 파일)
1. 서술 프롬프트 EN/KO 토글 제거
2. 한글 단일 textarea + `직접 편집/편집 완료` 버튼 적용
3. 툴팁/`한글 편집` 버튼 삭제
4. `최종 프롬프트 생성` 버튼 추가 및 수동 preview 연결
5. `영문 직접 편집` 활성 강조색 스타일 적용
6. `최종 프롬프트 생성` 클릭 시 override 자동 해제 처리 추가
7. 프롬프트 textarea 높이 1.2배 확대 + 스크롤 이동량 축소 스타일 적용
8. `AI로 재생성`/`최종 프롬프트 생성` 버튼 수평 배치 및 소형화
9. 작업 중 전체 UI 잠금(disabled) 적용
10. dirty 기반 버튼 활성/비활성 제어 적용
   - `최종 프롬프트 생성`은 `isNarrativePromptDirtyForFinal` 기준
   - 이미지/그리드/샷/영상 생성은 `isFinalPromptDirty` 기준

## Phase C. 데이터 유지성 보강
1. `itda-frontend/src/stores/sceneNode/mappers.ts`
   - `buildNodeSettings()`에 `promptEnFinal` 추가
2. 패널에서 수동 최종 생성 성공 시 `updateNode(...)`로 저장 반영
   - 자동 디바운스 preview는 기존처럼 로컬 반영 유지
3. 재진입/새로고침 후 `promptEnFinal` 표시 회귀 확인
4. override 자동 해제 후 저장값 일관성 확인
   - `promptEnFinalOverride`가 비워지고 `promptEnFinal`이 최신값으로 유지되는지 점검

## Phase D. VIDEO 도움말 스타일 수정
1. VIDEO 패널 도움말 CSS를 다른 패널과 동일한 가로 줄바꿈 기준으로 맞춤
2. 좁은 폭에서도 세로 글자화되지 않도록 `white-space`, `word-break`, `line-height` 보정

---

## 5. 영향 파일(예정)

- `itda-frontend/src/components/scene-editor/panels/MasterImagePanel.vue`
- `itda-frontend/src/components/scene-editor/panels/StoryboardGridPanel.vue`
- `itda-frontend/src/components/scene-editor/panels/ShotPanel.vue`
- `itda-frontend/src/components/scene-editor/panels/VideoPanel.vue`
- `itda-frontend/src/stores/sceneNode/mappers.ts`
- (필요 시) `itda-frontend/src/composables/useNodeGeneration.ts`

---

## 6. 수용 기준 (Acceptance Criteria)

1. 4개 패널 모두 서술 프롬프트가 한글 단일 textarea로 노출된다.
2. 서술 프롬프트 편집 토글이 동작하고, 편집 중 시각 강조가 명확하다.
3. 버튼명이 `AI로 재생성`으로 변경된다.
4. `최종 프롬프트 생성` 버튼 클릭 시 최종 프롬프트가 즉시 갱신된다.
5. 한글 프롬프트 수정 후 `최종 프롬프트 생성` 클릭 시 override가 자동 해제되고, 한글 수정분 기준으로 최종 프롬프트가 재생성된다.
6. `한글 편집` 버튼이 더 이상 보이지 않는다.
7. `영문 직접 편집` 활성 상태가 강조색으로 표시된다.
8. 노드 이탈 후 재진입 시 최신 `promptEnFinal`이 다시 보인다.
9. VIDEO 도움말 텍스트가 가로 줄바꿈으로 정상 표시된다.
10. 프롬프트 영역 높이가 기존 대비 확대되고, 스크롤 이동량이 더 촘촘해진다.
11. `AI로 재생성`/`최종 프롬프트 생성` 버튼이 한 줄에 표시된다.
12. 생성 작업 중 패널 내 값 수정이 불가능하다.
13. 최종 프롬프트 생성 직후, 수정 전까지 `최종 프롬프트 생성`/생성 버튼이 비활성화된다.
    - 단, `최종 프롬프트 생성`은 서술 프롬프트 변경 시에만 재활성화된다.
    - 영문 직접 편집만으로는 `최종 프롬프트 생성`이 재활성화되지 않는다.

---

## 7. 테스트 체크리스트

## 7-1. 수동 기능 테스트
- [ ] MASTER/GRID/SHOT/VIDEO 각각에서 서술 프롬프트 편집 토글 동작
- [ ] `AI로 재생성` 클릭 시 기존 생성 로직 그대로 동작
- [ ] `최종 프롬프트 생성` 클릭 시 `promptEnFinal` 갱신
- [ ] `영문 직접 편집` on/off 시 스타일 및 override 동작
- [ ] 한글 프롬프트 수정 후 `최종 프롬프트 생성` 클릭 시 override 자동 해제 + 재생성 동작
- [ ] 승인/생성 버튼 게이팅 기존과 동일 유지
- [ ] 프롬프트 textarea 높이 확대 및 스크롤 이동량 축소 체감 확인
- [ ] `AI로 재생성`/`최종 프롬프트 생성` 버튼 한 줄 배치 확인(모바일 포함)
- [ ] 생성 중 모든 입력/토글/버튼 비활성화 확인
- [ ] 최종 프롬프트 생성 직후 dirty=false 상태에서 생성 버튼 비활성화 확인
- [ ] 임의 필드 수정 시 dirty=true로 전환되어 생성 버튼 재활성화 확인
- [ ] 영문 직접 편집만 수행한 경우 `최종 프롬프트 생성` 버튼이 재활성화되지 않는지 확인
- [ ] 서술 프롬프트 수정 시에만 `최종 프롬프트 생성` 버튼이 재활성화되는지 확인

## 7-2. 재진입/복원 테스트
- [ ] 노드 A 선택 → 최종 프롬프트 생성 → 노드 B 이동 → 노드 A 재선택 시 값 유지
- [ ] 페이지 새로고침 후 노드 상세 재하이드레이션 시 값 표시

## 7-3. 회귀 테스트
- [ ] 자동 디바운스 preview 동작 유지
- [ ] VIDEO 한글 prompt 자동 preview skip 규칙 유지
- [ ] 이미지/영상 실제 생성(Job) 및 폴링 플로우 정상

---

## 8. 리스크 및 대응

1. **자동 preview와 수동 최종 생성의 충돌**
   - 대응: 수동 버튼은 `force=true`로 명시 호출, 로딩 상태 충돌 방지
2. **저장 빈도 증가**
   - 대응: 수동 `최종 프롬프트 생성` 시점에만 서버 저장 트리거
3. **패널별 구현 편차**
   - 대응: 4개 패널 동일 컴포넌트 패턴으로 맞추고 PR 체크리스트 공통 적용
4. **기존 override 사용자의 기대와 달라질 가능성**
   - 대응: 버튼 라벨/툴팁에 "최종 생성 시 영문 직접 편집은 해제됩니다" 안내 문구 추가 검토
5. **dirty 비교 기준 누락으로 인한 오작동**
   - 대응: nodeType별 비교 키를 명시하고 QA 시나리오에 필드별 변경 테스트 추가
6. **모바일에서 버튼 가로 배치 깨짐**
   - 대응: 좁은 폭에서 버튼 최소 너비/폰트/패딩 조정, 임계 폭 미만 시만 2줄 폴백 여부 검토

---

## 9. 구현 순서 제안

1. `MasterImagePanel`에 새 UX 패턴 먼저 적용(기준 패널)
2. `Grid/Shot/Video` 순으로 동일 패턴 이식
3. `mappers.ts` 저장 확장 + 재진입 복원 점검
4. VIDEO 도움말 스타일 마무리
5. 전체 회귀 테스트
