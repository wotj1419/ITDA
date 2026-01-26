import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'

export interface ScenarioData {
    projectId: number
    version: number
    currentStep: string
    input: {
        genre: string
        mood: string
        sceneCount: number
        keywords: string
        characterHints?: string | null
        backgroundHints?: string | null
        referenceStyle?: string | null
    }
    prompt: { text: string; status: string }
    plot: { text: string; status: string }
}

export async function fetchScenario(projectId: number): Promise<ScenarioData> {
    const response = await apiClient.get<ApiResponse<ScenarioData>>(`/projects/${projectId}/scenario`)
    if (!response.data.data) {
        throw new Error('No scenario data')
    }
    return response.data.data
}

export interface GenerateScenarioPromptRequest {
    genre: string
    mood: string
    sceneCount: number
    keywords: string[] | string
    characterHints?: string
    backgroundHints?: string
    referenceStyle?: string
}

export async function generateScenarioPrompt(
    projectId: number,
    request: GenerateScenarioPromptRequest
): Promise<{ text: string; status: string }> {
    const response = await apiClient.post<ApiResponse<{ prompt: { text: string; status: string } }>>(
        `/projects/${projectId}/scenario/prompt/generate`,
        request
    )
    const prompt = response.data.data?.prompt
    if (!prompt) {
        throw new Error('No prompt data')
    }
    return prompt
}

export async function updateScenarioPrompt(
    projectId: number,
    text: string,
    status: 'DRAFT' | 'APPROVED'
): Promise<void> {
    await apiClient.put(`/projects/${projectId}/scenario/prompt`, { text, status })
}

export async function generateScenarioPlot(projectId: number): Promise<{ text: string; status: string }> {
    const response = await apiClient.post<ApiResponse<{ plot: { text: string; status: string } }>>(
        `/projects/${projectId}/scenario/plot/generate`
    )
    const plot = response.data.data?.plot
    if (!plot) {
        throw new Error('No plot data')
    }
    return plot
}

export async function updateScenarioPlot(
    projectId: number,
    text: string,
    status: 'DRAFT' | 'APPROVED'
): Promise<void> {
    await apiClient.put(`/projects/${projectId}/scenario/plot`, { text, status })
}

export interface ScenarioSceneItem {
    sceneId: number
    order: number
    title: string
    description: string
}

export async function generateScenarioScenes(
    projectId: number
): Promise<{ scenes: ScenarioSceneItem[]; currentStep: string }> {
    const response = await apiClient.post<ApiResponse<{ scenes: ScenarioSceneItem[]; currentStep: string }>>(
        `/projects/${projectId}/scenario/scenes/generate`
    )
    if (!response.data.data) {
        throw new Error('No scene data')
    }
    return response.data.data
}
