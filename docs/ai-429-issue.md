# 429(Resource exhausted) 원인/해결 기록

## 개요
AI 호출(특히 Gemini)에서 429(Resource exhausted)가 빈번하게 발생했다.  
주로 **프롬프트 미리보기(preview)**가 사용자 입력 변경마다 자동 호출되면서 요청이 과도하게 쌓인 것이 원인이었다.

## 증상
- 서버 로그에 `GEMINI_CALL_FAILED: 429 Resource exhausted` 반복 발생
- 프롬프트 입력/옵션 변경 시 미리보기 요청이 연속적으로 발생
- 결과적으로 요금/쿼터가 빨리 소진되거나 제한에 걸림

## 원인
1) **프롬프트 미리보기 자동 호출**
   - 입력값/옵션 변경 시마다 `queuePromptPreview()` → `refreshPromptPreview()`가 호출됨
   - 사용자가 텍스트를 입력하는 동안에도 연속 호출
2) **미리보기에서도 AI 호출**
   - 미리보기는 `promptEnFinal` 렌더링을 위해 번역/프롬프트 렌더가 필요
   - 결국 AI 호출이 실시간으로 누적됨

## 해결 내용
### 1) 미리보기 자동 호출 비활성화
- `refreshPromptPreview(force=false)`는 기본적으로 호출되지 않도록 제한
- **명시적 트리거(예: AI 다듬기/재생성 버튼)**에서만 미리보기 호출

### 2) 실제 적용
- `generatePrompt()` 성공 시에만 `refreshPromptPreview(true)` 호출
- 그 외 입력 변화에 따른 자동 preview는 동작하지 않도록 유지

## 재발 방지 가이드
- **입력 이벤트마다 AI 호출하지 말 것**
  - 특히 타이핑/옵션 선택은 debounce만으로도 충분하지 않음
- 프롬프트 preview는 **사용자 명시 행동(버튼 클릭)**으로만 호출
- 로그에서 429가 보이면:
  1) `preview` 호출 경로가 자동으로 트리거되는지 확인
  2) 프론트에서 `refreshPromptPreview()` 호출 위치 점검
  3) 필요 시 호출을 완전히 차단하거나 강제 조건 추가

## 관련 변경 포인트(요약)
- `useNodeGeneration.ts`에서 preview 호출을 기본 비활성화
- `generatePrompt()` 성공 시에만 preview 강제 호출

---
이 문서는 429 재발 시 원인 추적/재현을 빠르게 하기 위한 기록이다.
