# Phase 4: 씬 에디터 구현 계획서

> **버전**: 1.0 | **작성일**: 2026-01-17  
> **목표**: `scene-edit.html` 디자인을 Vue 컴포넌트로 변환 (정적 MVP)

---

## 1. 개요

### 목표
- 노드 기반 씬 에디터 UI 구현 (Master → Grid → Shot → Video 흐름)
- 노드 선택 시 속성 패널 표시
- 미니 타임라인에 확정된 영상 표시
- **정적 UI 우선**: Vue Flow, 드래그 앤 드롭은 이후 고도화

### 레퍼런스
- HTML: `ams-v2_5-rose/pages/scene-edit.html`
- CSS: `ams-v2_5-rose/assets/css/components/nodes.css`

---

## 2. 파일 목록

### 새로 생성 (15개)

| # | 경로 | 설명 |
|---|------|------|
| 1 | `src/services/mock/nodes.ts` | 노드 Mock 데이터 + API 함수 |
| 2 | `src/stores/node.ts` | 노드 상태 관리 (Pinia) |
| 3 | `src/composables/useZoom.ts` | 줌 기능 컴포저블 |
| 4 | `src/layouts/EditorLayout.vue` | 에디터 전용 3컬럼 레이아웃 |
| 5 | `src/components/editor/EditorHeader.vue` | 헤더 (브레드크럼, 줌 레벨) |
| 6 | `src/components/editor/EditorSidebar.vue` | 왼쪽 사이드바 |
| 7 | `src/components/editor/NodeCanvas.vue` | 메인 캔버스 컨테이너 |
| 8 | `src/components/editor/NodeConnections.vue` | SVG 연결선 |
| 9 | `src/components/editor/nodes/MasterNode.vue` | 마스터 노드 |
| 10 | `src/components/editor/nodes/GridNode.vue` | 그리드 노드 |
| 11 | `src/components/editor/nodes/ShotNode.vue` | 샷 노드 |
| 12 | `src/components/editor/nodes/VideoNode.vue` | 비디오 노드 |
| 13 | `src/components/editor/PropertyPanel.vue` | 속성 패널 |
| 14 | `src/components/editor/MiniTimeline.vue` | 미니 타임라인 |
| 15 | `src/components/editor/ZoomControls.vue` | 줌 컨트롤 |

### 수정 (2개)

| 경로 | 변경 내용 |
|------|----------|
| `src/pages/SceneEditPage.vue` | 플레이스홀더 → 전체 에디터 |
| `src/types/index.ts` | TimelineClip, GridCell 타입 추가 |

---

## 3. 타입 정의

### `src/types/index.ts`에 추가

```typescript
// ================================
// Editor Types (Phase 4)
// ================================

export interface TimelineClip {
  clipId: string
  nodeId: number
  thumbnailUrl: string
  duration: number
  order: number
  label?: string
}

export interface GridCell {
  index: number
  imageUrl?: string
  selected: boolean
}

export type CameraMotion = 'ZOOM_IN' | 'ZOOM_OUT' | 'PAN_LEFT' | 'PAN_RIGHT' | 'TILT_UP' | 'TILT_DOWN' | 'STATIC'

// 기존 NodeSettings 확장 (이미 정의된 경우 병합)
export interface NodeSettings {
  style?: string
  ratio?: string
  cameraMotion?: CameraMotion
  duration?: number
  startShotNodeId?: number
  endShotNodeId?: number
  motionDescription?: string
  provider?: string
  gridLayout?: '2x2' | '2x3' | '3x3'
  gridCells?: GridCell[]
}
```

---

## 4. Mock 데이터

### `src/services/mock/nodes.ts`

```typescript
import type { Node, NodeType, NodeStatus, NodeSettings } from '../../types'

// 프로젝트별, 씬별 노드 데이터
const mockNodesData: Record<string, Node[]> = {
  '1-1': [ // projectId-sceneId
    {
      nodeId: 1,
      type: 'MASTER',
      parentNodeId: undefined,
      prompt: 'Wide shot of desolate red desert with ancient metallic structure...',
      status: 'SUCCEEDED',
      contentUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=600&auto=format',
      thumbnailUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=200&auto=format',
      position: { x: 0, y: 0 },
      isActive: true,
      title: 'Master Image',
    },
    {
      nodeId: 2,
      type: 'GRID',
      parentNodeId: 1,
      prompt: '2x3 storyboard grid based on Mars base scene...',
      status: 'SUCCEEDED',
      contentUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=600&auto=format',
      thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
      position: { x: 0, y: 200 },
      title: 'Grid (2x3)',
      settings: {
        gridLayout: '2x3',
        gridCells: [
          { index: 0, imageUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format', selected: true },
          { index: 1, imageUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&auto=format', selected: false },
          { index: 2, imageUrl: 'https://images.unsplash.com/photo-1516663713099-37eb6d60c825?w=200&auto=format', selected: false },
          { index: 3, imageUrl: 'https://images.unsplash.com/photo-1442544213729-6a890436906a?w=200&auto=format', selected: false },
          { index: 4, imageUrl: 'https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=200&auto=format', selected: false },
          { index: 5, imageUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&auto=format', selected: false },
        ],
      },
    },
    {
      nodeId: 3,
      type: 'SHOT',
      parentNodeId: 2,
      prompt: 'Cinematic shot of a vast red desert on Mars...',
      status: 'SUCCEEDED',
      contentUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=600&auto=format',
      thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
      position: { x: 0, y: 400 },
      title: 'Shot #1',
    },
    {
      nodeId: 4,
      type: 'VIDEO',
      parentNodeId: 3,
      prompt: 'Slow zoom in on astronaut looking out window...',
      status: 'SUCCEEDED',
      contentUrl: 'https://example.com/video.mp4',
      thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
      position: { x: 0, y: 600 },
      title: 'Video #1-A',
      isConfirmed: true,
      settings: {
        cameraMotion: 'ZOOM_IN',
        duration: 4,
        provider: 'Veo 3.1',
      },
    },
  ],
}

// Delay helper
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// API Functions
export async function fetchNodesBySceneId(projectId: number, sceneId: number): Promise<Node[]> {
  await delay(300)
  const key = `${projectId}-${sceneId}`
  return mockNodesData[key] || []
}

export async function updateNodeStatus(
  projectId: number,
  sceneId: number,
  nodeId: number,
  status: NodeStatus
): Promise<Node | null> {
  await delay(200)
  const key = `${projectId}-${sceneId}`
  const nodes = mockNodesData[key]
  if (!nodes) return null

  const node = nodes.find(n => n.nodeId === nodeId)
  if (node) {
    node.status = status
    return node
  }
  return null
}

export async function updateNodeSettings(
  projectId: number,
  sceneId: number,
  nodeId: number,
  settings: Partial<NodeSettings>
): Promise<Node | null> {
  await delay(200)
  const key = `${projectId}-${sceneId}`
  const nodes = mockNodesData[key]
  if (!nodes) return null

  const node = nodes.find(n => n.nodeId === nodeId)
  if (node) {
    node.settings = { ...node.settings, ...settings }
    return node
  }
  return null
}

export async function confirmVideoToTimeline(
  projectId: number,
  sceneId: number,
  nodeId: number
): Promise<boolean> {
  await delay(200)
  const key = `${projectId}-${sceneId}`
  const nodes = mockNodesData[key]
  if (!nodes) return false

  const node = nodes.find(n => n.nodeId === nodeId && n.type === 'VIDEO')
  if (node) {
    node.isConfirmed = true
    return true
  }
  return false
}

export async function unconfirmVideoFromTimeline(
  projectId: number,
  sceneId: number,
  nodeId: number
): Promise<boolean> {
  await delay(200)
  const key = `${projectId}-${sceneId}`
  const nodes = mockNodesData[key]
  if (!nodes) return false

  const node = nodes.find(n => n.nodeId === nodeId && n.type === 'VIDEO')
  if (node) {
    node.isConfirmed = false
    return true
  }
  return false
}
```

---

## 5. Pinia 스토어

### `src/stores/node.ts`

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Node, NodeSettings, TimelineClip } from '../types'
import {
  fetchNodesBySceneId as mockFetchNodes,
  updateNodeSettings as mockUpdateSettings,
  confirmVideoToTimeline as mockConfirmVideo,
  unconfirmVideoFromTimeline as mockUnconfirmVideo,
} from '../services/mock/nodes'

export const useNodeStore = defineStore('node', () => {
  // State
  const nodes = ref<Node[]>([])
  const selectedNodeId = ref<number | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const currentProjectId = ref<number | null>(null)
  const currentSceneId = ref<number | null>(null)

  // Getters
  const selectedNode = computed(() =>
    selectedNodeId.value
      ? nodes.value.find(n => n.nodeId === selectedNodeId.value) || null
      : null
  )

  const masterNodes = computed(() =>
    nodes.value.filter(n => n.type === 'MASTER')
  )

  const confirmedVideos = computed(() =>
    nodes.value.filter(n => n.type === 'VIDEO' && n.isConfirmed)
  )

  const timelineClips = computed<TimelineClip[]>(() =>
    confirmedVideos.value.map((node, index) => ({
      clipId: `clip-${node.nodeId}`,
      nodeId: node.nodeId,
      thumbnailUrl: node.thumbnailUrl || '',
      duration: node.settings?.duration || 4,
      order: index + 1,
      label: node.title,
    }))
  )

  const totalDuration = computed(() =>
    timelineClips.value.reduce((sum, clip) => sum + clip.duration, 0)
  )

  // Actions
  async function loadNodes(projectId: number, sceneId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    currentProjectId.value = projectId
    currentSceneId.value = sceneId

    try {
      nodes.value = await mockFetchNodes(projectId, sceneId)
    } catch (e) {
      error.value = 'Failed to load nodes'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  function selectNode(nodeId: number | null): void {
    selectedNodeId.value = nodeId
  }

  async function updateSettings(
    nodeId: number,
    settings: Partial<NodeSettings>
  ): Promise<boolean> {
    if (!currentProjectId.value || !currentSceneId.value) return false

    try {
      const updated = await mockUpdateSettings(
        currentProjectId.value,
        currentSceneId.value,
        nodeId,
        settings
      )
      if (updated) {
        const index = nodes.value.findIndex(n => n.nodeId === nodeId)
        if (index !== -1) {
          nodes.value[index] = updated
        }
        return true
      }
      return false
    } catch (e) {
      console.error(e)
      return false
    }
  }

  async function confirmVideo(nodeId: number): Promise<boolean> {
    if (!currentProjectId.value || !currentSceneId.value) return false

    try {
      const success = await mockConfirmVideo(
        currentProjectId.value,
        currentSceneId.value,
        nodeId
      )
      if (success) {
        const node = nodes.value.find(n => n.nodeId === nodeId)
        if (node) node.isConfirmed = true
      }
      return success
    } catch (e) {
      console.error(e)
      return false
    }
  }

  async function unconfirmVideo(nodeId: number): Promise<boolean> {
    if (!currentProjectId.value || !currentSceneId.value) return false

    try {
      const success = await mockUnconfirmVideo(
        currentProjectId.value,
        currentSceneId.value,
        nodeId
      )
      if (success) {
        const node = nodes.value.find(n => n.nodeId === nodeId)
        if (node) node.isConfirmed = false
      }
      return success
    } catch (e) {
      console.error(e)
      return false
    }
  }

  function clearNodes(): void {
    nodes.value = []
    selectedNodeId.value = null
    currentProjectId.value = null
    currentSceneId.value = null
    error.value = null
  }

  return {
    // State
    nodes,
    selectedNodeId,
    isLoading,
    error,
    currentProjectId,
    currentSceneId,
    // Getters
    selectedNode,
    masterNodes,
    confirmedVideos,
    timelineClips,
    totalDuration,
    // Actions
    loadNodes,
    selectNode,
    updateSettings,
    confirmVideo,
    unconfirmVideo,
    clearNodes,
  }
})
```

---

## 6. Composables

### `src/composables/useZoom.ts`

```typescript
import { ref, computed } from 'vue'

export function useZoom(initialZoom = 100, minZoom = 50, maxZoom = 200) {
  const zoomLevel = ref(initialZoom)

  const zoomPercentage = computed(() => `${zoomLevel.value}%`)

  const zoomScale = computed(() => zoomLevel.value / 100)

  function zoomIn(step = 10): void {
    zoomLevel.value = Math.min(maxZoom, zoomLevel.value + step)
  }

  function zoomOut(step = 10): void {
    zoomLevel.value = Math.max(minZoom, zoomLevel.value - step)
  }

  function zoomFit(): void {
    zoomLevel.value = 100
  }

  function setZoom(level: number): void {
    zoomLevel.value = Math.max(minZoom, Math.min(maxZoom, level))
  }

  return {
    zoomLevel,
    zoomPercentage,
    zoomScale,
    zoomIn,
    zoomOut,
    zoomFit,
    setZoom,
  }
}
```

---

## 7. 레이아웃

### `src/layouts/EditorLayout.vue`

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useUIStore } from '../stores/ui'
import Badge from '../components/common/Badge.vue'
import {
  ArrowLeft,
  BookOpen,
  Clapperboard,
  Layers,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next'

interface Props {
  projectTitle?: string
  sceneTitle?: string
  sceneBadge?: string
}

const props = withDefaults(defineProps<Props>(), {
  projectTitle: 'Project',
  sceneTitle: 'Scene',
  sceneBadge: 'Scene Editor',
})

const route = useRoute()
const uiStore = useUIStore()

const projectId = computed(() => Number(route.params.projectId))

const navItems = computed(() => [
  { key: 'story', icon: BookOpen, label: 'Story', to: { name: 'project-detail', params: { id: projectId.value } } },
  { key: 'scene-editor', icon: Clapperboard, label: 'Scene Editor', to: null, active: true },
  { key: 'timeline', icon: Layers, label: 'Timeline', to: { name: 'timeline', params: { id: projectId.value } } },
])

const sidebarClasses = computed(() => [
  'sidebar',
  { 'sidebar-collapsed': !uiStore.sidebarExpanded },
])
</script>

<template>
  <div class="app-container editor-app">
    <!-- Sidebar -->
    <aside :class="sidebarClasses">
      <div class="sidebar-section border-bottom">
        <RouterLink
          :to="{ name: 'project-detail', params: { id: projectId } }"
          class="nav-item"
          data-tooltip="Back to Project"
        >
          <ArrowLeft class="nav-icon" />
          <span class="nav-label">Back to Project</span>
        </RouterLink>
      </div>

      <div class="sidebar-section border-bottom sidebar-text">
        <h2 class="project-title">{{ projectTitle }}</h2>
        <Badge variant="rose">{{ sceneBadge }}</Badge>
      </div>

      <nav class="sidebar-nav">
        <template v-for="item in navItems" :key="item.key">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="nav-item"
            :data-tooltip="item.label"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
          </RouterLink>
          <button
            v-else
            :class="['nav-item', { active: item.active }]"
            :data-tooltip="item.label"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <div class="sidebar-section border-top">
        <div class="sidebar-text text-xs text-muted">{{ sceneTitle }}</div>
        <button class="sidebar-toggle" @click="uiStore.toggleSidebar">
          <ChevronLeft v-if="uiStore.sidebarExpanded" class="toggle-icon" />
          <ChevronRight v-else class="toggle-icon" />
        </button>
      </div>
    </aside>

    <!-- Main Content - 3 Column Layout -->
    <main class="editor-main">
      <!-- Header -->
      <slot name="header" />

      <!-- Editor Content -->
      <div class="editor-content">
        <!-- Canvas Area -->
        <div class="editor-canvas-area">
          <slot name="canvas" />
        </div>

        <!-- Right Sidebar (Properties) -->
        <aside class="editor-properties">
          <slot name="properties" />
        </aside>
      </div>

      <!-- Bottom Bar (Mini Timeline) -->
      <slot name="bottom" />
    </main>
  </div>
</template>

<style scoped>
.editor-app {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Sidebar - 기존 ProjectLayout 스타일 재사용 */
.sidebar {
  width: 260px;
  height: 100vh;
  background: white;
  border-right: 1px solid var(--rose-100);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 72px;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label {
  display: none;
}

.sidebar-section {
  padding: 1rem;
  position: relative;
}

.border-bottom {
  border-bottom: 1px solid var(--rose-100);
}

.border-top {
  border-top: 1px solid var(--rose-100);
  margin-top: auto;
}

.project-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.25rem;
}

.sidebar-nav {
  flex: 1;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 8px;
  color: var(--gray-600);
  text-decoration: none;
  background: transparent;
  border: none;
  width: 100%;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: var(--rose-50);
  color: var(--gray-900);
}

.nav-item.active {
  background: var(--rose-100);
  color: var(--rose-600);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.sidebar-toggle {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  z-index: 10;
}

.toggle-icon {
  width: 14px;
  height: 14px;
}

/* Editor Main */
.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-canvas-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: var(--rose-canvas);
}

.editor-properties {
  width: 400px;
  background: white;
  border-left: 1px solid var(--rose-200);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>
```

---

## 8. 컴포넌트 상세

### 8.1 EditorHeader.vue

```vue
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import Button from '../common/Button.vue'
import { ArrowLeft, Layers } from 'lucide-vue-next'

interface Props {
  projectTitle: string
  sceneTitle: string
  zoomLevel: string
  projectId: number
}

defineProps<Props>()
</script>

<template>
  <header class="editor-header">
    <div class="header-left">
      <RouterLink
        :to="{ name: 'project-detail', params: { id: projectId } }"
        class="btn btn-ghost btn-icon"
      >
        <ArrowLeft class="icon" />
      </RouterLink>
      <div class="breadcrumb">
        <RouterLink to="/dashboard">AI Movie Studio</RouterLink>
        <span>/</span>
        <RouterLink :to="{ name: 'project-detail', params: { id: projectId } }">
          {{ projectTitle }}
        </RouterLink>
        <span>/</span>
        <span class="current">{{ sceneTitle }}</span>
      </div>
    </div>

    <div class="header-right">
      <span class="zoom-indicator">{{ zoomLevel }}</span>
      <RouterLink
        :to="{ name: 'timeline', params: { id: projectId } }"
        class="btn btn-secondary btn-sm"
      >
        <Layers class="btn-icon" />
        Timeline
      </RouterLink>
      <Button variant="primary">Export Scene</Button>
    </div>
  </header>
</template>

<style scoped>
.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  background: white;
  border-bottom: 1px solid var(--rose-100);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.breadcrumb a {
  color: var(--gray-500);
  text-decoration: none;
}

.breadcrumb a:hover {
  color: var(--rose-500);
}

.breadcrumb .current {
  color: var(--gray-900);
  font-weight: 500;
}

.zoom-indicator {
  font-size: 0.75rem;
  font-family: monospace;
  background: var(--gray-100);
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  color: var(--gray-600);
}

.icon {
  width: 20px;
  height: 20px;
}

.btn-icon {
  width: 16px;
  height: 16px;
}
</style>
```

### 8.2 NodeCanvas.vue

```vue
<script setup lang="ts">
import type { Node } from '../../types'
import NodeConnections from './NodeConnections.vue'
import MasterNode from './nodes/MasterNode.vue'
import GridNode from './nodes/GridNode.vue'
import ShotNode from './nodes/ShotNode.vue'
import VideoNode from './nodes/VideoNode.vue'

interface Props {
  nodes: Node[]
  selectedNodeId: number | null
  zoomScale: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select-node', nodeId: number): void
  (e: 'regenerate', nodeId: number): void
  (e: 'add-grid', parentId: number): void
  (e: 'add-shot', parentId: number, cellIndex: number): void
  (e: 'add-video', parentId: number): void
  (e: 'confirm-video', nodeId: number): void
}>()

function getNodeComponent(type: string) {
  const components: Record<string, any> = {
    MASTER: MasterNode,
    GRID: GridNode,
    SHOT: ShotNode,
    VIDEO: VideoNode,
  }
  return components[type] || null
}
</script>

<template>
  <div class="canvas-container" :style="{ transform: `scale(${zoomScale})` }">
    <!-- SVG Connections -->
    <NodeConnections :nodes="nodes" />

    <!-- Nodes -->
    <div class="nodes-column">
      <component
        v-for="node in nodes"
        :key="node.nodeId"
        :is="getNodeComponent(node.type)"
        :node="node"
        :selected="selectedNodeId === node.nodeId"
        @select="emit('select-node', node.nodeId)"
        @regenerate="emit('regenerate', node.nodeId)"
        @add-grid="emit('add-grid', node.nodeId)"
        @add-shot="(cellIndex: number) => emit('add-shot', node.nodeId, cellIndex)"
        @add-video="emit('add-video', node.nodeId)"
        @confirm="emit('confirm-video', node.nodeId)"
      />
    </div>
  </div>
</template>

<style scoped>
.canvas-container {
  position: absolute;
  top: 40px;
  left: 50%;
  transform-origin: top center;
  transition: transform 0.2s ease;
}

.nodes-column {
  width: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 120px;
  padding: 20px 0;
  margin-left: -160px; /* center */
}
</style>
```

### 8.3 nodes/MasterNode.vue

```vue
<script setup lang="ts">
import type { Node } from '../../../types'
import Button from '../../common/Button.vue'
import { Film, RefreshCw, Plus, CheckCircle } from 'lucide-vue-next'

interface Props {
  node: Node
  selected?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'select'): void
  (e: 'regenerate'): void
  (e: 'add-grid'): void
}>()
</script>

<template>
  <div
    :class="['node', 'node-master', `state-${node.status.toLowerCase()}`, { selected }]"
    @click="emit('select')"
  >
    <div class="node-handle node-handle-bottom connected"></div>

    <div class="node-header">
      <div class="node-header-title">
        <Film class="node-icon" />
        <span class="node-title">{{ node.title || 'Master Image' }}</span>
      </div>
      <span v-if="node.status === 'SUCCEEDED'" class="node-status node-status-done">
        <CheckCircle class="status-icon" />
        완료
      </span>
    </div>

    <div class="node-body">
      <div
        class="node-preview"
        :style="node.contentUrl ? { backgroundImage: `url(${node.contentUrl})` } : {}"
      />

      <p v-if="node.prompt" class="node-description">
        "{{ node.prompt.substring(0, 60) }}..."
      </p>

      <div class="node-actions">
        <Button variant="secondary" @click.stop="emit('regenerate')">
          <RefreshCw class="btn-icon" />
          Regenerate
        </Button>
        <Button variant="primary" @click.stop="emit('add-grid')">
          <Plus class="btn-icon" />
          Grid
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 노드 공통 스타일 - nodes.css에서 가져옴 */
.node {
  width: 100%;
  background: white;
  border-radius: 12px;
  border: 2px solid var(--node-master);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.node.selected {
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.3);
}

.node-master {
  --node-color: var(--node-master, #FF85A1);
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, var(--node-color), color-mix(in srgb, var(--node-color) 80%, white));
}

.node-header-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: white;
}

.node-icon {
  width: 16px;
  height: 16px;
}

.node-title {
  font-weight: 600;
  font-size: 0.875rem;
}

.node-status {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: white;
  opacity: 0.9;
}

.status-icon {
  width: 14px;
  height: 14px;
}

.node-body {
  padding: 1rem;
}

.node-preview {
  width: 100%;
  height: 160px;
  background: var(--gray-100) center/cover no-repeat;
  border-radius: 8px;
  margin-bottom: 0.75rem;
}

.node-description {
  font-size: 0.75rem;
  color: var(--gray-600);
  margin: 0 0 0.75rem;
  line-height: 1.4;
}

.node-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

/* Handle */
.node-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: white;
  border: 2px solid var(--node-color);
  border-radius: 50%;
}

.node-handle-bottom {
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle.connected {
  background: var(--node-color);
}
</style>
```

### 8.4 PropertyPanel.vue

```vue
<script setup lang="ts">
import { computed } from 'vue'
import type { Node, CameraMotion } from '../../types'
import Button from '../common/Button.vue'
import { Trash2, Wand2 } from 'lucide-vue-next'

interface Props {
  node: Node | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update-settings', settings: { cameraMotion?: CameraMotion; duration?: number }): void
  (e: 'delete-node'): void
}>()

const cameraOptions: { value: CameraMotion; label: string }[] = [
  { value: 'ZOOM_IN', label: 'Zoom In' },
  { value: 'ZOOM_OUT', label: 'Zoom Out' },
  { value: 'PAN_LEFT', label: 'Pan Left' },
  { value: 'STATIC', label: 'Static' },
]

const durationOptions = [3, 4, 5, 6]

const selectedCameraMotion = computed(() =>
  props.node?.settings?.cameraMotion || 'ZOOM_IN'
)

const selectedDuration = computed(() =>
  props.node?.settings?.duration || 4
)

function handleCameraChange(motion: CameraMotion) {
  emit('update-settings', { cameraMotion: motion })
}

function handleDurationChange(duration: number) {
  emit('update-settings', { duration })
}
</script>

<template>
  <div v-if="node" class="property-panel">
    <div class="panel-header">
      <h3 class="panel-title">Properties</h3>
      <p class="panel-subtitle">
        <span class="node-type">{{ node.type }}</span> Node #{{ node.nodeId }}
      </p>
    </div>

    <div class="panel-body">
      <!-- Camera Motion -->
      <div class="property-section">
        <label class="property-label">Camera Motion</label>
        <div class="button-grid">
          <button
            v-for="option in cameraOptions"
            :key="option.value"
            :class="['option-btn', { active: selectedCameraMotion === option.value }]"
            @click="handleCameraChange(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- Duration -->
      <div class="property-section">
        <label class="property-label">Duration</label>
        <div class="chip-group">
          <button
            v-for="dur in durationOptions"
            :key="dur"
            :class="['chip', { selected: selectedDuration === dur }]"
            @click="handleDurationChange(dur)"
          >
            {{ dur }}s
          </button>
        </div>
      </div>

      <!-- Prompt -->
      <div class="property-section">
        <label class="property-label">Input Prompt</label>
        <textarea
          class="prompt-textarea"
          :value="node.prompt"
          readonly
        />
        <button class="improve-prompt-btn">
          <Wand2 class="btn-icon" />
          Improve Prompt
        </button>
      </div>

      <!-- Metadata -->
      <div class="property-section">
        <label class="property-label">Metadata</label>
        <div class="metadata-row">
          <span class="meta-label">Duration</span>
          <span class="meta-value">{{ selectedDuration }}s</span>
        </div>
        <div class="metadata-row">
          <span class="meta-label">Model</span>
          <span class="meta-value">{{ node.settings?.provider || 'Veo 3.1' }}</span>
        </div>
      </div>
    </div>

    <div class="panel-footer">
      <Button variant="secondary" class="delete-btn" @click="emit('delete-node')">
        <Trash2 class="btn-icon" />
        Delete Node
      </Button>
    </div>
  </div>

  <div v-else class="property-panel empty">
    <p class="empty-text">노드를 선택하세요</p>
  </div>
</template>

<style scoped>
.property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.panel-title {
  font-weight: 700;
  margin: 0 0 0.25rem;
}

.panel-subtitle {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

.node-type {
  text-transform: capitalize;
}

.panel-body {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.property-section {
  margin-bottom: 1.5rem;
}

.property-label {
  display: block;
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.option-btn {
  padding: 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  border: 1px solid var(--rose-200);
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.option-btn:hover {
  border-color: var(--rose-300);
}

.option-btn.active {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

.chip-group {
  display: flex;
  gap: 0.5rem;
}

.chip {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 9999px;
  background: white;
  cursor: pointer;
}

.chip.selected {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

.prompt-textarea {
  width: 100%;
  min-height: 80px;
  padding: 0.75rem;
  font-size: 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  resize: vertical;
}

.improve-prompt-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  font-size: 0.75rem;
  color: var(--rose-500);
  background: transparent;
  border: none;
  cursor: pointer;
}

.metadata-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.meta-label {
  color: var(--gray-500);
}

.meta-value {
  font-family: monospace;
}

.panel-footer {
  padding: 1rem;
  border-top: 1px solid var(--rose-100);
}

.delete-btn {
  width: 100%;
  color: var(--error);
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  color: var(--gray-400);
}
</style>
```

### 8.5 MiniTimeline.vue

```vue
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { TimelineClip } from '../../types'
import { Star, ArrowRight } from 'lucide-vue-next'

interface Props {
  clips: TimelineClip[]
  totalDuration: number
  maxDuration?: number
  projectId: number
}

const props = withDefaults(defineProps<Props>(), {
  maxDuration: 60,
})
</script>

<template>
  <div class="mini-timeline">
    <div class="timeline-label">
      <Star class="label-icon" />
      Confirmed Videos
    </div>

    <div class="timeline-clips">
      <div
        v-for="clip in clips"
        :key="clip.clipId"
        class="timeline-clip"
      >
        <img :src="clip.thumbnailUrl" :alt="clip.label" />
        <span class="clip-duration">{{ clip.duration }}s</span>
      </div>

      <div v-if="clips.length === 0" class="timeline-empty">
        확정된 클립이 없습니다
      </div>
    </div>

    <span class="timeline-total">
      총 {{ totalDuration }}초 / {{ maxDuration }}초
    </span>

    <RouterLink
      :to="{ name: 'timeline', params: { id: projectId } }"
      class="btn btn-secondary btn-sm"
    >
      <ArrowRight class="btn-icon" />
      Timeline
    </RouterLink>
  </div>
</template>

<style scoped>
.mini-timeline {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: white;
  border-top: 1px solid var(--rose-100);
}

.timeline-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
  white-space: nowrap;
}

.label-icon {
  width: 16px;
  height: 16px;
}

.timeline-clips {
  display: flex;
  gap: 0.5rem;
  flex: 1;
  overflow-x: auto;
}

.timeline-clip {
  position: relative;
  width: 64px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.timeline-clip img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clip-duration {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 0.625rem;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 1px 4px;
  border-radius: 2px;
}

.timeline-empty {
  font-size: 0.75rem;
  color: var(--gray-400);
}

.timeline-total {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
  white-space: nowrap;
}

.btn-icon {
  width: 16px;
  height: 16px;
}
</style>
```

### 8.6 ZoomControls.vue

```vue
<script setup lang="ts">
import { Plus, Minus, Maximize2 } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'zoom-in'): void
  (e: 'zoom-out'): void
  (e: 'zoom-fit'): void
}>()
</script>

<template>
  <div class="zoom-controls">
    <button class="zoom-btn" @click="emit('zoom-in')">
      <Plus class="icon" />
    </button>
    <button class="zoom-btn" @click="emit('zoom-out')">
      <Minus class="icon" />
    </button>
    <button class="zoom-btn" @click="emit('zoom-fit')">
      <Maximize2 class="icon" />
    </button>
  </div>
</template>

<style scoped>
.zoom-controls {
  position: fixed;
  bottom: 80px;
  left: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  z-index: 30;
}

.zoom-btn {
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.zoom-btn:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.icon {
  width: 20px;
  height: 20px;
  color: var(--gray-600);
}
</style>
```

---

## 9. 메인 페이지

### `src/pages/SceneEditPage.vue`

```vue
<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../stores/project'
import { useSceneStore } from '../stores/scene'
import { useNodeStore } from '../stores/node'
import { useUIStore } from '../stores/ui'
import { useZoom } from '../composables/useZoom'

import EditorLayout from '../layouts/EditorLayout.vue'
import EditorHeader from '../components/editor/EditorHeader.vue'
import NodeCanvas from '../components/editor/NodeCanvas.vue'
import PropertyPanel from '../components/editor/PropertyPanel.vue'
import MiniTimeline from '../components/editor/MiniTimeline.vue'
import ZoomControls from '../components/editor/ZoomControls.vue'

const route = useRoute()
const projectStore = useProjectStore()
const sceneStore = useSceneStore()
const nodeStore = useNodeStore()
const uiStore = useUIStore()

const { zoomLevel, zoomPercentage, zoomScale, zoomIn, zoomOut, zoomFit } = useZoom()

// Route params
const projectId = computed(() => Number(route.params.projectId))
const sceneId = computed(() => Number(route.params.sceneId))

// Data
const project = computed(() => projectStore.currentProject)
const currentScene = computed(() =>
  sceneStore.scenes.find(s => s.sceneId === sceneId.value)
)

// Load data
onMounted(async () => {
  if (projectId.value && sceneId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      nodeStore.loadNodes(projectId.value, sceneId.value),
    ])
  }
})

watch([projectId, sceneId], async ([newProjectId, newSceneId]) => {
  if (newProjectId && newSceneId) {
    await nodeStore.loadNodes(newProjectId, newSceneId)
  }
})

// Event handlers
function handleSelectNode(nodeId: number) {
  nodeStore.selectNode(nodeId)
}

function handleUpdateSettings(settings: any) {
  if (nodeStore.selectedNodeId) {
    nodeStore.updateSettings(nodeStore.selectedNodeId, settings)
  }
}

async function handleConfirmVideo(nodeId: number) {
  const success = await nodeStore.confirmVideo(nodeId)
  if (success) {
    uiStore.showToast('success', '확정 완료', '영상이 타임라인에 추가되었습니다.')
  }
}

function handleDeleteNode() {
  uiStore.showToast('info', '삭제', '노드 삭제 기능은 준비 중입니다.')
}
</script>

<template>
  <EditorLayout
    :project-title="project?.title || 'Project'"
    :scene-title="`Scene ${currentScene?.order || ''}: ${currentScene?.title || ''}`"
    scene-badge="Scene Editor"
  >
    <template #header>
      <EditorHeader
        :project-title="project?.title || 'Project'"
        :scene-title="`Scene ${currentScene?.order || ''}: ${currentScene?.title || ''}`"
        :zoom-level="zoomPercentage"
        :project-id="projectId"
      />
    </template>

    <template #canvas>
      <NodeCanvas
        :nodes="nodeStore.nodes"
        :selected-node-id="nodeStore.selectedNodeId"
        :zoom-scale="zoomScale"
        @select-node="handleSelectNode"
        @confirm-video="handleConfirmVideo"
      />
    </template>

    <template #properties>
      <PropertyPanel
        :node="nodeStore.selectedNode"
        @update-settings="handleUpdateSettings"
        @delete-node="handleDeleteNode"
      />
    </template>

    <template #bottom>
      <MiniTimeline
        :clips="nodeStore.timelineClips"
        :total-duration="nodeStore.totalDuration"
        :project-id="projectId"
      />
    </template>
  </EditorLayout>

  <ZoomControls
    @zoom-in="zoomIn"
    @zoom-out="zoomOut"
    @zoom-fit="zoomFit"
  />
</template>
```

---

## 10. 라우터 업데이트

### `src/router/index.ts` 수정

```typescript
// 기존 라우트에 추가
{
  path: '/projects/:projectId/scenes/:sceneId',
  name: 'scene-edit',
  component: () => import('../pages/SceneEditPage.vue'),
  meta: { requiresAuth: true },
},
```

---

## 11. 구현 순서 체크리스트

1. [ ] `src/types/index.ts` - 타입 추가
2. [ ] `src/services/mock/nodes.ts` - Mock 데이터
3. [ ] `src/stores/node.ts` - Pinia 스토어
4. [ ] `src/composables/useZoom.ts` - 줌 훅
5. [ ] `src/layouts/EditorLayout.vue` - 레이아웃
6. [ ] `src/components/editor/EditorHeader.vue`
7. [ ] `src/components/editor/NodeCanvas.vue`
8. [ ] `src/components/editor/NodeConnections.vue`
9. [ ] `src/components/editor/nodes/MasterNode.vue`
10. [ ] `src/components/editor/nodes/GridNode.vue`
11. [ ] `src/components/editor/nodes/ShotNode.vue`
12. [ ] `src/components/editor/nodes/VideoNode.vue`
13. [ ] `src/components/editor/PropertyPanel.vue`
14. [ ] `src/components/editor/MiniTimeline.vue`
15. [ ] `src/components/editor/ZoomControls.vue`
16. [ ] `src/pages/SceneEditPage.vue`
17. [ ] `src/router/index.ts` - 라우트 추가
18. [ ] `npm run build` - 에러 확인

---

## 12. 검증

```bash
npm run build  # TypeScript 에러 확인
npm run dev    # 개발 서버 시작
```

1. `/projects/1/scenes/1` 이동
2. 노드 4개 표시 확인
3. 노드 클릭 → 속성 패널 표시
4. Camera Motion, Duration 변경
5. Video 노드 Confirm → 미니 타임라인 추가
6. 줌 컨트롤 동작 확인
