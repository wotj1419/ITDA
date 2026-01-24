import apiClient from './client'
import type { Node, NodeStatus, NodeSettings } from '../../types'
import type { ApiResponse } from '../../types'

const BASE_URL = '/nodes'
const SCENE_URL = '/scenes'

// 노드 생성
export async function createNode(sceneId: number, data: any): Promise<Node> {
    const response = await apiClient.post<ApiResponse<Node>>(`${SCENE_URL}/${sceneId}/nodes`, data)
    return response.data.data!
}

// 씬 ID로 노드 목록 조회 (문서에 명시되지 않았으나 일반적으로 필요함)
export async function fetchNodesBySceneId(_projectId: number, sceneId: number): Promise<Node[]> {
    // 요약 목록에 GET /scenes/{id}/nodes가 없지만 보통 필요합니다. 씬 상세 조회에 포함될 수도 있습니다.
    // 문서 내용: "노드/캔버스 (12) - 노드 생성... 노드 목록?"
    // Mock 서비스에 fetchNodesBySceneId가 있으므로 REST 관례에 따라 GET /scenes/{sceneId}/nodes 시도.
    try {
        const response = await apiClient.get<ApiResponse<Node[]>>(`${SCENE_URL}/${sceneId}/nodes`)
        return response.data.data || []
    } catch (e) {
        // 호출 실패 시 예외 처리 또는 빈 배열 반환
        console.warn('노드 목록 조회 실패', e)
        return []
    }
}

// 노드 결과 생성 (이미지/비디오)
export async function generateNodeResult(nodeId: number): Promise<Node> {
    const response = await apiClient.post<ApiResponse<Node>>(`${BASE_URL}/${nodeId}/generate`)
    return response.data.data!
}

// 비디오 노드 타임라인 확정
export async function confirmVideoToTimeline(_projectId: number, _sceneId: number, nodeId: number): Promise<boolean> {
    const response = await apiClient.post<ApiResponse<void>>(`${BASE_URL}/${nodeId}/confirm`)
    return response.data.code === 'SUCCESS'
}

// 비디오 노드 확정 취소 (선택 사항, 토글 기능이 있는 경우 필요)
export async function unconfirmVideoFromTimeline(_projectId: number, _sceneId: number, _nodeId: number): Promise<boolean> {
    // 문서에 확정 취소가 명시되지 않음. DELETE /nodes/{id}/confirm 또는 토글 방식일 수 있음.
    // 현재는 확정이 멱등하거나 토글을 처리한다고 가정.
    // 지원되지 않을 경우, 단순히 false 반환.
    console.warn('확정 취소 API가 명시적으로 정의되지 않았습니다.')
    return false
}

// 노드 상태 업데이트 (Mock에 존재)
export async function updateNodeStatus(_projectId: number, _sceneId: number, nodeId: number, status: NodeStatus): Promise<Node | null> {
    // 보통 PATCH /nodes/{id} 사용
    const response = await apiClient.patch<ApiResponse<Node>>(`${BASE_URL}/${nodeId}`, { status })
    return response.data.data || null
}

// 노드 설정 업데이트 (Mock에 존재)
export async function updateNodeSettings(_projectId: number, _sceneId: number, nodeId: number, settings: Partial<NodeSettings>): Promise<Node | null> {
    const response = await apiClient.patch<ApiResponse<Node>>(`${BASE_URL}/${nodeId}`, { settings })
    return response.data.data || null
}
