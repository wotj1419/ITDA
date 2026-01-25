import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { CreateObjectRequest, ObjectSheet } from '../../types/api/objects'

const BASE_URL = '/projects'
const OBJECT_URL = '/objects'

// 프로젝트의 모든 오브젝트(캐릭터/사물) 조회
export async function fetchCharactersByProjectId(projectId: number): Promise<ObjectSheet[]> {
    const response = await apiClient.get<ApiResponse<ObjectSheet[]>>(`${BASE_URL}/${projectId}/objects`)
    return response.data.data || []
}

// 새 오브젝트(캐릭터/사물) 생성
export async function createCharacter(projectId: number, data: CreateObjectRequest): Promise<ObjectSheet> {
    const response = await apiClient.post<ApiResponse<ObjectSheet>>(`${BASE_URL}/${projectId}/objects`, data)
    // API는 확인 메시지만 반환할 수 있으나, 프론트엔드에서는 생성된 객체 정보를 필요로 함
    // 실제 응답에 따라 로직 조정 필요
    if (response.data.code === 'ACCEPTED') {
        // 비동기 처리일 경우 플레이스홀더를 반환하거나 다른 처리가 필요할 수 있음
        // 현재 스토어 구조(즉시 반환 기대)와의 호환성을 위해 가상 객체 반환:
        const { type, ...rest } = data
        void type
        return {
            objectId: -1, // 임시 ID
            type: 'CHARACTER', // 기본값, 데이터에 type이 있다면 덮어씌움
            sheetImageUrl: '', // 이미지 생성 대기 중
            ...rest // 데이터가 있으면 덮어쓰기
        } as ObjectSheet
    }
    return response.data.data as ObjectSheet
}

// 기존 오브젝트 수정
// 참고: services/mock/characters.ts의 시그니처: updateCharacter(projectId, characterId, data)
export async function updateCharacter(
    _projectId: number,
    characterId: number,
    data: Partial<ObjectSheet>
): Promise<ObjectSheet | null> {
    const response = await apiClient.put<ApiResponse<ObjectSheet>>(`${OBJECT_URL}/${characterId}`, data)
    return response.data.data || null
}

// 오브젝트 삭제
// 참고: services/mock/characters.ts의 시그니처: deleteCharacter(projectId, characterId)
export async function deleteCharacter(_projectId: number, characterId: number): Promise<boolean> {
    await apiClient.delete<ApiResponse<void>>(`${OBJECT_URL}/${characterId}`)
    return true
}

// AI로 오브젝트 생성
// 참고: mock 서비스의 generateCharacterWithAI에 대응
export async function generateCharacterWithAI(
    projectId: number,
    request: any
): Promise<ObjectSheet> {
    // 생성 엔드포인트(POST /api/projects/{id}/objects)와 동일하거나 별도의 AI 생성 엔드포인트 사용
    // 문서상 POST 요청 시 Job이 수락된다고 되어 있음
    return createCharacter(projectId, {
        name: request.name,
        description: request.description,
        style: request.style,
        type: 'CHARACTER'
    })
}
