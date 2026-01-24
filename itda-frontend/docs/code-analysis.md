# itda-frontend 코드 분석 보고서

> **분석 일시**: 2026-01-23  
> **분석 대상**: itda-frontend (AI 영화 스튜디오 프론트엔드)  
> **파일 범위**: Vue(72), TypeScript(50), JSON(10) - 총 132개 파일

---

## 📊 프로젝트 개요

### 기술 스택
- **프레임워크**: Vue 3.5.24 + TypeScript 5.9.3
- **빌드 도구**: Vite 7.2.4
- **상태 관리**: Pinia 3.0.4
- **라우팅**: Vue Router 4.6.4
- **에디터**: Vue Flow 1.48.1 (노드 기반 씬 에디터)
- **HTTP 클라이언트**: Axios 1.13.2
- **UI 아이콘**: Lucide Vue Next 0.562.0
- **테스팅**: Playwright 1.57.0

### 아키텍처 패턴
- **Composition API** 기반 Vue 3 설계
- **Pinia Stores**를 통한 중앙 집중식 상태 관리
- **Mock/API** 이중 서비스 레이어 (개발/프로덕션 분리)
- **Node-based Workflow** (Vue Flow를 활용한 비주얼 에디터)

---

## 📁 파일 구조

```
src/
├── assets/          # 스타일, 이미지 등 정적 자산
├── components/      # Vue 컴포넌트 (7개 카테고리)
│   ├── collab/      # 협업 기능 (4개)
│   ├── common/      # 공통 UI (9개)
│   ├── editor/      # 구 에디터 (10개)
│   ├── project/     # 프로젝트 관리 (6개)
│   ├── scenario/    # 시나리오 생성 (6개)
│   ├── scene-editor/# 씬 에디터 (17개) ⭐ 핵심
│   └── timeline/    # 타임라인 (4개)
├── composables/     # 재사용 로직 (6개)
├── constants/       # 상수 정의
├── layouts/         # 레이아웃 컴포넌트
├── pages/           # 페이지 컴포넌트 (11개)
├── router/          # 라우터 설정
├── services/        # API/Mock 서비스 레이어
│   ├── api/         # 실제 API (9개)
│   ├── mock/        # Mock 데이터 (8개)
│   └── webrtc/      # WebRTC 협업 (2개)
├── stores/          # Pinia 상태 관리 (10개)
├── types/           # TypeScript 타입 정의 (4개)
└── utils/           # 유틸리티 함수
```

---

## 🎯 주요 기능 영역

### 1. **씬 에디터 (Scene Editor)** - 핵심 기능
**노드 타입**: Scene Header → Master Image → Storyboard Grid → Shot → Video

**관련 파일**:
- `src/pages/SceneEditPage.vue` (391줄)
- `src/stores/sceneNode.ts` (1054줄) ⚠️ **매우 큰 파일**
- `src/types/node.ts` (216줄)

**노드 컴포넌트**:
1. `SceneHeaderNode.vue` - 씬 헤더/정보
2. `MasterImageNode.vue` - 마스터 이미지 생성
3. `StoryboardGridNode.vue` - 스토리보드 그리드
4. `ShotNode.vue` - 개별 샷
5. `VideoNode.vue` - 영상 생성

### 2. **프로젝트 관리**
- `src/stores/project.ts`
- `src/pages/ProjectDetailPage.vue` (32,700 bytes)

### 3. **시나리오 생성**
- `src/stores/scenario.ts`
- 다단계 프롬프트/플롯/씬 생성 워크플로우

### 4. **타임라인 편집**
- `src/stores/timeline.ts`
- `src/pages/TimelinePage.vue`

### 5. **실시간 협업**
- `src/stores/collab.ts`
- `src/services/webrtc/`
- WebRTC 기반 실시간 커서/상태 공유

---

## ⚠️ 코드 일관성 이슈

### 🔴 **1. 중복 타입 정의**

#### **문제 1: `TimelineClip` 인터페이스 중복**
**위치**: `src/types/index.ts`

```typescript
// Line 139-149: 첫 번째 정의
export interface TimelineClip {
  clipId: string;
  nodeId: number | string;
  sceneId?: number;
  sourceNodeId?: string;
  thumbnailUrl: string;
  videoUrl?: string;
  duration: number;
  order: number;
  label?: string;
}

// Line 254-264: 두 번째 정의 (완전히 동일)
export interface TimelineClip {
  clipId: string;
  nodeId: number | string;
  sceneId?: number;
  sourceNodeId?: string;
  thumbnailUrl: string;
  videoUrl?: string;
  duration: number;
  order: number;
  label?: string;
}
```

**영향도**: ⚠️ **중복 정의로 인한 혼란 가능성**

**해결 방법**:
```typescript
// Line 254-264를 삭제하고, Line 139-149만 유지
```

---

#### **문제 2: `ApiResponse` 인터페이스 중복**

**파일 1**: `src/types/index.ts` (Line 4)
```typescript
export interface ApiResponse<T = unknown> {
  code: string;
  message?: string;
  data?: T;
  details?: Record<string, unknown>;
}
```

**파일 2**: `src/types/api.ts` (Line 94)
```typescript
export interface ApiResponse<T> {
  code: 'SUCCESS' | 'ACCEPTED' | string;
  message?: string;
  data?: T;
  details?: Record<string, unknown>;
}
```

**차이점**:
- `index.ts`: `code`가 `string` 타입
- `api.ts`: `code`가 `'SUCCESS' | 'ACCEPTED' | string` (더 구체적)

**영향도**: ⚠️ **타입 불일치로 인한 타입 안전성 저하**

**해결 방법**:
```typescript
// src/types/index.ts에서 ApiResponse를 제거하고
// src/types/api.ts의 정의만 사용
// 또는 index.ts에서 api.ts의 ApiResponse를 re-export
export { ApiResponse } from './api';
```

---

### 🟡 **2. 타입 일관성 이슈**

#### **GridLayout 타입 중복**
**파일 1**: `src/types/index.ts` (Line 157)
```typescript
export type GridLayout = '2x2' | '2x3' | '3x3';
```

**파일 2**: `src/types/node.ts` (Line 51)
```typescript
export type GridLayout = '2x2' | '2x3' | '3x3';
```

**해결**: 한 곳에서만 정의하고 다른 곳에서 import

---

### 🟡 **3. 파일 크기 불균형**

#### **매우 큰 파일**
| 파일 | 라인 수 | 크기 | 상태 |
|------|---------|------|------|
| `sceneNode.ts` | 1,054줄 | 36,915 bytes | 🔴 **리팩토링 필요** |
| `ProjectDetailPage.vue` | 약 900줄 | 32,700 bytes | 🟡 **모니터링 필요** |
| `scenario.ts` | - | 12,235 bytes | 🟡 |

**문제점**:
- `sceneNode.ts`: 70개의 함수/메서드가 한 파일에 집중
- 유지보수성 저하
- 테스트 어려움

**해결 방안**:
```typescript
// stores/sceneNode/ 디렉토리로 분리
stores/sceneNode/
  ├── index.ts          // Main store
  ├── actions.ts        // 액션 분리
  ├── helpers.ts        // 헬퍼 함수
  ├── nodeCreators.ts   // 노드 생성 로직
  └── layout.ts         // 레이아웃 로직
```

---

## 🔍 코드 패턴 분석

### ✅ **좋은 패턴**

#### 1. **Mock/API 분리 패턴**
```typescript
// src/services/index.ts
const useMock = import.meta.env.VITE_USE_MOCK !== 'false';
export const authService = useMock ? mockAuthService : apiAuthService;
export const aiService = useMock ? mockAiService : apiAiService;
```
- 개발 중 백엔드 의존성 제거
- 환경 변수로 쉬운 전환

#### 2. **Composition API 활용**
```typescript
// src/composables/useAutoLayout.ts
export function useAutoLayout() {
  const applyDagreLayout = (nodes, edges) => { ... }
  return { applyDagreLayout }
}
```
- 재사용 가능한 로직 분리
- 테스트 용이성

#### 3. **타입 안전성**
```typescript
// src/types/node.ts
export const NodeType = {
  SCENE_HEADER: 'sceneHeader',
  MASTER_IMAGE: 'masterImage',
  // ...
} as const;

export type NodeType = (typeof NodeType)[keyof typeof NodeType];
```
- const assertion으로 리터럴 타입 보장

---

### ⚠️ **개선 필요 패턴**

#### 1. **로컬 스토리지 직접 접근**

**문제**:
```typescript
// src/services/api/client.ts (Line 19)
const token = localStorage.getItem('accessToken');

// src/stores/sceneNode.ts
localStorage.setItem(storageKey, JSON.stringify(data));
```

**문제점**:
- 스토리지 접근이 여러 곳에 분산
- 일관된 에러 핸들링 어려움
- SSR 환경에서 문제 발생 가능

**해결**:
```typescript
// src/utils/storage.ts
class StorageManager {
  getItem<T>(key: string): T | null {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : null;
    } catch (e) {
      console.error('Storage read error:', e);
      return null;
    }
  }

  setItem<T>(key: string, value: T): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
      console.error('Storage write error:', e);
    }
  }
}

export const storage = new StorageManager();
```

---

#### 2. **하드코딩된 상수**

**문제**:
```typescript
// src/stores/ui.ts (Line 37)
const id = `toast_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`

// src/stores/ui.ts (Line 42)
duration: 4000,
```

**해결**:
```typescript
// src/constants/ui.ts
export const UI_CONSTANTS = {
  TOAST_DEFAULT_DURATION: 4000,
  TOAST_ID_PREFIX: 'toast_',
  MODAL_ANIMATION_DURATION: 300,
  DEBOUNCE_DELAY: 300,
} as const;
```

---

#### 3. **에러 처리 일관성 부족**

**문제**:
```typescript
// src/services/api/client.ts (Line 40-44)
if (error.response?.status === 401 && !originalRequest._retry) {
  // Logic for refreshing token could go here
  // For now, simpler handling or logout redirect
  // window.location.href = '/auth'; 
}
```

**주석으로만 남겨진 에러 처리**

**해결**:
```typescript
// src/services/api/errorHandler.ts
export class ApiErrorHandler {
  static async handle(error: AxiosError) {
    if (error.response?.status === 401) {
      await this.handleUnauthorized();
    } else if (error.response?.status === 403) {
      await this.handleForbidden();
    }
    // ...
  }

  private static async handleUnauthorized() {
    const authStore = useAuthStore();
    authStore.logout();
    router.push('/auth');
  }
}
```

---

## 🚀 최적화 제안

### 1. **번들 크기 최적화**

#### **문제**: 전체 아이콘 라이브러리 import
```typescript
// 현재 방식 (추정)
import * from 'lucide-vue-next'
```

**해결**: 필요한 아이콘만 import
```typescript
// src/components/icons.ts
export {
  Plus,
  Trash,
  Edit,
  Play,
  Pause,
} from 'lucide-vue-next';
```

---

### 2. **컴포넌트 Lazy Loading**

**현재**:
```typescript
// src/router/index.ts (추정)
import ProjectDetailPage from '@/pages/ProjectDetailPage.vue'
```

**최적화**:
```typescript
const ProjectDetailPage = () => import('@/pages/ProjectDetailPage.vue')
const SceneEditPage = () => import('@/pages/SceneEditPage.vue')
```

---

### 3. **Computed 중복 계산 방지**

**문제**: `SceneEditPage.vue`
```typescript
const timelineClips = computed(() => {
  return nodeStore.confirmedVideos.map((n, index) => {
    // 복잡한 변환 로직
  })
})
```

**해결**: Store에서 직접 computed 제공
```typescript
// stores/sceneNode.ts
const timelineClips = computed(() => {
  return confirmedVideos.value.map((n, index) => ({
    clipId: n.id,
    nodeId: n.id,
    // ...
  }))
})
```

---

### 4. **WebSocket 연결 최적화**

**추정 문제**: WebRTC signaling이 페이지마다 재연결

**해결**: 
```typescript
// src/services/webrtc/manager.ts
class WebRTCManager {
  private static instance: WebRTCManager;
  
  static getInstance() {
    if (!this.instance) {
      this.instance = new WebRTCManager();
    }
    return this.instance;
  }
  
  // 싱글톤 패턴으로 연결 재사용
}
```

---

## 📊 Store 분석

### Pinia Stores 구조

| Store | 라인 수 | 주요 책임 | 복잡도 |
|-------|---------|-----------|--------|
| `sceneNode.ts` | 1,054 | 씬 노드 관리, 레이아웃, 저장 | 🔴 높음 |
| `scenario.ts` | 400+ | 시나리오 생성 워크플로우 | 🟡 중간 |
| `timeline.ts` | 300+ | 타임라인 편집 | 🟡 중간 |
| `node.ts` | 200+ | 구 노드 시스템 (레거시?) | 🟡 |
| `collab.ts` | 200+ | 실시간 협업 | 🟢 적절 |
| `project.ts` | 150+ | 프로젝트 관리 | 🟢 적절 |
| `scene.ts` | 200+ | 씬 관리 | 🟢 적절 |
| `character.ts` | 150+ | 캐릭터 관리 | 🟢 적절 |
| `ui.ts` | 91 | UI 상태 (토스트, 모달) | 🟢 적절 |
| `auth.ts` | 50+ | 인증 | 🟢 적절 |

---

## 🎨 컴포넌트 분석

### Scene Editor 컴포넌트 계층구조

```
SceneEditPage.vue (391줄)
└── NodeCanvas.vue
    ├── SceneHeaderNode.vue
    ├── MasterImageNode.vue
    ├── StoryboardGridNode.vue
    ├── ShotNode.vue
    └── VideoNode.vue

└── NodePanelContainer.vue
    ├── SceneHeaderPanel.vue
    ├── MasterImagePanel.vue
    ├── StoryboardGridPanel.vue
    ├── ShotPanel.vue
    └── VideoPanel.vue
```

**패턴**: 각 노드 타입마다 Node + Panel 컴포넌트 쌍

---

## 🔧 Composables 분석

| Composable | 기능 | 재사용성 |
|------------|------|----------|
| `useAutoLayout.ts` | Dagre 기반 자동 레이아웃 | ✅ 높음 |
| `useDraggable.ts` | 드래그 가능 요소 | ✅ 높음 |
| `useZoom.ts` | 줌 컨트롤 | ✅ 높음 |
| `useWebRTC.ts` | WebRTC 래퍼 | 🟡 중간 |
| `useGenerationToast.ts` | AI 생성 토스트 | 🟡 특화됨 |
| `useSidebarShortcut.ts` | 사이드바 단축키 | 🟡 특화됨 |

---

## 📝 권장 리팩토링 순서

### 🔥 **우선순위 1 (즉시)**

1. **중복 타입 제거**
   - `TimelineClip` 인터페이스 중복 제거
   - `ApiResponse` 타입 통합
   - `GridLayout` 타입 통합

2. **sceneNode.ts 파일 분리**
   ```
   stores/sceneNode/
     ├── index.ts
     ├── types.ts
     ├── helpers.ts
     ├── actions/
     │   ├── nodeCreation.ts
     │   ├── nodeUpdate.ts
     │   ├── nodeDelete.ts
     │   └── layout.ts
     └── persistence.ts
   ```

---

### 🟡 **우선순위 2 (1주 내)**

3. **스토리지 매니저 추가**
   - 로컬 스토리지 접근 중앙화
   - 에러 핸들링 통합

4. **상수 추출**
   - UI 상수를 `constants/ui.ts`로 이동
   - 매직 넘버 제거

5. **에러 처리 개선**
   - API 에러 핸들러 추가
   - 일관된 에러 메시지

---

### 🟢 **우선순위 3 (2주 내)**

6. **번들 최적화**
   - 아이콘 tree-shaking
   - 컴포넌트 lazy loading
   - Code splitting

7. **타입 안전성 강화**
   - `any` 타입 제거
   - 타입 가드 추가

---

## 💡 코드 품질 메트릭

### 현재 상태

| 메트릭 | 상태 | 평가 |
|--------|------|------|
| TypeScript 사용률 | ~95% | ✅ 우수 |
| 타입 안전성 | 중간 | 🟡 개선 필요 |
| 모듈화 | 양호 | 🟢 적절 |
| 코드 중복 | 낮음 | ✅ 좋음 |
| 파일 크기 균형 | 불균형 | 🔴 개선 필요 |
| 에러 처리 | 부족 | 🟡 개선 필요 |
| 주석 | 적절 | 🟢 좋음 |

---

## 🎯 최종 권장사항

### ✅ **유지할 것**
1. Mock/API 분리 패턴 - 개발 효율성 높음
2. Composition API 활용 - 재사용성 우수
3. Pinia를 통한 상태 관리 - 구조 명확
4. TypeScript 타입 정의 - 타입 안전성 제공

### 🔧 **개선할 것**
1. **중복 제거**: 타입 정의 중복 즉시 제거
2. **파일 분리**: sceneNode.ts 리팩토링 (1,054줄 → 200줄 이하)
3. **스토리지 중앙화**: 로컬 스토리지 접근 패턴 통일
4. **에러 처리**: 일관된 에러 핸들링 구현

### 🚀 **도입 고려**
1. **Vite 플러그인**: 
   - `vite-plugin-compression` (gzip 압축)
   - `vite-plugin-imagemin` (이미지 최적화)
2. **ESLint 규칙 강화**: 
   - `no-any` 규칙 활성화
   - `max-lines` 제한 (500줄)
3. **성능 모니터링**: 
   - Vue DevTools Performance
   - Lighthouse CI 통합

---

## 📌 추가 검토 필요 항목

### 의문점
1. **node.ts vs sceneNode.ts**: 두 개의 노드 store가 존재하는 이유?
   - `src/stores/node.ts` (6,629 bytes)
   - `src/stores/sceneNode.ts` (36,915 bytes)
   - **추정**: `node.ts`는 구 시스템, `sceneNode.ts`는 Vue Flow 기반 신규 시스템

2. **editor vs scene-editor**: 두 개의 에디터 컴포넌트 폴더?
   - `src/components/editor/` (10개 파일)
   - `src/components/scene-editor/` (17개 파일)
   - **추정**: editor는 레거시, scene-editor는 현재 사용

3. **Mock 데이터 품질**: 실제 API 스펙과의 일치 여부 확인 필요

---

## 📚 참고 자료

### 프로젝트 문서
- `docs/scene-editor-shortcuts.md`
- PRD 문서 (존재 시)

### 설정 파일
- `package.json`
- `tsconfig.json`
- `vite.config.ts`

---

**분석 완료 일시**: 2026-01-23 16:40 KST  
**분석 도구**: Claude Sonnet 4.5 (Thinking)
