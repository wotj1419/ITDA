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

## 샷 패널 그리드 셀 선택 토글
- 샷 노드 클릭 시 사이드바에서 그리드 레이아웃에 맞는 셀 번호를 토글로 선택
- 선택한 셀 번호가 샷 노드의 `gridCellIndex`로 저장
- 레이아웃(2x2/2x3/3x3)에 따라 토글 개수가 자동 변경
- "그리드 셀: #번호" 표시와 연동되어 즉시 반영

## 수정된 파일
- `itda-frontend/src/components/scene-editor/ShotPanel.vue`
  - 그리드 레이아웃 기반 셀 선택 토글(UI)과 선택 핸들러 추가

-------------------------------------------------------------------------------------

## 속성 패널(사이드바) 동적 표시
- 노드 미선택 시: 오른쪽 사이드바(속성 패널) 영역이 숨겨짐 (캔버스 전체 화면 확장)
- 노드 선택 시: 해당 노드의 속성 패널이 오른쪽에서 나타남

## 동작 흐름
1. `EditorLayout`의 고정된 `aside` 래퍼 제거
2. `NodePanelContainer`가 `selectedNode` 유무에 따라 자체적으로 렌더링 여부 결정
3. 노드 선택 → `NodePanelContainer` 렌더링 → 사이드바 표시
4. 노드 선택 해제 → `NodePanelContainer` 렌더링 안됨 → 사이드바 사라짐

## 수정된 파일
- `itda-frontend/src/layouts/EditorLayout.vue`
  - 고정된 `.editor-properties` 스타일 및 `aside` 태그 제거
