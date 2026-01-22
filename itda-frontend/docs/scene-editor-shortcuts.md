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

## 속성 패널(사이드바) 동적 표시 & 버튼 동기화
- 노드 미선택 시: 속성 패널이 숨겨지고 레이아웃 정렬 버튼이 오른쪽 끝(`right: 100px`)에 위치
- 노드 선택 시: 속성 패널이 부드럽게 슬라이드되어 나오고(`0.4s cubic-bezier`), 버튼도 동일한 속도로 밀려남(`right: 480px`)

## 동작 흐름
1. **패널 애니메이션**: `NodePanelContainer`에 `<Transition name="slide">` 추가하여 사이드바가 부드럽게 등장.
2. **버튼 이동**: `SceneEditPage`에서 노드 선택 상태(`isPanelOpen`)를 감지하여 버튼에 `.panel-open` 클래스 적용.
3. **일치된 움직임**: 두 요소 모두 `0.4s cubic-bezier(0.4, 0, 0.2, 1)` 애니메이션을 공유하여 완벽하게 동기화된 움직임 구현.

## 수정된 파일
- [SceneEditPage.vue](../src/pages/SceneEditPage.vue)
  - `isPanelOpen` 상태 감지 로직 추가
  - 버튼 위치 동기화를 위한 CSS 및 transition 설정
- [NodePanelContainer.vue](../src/components/scene-editor/panels/NodePanelContainer.vue)
  - 패널 등장/퇴장 시 슬라이드 애니메이션(`<Transition>`) 추가

-------------------------------------------------------------------------------------

## 자동 레이아웃 정렬 크기 계산 수정
- **수정 내용**: 자동 레이아웃(Auto Layout)이 노드의 실제 크기(변경된 dimensions)를 반영하도록 개선.
- **이전 문제**: 노드 크기를 변경해도 고정된 기본값(Type Constants)만 참조하여 레이아웃이 겹치거나 중심이 틀어짐.
- **해결**: `useAutoLayout` 로직이 1) 실제 렌더링 된 크기, 2) 스타일 지정 크기, 3) 기본 크기 순으로 확인하여 정렬 수행.

## 동작 흐름
1. 사용자가 노드 크기 Resize 변경 (Vue Flow `dimensions` 업데이트)
2. 레이아웃 정렬 버튼 클릭 → `useAutoLayout` 실행
3. `getNodeWidth/Height` 함수가 노드 객체의 `dimensions` 속성을 최우선으로 참조
4. Dagre 알고리즘이 변경된 크기에 맞춰 적절한 중앙 정렬 및 간격 확보

## 수정된 파일
- [useAutoLayout.ts](../src/composables/useAutoLayout.ts)
  - `getNodeWidth`, `getNodeHeight` 함수 로직 전면 수정 (노드 객체 참조 및 동적 크기 확인)

```typescript
// 수정 후: 노드 객체 전체를 받아서 실제 크기 확인
function getNodeWidth(nodeOrType: Node | string): number {
    const node = nodeOrType as any;
    // 1순위: 현재 화면에 그려진 실제 크기 (Vue Flow가 계산한 값)
    if (node.dimensions && node.dimensions.width > 0) {
        return node.dimensions.width;
    }
    
    // 2순위: 사용자가 스타일로 강제 지정한 크기
    if (node.style && typeof node.style === 'object' && node.style.width) { ... }
    
    // 3순위 (최후의 수단): 타입별 기본 크기
    return NODE_WIDTHS[type] || 200;
}
```

-------------------------------------------------------------------------------------

## 노드 크기 변경 Undo(Ctrl+Z) 지원
- **수정 내용**: 노드 크기 조절(Resize) 작업에 대한 실행 취소(Undo) 기능 추가.
- **이전 문제**: 노드 크기를 변경한 후 `Ctrl+Z`를 누르면 위치만 되돌려지고 크기는 복구되지 않음.
- **해결**: 1) Undo 스냅샷에 `dimensions` 정보 추가, 2) 리사이즈 시작 시점(`resize-start`)에 현재 상태 저장 트리거 연결.

## 동작 흐름
1. 사용자가 노드 모서리를 잡아 크기 조절 시작 (Resize Start)
2. `NodeResizer`의 `@resize-start` 또는 `<VueFlow>`의 `@node-resize-start` 이벤트 감지
3. `nodeStore.pushPositionSnapshot()` 실행하여 변경 전 크기/위치 저장
4. 사용자가 크기 조절 완료 후 `Ctrl+Z` 입력
5. `undoLastMove`가 스냅샷의 `dimensions`를 확인하여 `style.width/height` 복원

## 수정된 파일
- [sceneNode.ts](../src/stores/sceneNode.ts)
  - `NodeSnapshot` 타입 확장 (`dimensions` 추가)
  - `undoLastMove` 로직 수정: 크기 정보가 있으면 `node.style` 복원
  ```typescript
  function undoLastMove() {
      // ...
      // [Why] 단순 위치 이동뿐만 아니라, 리사이즈(크기 변경) 작업도 Undo 대상에 포함하기 위함입니다.
      // [Where] NodeSnapshot 타입에 새로 추가된 'dimensions' 속성을 참조합니다.
      
      // ... (위치 복원 로직)

      // [What] 스냅샷에 크기 정보가 있다면, 노드의 style.width/height를 강제로 덮어씌워
      //        사용자가 변경하기 전의 크기로 되돌립니다.
      if (item.dimensions) {
          node.style = {
              ...node.style,
              width: item.dimensions.width,
              height: item.dimensions.height,
          };
      }
  }
  ```
- [NodeCanvas.vue](../src/components/scene-editor/NodeCanvas.vue)
  - `<VueFlow>` 컴포넌트 전체에 `@node-resize-start` 이벤트 연결 (전역 감지 안전장치)
  ```vue
  <VueFlow
      <!-- ... -->
      <!-- [Why] 개별 노드에서 이벤트가 누락되는 경우를 대비한 '안전장치(Fallback)'입니다. -->
      <!-- [Where] Vue Flow 라이브러리에서 제공하는 캔버스 레벨 이벤트입니다. -->
      <!-- [What] 캔버스 내 어떤 노드든 리사이즈가 시작되면 이를 감지하고 handleNodeDragStart를 호출하여 -->
      <!--        현재 상태(위치+크기)를 스토어에 저장합니다. -->
      @node-resize-start="handleNodeDragStart"
  >
  ```
- [ShotNode/VideoNode/SceneHeaderNode...](../src/components/scene-editor/nodes/)
  - 각 노드 파일 내부의 `<NodeResizer>`에 `@resize-start` 직접 연결 (개별 노드 동작 신뢰성 확보)
  ```vue
  <NodeResizer
      :is-visible="props.selected"
      <!-- [Why] 사용자가 '특정 노드'를 조작하려는 의도를 가장 정확하게 포착하기 위해서입니다. -->
      <!-- [Where] useSceneNodeStore 훅에서 가져온 'pushPositionSnapshot' 액션을 사용합니다. -->
      <!-- [What] 리사이즈 핸들을 잡는 순간(@resize-start), 변경 전의 '원본 상태'를 스냅샷으로 찍어 -->
      <!--        Undo 스택에 쌓아둡니다. (이후 Ctrl+Z 시 이 시점으로 복귀) -->
      @resize-start="store.pushPositionSnapshot()"
  />
  ```

-------------------------------------------------------------------------------------

## 씬 데이터 자동 저장 (Persistence)
- **수정 내용**: 노드/엣지의 변경사항을 LocalStorage에 실시간으로 자동 저장하고, 씬 로드 시 복원.
- **이전 문제**: 페이지 새로고침 시 작업 중이던 노드 배치가 모두 초기화됨.
- **해결**: `localStorage`를 활용하여 데이터 영속성 보장.

### 수정된 파일
- [sceneNode.ts](../src/stores/sceneNode.ts)
  - `saveToLocalStorage`: 데이터 저장 로직 및 디바운스(Debounce) 처리
  - `loadSceneNodes`: 초기 로드 시 LocalStorage 데이터 우선 확인
  ```typescript
  // [Why] 페이지 새로고침 시 작업 내역 증발 방지. 고유 키 사용.
  function getStorageKey(id: string): string {
      return `scene-nodes-${id}`;
  }

  // [What] 핵심 데이터(노드, 엣지)를 JSON으로 변환하여 브라우저 저장소에 보관
  function saveToLocalStorage(): void {
      if (!sceneId.value) return;
      // ...
      const data = {
          nodes: nodes.value,
          edges: edges.value,
          updatedAt: new Date().toISOString(),
      };
      localStorage.setItem(getStorageKey(sceneId.value), JSON.stringify(data));
  }

  // [Where] Store의 state 변화를 감시하는 watch 함수 내부
  watch([nodes, edges], () => {
      // [Optimization] 너무 잦은 저장을 막기 위해 1초 Debounce 적용
      if (!isLoading.value && sceneId.value) {
          debouncedSave();
      }
  }, { deep: true });
  ```

-------------------------------------------------------------------------------------

## 협업 바 드래그/클릭 구분 (UX 개선)
- **수정 내용**: 플로팅 협업 바를 드래그할 때 패널이 열리지 않도록 클릭 이벤트와 구분.
- **이전 문제**: 위치를 옮기려고 드래그를 마치는 순간 `click` 이벤트가 발생하여 원치 않게 패널이 열림.
- **해결**: `useDraggable` 훅의 `shouldPreventClick` 상태를 확인하여 드래그 직후 클릭 무시.

### 수정된 파일
- [CollabContainer.vue](../src/components/collab/CollabContainer.vue)
  - `handleContainerClick`: 드래그 여부 확인 후 토글 실행
  ```typescript
  // [Where] useDraggable 훅에서 위치 정보와 드래그 상태 감지 로직 가져옴
  const { position, isDragging, onMouseDown, shouldPreventClick } = useDraggable({ ... });

  // [What] 컨테이너 클릭 핸들러
  function handleContainerClick() {
      // [Why] "방금 드래그를 했는가?" 확인. 드래그였다면 클릭 동작(패널 열기)을 차단.
      if (shouldPreventClick()) return;
      
      // 순수 클릭일 때만 패널 토글
      if (!collabStore.isPanelOpen) {
          collabStore.togglePanel();
      }
  }
  ```

-------------------------------------------------------------------------------------

## 마스터 이미지 캐릭터 선택
- **수정 내용**: 마스터 이미지 속성 패널에 캐릭터 선택 UI 추가 및 프롬프트 반영.
- **이전 문제**: 캐릭터를 지정할 수 있는 입력 필드가 없었음.
- **해결**: 라디오 버튼 그룹 UI 추가 및 `MasterImageNodeData` 모델 확장.

### 수정된 파일
- [MasterImagePanel.vue](../src/components/scene-editor/panels/MasterImagePanel.vue)
  - UI: 캐릭터 선택 라디오 버튼 그룹 추가
  - Logic: 선택된 캐릭터를 프롬프트 문자열에 포함
  ```vue
  <!-- [UI] 사용자 선택을 위한 라디오 버튼 그룹 -->
  <div class="panel-section">
    <label class="panel-label"><User /> 캐릭터</label>
    <div class="panel-radio-group">
      <!-- v-model로 form.character와 양방향 바인딩 -->
      <label v-for="char in ['나희도', '백이진', '고유림', '없음']" :key="char" class="panel-radio">
         <input type="radio" v-model="form.character" ... />
      </label>
    </div>
  </div>
  ```
  ```typescript
  // [What] 최종 프롬프트 생성 로직
  function generatePrompt(): void {
    // [Logic] 선택된 캐릭터가 있다면 프롬프트 파라미터로 추가
    const charPart = form.value.character ? `, character: ${form.value.character}` : '';
    
    // 스타일, 시간대, 분위기 등 기존 속성과 조합
    const promptText = `Wide establishing shot..., mood: ${form.value.mood}${charPart}`;
    
    nodeStore.updateNode(props.node.id, { ...form.value, prompt: promptText ... });
  }
  ```

-------------------------------------------------------------------------------------

## 설계 문서 정합성 수정: character → objectIds (2026-01-22)
- **수정 내용**: 설계 문서(`vue-flow-node-workflow-design.md`)에 맞게 `character` 필드를 제거하고 `objectIds` 배열로 통합.
- **이전 문제**: 설계 문서는 `objectIds: string[]`로 캐릭터+오브젝트를 관리하도록 되어 있으나, 구현에서 `character: string` 필드를 별도로 추가하여 불일치 발생.
- **해결**: `character` 필드 제거 및 `objectIds` 체크박스 다중 선택 UI로 변경.

### 수정된 파일
- [node.ts](../src/types/node.ts)
  - `MasterImageNodeData` 인터페이스에서 `character?: string` 필드 제거
  ```typescript


  // ✅ After (설계 문서와 일치)
  export interface MasterImageNodeData extends BaseNodeData {
      // ...
      objectIds: string[];  // 등장 오브젝트 IDs (캐릭터 포함)
  }
  ```

-------------------------------------------------------------------------------------

## 설계 문서 정합성 수정: objectIds 다중 선택 UI (2026-01-22)
- **수정 내용**: 마스터 이미지 패널의 캐릭터 라디오 버튼을 `objectIds` 체크박스 다중 선택으로 변경.
- **이전 문제**: 라디오 버튼으로 캐릭터 1명만 선택 가능했음. 설계 문서는 체크박스 다중 선택을 요구.
- **해결**: 캐릭터+오브젝트를 통합한 체크박스 그룹 UI 구현.

### 수정된 파일
- [MasterImagePanel.vue](../src/components/scene-editor/panels/MasterImagePanel.vue)
  - UI: 라디오 버튼 → 체크박스 그룹으로 변경
  - Logic: `form.objectIds` 배열로 다중 선택 관리
  ```typescript


  // ✅ After (다중 선택)
  const form = ref({
      objectIds: [] as string[],  // 캐릭터 + 오브젝트 여러 개
      // ...
  });
  ```
  ```vue
  <!-- ❌ Before: 라디오 버튼 (단일 선택) -->
  <div class="panel-radio-group">
    <label v-for="char in ['나희도', '백이진', ...]">
      <input type="radio" v-model="form.character" />
    </label>
  </div>

  <!-- ✅ After: 체크박스 (다중 선택) -->
  <div class="panel-checkbox-group">
    <label v-for="obj in objectOptions" :key="obj.id" class="panel-checkbox">
      <input 
        type="checkbox" 
        :checked="form.objectIds.includes(obj.id)"
        @change="toggleObject(obj.id)"
      />
      <span class="panel-checkbox-label">
        {{ obj.name }}
        <span class="panel-checkbox-tag">{{ obj.type === 'character' ? '캐릭터' : '오브젝트' }}</span>
      </span>
    </label>
  </div>
  ```
  ```typescript
  // [What] 오브젝트 선택 토글 함수
  function toggleObject(objectId: string): void {
      const idx = form.value.objectIds.indexOf(objectId);
      if (idx >= 0) {
          form.value.objectIds.splice(idx, 1);  // 이미 선택됨 → 제거
      } else {
          form.value.objectIds.push(objectId);   // 미선택 → 추가
      }
  }

  // [What] 프롬프트 생성 시 objectIds 사용
  function generatePrompt(): void {
      const objectNames = getSelectedObjectNames();  // 선택된 이름들 조합
      const objectPart = objectNames ? `, featuring: ${objectNames}` : '';
      const promptText = `Wide establishing shot..., mood: ${form.value.mood}${objectPart}`;
      // ...
  }
  ```

-------------------------------------------------------------------------------------

## 색상 규격 통일: Tailwind Rose → Project Rose (2026-01-22)
- **수정 내용**: 프로젝트 전체에서 Tailwind Rose 색상(`#F43F5E`, `rgba(244, 63, 94, ...)`)을 프로젝트 Rose 테마(`#FF85A1`, `rgba(255, 133, 161, ...)`)로 통일.
- **이전 문제**: 일부 컴포넌트에서 Tailwind CSS의 Rose-500 색상을 사용하여 `variables.css`에 정의된 프로젝트 테마와 불일치.
- **해결**: 모든 하드코딩된 `rgba(244, 63, 94, ...)` 값을 `rgba(255, 133, 161, ...)`로 변경.

### 수정된 파일 (15개)
| 경로 | 변경 내용 |
|------|----------|
| `pages/ProfileEditPage.vue` | focus box-shadow 색상 |
| `pages/DashboardPage.vue` | hover box-shadow 색상 |
| `components/scenario/StepIndicator.vue` | active step box-shadow |
| `components/scenario/ScenarioScenesStep.vue` | hover box-shadow |
| `components/scenario/ScenarioPromptStep.vue` | focus box-shadow |
| `components/scenario/ScenarioPlotStep.vue` | focus box-shadow |
| `components/scenario/ScenarioInputStep.vue` | focus box-shadow |
| `components/project/StoryPromptForm.vue` | focus box-shadow |
| `components/project/SceneCard.vue` | hover box-shadow |
| `components/project/ProjectCard.vue` | hover box-shadow |
| `components/project/NewProjectModal.vue` | focus box-shadow |
| `components/project/AddCharacterModal.vue` | focus box-shadow |
| `components/common/Card.vue` | hover box-shadow |
| `components/common/AppHeader.vue` | focus box-shadow |
| `components/collab/CollabPanel.vue` | focus box-shadow |

### 변경 예시
```css


box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
```

### 참고: 프로젝트 Rose 팔레트 (`variables.css`)
```css
--rose-500: #FF85A1;  /* RGB: 255, 133, 161 */
--rose-600: #FF6B8A;
--rose-700: #FF5C7A;
```

-------------------------------------------------------------------------------------

## VideoPanel 카메라 움직임 UI 개선 (2026-01-22)
- **수정 내용**: 영상 패널의 카메라 움직임 선택 UI에 아이콘 추가.
- **이전 문제**: 텍스트만 있어 직관성이 떨어짐. 설계 문서는 GIF 미리보기를 요구.
- **해결**: 각 카메라 옵션에 Lucide 아이콘 추가 (GIF는 에셋 준비 후 추가 예정).

### 수정된 파일
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
  - 카메라 옵션 배열에 `icon` 속성 추가
  - 버튼 내부에 아이콘 컴포넌트 표시
  ```typescript
  // 아이콘이 추가된 카메라 옵션
  const cameraOptions: { value: CameraMotion; label: string; icon: any }[] = [
    { value: 'static', label: '정지', icon: Circle },
    { value: 'zoomIn', label: '줌인', icon: ZoomIn },
    { value: 'zoomOut', label: '줌아웃', icon: ZoomOut },
    { value: 'panLeft', label: '팬 좌', icon: ArrowLeft },
    { value: 'panRight', label: '팬 우', icon: ArrowRight },
    { value: 'tiltUp', label: '틸트 업', icon: ArrowUp },
    { value: 'tiltDown', label: '틸트 다운', icon: ArrowDown },
  ];
  ```
  ```vue
  <!-- 아이콘 + 텍스트 레이아웃 -->
  <button class="panel-camera-option" ...>
    <component :is="opt.icon" class="panel-camera-icon" />
    <span>{{ opt.label }}</span>
  </button>
  ```

- [_panel.css](../src/assets/styles/_panel.css)
  - `.panel-camera-option` 레이아웃을 세로 정렬로 변경
  - `.panel-camera-icon` 스타일 추가
  ```css
  .panel-camera-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 56px;
  }

  .panel-camera-icon {
    width: 18px;
    height: 18px;
    margin-bottom: 0.25rem;
  }
  ```

                                                                                                                                                  

-------------------------------------------------------------------------------------

## AI API 연동 준비 (2026-01-22)
- **수정 내용**: 마스터 이미지, 그리드, 샷, 비디오 패널에 AI 프롬프트 생성 및 이미지/영상 생성 API 연동 준비
- **이전 문제**: 프롬프트 생성이 단순 문자열 조합이었고, 이미지/영상 생성은 console.log만 출력
- **해결**: 실제 API 호출 구조로 변경, 로딩 상태 및 에러 처리 추가

### 사용자 흐름
1. 옵션 선택 → "프롬프트 생성" 클릭 → AI가 영어 프롬프트 생성
2. 프롬프트 확인 → "승인" 클릭
3. "이미지/영상 생성" 클릭 → 로딩 표시 → 결과 URL 노드에 반영

### 신규 파일
- [api.ts](../src/types/api.ts) - AI API 요청/응답 타입 정의
- [ai.ts](../src/services/api/ai.ts) - AI 서비스 (generatePrompt, generateNode, pollJobUntilComplete)

### 수정된 파일
- [index.ts](../src/services/index.ts) - `aiService` export 추가
- [MasterImagePanel.vue](../src/components/scene-editor/panels/MasterImagePanel.vue)
- [StoryboardGridPanel.vue](../src/components/scene-editor/panels/StoryboardGridPanel.vue)
- [ShotPanel.vue](../src/components/scene-editor/panels/ShotPanel.vue)
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)

### 공통 변경사항
```typescript
// 로딩 상태 추가
const isGeneratingPrompt = ref(false);
const isGeneratingImage = ref(false);
const errorMessage = ref<string | null>(null);

// AI API 호출 (비동기)
async function generatePrompt(): Promise<void> {
  isGeneratingPrompt.value = true;
  try {
    const prompt = await aiService.generatePrompt({
      nodeType: 'MASTER', // or 'GRID', 'SHOT', 'VIDEO'
      style: form.value.style,
      // ... 기타 옵션
    });
    form.value.prompt = prompt;
    nodeStore.updateNode(props.node.id, { prompt, promptStatus: PromptStatus.GENERATED });
  } catch (error) {
    errorMessage.value = '프롬프트 생성에 실패했습니다.';
  } finally {
    isGeneratingPrompt.value = false;
  }
}

// 이미지/영상 생성 (폴링 포함)
async function generateImage(): Promise<void> {
  isGeneratingImage.value = true;
  try {
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.RUNNING });
    const jobId = await aiService.generateNode(props.node.id, form.value.prompt);
    const result = await aiService.pollJobUntilComplete(jobId);
    nodeStore.updateNode(props.node.id, {
      jobStatus: JobStatus.SUCCEEDED,
      imageUrl: result.resultUrl,
    });
  } catch (error) {
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
  } finally {
    isGeneratingImage.value = false;
  }
}
```

### API 스펙 확정 시 수정 가이드
백엔드 API 스펙이 확정되면 `src/types/api.ts` 파일의 인터페이스만 수정하면 됩니다.
```typescript
// 예: 필드명이 변경된 경우
export interface GeneratePromptRequest {
  nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
  artStyle: string;        // style → artStyle
  atmosphere: string;      // mood → atmosphere
  // ...
}
```
