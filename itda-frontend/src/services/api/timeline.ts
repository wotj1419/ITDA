import apiClient from './client'
import type { ApiResponse } from '../../types'

export interface TimelineItem {
    videoNodeId: number
    sceneId: number
    order: number
    url: string
}

export interface ProjectTimelineResponse {
    items: TimelineItem[]
}

export async function fetchProjectTimeline(projectId: number): Promise<ProjectTimelineResponse> {
    const response = await apiClient.get<ApiResponse<ProjectTimelineResponse>>(`/projects/${projectId}/timeline`)
    return response.data.data || { items: [] }
}

export async function requestProjectMerge(projectId: number): Promise<{ jobId: number; status: string }> {
    const response = await apiClient.post<ApiResponse<{ jobId: number; status: string }>>(`/projects/${projectId}/merge`)
    if (!response.data.data) {
        throw new Error('Failed to request merge')
    }
    return response.data.data
}

export async function fetchProjectExport(projectId: number): Promise<string | null> {
    const response = await apiClient.get<ApiResponse<{ exportUrl: string }>>(`/projects/${projectId}/export`)
    return response.data.data?.exportUrl || null
}
