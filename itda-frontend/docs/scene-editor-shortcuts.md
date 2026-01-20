# Scene Editor Keyboard Shortcuts (Delete / Undo)

## 수정된 동작 내용
- Delete/Backspace: 선택된 노드 박스 삭제 (하위 노드 포함, Scene Header/마스터 이미지는 삭제 불가)
- Ctrl/Cmd + Z: 마지막 이동 이전 위치로 복원
- 입력창/텍스트 편집 중에는 단축키 무시

이 문서는 씬 에디터 노드 박스에 대해 Delete와 Ctrl+Z 동작을 추가한 변경점을 설명합니다.

## 동작 흐름
1. NodeCanvas에서 노드 드래그 시작 → 위치 스냅샷 저장 (store)
2. SceneEditPage에서 키보드 입력 감지 → delete/undo 호출
3. sceneNode store가 상태를 변경 → Vue Flow v-model이 갱신되며 캔버스 반영

## 수정된 파일
- `itda-frontend/src/components/scene-editor/NodeCanvas.vue`
  - 노드 드래그 시작 시 `pushPositionSnapshot()` 호출
  - 결과: 다음 Ctrl/Cmd+Z에서 복원할 기준 위치 확보
- `itda-frontend/src/stores/sceneNode.ts`
  - positionHistory 스택 추가
  - `pushPositionSnapshot`, `undoLastMove` 액션 추가
  - 씬 로드/Mock/clear 시 히스토리 초기화
  - Scene Header/마스터 이미지 노드는 삭제 차단
- `itda-frontend/src/pages/SceneEditPage.vue`
  - 전역 keydown 핸들러 추가
  - Delete/Backspace → `deleteNode`
  - Ctrl/Cmd+Z → `undoLastMove`

## 참고
- Undo는 위치만 되돌리며, 삭제된 노드는 복원하지 않습니다.

---
