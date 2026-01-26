import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { NodeType as ApiNodeTypeValue, NodePosition } from '../../types/api/nodes'

export type ApiNodeType = ApiNodeTypeValue

export interface NodeSummary {
    nodeId: number
    type: ApiNodeType
    title?: string | null
    description?: string | null
    parentNodeId?: number | null
    status?: string | null
    isActive?: boolean | null
    isConfirmed?: boolean | null
    contentUrl?: string | null
    position?: NodePosition | null
}

export interface NodeTreeResponse {
    nodes: NodeSummary[]
}

export interface CreateNodeRequest {
    nodeType: ApiNodeType
    parentNodeId?: number | null
    prompt?: string
    settings?: Record<string, unknown>
}

export async function fetchSceneNodes(sceneId: number): Promise<NodeSummary[]> {
    const response = await apiClient.get<ApiResponse<NodeTreeResponse>>(`/scenes/${sceneId}/nodes`)
    return response.data.data?.nodes || []
}

export async function createNode(sceneId: number, data: CreateNodeRequest): Promise<number> {
    const response = await apiClient.post<ApiResponse<{ nodeId: number }>>(`/scenes/${sceneId}/nodes`, data)
    if (!response.data.data?.nodeId) {
        throw new Error('Failed to create node')
    }
    return response.data.data.nodeId
}

export async function updateNode(nodeId: number, data: { prompt?: string; settings?: Record<string, unknown> }): Promise<void> {
    await apiClient.put(`/nodes/${nodeId}`, data)
}

export async function deleteNode(nodeId: number): Promise<void> {
    await apiClient.delete(`/nodes/${nodeId}`)
}

export async function confirmNode(nodeId: number): Promise<void> {
    await apiClient.post(`/nodes/${nodeId}/confirm`)
}

export async function unconfirmNode(nodeId: number): Promise<void> {
    await apiClient.delete(`/nodes/${nodeId}/confirm`)
}

export async function activateMaster(nodeId: number): Promise<void> {
    await apiClient.post(`/nodes/${nodeId}/activate`)
}

export async function updateNodePositions(sceneId: number, positions: Array<{ nodeId: number; x: number; y: number }>): Promise<void> {
    await apiClient.put(`/scenes/${sceneId}/nodes/positions`, { positions })
}
