import apiClient from './client'
import type { ApiResponse } from '../../types'
// Imports for specific node data types would go here
import type { NodeType } from '../../types/node'

export interface CreateNodeRequest {
    type: NodeType
    parentNodeId?: string
    // Add other properties as needed from spec
}

export async function createNode(sceneId: number, data: CreateNodeRequest): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(`/scenes/${sceneId}/nodes`, data)
    return response.data.data
}

export async function generateNodeResult(nodeId: string, prompt: string): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(`/nodes/${nodeId}/generate`, { prompt })
    return response.data.data
}

export async function confirmVideo(nodeId: string): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(`/nodes/${nodeId}/confirm`)
    return response.data.data
}
