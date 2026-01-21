# 프론트엔드-백엔드 API 연동 가이드

> **작성일**: 2026-01-22  
> **목적**: 백엔드 API가 준비되면 수정해야 할 프론트엔드 코드 정리

---

## 📋 수정 대상 파일 목록

| # | 파일 경로 | 수정 내용 |
|---|----------|----------|
| 1 | `src/components/scene-editor/panels/MasterImagePanel.vue` | `generateImage()` API 연동 |
| 2 | `src/components/scene-editor/panels/StoryboardGridPanel.vue` | `generateGrid()` API 연동 |
| 3 | `src/components/scene-editor/panels/ShotPanel.vue` | `generateShot()` API 연동 |
| 4 | `src/components/scene-editor/panels/VideoPanel.vue` | `generateVideo()` API 연동 |
| 5 | `src/services/api/nodes.ts` | 노드 생성 API 서비스 추가 (신규) |
| 6 | `src/stores/sceneNode.ts` | API 호출 로직 추가 (선택) |

---

## 1. MasterImagePanel.vue - `generateImage()`

### 📍 위치
`itda-frontend/src/components/scene-editor/panels/MasterImagePanel.vue` (line 65-67)

### ❌ 현재 코드 (Mock)
```typescript
function generateImage(): void {
  console.log('Generate image with:', form.value);
}
```

### ✅ API 연동 후 코드
```typescript
import { JobStatus } from '../../../types/node';
import { nodeApiService } from '../../../services/api/nodes';

const isGenerating = ref(false);

async function generateImage(): Promise<void> {
  if (!canGenerate.value || isGenerating.value) return;
  
  isGenerating.value = true;

  // 1. 상태를 PENDING으로 변경
  nodeStore.updateNode(props.node.id, { 
    jobStatus: JobStatus.PENDING 
  });

  try {
    // 2. API 호출
    const response = await nodeApiService.generateMasterImage({
      nodeId: props.node.id,
      prompt: form.value.prompt,
      style: form.value.style,
      timeOfDay: form.value.timeOfDay,
      mood: form.value.mood,
      objectIds: form.value.objectIds,
    });

    // 3. 성공 시 상태 업데이트
    nodeStore.updateNode(props.node.id, {
      jobStatus: JobStatus.SUCCEEDED,
      imageUrl: response.imageUrl,
      thumbnailUrl: response.thumbnailUrl,
    });
  } catch (error) {
    // 4. 실패 시 상태 업데이트
    nodeStore.updateNode(props.node.id, { 
      jobStatus: JobStatus.FAILED 
    });
    console.error('이미지 생성 실패:', error);
    // TODO: 토스트 알림 추가
  } finally {
    isGenerating.value = false;
  }
}
```

### 🔧 추가 변경 사항
```vue
<!-- 버튼에 로딩 상태 추가 -->
<template #footer>
  <button
    class="panel-btn panel-btn--primary panel-btn--full"
    :disabled="!canGenerate || isGenerating"
    @click="generateImage"
  >
    <Loader2 v-if="isGenerating" class="panel-btn-icon animate-spin" />
    <Image v-else class="panel-btn-icon" />
    {{ isGenerating ? '생성 중...' : '이미지 생성' }}
  </button>
</template>
```

---

## 2. StoryboardGridPanel.vue - `generateGrid()`

### 📍 위치
`itda-frontend/src/components/scene-editor/panels/StoryboardGridPanel.vue` (line 64-66)

### ❌ 현재 코드 (Mock)
```typescript
function generateGrid(): void {
  console.log('Generate grid:', form.value);
}
```

### ✅ API 연동 후 코드
```typescript
import { JobStatus } from '../../../types/node';
import { nodeApiService } from '../../../services/api/nodes';

const isGenerating = ref(false);

async function generateGrid(): Promise<void> {
  if (!canGenerate.value || isGenerating.value) return;
  
  isGenerating.value = true;

  // 1. 상태를 PENDING으로 변경
  nodeStore.updateNode(props.node.id, { 
    jobStatus: JobStatus.PENDING 
  });

  try {
    // 2. API 호출
    const response = await nodeApiService.generateStoryboardGrid({
      nodeId: props.node.id,
      prompt: form.value.prompt,
      layout: form.value.layout,
      shotTypes: form.value.shotTypes,
      compositionHint: form.value.compositionHint,
      parentMasterImageId: props.node.data.parentNodeId,
    });

    // 3. 성공 시 상태 업데이트
    nodeStore.updateNode(props.node.id, {
      jobStatus: JobStatus.SUCCEEDED,
      imageUrl: response.imageUrl,
      thumbnailUrl: response.thumbnailUrl,
    });
  } catch (error) {
    // 4. 실패 시 상태 업데이트
    nodeStore.updateNode(props.node.id, { 
      jobStatus: JobStatus.FAILED 
    });
    console.error('그리드 생성 실패:', error);
    // TODO: 토스트 알림 추가
  } finally {
    isGenerating.value = false;
  }
}
```

### 🔧 추가 변경 사항
```vue
<!-- 버튼에 로딩 상태 추가 -->
<template #footer>
  <button
    class="panel-btn panel-btn--primary panel-btn--full"
    :disabled="!canGenerate || isGenerating"
    @click="generateGrid"
  >
    <Loader2 v-if="isGenerating" class="panel-btn-icon animate-spin" />
    <LayoutGrid v-else class="panel-btn-icon" />
    {{ isGenerating ? '생성 중...' : '그리드 생성' }}
  </button>
</template>
```

---

## 3. ShotPanel.vue - `generateShot()`

### 📍 위치
`itda-frontend/src/components/scene-editor/panels/ShotPanel.vue` (line 72-74)

### ❌ 현재 코드 (Mock)
```typescript
function generateShot(): void {
  console.log('Generate shot:', form.value);
}
```

### ✅ API 연동 후 코드
```typescript
import { JobStatus } from '../../../types/node';
import { nodeApiService } from '../../../services/api/nodes';

const isGenerating = ref(false);

async function generateShot(): Promise<void> {
  if (!canGenerate.value || isGenerating.value) return;
  
  isGenerating.value = true;

  // 1. 상태를 PENDING으로 변경
  nodeStore.updateNode(props.node.id, { 
    jobStatus: JobStatus.PENDING 
  });

  try {
    // 2. API 호출
    const response = await nodeApiService.generateShot({
      nodeId: props.node.id,
      prompt: form.value.prompt,
      gridCellIndex: data.value?.gridCellIndex ?? 0,
      shotType: form.value.shotType,
      expression: form.value.expression,
      additionalDetail: form.value.additionalDetail,
      parentGridId: props.node.data.parentNodeId,
    });

    // 3. 성공 시 상태 업데이트
    nodeStore.updateNode(props.node.id, {
      jobStatus: JobStatus.SUCCEEDED,
      imageUrl: response.imageUrl,
      thumbnailUrl: response.thumbnailUrl,
    });
  } catch (error) {
    // 4. 실패 시 상태 업데이트
    nodeStore.updateNode(props.node.id, { 
      jobStatus: JobStatus.FAILED 
    });
    console.error('샷 생성 실패:', error);
    // TODO: 토스트 알림 추가
  } finally {
    isGenerating.value = false;
  }
}
```

### 🔧 추가 변경 사항
```vue
<!-- 버튼에 로딩 상태 추가 -->
<template #footer>
  <button
    class="panel-btn panel-btn--primary panel-btn--full"
    :disabled="!canGenerate || isGenerating"
    @click="generateShot"
  >
    <Loader2 v-if="isGenerating" class="panel-btn-icon animate-spin" />
    <Camera v-else class="panel-btn-icon" />
    {{ isGenerating ? '생성 중...' : '샷 생성' }}
  </button>
</template>
```

---

## 4. VideoPanel.vue - `generateVideo()`

### 📍 위치
`itda-frontend/src/components/scene-editor/panels/VideoPanel.vue` (line 90-92)

### ❌ 현재 코드 (Mock)
```typescript
function generateVideo(): void {
  console.log('Generate video:', form.value);
}
```

### ✅ API 연동 후 코드
```typescript
import { JobStatus } from '../../../types/node';
import { nodeApiService } from '../../../services/api/nodes';

const isGenerating = ref(false);

async function generateVideo(): Promise<void> {
  if (!canGenerate.value || isGenerating.value) return;
  
  isGenerating.value = true;

  // 1. 상태를 PENDING으로 변경
  nodeStore.updateNode(props.node.id, { 
    jobStatus: JobStatus.PENDING 
  });

  try {
    // 2. API 호출
    const response = await nodeApiService.generateVideo({
      nodeId: props.node.id,
      prompt: form.value.prompt,
      startShotId: props.node.data.parentNodeId,
      endShotId: form.value.isTransition ? data.value?.endShotId : null,
      cameraMotion: form.value.cameraMotion,
      duration: form.value.duration,
      motionDescription: form.value.motionDescription,
    });

    // 3. 성공 시 상태 업데이트
    nodeStore.updateNode(props.node.id, {
      jobStatus: JobStatus.SUCCEEDED,
      videoUrl: response.videoUrl,
      thumbnailUrl: response.thumbnailUrl,
    });
  } catch (error) {
    // 4. 실패 시 상태 업데이트
    nodeStore.updateNode(props.node.id, { 
      jobStatus: JobStatus.FAILED 
    });
    console.error('영상 생성 실패:', error);
    // TODO: 토스트 알림 추가
  } finally {
    isGenerating.value = false;
  }
}
```

### 🔧 추가 변경 사항
```vue
<!-- 버튼에 로딩 상태 추가 -->
<template #footer>
  <button
    class="panel-btn panel-btn--primary panel-btn--full"
    :disabled="!canGenerate || isGenerating"
    @click="generateVideo"
  >
    <Loader2 v-if="isGenerating" class="panel-btn-icon animate-spin" />
    <Video v-else class="panel-btn-icon" />
    {{ isGenerating ? '생성 중...' : '영상 생성' }}
  </button>
</template>
```

---

## 5. API 서비스 파일 (신규 생성)

### 📍 파일 생성 위치
`itda-frontend/src/services/api/nodes.ts`

### ✅ 생성할 코드
```typescript
/**
 * Node API Service
 * 노드 관련 AI 생성 API 호출
 */
import { apiClient } from './client';
import type { CameraMotion } from '../../types/node';

// 요청/응답 타입 정의
interface GenerateMasterImageRequest {
  nodeId: string;
  prompt: string;
  style: string;
  timeOfDay: string;
  mood: string;
  objectIds: string[];
}

interface GenerateStoryboardGridRequest {
  nodeId: string;
  prompt: string;
  layout: '2x2' | '2x3' | '3x3';
  shotTypes: string[];
  compositionHint: string;
  parentMasterImageId: string | null;
}

interface GenerateShotRequest {
  nodeId: string;
  prompt: string;
  gridCellIndex: number;
  shotType: string;
  expression: string;
  additionalDetail: string;
  parentGridId: string | null;
}

interface GenerateVideoRequest {
  nodeId: string;
  prompt: string;
  startShotId: string | null;
  endShotId: string | null;
  cameraMotion: CameraMotion;
  duration: number;
  motionDescription: string;
}

interface GenerateImageResponse {
  imageUrl: string;
  thumbnailUrl: string;
  jobId?: string;
}

interface GenerateVideoResponse {
  videoUrl: string;
  thumbnailUrl: string;
  jobId?: string;
}

export const nodeApiService = {
  /**
   * 마스터 이미지 생성 API
   */
  async generateMasterImage(data: GenerateMasterImageRequest): Promise<GenerateImageResponse> {
    const response = await apiClient.post(`/api/nodes/${data.nodeId}/generate-master-image`, data);
    return response.data;
  },

  /**
   * 스토리보드 그리드 생성 API
   */
  async generateStoryboardGrid(data: GenerateStoryboardGridRequest): Promise<GenerateImageResponse> {
    const response = await apiClient.post(`/api/nodes/${data.nodeId}/generate-grid`, data);
    return response.data;
  },

  /**
   * 샷 이미지 생성 API
   */
  async generateShot(data: GenerateShotRequest): Promise<GenerateImageResponse> {
    const response = await apiClient.post(`/api/nodes/${data.nodeId}/generate-shot`, data);
    return response.data;
  },

  /**
   * 영상 생성 API
   */
  async generateVideo(data: GenerateVideoRequest): Promise<GenerateVideoResponse> {
    const response = await apiClient.post(`/api/nodes/${data.nodeId}/generate-video`, data);
    return response.data;
  },

  /**
   * Job 상태 조회 API (폴링용)
   */
  async getJobStatus(jobId: string): Promise<{ status: string; result?: GenerateImageResponse | GenerateVideoResponse }> {
    const response = await apiClient.get(`/api/ai/jobs/${jobId}`);
    return response.data;
  },
};
```

---

## 6. 예상 API 엔드포인트 (백엔드 확인 필요)

| 기능 | Method | Endpoint | 요청 Body |
|------|--------|----------|----------|
| 마스터 이미지 생성 | POST | `/api/nodes/{nodeId}/generate-master-image` | `{ prompt, style, timeOfDay, mood, objectIds }` |
| 스토리보드 그리드 생성 | POST | `/api/nodes/{nodeId}/generate-grid` | `{ prompt, layout, shotTypes, compositionHint }` |
| 샷 이미지 생성 | POST | `/api/nodes/{nodeId}/generate-shot` | `{ prompt, gridCellIndex, shotType, expression, additionalDetail }` |
| 영상 생성 | POST | `/api/nodes/{nodeId}/generate-video` | `{ prompt, startShotId, endShotId, cameraMotion, duration, motionDescription }` |
| Job 상태 조회 | GET | `/api/ai/jobs/{jobId}` | - |

---

## 7. WebSocket 연동 (선택 - 긴 작업용)

AI 생성이 오래 걸리는 경우, WebSocket으로 실시간 상태 업데이트를 받을 수 있습니다.

### 이벤트 구독 예시
```typescript
// stores/sceneNode.ts 또는 별도 composable
import { useWebSocket } from '../composables/useWebSocket';

const { subscribe } = useWebSocket();

subscribe('job.progress', (data) => {
  // { nodeId, progress: 50, status: 'running' }
  updateNode(data.nodeId, { jobStatus: 'running' });
});

subscribe('job.done', (data) => {
  // { nodeId, imageUrl, thumbnailUrl, videoUrl }
  updateNode(data.nodeId, {
    jobStatus: 'succeeded',
    imageUrl: data.imageUrl,
    thumbnailUrl: data.thumbnailUrl,
    videoUrl: data.videoUrl,
  });
});

subscribe('job.failed', (data) => {
  // { nodeId, error }
  updateNode(data.nodeId, { jobStatus: 'failed' });
});
```

---

## 📝 체크리스트

API 연동 시 아래 순서로 작업하세요:

- [ ] `src/services/api/nodes.ts` 파일 생성
- [ ] `MasterImagePanel.vue` - `generateImage()` 수정
- [ ] `StoryboardGridPanel.vue` - `generateGrid()` 수정
- [ ] `ShotPanel.vue` - `generateShot()` 수정
- [ ] `VideoPanel.vue` - `generateVideo()` 수정
- [ ] 로딩 UI 추가 (`isGenerating` 상태)
- [ ] 에러 처리 및 토스트 알림 추가
- [ ] (선택) WebSocket 연동

---

> 📌 **참고**: 이 문서는 백엔드 API 스펙이 확정되면 업데이트가 필요할 수 있습니다.

