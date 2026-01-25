import apiClient from './client'
import type { Scene, CreateSceneRequest, ApiResponse } from '../../types'

const BASE_URL = '/projects'
const SCENE_URL = '/scenes'

// 프로젝트의 모든 씬 조회
// 참고: mock 서비스는 정렬된 데이터를 반환함. API가 정렬된 데이터를 주거나 여기서 정렬해야 함.
export async function fetchScenesByProjectId(projectId: number): Promise<Scene[]> {
    const response = await apiClient.get<ApiResponse<Scene[]>>(`${BASE_URL}/${projectId}/scenes`)
    return response.data.data || []
}

// 새 씬 생성
export async function createScene(projectId: number, data: CreateSceneRequest): Promise<Scene> {
    const response = await apiClient.post<ApiResponse<Scene>>(`${BASE_URL}/${projectId}/scenes`, data)
    return response.data.data!
}

// 씬 일괄 생성 (문서에 명시되지 않았으나 Mock에는 존재. API 미지원 시 루프 사용)
export async function createScenes(projectId: number, scenesData: CreateSceneRequest[]): Promise<Scene[]> {
    // API가 일괄 생성을 지원한다면:
    // const response = await apiClient.post<ApiResponse<Scene[]>>(`${BASE_URL}/${projectId}/scenes/batch`, scenesData)
    // return response.data.data || []

    // 대체 방안: 병렬 요청
    const promises = scenesData.map(data => createScene(projectId, data))
    return Promise.all(promises)
}

// 씬 수정
export async function updateScene(
    _projectId: number,
    sceneId: number,
    data: Partial<Scene>
): Promise<Scene | null> {
    const response = await apiClient.put<ApiResponse<Scene>>(`${SCENE_URL}/${sceneId}`, data)
    // API가 업데이트된 씬 데이터를 반환한다고 가정
    return response.data.data || null
}

// 씬 삭제
export async function deleteScene(_projectId: number, sceneId: number): Promise<boolean> {
    await apiClient.delete<ApiResponse<void>>(`${SCENE_URL}/${sceneId}`)
    return true
}

// 씬 순서 변경
// 문서에 특정 재정렬 엔드포인트가 명시되지 않음. 보통 PUT /scenes/reorder 등을 사용.
// Mock 구현체는 메모리상에서 순서를 변경함.
export async function reorderScenes(projectId: number, sceneIds: number[]): Promise<Scene[]> {
    try {
        await apiClient.post<ApiResponse<void>>(`${BASE_URL}/${projectId}/scenes/reorder`, { sceneIds })
        // 재정렬 후, 최신 목록을 다시 가져오는 것이 일반적임
        return fetchScenesByProjectId(projectId)
    } catch (e) {
        console.warn('재정렬 API가 구현되지 않았거나 실패했습니다.', e)
        return []
    }
}

// AI로 씬 생성
// 문서: POST /api/projects/{id}/scenario/scenes/generate (예상)
// 또는 수동 생성 API 사용. Mock 동작을 대체하거나 특정 엔드포인트 확인 필요.
export async function generateScenesWithAI(
    projectId: number,
    request: any
): Promise<Scene[]> {
    // 문서에 따른 AI 씬 생성 엔드포인트 호출 가정
    // "씬 스토리 생성: POST /api/projects/{id}/scenario/scenes/generate"
    const response = await apiClient.post<ApiResponse<Scene[]>>(`${BASE_URL}/${projectId}/scenario/scenes/generate`, request)
    return response.data.data || []
}
