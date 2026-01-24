import apiClient from './client'
import type { ApiResponse } from '../../types'

export interface ScenarioData {
    // Define scenario content structure
    content: string
}

export async function fetchScenario(projectId: number): Promise<ScenarioData> {
    const response = await apiClient.get<ApiResponse<ScenarioData>>(`/projects/${projectId}/scenario`)
    return response.data.data || { content: '' }
}

export async function generateScenarioPrompt(projectId: number, topic: string): Promise<string> {
    const response = await apiClient.post<ApiResponse<string>>(`/projects/${projectId}/scenario/prompt/generate`, { topic })
    return response.data.data || ''
}

export async function generateScenarioScenes(projectId: number, script: string): Promise<any[]> {
    const response = await apiClient.post<ApiResponse<any[]>>(`/projects/${projectId}/scenario/scenes/generate`, { script })
    return response.data.data || []
}
