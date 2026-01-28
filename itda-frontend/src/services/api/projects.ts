import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { Project, ProjectDetail, CreateProjectRequest } from '../../types/api/projects'

type ProjectListResponse = {
    items: Project[];
    page: number;
    size: number;
    total: number;
};

// 프로젝트 목록 조회
export async function fetchProjects(): Promise<Project[]> {
    const response = await apiClient.get<ApiResponse<Project[] | ProjectListResponse>>('/projects')
    const data = response.data.data
    if (!data) return []
    if (Array.isArray(data)) return data
    return data.items || []
}

// 프로젝트 상세 조회
export async function fetchProjectById(projectId: number): Promise<ProjectDetail | null> {
    const response = await apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${projectId}`)
    return response.data.data || null
}

// 프로젝트 생성
export async function createProject(data: CreateProjectRequest): Promise<Project> {
    const response = await apiClient.post<ApiResponse<Project>>('/projects', data)
    if (!response.data.data) {
        throw new Error('createProject에서 데이터를 받지 못했습니다.')
    }
    return response.data.data
}

// 프로젝트 삭제
export async function deleteProject(projectId: number): Promise<void> {
    await apiClient.delete(`/projects/${projectId}`)
}

// 프로젝트 수정
export async function updateProject(projectId: number, data: Partial<Project>): Promise<Project | null> {
    const response = await apiClient.put<ApiResponse<Project>>(`/projects/${projectId}`, data)
    return response.data.data || null
}

// 프로젝트 진행률 조회
export async function getProjectProgress(projectId: number): Promise<{ completed: number; total: number }> {
    // 백엔드에서 진행률 전용 엔드포인트를 제공하거나 프로젝트 상세 정보에 포함됨을 가정
    // 현재 구현에서는 별도의 진행률 엔드포인트를 요청함
    const response = await apiClient.get<ApiResponse<{ completed: number; total: number }>>(`/projects/${projectId}/progress`)
    return response.data.data || { completed: 0, total: 0 }
}

// ============================================================================
// Members
// ============================================================================

export interface ProjectMember {
    memberId: number
    userId: number
    name: string
    role: 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER'
}

export async function fetchProjectMembers(projectId: number): Promise<ProjectMember[]> {
    const response = await apiClient.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`)
    return response.data.data || []
}

export async function inviteMember(projectId: number, email: string, role: 'ADMIN' | 'EDITOR' | 'VIEWER'): Promise<void> {
    await apiClient.post(`/projects/${projectId}/members`, { email, role })
}

export async function updateMemberRole(projectId: number, memberId: number, role: string): Promise<void> {
    await apiClient.patch(`/projects/${projectId}/members/${memberId}`, { role })
}

export async function removeMember(projectId: number, userId: number): Promise<void> {
    await apiClient.delete(`/projects/${projectId}/members/${userId}`)
}

// ============================================================================
// Object Sheets
// ============================================================================

export interface ObjectSheet {
    objectId: number
    name: string
    description: string
    imageUrl?: string
}

export async function fetchProjectObjects(projectId: number): Promise<ObjectSheet[]> {
    const response = await apiClient.get<ApiResponse<ObjectSheet[]>>(`/projects/${projectId}/objects`)
    return response.data.data || []
}

export async function createProjectObject(projectId: number, data: { name: string; description: string }): Promise<ObjectSheet> {
    const response = await apiClient.post<ApiResponse<ObjectSheet>>(`/projects/${projectId}/objects`, data)
    if (!response.data.data) throw new Error('Failed to create object')
    return response.data.data
}

export async function fetchObjectById(objectId: number): Promise<ObjectSheet> {
    const response = await apiClient.get<ApiResponse<ObjectSheet>>(`/objects/${objectId}`)
    if (!response.data.data) throw new Error('Object not found')
    return response.data.data
}
