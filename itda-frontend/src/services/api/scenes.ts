import apiClient from './client'
import type { ApiResponse, Scene, CreateSceneRequest } from '../../types'

interface CreateSceneApiRequest extends CreateSceneRequest {
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
    const response = await apiClient.post<ApiResponse<{ sceneId: number }>>(
        `/projects/${projectId}/scenes`,
        data as CreateSceneApiRequest
    )
    const sceneId = response.data.data?.sceneId
    if (!sceneId) {
        throw new Error('No sceneId received from createScene')
    }
    const detail = await fetchSceneById(sceneId)
    if (!detail) {
        throw new Error('No data received from fetchSceneById')
    }
    return detail
}

export async function createScenes(projectId: number, dataList: CreateSceneRequest[]): Promise<Scene[]> {
    const created: Scene[] = []
    for (const data of dataList) {
        const scene = await createScene(projectId, data)
        created.push(scene)
    }
    return created
}

export async function updateScene(sceneId: number, data: Partial<Scene>): Promise<Scene | null> {
    const response = await apiClient.put<ApiResponse<Scene>>(`/scenes/${sceneId}`, data)
    return response.data.data || null
}

export async function deleteScene(sceneId: number): Promise<void> {
    await apiClient.delete(`/scenes/${sceneId}`)
}

export async function reorderScenes(projectId: number, orderedSceneIds: number[]): Promise<void> {
    await apiClient.put(`/projects/${projectId}/scenes/order`, { orderedSceneIds })
}
