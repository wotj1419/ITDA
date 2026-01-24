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

-------------------------------------------------------------------------------------

## TypeScript 빌드 에러 수정 (2026-01-22)
- **수정 내용**: Jenkins CI/CD 빌드 실패를 유발하던 TypeScript 컴파일 에러 3건 수정.
- **이전 문제**: `npm run build` 실행 시 `vue-tsc` 단계에서 타입 에러로 빌드 실패.
- **해결**: 미사용 매개변수 처리 및 타입 가드 추가.

### 에러 목록 및 해결
| 파일 | 라인 | 에러 코드 | 원인 | 해결 방법 |
|------|------|-----------|------|-----------|
| `mock/auth.ts` | 21 | TS6133 | `data` 매개변수 미사용 | `_data`로 변경 |
| `stores/sceneNode.ts` | 87-88 | TS2339 | `node.style` 타입 불일치 | 타입 가드 추가 |

### 수정된 파일
- [auth.ts](../src/services/mock/auth.ts)
  - 미사용 매개변수 앞에 언더스코어 prefix 추가
  ```typescript
  // ❌ Before: 에러 발생
  async signup(data: SignupRequest) { ... }

  // ✅ After: 에러 해결
  async signup(_data: SignupRequest) { ... }
  ```

- [sceneNode.ts](../src/stores/sceneNode.ts)
  - `buildPositionSnapshot` 함수에 타입 가드 추가
  ```typescript
  // ❌ Before: node.style이 함수일 수 있어 에러 발생
  dimensions: {
      width: node.style?.width ?? '',
      height: node.style?.height ?? '',
  }

  // ✅ After: 객체 타입 확인 후 접근
  const style = typeof node.style === 'object' && node.style !== null ? node.style : {};
  dimensions: {
      width: ('width' in style ? style.width : '') ?? '',
      height: ('height' in style ? style.height : '') ?? '',
  }
  ```

### 참고
- **TS6133**: 선언되었지만 사용되지 않는 변수/매개변수 경고. 언더스코어(`_`)를 붙이면 "의도적으로 무시"함을 표시.
- **TS2339**: 객체에 해당 속성이 존재하지 않음. Vue Flow의 `Node.style` 타입이 `Styles | StyleFunc` 유니온이라 함수일 경우 `width` 속성 접근 불가.

-------------------------------------------------------------------------------------

## 씬 정보 박스에 전 페이지 씬 데이터 연동 (2026-01-22)
- **수정 내용**: 전 페이지(ProjectDetailPage)에서 가져온 씬 제목/설명/순서를 씬 에디터의 씬 정보 박스에 표시.
- **이전 문제**: "씬 1: 새 씬", "씬 설명을 입력하세요"와 같은 기본값만 하드코딩되어 표시됨.
- **해결**: `loadSceneNodes` 함수에 씬 정보 파라미터 추가 및 `ensureSceneHeaderNode`에서 전달받은 값 사용.

### 수정된 파일
- [sceneNode.ts](../src/stores/sceneNode.ts)
  - `loadSceneNodes` 함수에 `sceneInfo` 파라미터 추가
  - `ensureSceneHeaderNode` 함수에서 전달받은 씬 정보 사용
  ```typescript
  // 변경 전
  async function loadSceneNodes(sceneIdParam: string): Promise<void>

  // 변경 후 - 씬 정보 객체를 파라미터로 추가
  async function loadSceneNodes(
    sceneIdParam: string, 
    sceneInfo?: { title: string; description: string; order: number }
  ): Promise<void>
  ```
  ```typescript
  // 변경 전 (ensureSceneHeaderNode 내부)
  title: '새 씬',
  description: '씬 설명을 입력하세요.',
  sceneOrder: 1,

  // 변경 후
  title: sceneInfo?.title || '새 씬',
  description: sceneInfo?.description || '씬 설명을 입력하세요.',
  sceneOrder: sceneInfo?.order || 1,
  ```

- [SceneEditPage.vue](../src/pages/SceneEditPage.vue)
  - 호출 시 `currentScene` 정보를 전달하도록 수정
  ```typescript
  // 변경 전
  await nodeStore.loadSceneNodes(sceneId.value);

  // 변경 후
  const scene = currentScene.value;
  await nodeStore.loadSceneNodes(sceneId.value, scene ? {
    title: scene.title,
    description: scene.description || '',
    order: scene.order,
  } : undefined);
  ```

-------------------------------------------------------------------------------------

## 노드 삭제 경고 팝업 (자식 노드 있을 때만) (2026-01-22)
- **수정 내용**: Delete/Backspace로 노드 삭제 시, 자식 노드가 있으면 경고 팝업을 띄운 뒤 확인 시에만 삭제.
- **이전 문제**: 자식 노드가 있는 경우에도 바로 삭제되어 실수 위험이 있었음.
- **해결**: `sceneNode`에서 자식 여부 판별 함수 추가하고, `SceneEditPage`에서 삭제 요청을 확인 모달로 분기.

### 수정된 파일
- [SceneEditPage.vue](../src/pages/SceneEditPage.vue)
  - Delete/Backspace 처리 시 `requestDeleteNode()`로 분기
  - 자식 노드가 있으면 모달 오픈, 확인 시 삭제
  ```typescript
  // 변경 전
  nodeStore.deleteNode(selectedId);

  // 변경 후
  requestDeleteNode(selectedId);
  ```

- [sceneNode.ts](../src/stores/sceneNode.ts)
  - 자식 노드 여부/삭제 가능 여부 헬퍼 추가
  ```typescript
  function hasDescendants(nodeId: string): boolean {
    return getDescendantIds(nodeId).length > 0;
  }

  function canDeleteNode(nodeId: string): boolean {
    const targetNode = nodes.value.find((n) => n.id === nodeId);
    if (!targetNode?.data) return false;
    return (
      targetNode.data.type !== NodeType.SCENE_HEADER &&
      targetNode.data.type !== NodeType.MASTER_IMAGE
    );
  }
  ```

- [NodeDeleteConfirmModal.vue](../src/components/scene-editor/modals/NodeDeleteConfirmModal.vue)
  - 경고 문구/삭제/취소 버튼 모달 추가
  ```vue
  <p class="delete-warning">
    이 노드를 삭제하면 하위 노드들도 함께 삭제됩니다. 계속 하시겠습니까?
  </p>
  ```

### 추후 수정할 사항
- 삭제 API 연동 시 실패/권한 오류를 토스트로 안내하고 UI 상태 롤백
- 키보드 외 삭제 진입점(우클릭 메뉴/버튼)에도 동일 모달 적용
- 하위 노드 개수 표시 등 경고 문구 강화 (선택)

-------------------------------------------------------------------------------------

## 생성 실패 팝업에 원인/권고 표시 (우측 하단) (2026-01-22)
- **수정 내용**: 이미지/그리드/샷/영상 생성 실패 시, 우측 하단 팝업에 원인과 권고 조치를 표시하고 X 버튼으로 닫기 가능.
- **이전 문제**: 실패 팝업에 단순 실패 메시지만 표시되어 원인/대응 확인이 어려웠음.
- **해결**: 기존 Toast 컴포넌트/스토어 규격을 확장해 에러 메시지에 원인/권고 문구를 포함하고, 에러 토스트는 자동 종료를 끔.

### 수정된 파일
- [useGenerationToast.ts](../src/composables/useGenerationToast.ts)
  - `finishGenerationToast`에 원인/권고 옵션 추가
  - 에러 시 `autoClose: false` 및 `duration: 0` 설정
  ```typescript
  // 수정 전
  finishGenerationToast(toastId, kind, result): void {
    uiStore.removeToast(toastId)
    uiStore.showToast({
      type: result,
      title: result === 'success' ? `${label} 생성 완료` : `${label} 생성 실패`,
      message: result === 'success'
        ? `${label} 생성이 완료되었습니다.`
        : `${label} 생성에 실패했습니다.`,
      position: 'bottom-right',
    })
  }

  // 수정 후
  finishGenerationToast(toastId, kind, result, options = {}): void {
    uiStore.removeToast(toastId)
    const reason = options.reason?.trim() || '알 수 없는 오류'
    const advice = options.advice?.trim() || '잠시 후 다시 시도하거나 프롬프트를 간단히 수정해 주세요.'
    uiStore.showToast({
      type: result,
      title: result === 'success' ? `${label} 생성 완료` : `${label} 생성 실패`,
      message: result === 'success'
        ? `${label} 생성이 완료되었습니다.`
        : `원인: ${reason}\n권고: ${advice}`,
      position: 'bottom-right',
      autoClose: result !== 'error',
      duration: result === 'error' ? 0 : undefined,
    })
  }
  ```

- [ToastContainer.vue](../src/components/common/ToastContainer.vue)
  - `.toast-message`에 `white-space: pre-line` 추가 (줄바꿈 지원)
  ```css
  /* 수정 전 */
  .toast-message {
    font-size: 0.75rem;
    color: var(--gray-500);
    margin-top: 0.25rem;
  }

  /* 수정 후 */
  .toast-message {
    font-size: 0.75rem;
    color: var(--gray-500);
    margin-top: 0.25rem;
    white-space: pre-line;
  }
  ```

- [MasterImagePanel.vue](../src/components/scene-editor/panels/MasterImagePanel.vue)
- [StoryboardGridPanel.vue](../src/components/scene-editor/panels/StoryboardGridPanel.vue)
- [ShotPanel.vue](../src/components/scene-editor/panels/ShotPanel.vue)
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
  - 에러 발생 시 원인 메시지 전달
  ```typescript
  // 수정 전
  finishGenerationToast(toastId, 'image', 'error');

  // 수정 후
  const reason = error instanceof Error ? error.message : '알 수 없는 오류';
  finishGenerationToast(toastId, 'image', 'error', { reason });
  ```

### 추후 수정할 사항
- API에서 원인/권고 메시지를 제공하면 그대로 매핑
- 원인 문구 표준화(네트워크/권한/타임아웃 등) 및 다국어 처리
- 특정 오류 코드에 따라 자동 재시도 옵션 제공

-------------------------------------------------------------------------------------

## 샷 타입 안내 i 버튼 팝업 (StoryboardGridPanel) (2026-01-22)
- **수정 내용**: 샷 타입(다중 선택) 라벨 옆에 회색 원형 i 버튼을 추가하고, 클릭 시 샷 타입 설명 팝업을 표시.
- **이전 문제**: 샷 타입 의미를 UI에서 확인할 수 없어 선택 기준이 불명확했음.
- **해결**: 기존 패널 라벨/컬러 규격을 활용해 i 버튼과 팝업 UI를 추가하고, 바깥 클릭/ESC로 닫히도록 처리.

### 수정된 파일
- [StoryboardGridPanel.vue](../src/components/scene-editor/panels/StoryboardGridPanel.vue)
  - 샷 타입 라벨과 i 버튼을 같은 행으로 배치
  - 샷 타입 설명 팝업/목록 추가
  - 바깥 클릭/ESC로 닫히는 동작 추가
  - i 버튼/팝업 스타일 추가
  ```vue
  <!-- 수정 후 -->
  <div class="panel-label-row">
    <label class="panel-label">
      <Camera class="panel-label-icon" />
      샷 타입 (다중 선택)
    </label>
    <div ref="shotTypeHelpRef" class="panel-info">
      <button
        type="button"
        class="panel-info-button"
        aria-label="샷 타입 안내"
        :aria-expanded="isShotTypeHelpOpen"
        @click="toggleShotTypeHelp"
      >
        <span class="panel-info-icon">i</span>
      </button>
      <div v-if="isShotTypeHelpOpen" class="panel-info-popover">
        <div class="panel-info-title">샷 타입 안내</div>
        <ul class="panel-info-list">
          <li v-for="item in shotTypeHelpItems" :key="item.label" class="panel-info-item">
            <span class="panel-info-label">{{ item.label }}</span>
            <span class="panel-info-desc">{{ item.description }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
  ```

### 추후 수정할 사항
- 샷 타입 설명 문구 확정 후 고정
- 다국어 지원 시 문구 번역 추가

-------------------------------------------------------------------------------------

## 샷 타입 다중 선택 체크박스 전환 (2026-01-23)
- **수정 내용**: 샷 타입 선택 UI를 드롭다운에서 체크박스(다중 선택)로 변경하고, 저장 포맷은 그리드와 동일하게 배열(`shotTypes`)로 유지.
- **이전 문제**: 샷 타입이 단일 선택만 가능했고, 그리드와 UI/데이터 구조가 달라 일관성이 떨어짐.
- **해결**: 체크박스 그룹으로 전환하고 `shotTypes` 배열을 저장하되, 기존 `shotType` 문자열도 계속 업데이트해 API/기존 화면 호환 유지.

### 수정된 파일
- [ShotPanel.vue](../src/components/scene-editor/panels/ShotPanel.vue)
  - 샷 타입 입력을 체크박스 다중 선택으로 변경
  - 기존 단일 문자열 값을 배열로 복원(레거시 호환)
  - 프롬프트 생성 시 `shotTypes` 배열을 조인해 기존 `shotType` 문자열도 저장
  ```typescript
  // 수정 전
  const form = ref({
    shotType: '',
    expression: '',
    additionalDetail: '',
    prompt: '',
  });

  // 수정 후
  const form = ref({
    shotTypes: [] as string[],
    expression: '',
    additionalDetail: '',
    prompt: '',
  });
  ```
  ```typescript
  // 수정 전
  form.value = {
    shotType: data.value.shotType || '',
    expression: data.value.expression || '',
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.prompt || '',
  };

  // 수정 후
  const fallbackShotTypes =
    data.value.shotTypes && data.value.shotTypes.length > 0
      ? [...data.value.shotTypes]
      : normalizeShotTypes(data.value.shotType);

  form.value = {
    shotTypes: fallbackShotTypes,
    expression: data.value.expression || '',
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.prompt || '',
  };
  ```
  ```vue
  <!-- 수정 전 -->
  <select v-model="form.shotType" class="panel-select">
    <option value="">선택하세요</option>
    <option v-for="opt in shotTypeOptions" :key="opt" :value="opt">{{ opt }}</option>
  </select>

  <!-- 수정 후 -->
  <div class="panel-checkbox-group">
    <label v-for="opt in shotTypeOptions" :key="opt" class="panel-checkbox">
      <input
        type="checkbox"
        :checked="form.shotTypes.includes(opt)"
        @change="toggleShotType(opt)"
      />
      <span class="panel-checkbox-label">{{ opt }}</span>
    </label>
  </div>
  ```

- [node.ts](../src/types/node.ts)
  - 샷 노드에 `shotTypes?: string[]` 필드 추가(그리드와 동일 포맷 유지)
  ```typescript
  // 수정 전
  shotType: string;

  // 수정 후
  shotTypes?: string[];
  shotType: string;
  ```

- [sceneNode.ts](../src/stores/sceneNode.ts)
  - 신규 샷 노드 기본값에 `shotTypes: []` 추가
  ```typescript
  // 수정 전
  shotType: '',

  // 수정 후
  shotTypes: [],
  shotType: '',
  ```

### 추후 수정할 사항
- API가 배열을 직접 받도록 변경되면 `shotType` 문자열 필드 제거 검토
- 샷 노드 카드 표시 문구를 배열 기반으로 개선(복수 표기 방식 결정)

-------------------------------------------------------------------------------------

## 영상 카메라 움직임 옵션 교체 (2026-01-23)
- **수정 내용**: 영상 패널의 카메라 움직임 옵션을 신규 세트(로우 줌인/줌아웃/좌→우 팬/틸트 업/정지 카메라)로 교체하고, 선택값을 프롬프트에 반영.
- **이전 문제**: 기존 옵션(줌인/줌아웃/팬 좌·우/틸트 업·다운/정지)과 요청된 신규 옵션 세트가 불일치.
- **해결**: 카메라 옵션/내부 값/라벨을 새 규격으로 맞추고, 기존 저장값은 패널에서 열 때 새 값으로 정규화.

### 수정된 파일
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
  - 카메라 옵션을 새 값으로 교체
  - 기존 저장값을 새 값으로 정규화
  ```typescript
  // 수정 전
  const cameraOptions = [
    { value: 'static', label: '정지' },
    { value: 'zoomIn', label: '줌인' },
    { value: 'zoomOut', label: '줌아웃' },
    { value: 'panLeft', label: '팬 좌' },
    { value: 'panRight', label: '팬 우' },
    { value: 'tiltUp', label: '틸트 업' },
    { value: 'tiltDown', label: '틸트 다운' },
  ];

  // 수정 후
  const cameraOptions = [
    { value: 'lowZoomIn', label: '로우 줌인' },
    { value: 'zoomOut', label: '줌아웃' },
    { value: 'panLeftToRight', label: '좌->우 팬' },
    { value: 'tiltUp', label: '틸트 업' },
    { value: 'staticCamera', label: '정지 카메라' },
  ];
  ```

- [node.ts](../src/types/node.ts)
  - `CameraMotion` 타입을 신규 값으로 교체
  ```typescript
  // 수정 전
  type CameraMotion =
    | 'zoomIn' | 'zoomOut' | 'panLeft' | 'panRight' | 'tiltUp' | 'tiltDown' | 'static';

  // 수정 후
  type CameraMotion =
    | 'lowZoomIn' | 'zoomOut' | 'panLeftToRight' | 'tiltUp' | 'staticCamera';
  ```

- [sceneNode.ts](../src/stores/sceneNode.ts)
  - 신규 비디오 기본값을 `staticCamera`로 변경

- [VideoNode.vue](../src/components/scene-editor/nodes/VideoNode.vue)
  - 카드 표시 라벨을 신규 값 기준으로 변경(구값도 매핑)

- [sceneNodes.ts](../src/services/mock/sceneNodes.ts)
  - Mock 비디오 모션 값도 신규 값으로 정리

### 추후 수정할 사항
- 실제 API 스펙 확정 시 `CameraMotion` 값과 1:1 매핑 확인
- 구 데이터 마이그레이션(로컬 저장/서버 저장) 시 변환 규칙 적용 여부 검토

-------------------------------------------------------------------------------------

## 카메라 움직임 안내 팝업 추가 (2026-01-23)
- **수정 내용**: 영상 사이드바의 "카메라 움직임" 라벨 옆에 안내 팝업(i 버튼) 추가.
- **이전 문제**: 카메라 움직임 옵션의 의미를 설명하는 UI가 없어 이해가 어려움.
- **해결**: 그리드 샷 타입 안내 팝업과 동일한 동작/스타일로 팝업을 구현하고, 각 옵션별 설명을 제공.

### 수정된 파일
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
  - 안내 팝업 토글/닫기 로직 추가(바깥 클릭/ESC 닫힘)
  - i 버튼 및 설명 리스트 UI 추가
  ```typescript
  // 추가된 안내 데이터
  const cameraMotionHelpItems = [
    { label: '로우 줌인', description: '천천히 피사체를 가까이 당겨 긴장감을 높입니다.' },
    { label: '줌아웃', description: '화면을 멀리 물려 배경과 상황을 넓게 보여줍니다.' },
    { label: '좌->우 팬', description: '좌측에서 우측으로 시선을 이동시키며 공간을 훑습니다.' },
    { label: '틸트 업', description: '아래에서 위로 시야를 올려 크기나 높이를 강조합니다.' },
    { label: '정지 카메라', description: '카메라를 고정해 인물/장면의 안정감을 강조합니다.' },
  ];
  ```
  ```vue
  <!-- 라벨 우측 안내 버튼 + 팝업 -->
  <div ref="cameraMotionHelpRef" class="panel-info">
    <button class="panel-info-button" aria-label="카메라 움직임 안내">
      <span class="panel-info-icon">i</span>
    </button>
    <div v-if="isCameraMotionHelpOpen" class="panel-info-popover">
      <div class="panel-info-title">카메라 움직임 안내</div>
      <ul class="panel-info-list">
        <li v-for="item in cameraMotionHelpItems" :key="item.label" class="panel-info-item">
          <span class="panel-info-label">{{ item.label }}</span>
          <span class="panel-info-desc">{{ item.description }}</span>
        </li>
      </ul>
    </div>
  </div>
  ```

### 추후 수정할 사항
- 옵션 설명 문구 다듬기(용어 통일/길이 조정)
- 디자인 시스템에 맞춘 팝업 스타일 공통화 여부 검토

-------------------------------------------------------------------------------------

## 영상 타임라인 확정 체크박스 추가 (2026-01-23)
- **수정 내용**: 영상 사이드바 하단에 "타임라인 확정" 체크박스를 추가하고, 생성 성공 시에만 토글 가능하도록 제한.
- **이전 문제**: 타임라인 확정 UI가 버튼 형태로 별도 위치에 있어 요청한 체크박스 형태/위치와 달랐음.
- **해결**: 기존 확정 로직(`toggleVideoConfirm`)을 유지하면서, "영상 생성" 버튼 하단에 체크박스 형태로 이동/정렬.

### 수정된 파일
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
  - 확정 버튼 UI 제거
  - 하단에 "타임라인 확정" 체크박스 추가
  - 생성 성공(`SUCCEEDED`)일 때만 체크 가능하도록 `disabled` 처리
  ```vue
  <!-- 수정 전: 별도 확정 버튼 -->
  <div v-if="isSucceeded" class="panel-section">
    <button
      :class="['panel-btn panel-btn--full', isConfirmed ? 'panel-btn--confirmed' : 'panel-btn--confirm']"
      @click="toggleConfirm"
    >
      {{ isConfirmed ? '★ 확정됨 (취소하려면 클릭)' : '☆ 타임라인에 확정' }}
    </button>
  </div>

  <!-- 수정 후: 하단 체크박스 -->
  <div class="panel-toggle-row">
    <span class="panel-checkbox-label">타임라인 확정</span>
    <input
      type="checkbox"
      class="panel-toggle"
      :checked="isConfirmed"
      :disabled="!isSucceeded"
      @change="toggleConfirm"
    />
  </div>
  ```

### 추후 수정할 사항
- 체크박스 비활성 상태 안내 문구(생성 완료 후 가능) 표시 여부 검토
- 타임라인 화면 반영 시 사용자 피드백(토스트 등) 추가 여부 검토

-------------------------------------------------------------------------------------

## 씬 에디터/타임라인 확정 영상 동기화 + 순차 재생 (2026-01-23)
- **수정 내용**: 확정된 영상이 미니 타임라인과 TimelinePage에 반영되고, 드래그로 순서 변경/확정 해제/X 삭제 및 순차 재생(재생 버튼)을 지원.
- **이전 문제**: 씬 에디터 확정 상태와 타임라인 페이지가 분리되어 있고, 미니 타임라인에는 재생/정렬/해제 기능이 없었음.
- **해결**: 확정 영상에 `timelineOrder`를 저장해 정렬을 유지하고, 미니 타임라인과 TimelinePage가 동일 데이터를 기반으로 동작하도록 연결.

### 수정된 파일
- [node.ts](../src/types/node.ts)
  - `VideoNodeData`에 `timelineOrder?: number` 추가

- [index.ts](../src/types/index.ts)
  - `TimelineClip`에 `sceneId`/`sourceNodeId`/`videoUrl` 추가 및 `nodeId` 타입 확장

- [sceneNode.ts](../src/stores/sceneNode.ts)
  - 확정/해제 시 `timelineOrder` 저장
  - 정렬/순서 변경 유틸 추가(`ensureTimelineOrder`, `updateTimelineOrder`)

- [SceneEditPage.vue](../src/pages/SceneEditPage.vue)
  - 확정 영상 → 미니 타임라인 클립 매핑 개선(영상 URL 포함)
  - 미니 타임라인 드래그/삭제/재생 이벤트 연결
  - 재생 모달 연결 및 빈 리스트 재생 시 토스트 처리

- [MiniTimeline.vue](../src/components/scene-editor/MiniTimeline.vue)
  - 드래그 앤 드롭 순서 변경
  - X 버튼으로 확정 해제
  - 재생 버튼 추가

- [TimelinePlaybackModal.vue](../src/components/timeline/TimelinePlaybackModal.vue)
  - 순차 재생 모달 신규 추가(재생 실패 시 토스트)

- [TimelinePage.vue](../src/pages/TimelinePage.vue)
  - `sceneId` 쿼리로 확정 영상 로드
  - 미리보기 상단 재생 버튼 추가, 재생 모달 연결

- [timeline.ts](../src/stores/timeline.ts)
  - `sceneId`가 있을 때 로컬 저장된 확정 영상 기반 로드/정렬/삭제 동작

- [ui.ts](../src/stores/ui.ts)
  - 타임라인 재생 모달 ID 상수 추가

### 추후 수정할 사항
- TimelinePage의 병합/다운로드 로직을 확정 영상 데이터와 1:1로 동기화
- 재생 모달에서 재생 목록/현재 순서 UI 표시 추가 여부 검토
- 서버 연동 시 `timelineOrder` 저장 방식(API 스펙) 확정

-------------------------------------------------------------------------------------

## 코드 분석 및 품질 문서화 (2026-01-23)
- **수정 내용**: itda-frontend 전체 코드베이스(Vue 72개, TypeScript 50개, JSON 10개)를 분석하여 코드 품질 보고서 작성.
- **이전 문제**: 프로젝트의 코드 일관성, 중복 정의, 최적화 포인트에 대한 공식 문서가 없었음.
- **해결**: `docs/code-analysis.md` 문서를 생성하여 타입 중복, 파일 크기 불균형, 개선 권장사항을 체계적으로 정리.

### 발견된 주요 이슈

#### 🔴 중복 타입 정의
| 타입 | 위치 | 문제 |
|------|------|------|
| `TimelineClip` | `types/index.ts` L139, L254 | 동일 인터페이스 2회 정의 |
| `ApiResponse` | `types/index.ts` L4, `types/api.ts` L94 | 타입 불일치 (`code` 필드) |
| `GridLayout` | `types/index.ts` L157, `types/node.ts` L51 | 동일 타입 2회 정의 |

#### 🟡 파일 크기 불균형
| 파일 | 라인 수 | 권장 |
|------|---------|------|
| `stores/sceneNode.ts` | 1,054줄 | 200줄 이하로 분리 필요 |
| `pages/ProjectDetailPage.vue` | ~900줄 | 모니터링 필요 |

### 신규 파일
- [code-analysis.md](./code-analysis.md) - 전체 코드 분석 보고서

### 권장 리팩토링 순서
1. **즉시**: 중복 타입 제거 (`TimelineClip`, `ApiResponse`, `GridLayout`)
2. **1주 내**: `sceneNode.ts` 파일 분리, 스토리지 매니저 추가
3. **2주 내**: 번들 최적화, 타입 안전성 강화

-------------------------------------------------------------------------------------

## Merge feat/UI 브랜치 통합 (2026-01-23)
- **수정 내용**: `feat/UI` 브랜치의 변경사항을 `feat/node-0119` 브랜치에 머지.
- **이전 문제**: UI 관련 변경사항이 별도 브랜치에 있어 최신 기능과 분리되어 있었음.
- **해결**: `git merge origin/feat/UI`를 통해 UI 개선사항을 현재 작업 브랜치에 통합.

### 머지된 주요 변경사항
- 미니 타임라인 드래그 앤 드롭 기능
- 타임라인 재생 모달
- 카메라 움직임 옵션 UI 개선
- 영상 패널 확정 체크박스 추가

### 영향받은 파일
- [SceneEditPage.vue](../src/pages/SceneEditPage.vue)
- [TimelinePage.vue](../src/pages/TimelinePage.vue)
- [MiniTimeline.vue](../src/components/scene-editor/MiniTimeline.vue)
- [VideoPanel.vue](../src/components/scene-editor/panels/VideoPanel.vue)
- [sceneNode.ts](../src/stores/sceneNode.ts)
- [timeline.ts](../src/stores/timeline.ts)
- [ui.ts](../src/stores/ui.ts)
- [node.ts](../src/types/node.ts)
- [index.ts](../src/types/index.ts)

-------------------------------------------------------------------------------------

## Jenkins/Mattermost 연동 (2026-01-23)
- **수정 내용**: CI/CD 파이프라인(Jenkins)과 Mattermost 알림 연동 설정.
- **이전 문제**: 빌드/배포 상태를 수동으로 확인해야 했음.
- **해결**: `Jenkinsfile` 추가 및 Mattermost Webhook 연결.

### 신규/수정 파일
- `Jenkinsfile` - 🆕 Jenkins 파이프라인 정의
- `deploy/docker-compose.yml` - 🆕 Docker Compose 설정
- `deploy/nginx/conf.d/app.conf` - 🆕 Nginx 설정
- `deploy/prometheus/prometheus.yml` - 🆕 Prometheus 모니터링 설정

### 테스트 커밋
- `1108732`: 멀티브랜치 적용 테스트
- `f5a4428`: Mattermost 연결 테스트
- `a94a8fc`: Mattermost 연결 테스트

-------------------------------------------------------------------------------------

## AI 생성 토스트 개선 (2026-01-23)
- **수정 내용**: AI 이미지/영상 생성 프로세스에 대한 토스트 알림 UX 개선.
- **이전 문제**: 생성 중/완료/실패 상태에 대한 피드백이 불명확했음.
- **해결**: `useGenerationToast` 컴포저블을 통해 일관된 토스트 알림 제공.

### 수정된 파일
- [useGenerationToast.ts](../src/composables/useGenerationToast.ts)
  - 생성 시작/완료/실패에 대한 토스트 함수 제공
  - 에러 시 원인/권고 메시지 포함
  - 에러 토스트는 자동 닫힘 비활성화
  ```typescript
  // 사용 예시
  const { startGenerationToast, finishGenerationToast } = useGenerationToast();
  
  // 생성 시작
  const toastId = startGenerationToast('image');
  
  // 성공 시
  finishGenerationToast(toastId, 'image', 'success');
  
  // 실패 시 (원인/권고 포함)
  finishGenerationToast(toastId, 'image', 'error', {
    reason: '네트워크 오류',
    advice: '인터넷 연결을 확인해주세요.',
  });
  ```

-------------------------------------------------------------------------------------

## Mock AI 서비스 개선 (2026-01-23)
- **수정 내용**: 개발 중 AI API 호출을 대체하는 Mock 서비스 개선.
- **이전 문제**: Mock 데이터가 실제 API 응답과 일치하지 않아 통합 시 문제 발생 가능성.
- **해결**: Mock 응답 구조를 실제 API 스펙에 맞게 정렬.

### 수정된 파일
- [ai.ts](../src/services/mock/ai.ts)
  - 프롬프트 생성 Mock 응답 개선
  - 이미지/영상 생성 Job 폴링 시뮬레이션 추가
  ```typescript
  // Mock AI 서비스 구조
  export const mockAiService = {
    generatePrompt: async (params) => { ... },
    generateNode: async (nodeId, prompt) => { ... },
    pollJobUntilComplete: async (jobId) => { ... },
  };
  ```

- [sceneNodes.ts](../src/services/mock/sceneNodes.ts)
  - Mock 씬 노드 데이터에 신규 카메라 모션 값 적용
  - `timelineOrder` 필드 추가

-------------------------------------------------------------------------------------

## UI 상수 정의 (2026-01-23)
- **수정 내용**: UI 관련 상수를 중앙 집중화하여 관리.
- **이전 문제**: 매직 넘버와 문자열이 여러 파일에 흩어져 있어 유지보수 어려움.
- **해결**: `constants/ui.ts`에 UI 상수 정의.

### 수정된 파일
- [ui.ts](../src/constants/ui.ts)
  - 토스트 기본 duration
  - 모달 ID 상수
  - 애니메이션 타이밍 등
  ```typescript
  export const UI_CONSTANTS = {
    TOAST_DEFAULT_DURATION: 4000,
    MODAL_ANIMATION_DURATION: 300,
    DEBOUNCE_DELAY: 300,
  } as const;
  
  export const MODAL_IDS = {
    TIMELINE_PLAYBACK: 'timeline-playback',
    NODE_DELETE_CONFIRM: 'node-delete-confirm',
  } as const;
  ```

-------------------------------------------------------------------------------------

 

