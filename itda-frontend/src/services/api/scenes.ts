import apiClient from './client'
import type { ApiResponse, Scene } from '../../types'

interface CreateSceneRequest {
    title: string
    description?: string
    sceneOrder?: number
}

export async function fetchScenes(projectId: number): Promise<Scene[]> {
    const response = await apiClient.get<ApiResponse<Scene[]>>(`/projects/${projectId}/scenes`)
    return response.data.data || []
}

export async function fetchSceneById(sceneId: number): Promise<Scene | null> {
    const response = await apiClient.get<ApiResponse<Scene>>(`/scenes/${sceneId}`)
    return response.data.data || null
}

export async function createScene(projectId: number, data: CreateSceneRequest): Promise<Scene> {
    const response = await apiClient.post<ApiResponse<Scene>>(`/projects/${projectId}/scenes`, data)
    if (!response.data.data) {
        throw new Error('No data received from createScene')
    }
    return response.data.data
}
