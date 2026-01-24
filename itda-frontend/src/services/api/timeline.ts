import apiClient from './client'
import type { TimelineClip, ApiResponse } from '../../types'

const PROJECT_URL = '/projects'
const SCENE_URL = '/scenes'

// 프로젝트 타임라인 조회
export async function fetchProjectTimeline(projectId: number): Promise<TimelineClip[]> {
    const response = await apiClient.get<ApiResponse<TimelineClip[]>>(`${PROJECT_URL}/${projectId}/timeline`)
    return response.data.data || []
}

// 씬 타임라인 조회
export async function fetchSceneTimeline(sceneId: number): Promise<TimelineClip[]> {
    const response = await apiClient.get<ApiResponse<TimelineClip[]>>(`${SCENE_URL}/${sceneId}/timeline`)
    return response.data.data || []
}

// 시나리오 병합 (최종 비디오 생성)
// 성공 상태 또는 다운로드 URL 반환
export async function mergeScenarios(projectId: number): Promise<{ success: boolean; downloadUrl?: string }> {
    const response = await apiClient.post<ApiResponse<{ url: string }>>(`${PROJECT_URL}/${projectId}/merge`)
    // data에 다운로드 URL이 포함되어 있다고 가정
    return {
        success: response.data.code === 'SUCCESS',
        downloadUrl: response.data.data?.url
    }
}

// 클립 순서 변경 (지원되는 경우)
export async function reorderClips(_projectId: number, _clipIds: string[]): Promise<TimelineClip[]> {
    // 문서에 없음, 프론트엔드 편집 시 자주 필요한 기능
    return []
}

// 클립 삭제 (지원되는 경우)
export async function removeClip(_projectId: number, _clipId: string): Promise<boolean> {
    // 문서에 없음
    return false
}
