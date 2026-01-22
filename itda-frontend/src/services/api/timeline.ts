import apiClient from './client'
import type { ApiResponse } from '../../types'

export interface TimelineData {
    // Define timeline response structure
    clips: any[]
}

export async function fetchSceneTimeline(sceneId: number): Promise<TimelineData> {
    const response = await apiClient.get<ApiResponse<TimelineData>>(`/scenes/${sceneId}/timeline`)
    return response.data.data || { clips: [] }
}

export async function fetchProjectTimeline(projectId: number): Promise<TimelineData> {
    const response = await apiClient.get<ApiResponse<TimelineData>>(`/projects/${projectId}/timeline`)
    return response.data.data || { clips: [] }
}

export async function requestProjectMerge(projectId: number): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(`/projects/${projectId}/merge`)
    return response.data.data
}
