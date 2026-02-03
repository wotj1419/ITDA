import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { ProjectTimeline, SceneTimeline } from '../../types/api/timeline'

export async function fetchProjectTimeline(projectId: number): Promise<ProjectTimeline> {
    const response = await apiClient.get<ApiResponse<ProjectTimeline>>(`/projects/${projectId}/timeline`)
    return response.data.data || { items: [], totalDuration: 0 }
}

export async function fetchSceneTimeline(sceneId: number): Promise<SceneTimeline> {
    const response = await apiClient.get<ApiResponse<SceneTimeline>>(`/scenes/${sceneId}/timeline`)
    return response.data.data || { items: [], totalDuration: 0 }
}

export async function reorderSceneTimeline(sceneId: number, orderedVideoNodeIds: number[]): Promise<void> {
    await apiClient.put<ApiResponse<void>>(`/scenes/${sceneId}/timeline/order`, {
        orderedVideoNodeIds,
    })
}

export async function reorderProjectTimeline(projectId: number, orderedSceneVideoIds: number[]): Promise<void> {
    await apiClient.put<ApiResponse<void>>(`/projects/${projectId}/timeline/order`, {
        orderedSceneVideoIds,
    })
}

export async function requestProjectMerge(projectId: number): Promise<{ jobId: number; status: string }> {
    const response = await apiClient.post<ApiResponse<{ jobId: number; status: string }>>(`/projects/${projectId}/merge`)
    if (!response.data.data) {
        throw new Error('Failed to request merge')
    }
    return response.data.data
}

export async function requestSceneMerge(sceneId: number): Promise<{ jobId: number; status: string }> {
    const response = await apiClient.post<ApiResponse<{ jobId: number; status: string }>>(`/scenes/${sceneId}/merge`)
    if (!response.data.data) {
        throw new Error('Failed to request merge')
    }
    return response.data.data
}

export async function fetchSceneExport(sceneId: number): Promise<string | null> {
    const response = await apiClient.get<ApiResponse<{ downloadUrl: string }>>(`/scenes/${sceneId}/export`)
    return response.data.data?.downloadUrl || null
}

export async function fetchProjectExport(projectId: number): Promise<string | null> {
    const response = await apiClient.get<ApiResponse<{ downloadUrl: string }>>(`/projects/${projectId}/export`)
    return response.data.data?.downloadUrl || null
}

export type { TimelineItem } from '../../types/api/timeline'
