/**
 * Mock AI Service
 * @module services/mock/ai
 */
import type {
  GeneratePromptRequest,
  JobStatusResponse,
  JobStatusType,
} from '../../types/api'

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

let jobIdCounter = 1000
const jobStore = new Map<number, JobStatusResponse>()

function buildImageUrl(seed: number): string {
  return `https://picsum.photos/seed/${seed}/1024/576`
}

function buildThumbnailUrl(seed: number): string {
  return `https://picsum.photos/seed/${seed}/320/180`
}

function isVideoPrompt(prompt: string): boolean {
  const normalized = prompt.toLowerCase()
  return normalized.includes('video')
}

function buildPromptSummary(request: GeneratePromptRequest): string {
  const parts: string[] = []
  if (request.style) parts.push(`style: ${request.style}`)
  if (request.timeOfDay) parts.push(`time: ${request.timeOfDay}`)
  if (request.mood) parts.push(`mood: ${request.mood}`)
  if (request.layout) parts.push(`layout: ${request.layout}`)
  if (request.shotTypes?.length) parts.push(`shots: ${request.shotTypes.join(', ')}`)
  if (request.compositionHint) parts.push(`hint: ${request.compositionHint}`)
  if (request.shotType) parts.push(`shot: ${request.shotType}`)
  if (request.expression) parts.push(`expression: ${request.expression}`)
  if (request.additionalDetail) parts.push(`detail: ${request.additionalDetail}`)
  if (request.cameraMotion) parts.push(`camera: ${request.cameraMotion}`)
  if (request.duration) parts.push(`duration: ${request.duration}s`)
  if (request.motionDescription) parts.push(`motion: ${request.motionDescription}`)
  return parts.join(' | ')
}

export async function generatePrompt(
  request: GeneratePromptRequest
): Promise<{ promptEnBase: string; promptKo: string }> {
  await delay(500)
  const summary = buildPromptSummary(request)
  const promptEnBase = `[${request.nodeType}] Mock prompt${summary ? ` - ${summary}` : ''}`
  return { promptEnBase, promptKo: `번역: ${promptEnBase}` }
}

export async function improvePrompt(
  currentPrompt: string,
  userFeedback: string,
  _nodeType?: GeneratePromptRequest['nodeType'],
  _context?: { endFrameHint?: string }
): Promise<{ promptEnBase: string; promptKo: string }> {
  await delay(400)
  const promptEnBase = `${currentPrompt} (improved: ${userFeedback})`
  return { promptEnBase, promptKo: `번역: ${promptEnBase}` }
}

export async function translatePrompt(
  promptEn: string
): Promise<{ promptEnBase: string; promptKo: string }> {
  await delay(200)
  return { promptEnBase: promptEn, promptKo: `번역: ${promptEn}` }
}

export async function rewritePrompt(
  promptKo: string
): Promise<{ promptEnBase: string; promptKo: string }> {
  await delay(200)
  return { promptEnBase: `Rewrite: ${promptKo}`, promptKo }
}

export async function generateNode(
  nodeId: string | number,
  prompt: string,
  _options?: {
    nodeType?: GeneratePromptRequest['nodeType'];
    settings?: Record<string, unknown>;
    promptEnFinalOverride?: string;
    referenceObjectIds?: number[];
  }
): Promise<number> {
  await delay(300)
  const jobId = ++jobIdCounter
  const nodeSeed =
    typeof nodeId === 'number' ? nodeId : nodeId.toString().length
  const seed = Date.now() + jobId + nodeSeed
  const isVideo = isVideoPrompt(prompt)
  jobStore.set(jobId, {
    jobId,
    type: isVideo ? 'VIDEO_GENERATION' : 'IMAGE_GENERATION',
    status: 'PENDING',
    resultUrl: isVideo ? 'https://sample-videos.com/video.mp4' : buildImageUrl(seed),
    thumbnailUrl: buildThumbnailUrl(seed),
  })
  return jobId
}

export async function previewPrompt(
  _nodeId: string | number,
  request: { prompt: string; settings?: Record<string, unknown>; promptEnFinalOverride?: string }
): Promise<{ promptEnFinal: string; source: 'RENDERED' | 'OVERRIDE' }> {
  await delay(150)
  if (request.promptEnFinalOverride && request.promptEnFinalOverride.trim()) {
    return { promptEnFinal: request.promptEnFinalOverride, source: 'OVERRIDE' }
  }
  return { promptEnFinal: `Preview: ${request.prompt}`, source: 'RENDERED' }
}

export async function getJobStatus(jobId: number): Promise<JobStatusResponse> {
  await delay(200)
  const job = jobStore.get(jobId)
  if (!job) {
    return {
      jobId,
      type: 'IMAGE_GENERATION',
      status: 'FAILED',
      error: { code: 'NOT_FOUND', message: 'Mock job not found' },
    }
  }
  return job
}

export async function pollJobUntilComplete(
  jobId: number,
  onProgress?: (status: JobStatusResponse) => void,
  intervalMs: number = 800,
  maxAttempts: number = 10
): Promise<JobStatusResponse> {
  let attempts = 0

  while (attempts < maxAttempts) {
    const job = await getJobStatus(jobId)
    const nextStatus: JobStatusType =
      attempts < 1 ? 'RUNNING' : 'SUCCEEDED'
    const nextJob: JobStatusResponse = {
      ...job,
      status: nextStatus,
    }
    jobStore.set(jobId, nextJob)
    if (onProgress) {
      onProgress(nextJob)
    }
    if (nextStatus === 'SUCCEEDED') {
      return nextJob
    }
    await delay(intervalMs)
    attempts += 1
  }

  throw new Error('Mock job polling timeout')
}

export const mockAiService = {
  generatePrompt,
  improvePrompt,
  translatePrompt,
  rewritePrompt,
  previewPrompt,
  generateNode,
  getJobStatus,
  pollJobUntilComplete,
}
