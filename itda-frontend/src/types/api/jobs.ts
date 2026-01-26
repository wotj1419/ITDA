export type JobType = 'VIDEO_GENERATION' | 'IMAGE_GENERATION' | 'SCENE_MERGE' | 'PROJECT_MERGE';
export type JobStatus = 'pending' | 'running' | 'done' | 'failed';

export interface Job {
  jobId: string;
  type: JobType;
  status: JobStatus;
  progress?: number | null;
  target: {
    type: 'NODE' | 'SCENE_VIDEO' | 'PROJECT';
    id: number;
  };
  resultUrl?: string;
  error?: {
    code: string;
    message: string;
  };
}
