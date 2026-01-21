# Vue Flow 노드 기반 워크플로우 상세 설계서

> **버전**: 1.1  
> **작성일**: 2026-01-18  
> **수정일**: 2026-01-18 (Rose 테마 적용)  
> **관련 PRD**: PRD_AI_Movie_Studio_v2.5.md - Section 4.2.6, 4.3.1

---

## 📋 설계 결정 맥락 (Discussion Context)

> 이 섹션은 설계 과정에서 논의된 주요 결정사항과 그 근거를 정리합니다.

### 질문 1: 노드 연결(Edge) 방식
- **질문**: 사용자가 직접 드래그로 연결할 수 있어야 하나요?
- **결정**: 
  - P0에서는 `[+]` 버튼 클릭 시 `parentNodeId` 기반 **자동 연결**
  - 트랜지션 영상(시작+끝 샷)은 VideoPanel에서 end shot 선택 모드로 지원
  - 엣지는 `parentNodeId`에서 **파생/계산** (수동 드래그 편집은 P1 이후)

### 질문 2: 노드 레이아웃
- **질문**: 자동 배치 vs 자유 드래그?
- **결정**: 
  - **최초 진입 시 Dagre 자동 배치 1회** 적용
  - 이후 사용자가 자유롭게 드래그하여 배치 가능
  - 버튼 클릭으로 재정렬 지원
  - PRD 4.3.1 기준: **세로 흐름(Top→Bottom)**, 분기는 좌우

### 질문 3: Active Master 전환 UX
- **결정**: 
  - 노드에 **Active 뱃지(★)** 표시
  - 사이드바에서 **"Active로 설정"** 버튼 제공
  - Non-Active 브랜치는 기본 **접힘 상태** (펼치기 가능)

### 질문 4: 사이드바 UI
- **결정**: 노드 클릭 → 우측 사이드바 열림, **한 번에 하나**만 표시

### 질문 5: 확정(Confirm) 표시 방식
- **결정**: 
  - **★ 별 아이콘 + 녹색 보더** 조합 (PRD에 `★ 확정`으로 표기)
  - 확정 버튼: **노드 호버 시 + 사이드바** 둘 다 지원
  - 확정된 영상만 **Success 녹색(#22C55E)** 사용하여 눈에 띄게

### 질문 6: 노드 위치 저장 여부
- **결정**: 
  - **P0에서는 저장하지 않음** (MVP 범위 축소)
  - 세션 중 임시 유지, 새로고침 시 자동 레이아웃 재적용
  - P1 이후 localStorage 또는 서버 저장 검토

### 질문 7: 색상 체계
- **질문**: 노드마다 다른 색상 vs 통일된 테마?
- **결정**: 
  - 기존 디자인 시스템이 **Rose(로즈핑크) 테마**
  - 알록달록한 색상 대신 **Rose 모노톤**으로 통일
  - 노드 타입은 Rose 농도로 계층 표현 (마스터=진함 → 영상=연함)
  - **상태(idle/running/succeeded/failed)**만 Status Colors 사용
  - **확정된 영상만 Success 녹색** 사용 (유일한 예외)

### 질문 8: 상태 체계
- **결정**:
  - PRD 기준 `JobStatus`: pending → running → succeeded/failed
  - UI 로직용 `PromptStatus`: draft → generated → approved
  - `idle` 상태는 `jobStatus === null`로 표현

### 질문 9: [+] 버튼 UX
- **문제**: 우하단 작은 버튼은 클릭하기 어려움
- **결정**: 
  - 호버 시 **노드 하단 중앙에 원형 `⊕` 버튼** 표시
  - 모바일/키보드 접근성: **선택 시에도 노출**

### 질문 10: 트랜지션 영상
- **결정**:
  - P0 MVP에 포함 (P1 아님)
  - VideoPanel에서 토글 ON → end shot 선택 모드
  - 엣지 2개 생성 (시작샷→영상, 끝샷→영상)
  - `isTransition`은 `endShotId !== null`로 파생

---

## 목차

1. [개요](#1-개요)
2. [아키텍처](#2-아키텍처)
3. [노드 타입 상세 스펙](#3-노드-타입-상세-스펙)
4. [엣지(연결) 규칙](#4-엣지연결-규칙)
5. [상태 관리 (Pinia Store)](#5-상태-관리-pinia-store)
6. [사이드바 패널 설계](#6-사이드바-패널-설계)
7. [주요 인터랙션 시나리오](#7-주요-인터랙션-시나리오)
8. [레이아웃 및 스타일링](#8-레이아웃-및-스타일링)
9. [구현 단계](#9-구현-단계)
10. [파일 구조](#10-파일-구조)

---

## 1. 개요

### 1.1 목적
씬 편집 화면에서 Vue Flow를 활용하여 **마스터 이미지 → 스토리보드 그리드 → 샷 → 영상** 생성 흐름을 시각적으로 관리하는 노드 기반 캔버스를 구현합니다.

### 1.2 핵심 원칙
- **세로 흐름 (Top → Bottom)**: 마스터에서 영상까지 위에서 아래로 진행
- **좌우 분기**: 같은 레벨의 형제 노드는 좌우로 배치
- **단일 사이드바**: 노드 클릭 시 우측에 상세 패널 표시 (한 번에 하나)
- **확정(Confirm) 개념**: 영상 노드 중 타임라인에 사용할 것만 확정 표시
- **트랜지션 영상**: 시작+끝 샷을 지정하여 자연스러운 연결 지원 (MVP 포함)
- **엣지 파생**: P0에서는 `parentNodeId` 기반으로 엣지를 자동 계산
- **접근성**: hover-only 액션은 선택 시에도 노출 (모바일/키보드 대응)

### 1.3 기술 스택
```
@vue-flow/core: ^1.x
@vue-flow/background: ^1.x
@vue-flow/controls: ^1.x
dagre: ^0.8.x (자동 레이아웃)
pinia: 상태 관리
```

---

## 2. 아키텍처

### 2.1 컴포넌트 구조

```
src/
├── pages/
│   └── SceneEditPage.vue          # 메인 페이지 (캔버스 + 사이드바 컨테이너)
│
├── components/
│   └── scene-editor/
│       ├── NodeCanvas.vue         # Vue Flow 캔버스 래퍼
│       ├── nodes/                  # 커스텀 노드 컴포넌트
│       │   ├── index.ts                # 노드 레지스트리
│       │   ├── SceneHeaderNode.vue     # 📖 씬 헤더 노드
│       │   ├── MasterImageNode.vue     # 🎬 마스터 이미지 노드
│       │   ├── StoryboardGridNode.vue  # 📐 그리드 노드
│       │   ├── ShotNode.vue            # 📷 샷 노드
│       │   └── VideoNode.vue           # 🎥 영상 노드
│       ├── panels/                 # 사이드바 패널 컴포넌트
│       │   ├── NodePanelContainer.vue  # 패널 컨테이너 (라우팅)
│       │   ├── index.ts                # 패널 레지스트리
│       │   ├── BasePanel.vue           # 패널 공통 레이아웃
│       │   ├── SceneHeaderPanel.vue    # 씬 헤더 패널 (읽기+편집)
│       │   ├── MasterImagePanel.vue    # 마스터 생성/편집 패널
│       │   ├── StoryboardGridPanel.vue # 그리드 생성/편집 패널
│       │   ├── ShotPanel.vue           # 샷 생성/편집 패널
│       │   └── VideoPanel.vue          # 영상 생성/편집 패널
│       ├── edges/                  # 커스텀 엣지 (필요시)
│       │   └── DefaultEdge.vue
│       └── controls/               # 캔버스 컨트롤
│           ├── CanvasControls.vue      # 줌, 리셋, 자동정렬 버튼
│           └── MiniMap.vue             # 미니맵 (선택)
│
├── composables/
│   ├── useNodeCanvas.ts            # Vue Flow 초기화 및 유틸리티
│   ├── useAutoLayout.ts            # Dagre 자동 레이아웃
│   └── useNodeActions.ts           # 노드 CRUD 액션
│
├── stores/
│   └── nodeStore.ts                # 노드/엣지 상태 관리
│
└── types/
    └── node.ts                     # 노드 관련 타입 정의
```

### 2.2 데이터 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        SceneEditPage                            │
│  ┌─────────────────────────────┐  ┌─────────────────────────┐  │
│  │       NodeCanvas            │  │   NodePanelContainer    │  │
│  │  ┌─────────────────────┐   │  │  ┌───────────────────┐  │  │
│  │  │     Vue Flow        │   │  │  │  Selected Node    │  │  │
│  │  │  ┌───────────────┐  │   │  │  │  Panel Component  │  │  │
│  │  │  │ Custom Nodes  │  │◄──┼──┼──│  (동적 렌더링)    │  │  │
│  │  │  └───────────────┘  │   │  │  └───────────────────┘  │  │
│  │  └─────────────────────┘   │  └─────────────────────────┘  │
│  └──────────────┬──────────────┘                               │
│                 │                                               │
│                 ▼                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Pinia nodeStore                       │   │
│  │  - nodes: Node[]                                         │   │
│  │  - edges: Edge[]                                         │   │
│  │  - selectedNodeId: string | null                         │   │
│  │  - selectionMode: 'none' | 'selectEndShot'               │   │
│  │  - actions: addNode, updateNode, deleteNode, etc.        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 노드 타입 상세 스펙

### 3.1 노드 타입 / 상태 Enum

```typescript
// types/node.ts
export enum NodeType {
  SCENE_HEADER = 'sceneHeader',
  MASTER_IMAGE = 'masterImage',
  STORYBOARD_GRID = 'storyboardGrid',
  SHOT = 'shot',
  VIDEO = 'video',
}

// PRD 기준 상태값 (Job Status)
export enum JobStatus {
  PENDING = 'pending',     // AI 생성 대기 (큐)
  RUNNING = 'running',     // AI 생성 중
  SUCCEEDED = 'succeeded', // 완료
  FAILED = 'failed',       // 실패
}

// 프롬프트 승인 상태 (UI/업무 로직용)
export enum PromptStatus {
  DRAFT = 'draft',         // 입력 중
  GENERATED = 'generated', // 프롬프트 생성됨
  APPROVED = 'approved',   // 승인 완료
}
```

### 3.2 노드 공통 인터페이스

```typescript
// types/node.ts
export interface BaseNodeData {
  id: string;
  type: NodeType;
  jobStatus: JobStatus | null; // pending/running/succeeded/failed (미요청 시 null)
  promptStatus: PromptStatus; // draft/generated/approved
  createdAt: string;
  updatedAt: string;
  
  // 버전 관리
  versionGroupId: string;     // 동일 계열 묶음 (최초 노드 id 권장)
  version: number;            // v1, v2, v3...
  
  // 트리 구조 (P0 기준 단일 진실)
  parentNodeId: string | null;
  
  // UI 상태
  isCollapsed: boolean;       // 접기 상태 (마스터 브랜치용)
  childCount: number;         // 접혔을 때 표시할 자식 수
}
```

> **유지보수 포인트**: 노드/패널은 `index.ts` 레지스트리에서 매핑하여  
> `NodeCanvas`/`NodePanelContainer`가 타입별 분기 없이 동작하도록 구성합니다.

> **단일 기준 원칙(P0)**: 트리 관계는 `parentNodeId`만 사용합니다.  
> 그리드/샷/영상의 상위 정보는 `parentNodeId`에서 파생됩니다.

### 3.3 씬 헤더 노드 (SceneHeaderNode)

> **읽기 전용** 메타 노드. 씬 진입 시 자동 생성되며 사용자가 추가/삭제 불가.  
> Scene API의 제목/설명과 동기화되는 **가상 노드**.

```typescript
export interface SceneHeaderNodeData extends BaseNodeData {
  type: NodeType.SCENE_HEADER;
  sceneId: string;
  title: string;              // 씬 제목 (예: "고립된 아침")
  description: string;        // 씬 스토리 설명
  sceneOrder: number;         // 씬 순서
}
```

**노드 UI 스펙:**
```
┌─────────────────────────────────────┐
│  📖 씬 1: 고립된 아침               │   ← 왼쪽: 아이콘 + 씬번호 + 제목
├─────────────────────────────────────┤
│  화성 기지 아침. 창밖으로 붉은       │   ← 설명 텍스트 (최대 2줄, ellipsis)
│  사막이 펼쳐진다...                 │
├─────────────────────────────────────┤
│  [프로젝트 상세에서 편집]            │   ← 하단 링크 버튼 (호버 시 표시)
└─────────────────────────────────────┘

크기: 너비 280px, 높이 auto (min 100px)
색상: 배경 var(--rose-50), 보더 var(--rose-200), 제목 var(--gray-900)
      → #FFFAFC 배경, #FFE8F2 보더
```

**동작:**
- 클릭 시 사이드바에 읽기 전용 정보 표시
- [편집] 버튼 제공 → 인라인 편집 모드
- [프로젝트 상세로 이동] 링크 제공 (Story 탭)
- `[+]` 버튼 없음 (마스터 노드는 자동 생성)

---

### 3.4 마스터 이미지 노드 (MasterImageNode)

> 씬의 기준이 되는 와이드샷 이미지. 씬당 최대 3개, Active 1개.

```typescript
export interface MasterImageNodeData extends BaseNodeData {
  type: NodeType.MASTER_IMAGE;
  sceneId: string;
  
  // 마스터 고유
  isActive: boolean;          // Active Master 여부 (씬당 1개만 true)
  imageUrl: string | null;    // 생성된 이미지 URL
  thumbnailUrl: string | null;
  
  // 입력 파라미터
  prompt: string;             // AI 프롬프트 (확정된)
  style: string;              // 실사, 애니, 픽사 등
  timeOfDay: string;          // 아침, 낮, 저녁, 밤
  mood: string;               // 편안, 고독, 긴장 등
  objectIds: string[];        // 등장 오브젝트 IDs
}
```

**노드 UI 스펙:**
```
┌─────────────────────────────────────┐
│ ★ Active                           │   ← Active 뱃지 (Active일 때만)
├─────────────────────────────────────┤
│  🎬 마스터 이미지 v1                │   ← 아이콘 + 타입 + 버전
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │                                 │ │   ← 썸네일 (160x90, 16:9)
│ │        [이미지 썸네일]           │ │      없으면 placeholder
│ │                                 │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ 상태: ✅ 완료                       │   ← 상태 뱃지 (보더 색상으로 표현)
└─────────────────────────────────────┘
         │
         ⊕  ← 호버 시 하단 중앙에 [+] 원형 버튼 표시

크기: 너비 200px (통일), 높이 auto (min 180px)

Active 색상 (Rose 테마):
  - 보더: var(--rose-500) → #FF85A1 (진한 핑크)
  - 배경: var(--rose-100) → #FFF0F5
  - 그림자: var(--shadow-rose)

비Active 색상:
  - 보더: var(--rose-300) → #FFD9E8
  - 배경: var(--rose-50) → #FFFAFC
```

**상태별 보더 색상 (Rose 기본 + 상태 강조):**
| 상태 | 보더 색상 | 아이콘 | 추가 효과 |
|------|----------|--------|----------|
| idle | var(--gray-300) | ⏳ | - |
| pending | var(--warning) | 🔄 | - |
| running | var(--info) | ⏳ | 펄스 애니메이션 |
| succeeded | var(--rose-500) | ✅ | - |
| failed | var(--error) | ❌ | - |

> **idle 처리**: jobStatus가 null이고 promptStatus가 draft/generated인 경우 UI에서 `idle`로 표시합니다.

**동작:**
- 클릭 → 사이드바에 MasterImagePanel 표시
- **선택 시 피드백**: Rose 포커스 링 + 약간 확대 (scale 1.02)
- 호버 시 노드 하단 중앙에 `⊕` 원형 버튼 표시 → 클릭 시 StoryboardGrid 노드 생성
- Active 전환 → 사이드바에서 "Active로 설정" 버튼

---

### 3.5 스토리보드 그리드 노드 (StoryboardGridNode)

> 마스터 기반으로 여러 샷 타입을 그리드 형태로 생성한 이미지.

```typescript
export interface StoryboardGridNodeData extends BaseNodeData {
  type: NodeType.STORYBOARD_GRID;
  // parentNodeId = 상위 마스터 노드 ID (단일 기준)
  
  imageUrl: string | null;    // 그리드 이미지 URL
  thumbnailUrl: string | null;
  
  // 입력 파라미터
  prompt: string;
  layout: '2x2' | '2x3' | '3x3';   // 레이아웃
  shotTypes: string[];         // 선택된 샷 타입들
  compositionHint: string;     // 구도 힌트 (선택)
}
```

**노드 UI 스펙:**
```
┌─────────────────────────────────────┐
│  📐 그리드 v1                       │   ← 아이콘 + 타입 + 버전
│     2x3 레이아웃                    │   ← 레이아웃 정보
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │                                 │ │   ← 썸네일 (160x90)
│ │        [그리드 이미지]           │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ 상태: ✅ 완료                       │
└─────────────────────────────────────┘
         │
         ⊕  ← 호버 시 [+] 버튼

크기: 너비 200px (통일), 높이 auto
색상 (Rose 테마):
  - 보더: var(--rose-400) → #FFB3C6
  - 배경: var(--rose-50) → #FFFAFC
```

**동작:**
- 클릭 → 사이드바에 StoryboardGridPanel 표시
- `[+]` 버튼 클릭 → Shot 노드 생성 (그리드에서 칸 선택)

---

### 3.6 샷 노드 (ShotNode)

> 그리드에서 추출하여 AI로 고품질 재생성한 개별 샷 이미지.

```typescript
export interface ShotNodeData extends BaseNodeData {
  type: NodeType.SHOT;
  // parentNodeId = 상위 그리드 노드 ID (단일 기준)
  
  imageUrl: string | null;
  thumbnailUrl: string | null;
  
  // 입력 파라미터
  prompt: string;
  gridCellIndex: number;      // 그리드에서 선택한 칸 번호 (0-based)
  shotType: string;           // 와이드샷, 클로즈업 등
  expression: string;         // 표정/분위기 조정
  additionalDetail: string;   // 추가 디테일 (선택)
}
```

**노드 UI 스펙:**
```
┌───────────────────────────┐
│  📷 샷 A v1               │   ← 아이콘 + 샷 라벨 + 버전
│     클로즈업              │   ← 샷 타입
├───────────────────────────┤
│ ┌───────────────────────┐ │
│ │                       │ │   ← 썸네일 (120x120, 정사각형)
│ │    [이미지 썸네일]     │ │
│ │                       │ │
│ └───────────────────────┘ │
├───────────────────────────┤
│ ✅ 완료                   │
└───────────────────────────┘
         │
         ⊕  ← 호버 시 [+] 버튼

크기: 너비 200px (통일), 높이 auto
색상 (Rose 테마):
  - 보더: var(--rose-300) → #FFD9E8
  - 배경: white
```

**동작:**
- 클릭 → 사이드바에 ShotPanel 표시
- `[+]` 버튼 클릭 → Video 노드 생성

---

### 3.7 영상 노드 (VideoNode)

> 샷 이미지를 기반으로 AI 영상을 생성. **확정(Confirm)** 개념 적용.  
> 트랜지션 영상은 **시작+끝 샷**을 모두 지정하는 방식으로 지원.

```typescript
export interface VideoNodeData extends BaseNodeData {
  type: NodeType.VIDEO;
  startShotId: string;        // 시작 샷 노드 ID (parentNodeId와 동일)
  endShotId: string | null;   // 끝 샷 노드 ID (트랜지션용)
  
  videoUrl: string | null;
  thumbnailUrl: string | null;
  duration: number;           // 영상 길이 (초)
  
  // 확정 상태
  isConfirmed: boolean;       // 타임라인에 확정 여부
  
  // 입력 파라미터
  prompt: string;
  cameraMotion: 'zoomIn' | 'zoomOut' | 'panLeft' | 'panRight' | 'tiltUp' | 'tiltDown' | 'static';
  motionDescription: string;  // 모션 설명 (선택)
}
```

> `startShotId`는 `parentNodeId`와 동일하게 유지합니다 (중복 저장 방지 목적).  
> `isTransition`은 `endShotId !== null`로 파생합니다.

**노드 UI 스펙:**
```
┌───────────────────────────┐
│  🎥 영상 A v1             │
│     줌인 · 5초            │   ← 카메라 + 길이
├───────────────────────────┤
│ ┌───────────────────────┐ │
│ │     ▶               │ │   ← 썸네일 + 재생 아이콘
│ │    [영상 썸네일]      │ │      (호버 시 미리보기)
│ │                       │ │
│ └───────────────────────┘ │
├───────────────────────────┤
│ ☆ 타임라인에 확정        │   ← 호버 시 확정 버튼 표시
└───────────────────────────┘

크기: 너비 200px (통일), 높이 auto

기본 색상 (Rose 테마):
  - 보더: var(--rose-200) → #FFE8F2
  - 배경: white

확정 색상 (Success 강조 - 유일하게 녹색 사용):
  - 보더: var(--success) → #22C55E
  - 배경: var(--success-bg) → #F0FDF4
  - 하단 녹색 강조 바
```

**확정 UI 강조:**
```
확정된 노드:
┌───────────────────────────┐
│  🎥 영상 A v1          ★ │   ← 우상단 별 아이콘 (녹색)
│     줌인 · 5초            │
├───────────────────────────┤
│ ┌───────────────────────┐ │
│ │                       │ │
│ │                       │ │
│ └───────────────────────┘ │
├───────────────────────────┤
│ 📍 타임라인에 추가됨      │   ← 확정 상태 뱃지
│ ████████████████████████ │   ← 하단 녹색 강조 바
└───────────────────────────┘
```

**동작:**
- 클릭 → 사이드바에 VideoPanel 표시
- 호버 시 영상 미리보기 (썸네일 위에 재생)
- **확정 버튼**: 노드 호버 시 + 사이드바 (둘 다 지원)
- 영상 노드는 최종 결과물이므로 `[+]` 버튼 없음

---

## 4. 엣지(연결) 규칙

### 4.1 연결 가능 관계

```typescript
// types/node.ts
export const VALID_CONNECTIONS: Record<NodeType, NodeType[]> = {
  [NodeType.SCENE_HEADER]: [NodeType.MASTER_IMAGE],
  [NodeType.MASTER_IMAGE]: [NodeType.STORYBOARD_GRID],
  [NodeType.STORYBOARD_GRID]: [NodeType.SHOT],
  [NodeType.SHOT]: [NodeType.VIDEO],
  [NodeType.VIDEO]: [], // 최종 노드, 하위 연결 없음
};
```

### 4.2 엣지 생성 방식

> **P0 원칙**: 엣지는 `parentNodeId` 기반 트리 구조에서 **자동 파생**됩니다.  
> - 클라이언트는 노드 생성/삭제만 수행하고, 엣지는 파생/계산됩니다.  
> - 수동 드래그로 엣지 편집은 P1 이후 범위.

| 케이스 | 트리거 | 엣지 생성 |
|--------|--------|----------|
| **자식 노드 생성** | 부모 노드의 `[+]` 버튼 클릭 | 자동 연결 |
| **트랜지션 영상** | VideoPanel에서 end shot 선택 | 2개 엣지 (시작샷→영상, 끝샷→영상) |

### 4.3 엣지 스타일

```typescript
// types/node.ts
export interface CustomEdgeData {
  isTransition: boolean;  // 트랜지션 영상용 연결인지
}

// 기본 엣지 스타일 (Rose 테마)
const defaultEdgeStyle = {
  stroke: 'var(--rose-300)',  // #FFD9E8
  strokeWidth: 2,
  type: 'smoothstep',
};

// 확정된 영상으로 가는 엣지 (Success 강조)
const confirmedEdgeStyle = {
  stroke: 'var(--success)',  // #22C55E
  strokeWidth: 3,
};

// 확정 상태일 때 해당 영상으로 향하는 모든 엣지에 적용

// 트랜지션 엣지 (선택 강조)
const transitionEdgeStyle = {
  stroke: 'var(--rose-400)',
  strokeWidth: 2,
  strokeDasharray: '6 4',
};
```

### 4.4 엣지 핸들 위치

```
노드 핸들 배치:
┌─────────────────────┐
│     ● (source-top)  │   ← 상단 중앙 (부모로부터 연결받음)
│                     │
│       NODE          │
│                     │
│     ● (target-bottom)│   ← 하단 중앙 (자식으로 연결)
└─────────────────────┘

- 세로 흐름이므로 상단/하단 핸들 사용
- 트랜지션 영상: 좌우 핸들 추가 필요
```

---

## 5. 상태 관리 (Pinia Store)

### 5.1 Node Store

```typescript
// stores/nodeStore.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Node, Edge } from '@vue-flow/core';
import type { BaseNodeData, NodeType } from '@/types/node';
import { VALID_CONNECTIONS } from '@/types/node';

export const useNodeStore = defineStore('node', () => {
  // ==================== State ====================
  const nodes = ref<Node<BaseNodeData>[]>([]);
  const edges = ref<Edge[]>([]);
  const selectedNodeId = ref<string | null>(null);
  
  // end shot 선택 모드 (트랜지션 영상용)
  const selectionMode = ref<'none' | 'selectEndShot'>('none'); // UI 전용
  const endShotTargetVideoId = ref<string | null>(null);        // UI 전용
  const sceneId = ref<string | null>(null);
  
  // 로딩 상태
  const isLoading = ref(false);
  const isSaving = ref(false);
  
  // ==================== Getters ====================
  const selectedNode = computed(() => 
    nodes.value.find(n => n.id === selectedNodeId.value) || null
  );
  
  const nodesByType = computed(() => (type: NodeType) =>
    nodes.value.filter(n => n.data.type === type)
  );
  
  const activeMaster = computed(() =>
    nodes.value.find(n => 
      n.data.type === 'masterImage' && n.data.isActive
    ) || null
  );
  
  const confirmedVideos = computed(() =>
    nodes.value.filter(n => 
      n.data.type === 'video' && n.data.isConfirmed
    )
  );
  
  const childNodes = computed(() => (parentId: string) =>
    edges.value
      .filter(e => e.source === parentId)
      .map(e => nodes.value.find(n => n.id === e.target))
      .filter(Boolean)
  );
  
  // ==================== Actions ====================
  
  // 씬 데이터 로드
  async function loadSceneNodes(sceneIdParam: string) {
    isLoading.value = true;
    try {
      sceneId.value = sceneIdParam;
      // TODO: API 호출
      // const response = await api.get(`/scenes/${sceneIdParam}/nodes`);
      // nodes.value = response.data.nodes;
      // edges.value = response.data.edges ?? deriveEdges(nodes.value);
      
      // 씬 헤더 노드가 없으면 자동 생성
      ensureSceneHeaderNode();
      // 마스터 노드가 없으면 자동 생성
      ensureActiveMasterNode();
    } finally {
      isLoading.value = false;
    }
  }
  
  // 씬 헤더 노드 자동 생성
  function ensureSceneHeaderNode() {
    const hasHeader = nodes.value.some(n => n.data.type === 'sceneHeader');
    if (!hasHeader && sceneId.value) {
      addNode({
        type: 'sceneHeader',
        sceneId: sceneId.value,
        title: '씬 제목', // TODO: 실제 씬 정보로 대체
        description: '씬 설명',
      });
    }
  }
  
  // Active 마스터 노드 자동 생성
  function ensureActiveMasterNode() {
    const hasMaster = nodes.value.some(n => n.data.type === 'masterImage');
    if (!hasMaster) {
      const headerNode = nodes.value.find(n => n.data.type === 'sceneHeader');
      if (headerNode) {
        addNode({
          type: 'masterImage',
          parentNodeId: headerNode.id,
          isActive: true,
          version: 1,
        });
      }
    }
  }
  
  // 노드 추가 (제약 위반 시 null 반환)
  function addNode(data: Partial<BaseNodeData>) {
    const id = `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    
    // 제약 체크 (P0)
    if (data.type === 'sceneHeader') {
      const hasHeader = nodes.value.some(n => n.data.type === 'sceneHeader');
      if (hasHeader) return null;
    }
    if (data.type === 'masterImage') {
      const masterCount = nodes.value.filter(n => n.data.type === 'masterImage').length;
      if (masterCount >= 3) return null;
    }
    if (data.parentNodeId && !canConnect(data.parentNodeId, data.type as NodeType)) {
      return null;
    }
    
    const newNode: Node<BaseNodeData> = {
      id,
      type: data.type, // Vue Flow 커스텀 노드 타입
      position: { x: 0, y: 0 }, // 자동 레이아웃에서 계산
      data: {
        id,
        type: data.type,
        jobStatus: null,
        promptStatus: 'draft',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        versionGroupId: data.versionGroupId || id, // 최초 노드 id 권장
        version: data.version || 1,
        parentNodeId: data.parentNodeId || null,
        isCollapsed: false,
        childCount: 0,
        ...data,
      } as BaseNodeData,
    };
    
    nodes.value.push(newNode);
    
    // 부모가 있으면 엣지 자동 생성 (P0: 파생/계산 기준)
    if (data.parentNodeId) {
      addEdge(data.parentNodeId, id);
    }
    
    // 자동 레이아웃 트리거
    // triggerAutoLayout();
    
    return newNode;
  }
  
  // 노드 업데이트
  function updateNode(nodeId: string, updates: Partial<BaseNodeData>) {
    const nodeIndex = nodes.value.findIndex(n => n.id === nodeId);
    if (nodeIndex !== -1) {
      nodes.value[nodeIndex].data = {
        ...nodes.value[nodeIndex].data,
        ...updates,
        updatedAt: new Date().toISOString(),
      };
    }
  }
  
  // 노드 삭제 (하위 노드 함께 삭제)
  function deleteNode(nodeId: string) {
    // 하위 노드 찾기 (재귀)
    const descendants = getDescendantIds(nodeId);
    const toDelete = [nodeId, ...descendants];
    
    // 노드 삭제
    nodes.value = nodes.value.filter(n => !toDelete.includes(n.id));
    
    // 관련 엣지 삭제
    edges.value = edges.value.filter(
      e => !toDelete.includes(e.source) && !toDelete.includes(e.target)
    );
  }
  
  // 하위 노드 ID 재귀 수집
  function getDescendantIds(nodeId: string): string[] {
    const directChildren = edges.value
      .filter(e => e.source === nodeId)
      .map(e => e.target);
    
    return directChildren.flatMap(childId => [
      childId,
      ...getDescendantIds(childId),
    ]);
  }
  
  // 엣지 추가
  function addEdge(sourceId: string, targetId: string) {
    if (!canConnect(sourceId, nodes.value.find(n => n.id === targetId)?.data.type as NodeType)) {
      return;
    }
    const edgeId = `edge-${sourceId}-${targetId}`;
    if (!edges.value.some(e => e.id === edgeId)) {
      edges.value.push({
        id: edgeId,
        source: sourceId,
        target: targetId,
        type: 'smoothstep',
      });
    }
  }

  function deriveEdges(nodeList: Node<BaseNodeData>[]) {
    return nodeList
      .filter(n => n.data.parentNodeId)
      .map(n => ({
        id: `edge-${n.data.parentNodeId}-${n.id}`,
        source: n.data.parentNodeId as string,
        target: n.id,
        type: 'smoothstep',
      }));
  }
  
  function canConnect(sourceId: string, targetType: NodeType) {
    const sourceNode = nodes.value.find(n => n.id === sourceId);
    if (!sourceNode) return false;
    return VALID_CONNECTIONS[sourceNode.data.type]?.includes(targetType);
  }
  
  // 노드 선택
  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId;
  }
  
  // Active 마스터 전환
  function setActiveMaster(masterId: string) {
    nodes.value.forEach(n => {
      if (n.data.type === 'masterImage') {
        n.data.isActive = n.id === masterId;
      }
    });
  }
  
  // 영상 확정/해제
  function toggleVideoConfirm(videoId: string) {
    const node = nodes.value.find(n => n.id === videoId);
    if (node && node.data.type === 'video' && node.data.jobStatus === 'succeeded') {
      // 같은 부모(샷)의 다른 영상들 확정 해제
      const parentShotId = node.data.parentNodeId;
      nodes.value.forEach(n => {
        if (n.data.type === 'video' && n.data.parentNodeId === parentShotId) {
          n.data.isConfirmed = n.id === videoId ? !n.data.isConfirmed : false;
        }
      });
    }
  }
  
  // 브랜치 접기/펼치기
  function toggleCollapse(nodeId: string) {
    const node = nodes.value.find(n => n.id === nodeId);
    if (node) {
      node.data.isCollapsed = !node.data.isCollapsed;
      if (node.data.isCollapsed) {
        node.data.childCount = getDescendantIds(nodeId).length;
      }
      syncHiddenByCollapse();
    }
  }
  
  function syncHiddenByCollapse() {
    nodes.value.forEach(n => {
      n.hidden = hasCollapsedAncestor(n.id);
    });
  }
  
  function hasCollapsedAncestor(nodeId: string): boolean {
    const parentId = nodes.value.find(n => n.id === nodeId)?.data.parentNodeId;
    if (!parentId) return false;
    const parent = nodes.value.find(n => n.id === parentId);
    if (!parent) return false;
    return parent.data.isCollapsed || hasCollapsedAncestor(parentId);
  }
  
  // end shot 선택 모드
  function startSelectEndShot(videoId: string) {
    selectionMode.value = 'selectEndShot';
    endShotTargetVideoId.value = videoId;
  }
  
  function setEndShot(shotId: string) {
    if (selectionMode.value !== 'selectEndShot' || !endShotTargetVideoId.value) return;
    const videoNode = nodes.value.find(n => n.id === endShotTargetVideoId.value);
    if (videoNode?.data.endShotId) {
      edges.value = edges.value.filter(
        e => !(e.source === videoNode.data.endShotId && e.target === endShotTargetVideoId.value)
      );
    }
    updateNode(endShotTargetVideoId.value, { endShotId: shotId });
    // 엣지 추가 (트랜지션)
    addEdge(shotId, endShotTargetVideoId.value);
    selectionMode.value = 'none';
    endShotTargetVideoId.value = null;
  }
  
  return {
    // State
    nodes,
    edges,
    selectedNodeId,
    selectionMode,
    endShotTargetVideoId,
    sceneId,
    isLoading,
    isSaving,
    
    // Getters
    selectedNode,
    nodesByType,
    activeMaster,
    confirmedVideos,
    childNodes,
    
    // Actions
    loadSceneNodes,
    addNode,
    updateNode,
    deleteNode,
    addEdge,
    selectNode,
    startSelectEndShot,
    setEndShot,
    setActiveMaster,
    toggleVideoConfirm,
    toggleCollapse,
  };
});
```

**제약/검증 (P0)**
- 씬당 master 최대 3, Active master 1
- 연결 규칙(VALID_CONNECTIONS) 위반 시 노드 생성/엣지 연결 차단
- shot당 confirm 1개 (서버에서도 동일하게 강제)

**버전 정책**
- 재생성(regenerate)은 기존 노드를 덮어쓰지 않고 **동일 `versionGroupId`**에 새 노드를 추가
- 새 노드는 `version = 이전 + 1`, 최신 버전 자동 선택

---

## 6. 사이드바 패널 설계

### 6.1 패널 컨테이너

```vue
<!-- components/scene-editor/panels/NodePanelContainer.vue -->
<template>
  <aside 
    v-if="selectedNode" 
    class="node-panel"
  >
    <component 
      :is="panelComponent" 
      :node="selectedNode"
      @close="handleClose"
    />
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useNodeStore } from '@/stores/nodeStore';
import { panelRegistry } from './index';

const nodeStore = useNodeStore();
const selectedNode = computed(() => nodeStore.selectedNode);

const panelComponent = computed(() => 
  selectedNode.value 
    ? panelRegistry[selectedNode.value.data.type] 
    : null
);

function handleClose() {
  nodeStore.selectNode(null);
}
</script>

<style scoped>
.node-panel {
  width: 380px;
  height: 100%;
  background: white;
  border-left: 1px solid var(--rose-200);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}
</style>
```

### 6.2 패널 공통 구조 (Sticky Footer 패턴)

각 패널은 다음 섹션으로 구성 (생성 버튼은 하단 고정):

```
┌─────────────────────────────────────┐ ← 고정 헤더
│ [X] 🎬 마스터 이미지 생성            │
├─────────────────────────────────────┤
│ ▼ 기본 입력                        │  ← 펼쳐진 섹션 (Accordion)
│   🎨 등장 오브젝트: [✓ 민준] [  벨]  │
│   🎨 스타일: [● 실사] [○ 애니]      │
│   ☀️ 시간대: [● 아침] [○ 낮]        │
│   💭 분위기: [○ 편안] [● 고독]       │
├─────────────────────────────────────┤
│ ▶ 고급 옵션                         │  ← 접힌 섹션
├─────────────────────────────────────┤
│ ▼ 프롬프트                           │
│ ┌─────────────────────────────────┐ │
│ │ Wide shot of Mars base...      │ │
│ └─────────────────────────────────┘ │
│          [🔄 재생성] [✅ 승인]       │
├─────────────────────────────────────┤ ← Sticky Footer
│ [✨ 프롬프트 생성]  [🎨 이미지 생성]  │
│                     상태: ⏳ 승인대기 │
└─────────────────────────────────────┘

패널 CSS 구조:
- 헤더: 고정 (position: sticky, top: 0)
- 컨텐츠: 스크롤 가능 (flex: 1, overflow-y: auto)
- 푸터: 고정 (position: sticky, bottom: 0)
- 입력 필드: form-input, form-select 클래스 활용
- 버튼: btn, btn-primary, btn-secondary 클래스 활용
```

### 6.3 씬 헤더 패널 (SceneHeaderPanel)

> 씬 메타(제목/설명/순서)를 **읽기 전용으로 보여주고**, [편집] 클릭 시 **인라인 편집 모드**로 전환합니다.  
> 저장은 **Scene API**로 처리 (Node API 아님).

구성:
- 제목/설명/씬 순서 표시
- 등장 오브젝트 요약 (선택)
- [편집] 버튼 → 인라인 편집 모드
- [프로젝트 상세로 이동] 링크 (Story 탭)

---

### 6.4 영상 패널 (VideoPanel) 상세

```vue
<!-- components/scene-editor/panels/VideoPanel.vue -->
<template>
  <div class="video-panel">
    <!-- 헤더 -->
    <header class="panel-header">
      <button @click="$emit('close')" class="close-btn">×</button>
      <h3>🎥 영상 생성</h3>
    </header>
    
    <!-- 참조 샷 -->
    <section class="panel-section">
      <label>📸 시작 샷</label>
      <div class="shot-preview">
        <img :src="startShot?.thumbnailUrl" alt="시작 샷" />
        <button @click="changeStartShot">변경</button>
      </div>
      
      <!-- 트랜지션 -->
      <div class="toggle-row">
        <label>🔁 트랜지션 영상</label>
        <input type="checkbox" v-model="form.isTransition" />
      </div>
      
      <label v-if="form.isTransition">📸 끝 샷</label>
      <div v-if="form.isTransition" class="shot-preview">
        <!-- 데스크탑: 캔버스에서 선택 모드 -->
        <button @click="startSelectEndShot">캔버스에서 선택</button>
        <!-- 모바일 대체: 리스트 선택 -->
        <button @click="openShotList">샷 리스트에서 선택</button>
      </div>
    </section>
    
    <!-- 카메라 모션 -->
    <section class="panel-section">
      <label>🎥 카메라 움직임</label>
      <div class="camera-options">
        <button 
          v-for="option in cameraOptions" 
          :key="option.value"
          :class="{ active: form.cameraMotion === option.value }"
          @click="form.cameraMotion = option.value"
        >
          <img :src="option.gifUrl" :alt="option.label" />
          <span>{{ option.label }}</span>
        </button>
      </div>
    </section>
    
    <!-- 길이 -->
    <section class="panel-section">
      <label>⏱️ 길이</label>
      <select v-model="form.duration">
        <option :value="3">3초</option>
        <option :value="5">5초</option>
        <option :value="8">8초</option>
        <option :value="10">10초</option>
      </select>
    </section>
    
    <!-- 모션 설명 -->
    <section class="panel-section">
      <label>🎭 모션 설명 (선택)</label>
      <textarea 
        v-model="form.motionDescription" 
        placeholder="예: 우주인이 창밖을 바라보다 고개를 돌린다"
      />
    </section>
    
    <!-- 프롬프트 생성 -->
    <button @click="generatePrompt" class="btn-primary">
      ✨ 프롬프트 생성
    </button>
    
    <!-- 생성된 프롬프트 -->
    <section v-if="generatedPrompt" class="panel-section">
      <label>📝 AI 프롬프트 (검토/수정 가능)</label>
      <textarea v-model="form.prompt" rows="4" />
      <div class="prompt-actions">
        <button @click="regeneratePrompt">🔄 재생성</button>
        <button @click="approvePrompt" class="btn-success">✅ 승인</button>
      </div>
    </section>
    
    <!-- 영상 생성 -->
    <button 
      @click="generateVideo" 
      :disabled="!isPromptApproved || (form.isTransition && !form.endShotId)"
      class="btn-primary btn-large"
    >
      🎬 영상 생성
    </button>
    <div class="status">상태: {{ statusText }}</div>
    
    <!-- 확정 버튼 (완료 상태에서만) -->
    <button 
      v-if="node.data.jobStatus === 'succeeded'"
      @click="toggleConfirm"
      :class="['btn-confirm', { confirmed: node.data.isConfirmed }]"
    >
      {{ node.data.isConfirmed ? '★ 확정됨 (취소)' : '☆ 타임라인에 확정' }}
    </button>
  </div>
</template>
```

**트랜지션 제약**
- `form.isTransition = true`인 경우 endShotId 선택 필수
- endShot이 미선택이면 "영상 생성" 버튼 비활성화

---

## 7. 주요 인터랙션 시나리오

### 7.1 씬 진입 시 초기화

```
1. SceneEditPage 마운트
2. nodeStore.loadSceneNodes(sceneId) 호출
3. 씬 헤더 노드 자동 생성 (없으면)
4. Active 마스터 노드 자동 생성 (없으면)
5. Dagre 자동 레이아웃 1회 적용 (이후 수동 버튼)
6. 캔버스 렌더링
```

### 7.2 노드 생성 플로우 (마스터 → 그리드)

```
1. 마스터 노드의 [+] 버튼 클릭
2. nodeStore.addNode({ type: 'storyboardGrid', parentNodeId: masterId })
3. 새 그리드 노드 생성 + 엣지 자동 연결
4. 새 노드는 부모 기준 오프셋 배치 (필요 시 자동 레이아웃 버튼)
5. 새 노드 자동 선택 → 사이드바 StoryboardGridPanel 열림
6. 사이드바에서 입력 후 [프롬프트 생성] → [승인] → [그리드 생성]
7. AI 생성 완료 시 노드 상태 업데이트 (succeeded)
```

### 7.3 트랜지션 영상 (end shot) 선택 플로우

```
1. VideoPanel에서 [트랜지션 영상] 토글 ON
2. 데스크탑: [캔버스에서 선택] 클릭 → end shot 선택 모드 진입
   모바일: [샷 리스트에서 선택] 클릭 → 썸네일 리스트에서 선택
3. 샷 노드 1개 클릭/선택
4. endShotId 설정 + 엣지 2개 생성 (시작샷→영상, 끝샷→영상)
5. 선택 모드 종료
```

### 7.4 영상 확정 플로우

```
1. 영상 노드 클릭 → VideoPanel 열림
2. [타임라인에 확정] 버튼 클릭
   - jobStatus가 succeeded일 때만 활성화
3. nodeStore.toggleVideoConfirm(videoId) 호출
4. 같은 부모 샷의 다른 영상들은 확정 해제
5. 노드 UI 업데이트 (녹색 보더 + ★ 아이콘)
6. 엣지 스타일 업데이트 (녹색)
```

### 7.5 브랜치 접기/펼치기

```
1. Non-Active 마스터 노드의 [◀ 접기] 버튼 클릭
2. nodeStore.toggleCollapse(masterId)
3. 해당 마스터의 모든 하위 노드 hidden = true
4. 마스터 노드에 "3개 노드 접힘" 표시
5. [▶ 펼치기] 버튼으로 복원
```

> 숨김 여부는 "접힌 조상 노드 존재 여부"로 계산하여 중첩 접기에도 일관되게 동작합니다.

### 7.6 Active 마스터 전환

```
1. Non-Active 마스터 노드 클릭 → 사이드바 열림
2. [Active로 설정] 버튼 클릭
3. nodeStore.setActiveMaster(masterId)
4. 기존 Active 마스터 → Non-Active로 전환
5. 새 Active 마스터 → Rose 강조 보더 + ★ 뱃지
6. (선택) 기존 Active 브랜치 자동 접기
```

---

## 8. 레이아웃 및 스타일링

### 8.1 자동 레이아웃 (Dagre)

```typescript
// composables/useAutoLayout.ts
import dagre from 'dagre';
import type { Node, Edge } from '@vue-flow/core';

export function useAutoLayout() {
  // 원칙: 최초 로드 1회 + 버튼 클릭 시에만 자동 레이아웃
  // 이후에는 사용자가 드래그한 위치를 저장/유지
  function getLayoutedElements(
    nodes: Node[], 
    edges: Edge[],
    direction: 'TB' | 'LR' = 'TB'
  ) {
    const dagreGraph = new dagre.graphlib.Graph();
    dagreGraph.setDefaultEdgeLabel(() => ({}));
    dagreGraph.setGraph({ 
      rankdir: direction,
      nodesep: 50,    // 노드 간 가로 간격
      ranksep: 80,    // 노드 간 세로 간격
      marginx: 20,
      marginy: 20,
    });

    // 노드 추가
    nodes.forEach((node) => {
      dagreGraph.setNode(node.id, { 
        width: getNodeWidth(node.data.type), 
        height: getNodeHeight(node.data.type),
      });
    });

    // 엣지 추가
    edges.forEach((edge) => {
      dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    // 계산된 위치 적용
    const layoutedNodes = nodes.map((node) => {
      const nodeWithPosition = dagreGraph.node(node.id);
      return {
        ...node,
        position: {
          x: nodeWithPosition.x - getNodeWidth(node.data.type) / 2,
          y: nodeWithPosition.y - getNodeHeight(node.data.type) / 2,
        },
      };
    });

    return { nodes: layoutedNodes, edges };
  }

  function getNodeWidth(type: string): number {
    const widths = {
      sceneHeader: 280,
      masterImage: 200,
      storyboardGrid: 200,
      shot: 200,
      video: 200,
    };
    return widths[type] || 200;
  }

  function getNodeHeight(type: string): number {
    const heights = {
      sceneHeader: 120,
      masterImage: 200,
      storyboardGrid: 180,
      shot: 180,
      video: 180,
    };
    return heights[type] || 150;
  }

  return { getLayoutedElements };
}
```

### 8.2 CSS 변수 (Rose 테마 통합)

```css
/* assets/styles/node-canvas.css */
/* 기존 variables.css의 Rose 변수를 활용합니다 */

/* 노드 타입별 색상 (Rose 계열 농도로 계층 표현) */
.node {
  border-radius: var(--radius-lg);  /* 1rem */
  border-width: 2px;
  transition: all var(--transition-normal);
}

/* 씬 헤더 - 가장 연함 */
.node--scene-header {
  background: var(--rose-50);       /* #FFFAFC */
  border-color: var(--rose-200);    /* #FFE8F2 */
}

/* 마스터 Active - 가장 진함 */
.node--master.active {
  background: var(--rose-100);      /* #FFF0F5 */
  border-color: var(--rose-500);    /* #FF85A1 */
  box-shadow: var(--shadow-rose);
}

/* 마스터 Non-Active */
.node--master:not(.active) {
  background: var(--rose-50);
  border-color: var(--rose-300);    /* #FFD9E8 */
}

/* 그리드 */
.node--grid {
  background: var(--rose-50);
  border-color: var(--rose-400);    /* #FFB3C6 */
}

/* 샷 */
.node--shot {
  background: white;
  border-color: var(--rose-300);    /* #FFD9E8 */
}

/* 영상 기본 */
.node--video {
  background: white;
  border-color: var(--rose-200);    /* #FFE8F2 */
}

/* 영상 확정 - 유일하게 Success 색상 사용 */
.node--video.confirmed {
  background: var(--success-bg);    /* #F0FDF4 */
  border-color: var(--success);     /* #22C55E */
}

/* 상태별 보더 색상 (노드 타입보다 우선) */
.node--idle { border-color: var(--gray-300); }
.node--pending { border-color: var(--warning); }
.node--running { 
  border-color: var(--info); 
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
.node--succeeded { border-color: var(--rose-500); }
.node--failed { border-color: var(--error); }

/* 선택 상태 */
.node--selected {
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.4);  /* Rose 포커스 링 */
  transform: scale(1.02);
  z-index: 10;
}

/* [+] 버튼 (호버 시 표시) */
.node__add-btn {
  position: absolute;
  bottom: -24px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--rose-500);
  color: white;
  border: 2px solid white;
  box-shadow: var(--shadow-md);
  opacity: 0;
  transition: opacity var(--transition-fast);
  cursor: pointer;
}

.node:hover .node__add-btn {
  opacity: 1;
}

/* 모바일/키보드 접근성: 선택 시에도 노출 */
.node--selected .node__add-btn {
  opacity: 1;
}

/* 확정 버튼 */
.node__confirm-bar {
  height: 4px;
  background: var(--success);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}
```

---

## 9. 구현 단계

### Phase 1: 기본 캔버스 설정 (1-2일)

- [ ] Vue Flow 패키지 설치 및 설정
- [ ] `NodeCanvas.vue` 기본 구조 생성
- [ ] `nodeStore.ts` 기본 상태 관리 구현
- [ ] `useAutoLayout.ts` Dagre 레이아웃 설정

### Phase 2: 커스텀 노드 컴포넌트 (2-3일)

- [ ] `SceneHeaderNode.vue` 구현
- [ ] `MasterImageNode.vue` 구현
- [ ] `StoryboardGridNode.vue` 구현
- [ ] `ShotNode.vue` 구현
- [ ] `VideoNode.vue` 구현
- [ ] 노드 상태별 UI (pending, running, succeeded, failed / idle은 jobStatus null + promptStatus 기반)

### Phase 3: 사이드바 패널 (2-3일)

- [ ] `NodePanelContainer.vue` 라우팅 로직
- [ ] `SceneHeaderPanel.vue` (읽기 + 편집 진입)
- [ ] `BasePanel.vue` 공통 레이아웃
- [ ] `MasterImagePanel.vue` 폼 및 프롬프트 생성
- [ ] `StoryboardGridPanel.vue` 폼 구현
- [ ] `ShotPanel.vue` 폼 구현
- [ ] `VideoPanel.vue` 폼 및 확정 기능

### Phase 4: 노드 액션 및 연결 (1-2일)

- [ ] `[+]` 버튼 → 자식 노드 생성 + 자동 연결
- [ ] 노드 삭제 (하위 노드 연쇄 삭제)
- [ ] Active 마스터 전환
- [ ] 영상 확정/해제
- [ ] 트랜지션 영상(end shot) 선택 모드 + 엣지 2개 생성

### Phase 5: 고급 기능 (1-2일)

- [ ] 브랜치 접기/펼치기
- [ ] 재생성 (버전 노드 생성)
- [ ] 자동 레이아웃 버튼
- [ ] 미니맵 (선택)

### Phase 6: API 연동 (병렬 진행)

- [ ] 씬 노드 조회 API 연동
- [ ] AI 프롬프트 생성 API 연동
- [ ] AI 이미지/영상 생성 API 연동
- [ ] WebSocket 상태 업데이트 연동

---

## 10. 파일 구조

```
itda-frontend/src/
├── components/
│   └── scene-editor/
│       ├── NodeCanvas.vue
│       ├── nodes/
│       │   ├── index.ts               # 노드 컴포넌트 export
│       │   ├── BaseNode.vue           # 공통 노드 래퍼
│       │   ├── SceneHeaderNode.vue
│       │   ├── MasterImageNode.vue
│       │   ├── StoryboardGridNode.vue
│       │   ├── ShotNode.vue
│       │   └── VideoNode.vue
│       ├── panels/
│       │   ├── NodePanelContainer.vue
│       │   ├── index.ts
│       │   ├── BasePanel.vue
│       │   ├── SceneHeaderPanel.vue
│       │   ├── MasterImagePanel.vue
│       │   ├── StoryboardGridPanel.vue
│       │   ├── ShotPanel.vue
│       │   └── VideoPanel.vue
│       └── controls/
│           └── CanvasControls.vue
│
├── composables/
│   ├── useNodeCanvas.ts
│   ├── useAutoLayout.ts
│   └── useNodeActions.ts
│
├── stores/
│   └── nodeStore.ts
│
├── types/
│   └── node.ts
│
└── assets/
    └── styles/
        └── node-canvas.css
```

---

## 부록: 노드 타입 참조 테이블 (Rose 테마)

| 타입 | 아이콘 | 보더 색상 | 배경 색상 | 너비 | `[+]` 버튼 | 확정 가능 |
|------|--------|----------|----------|------|-----------|----------|
| sceneHeader | 📖 | rose-200 (#FFE8F2) | rose-50 (#FFFAFC) | 280px | ❌ | ❌ |
| masterImage (Active) | 🎬 | rose-500 (#FF85A1) | rose-100 (#FFF0F5) | 200px | ✅ | ❌ |
| masterImage (Non-Active) | 🎬 | rose-300 (#FFD9E8) | rose-50 (#FFFAFC) | 200px | ✅ | ❌ |
| storyboardGrid | 📐 | rose-400 (#FFB3C6) | rose-50 (#FFFAFC) | 200px | ✅ | ❌ |
| shot | 📷 | rose-300 (#FFD9E8) | white | 200px | ✅ | ❌ |
| video | 🎥 | rose-200 (#FFE8F2) | white | 200px | ❌ | ✅ |
| video (확정) | 🎥 | success (#22C55E) | success-bg (#F0FDF4) | 200px | ❌ | ✅ |

---

> 📝 **Note**: 이 문서는 구현 진행에 따라 업데이트될 수 있습니다.
