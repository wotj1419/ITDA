// =============================================================================
// AI Prompt Generation
// =============================================================================

export interface GeneratePromptRequest {
  nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
  sceneOneLine?: string;
  // Master Image
  style?: string;
  timeOfDay?: string;
  mood?: string;
  objectIds?: number[];
  objects?: string[];
  // Storyboard Grid
  layout?: string;
  shotTypes?: string[];
  compositionHint?: string;
  // Shot
  shotType?: string;
  expression?: string;
  additionalDetail?: string;
  // Video
  cameraMotion?: string;
  duration?: number;
  motionDescription?: string;
}

export interface GeneratePromptResponse {
  promptEnBase: string;
  promptKo: string;
}

export interface TranslatePromptResponse {
  promptEnBase: string;
  promptKo: string;
}

// =============================================================================
// Node Generation (Image/Video)
// =============================================================================

export interface GenerateNodeRequest {
  prompt: string;
  nodeType: 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
  settings?: Record<string, unknown>;
  promptEnFinalOverride?: string;
}

export interface PromptPreviewRequest {
  prompt: string;
  settings?: Record<string, unknown>;
  promptEnFinalOverride?: string;
}

export interface PromptPreviewResponse {
  promptEnFinal: string;
  source: 'RENDERED' | 'OVERRIDE';
}

export interface GenerateJobResponse {
  jobId: number;
  status: 'PENDING' | 'QUEUED';
}

// =============================================================================
// Job Status
// =============================================================================

export type JobStatusType = 'PENDING' | 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface JobStatusResponse {
  jobId: number;
  type: 'IMAGE_GENERATION' | 'VIDEO_GENERATION' | 'SCENE_MERGE' | 'PROJECT_MERGE';
  status: JobStatusType;
  resultUrl?: string;
  thumbnailUrl?: string;
  error?: {
    code: string;
    message: string;
  };
}
