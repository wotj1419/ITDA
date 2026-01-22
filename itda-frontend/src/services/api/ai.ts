import apiClient from './client'
import type { ApiResponse } from '../../types'

export interface GeneratePromptRequest {
    text: string
    style?: string
}

export interface ImprovePromptRequest {
    originalPrompt: string
    feedback?: string
}

export interface JobStatusResponse {
    jobId: string
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
    result?: any
    progress?: number
}

export async function generatePrompt(data: GeneratePromptRequest): Promise<string> {
    const response = await apiClient.post<ApiResponse<string>>('/ai/prompts/generate', data)
    return response.data.data || ''
}

export async function improvePrompt(data: ImprovePromptRequest): Promise<string> {
    const response = await apiClient.post<ApiResponse<string>>('/ai/prompts/improve', data)
    return response.data.data || ''
}

export async function getJobStatus(jobId: string): Promise<JobStatusResponse> {
    const response = await apiClient.get<ApiResponse<JobStatusResponse>>(`/ai/jobs/${jobId}`)
    if (!response.data.data) throw new Error('Job not found')
    return response.data.data
}

export async function requeueJob(jobId: string): Promise<void> {
    await apiClient.post(`/ai/jobs/${jobId}/requeue`)
}
