import apiClient from './client'
import type { Project, ProjectDetail, CreateProjectRequest, ApiResponse } from '../../types'

type ProjectListResponse = {
    items: Project[];
    page: number;
    size: number;
    total: number;
};

export async function fetchProjects(): Promise<Project[]> {
    const response = await apiClient.get<ApiResponse<Project[] | ProjectListResponse>>('/projects')
    const data = response.data.data
    if (!data) return []
    if (Array.isArray(data)) return data
    return data.items || []
}

export async function fetchProjectById(projectId: number): Promise<ProjectDetail | null> {
    const response = await apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${projectId}`)
    return response.data.data || null
}

export async function createProject(data: CreateProjectRequest): Promise<Project> {
    const response = await apiClient.post<ApiResponse<Project>>('/projects', data)
    if (!response.data.data) {
        throw new Error('No data received from createProject')
    }
    return response.data.data
}

export async function deleteProject(projectId: number): Promise<void> {
    await apiClient.delete(`/projects/${projectId}`)
}

export async function updateProject(projectId: number, data: Partial<Project>): Promise<Project | null> {
    const response = await apiClient.put<ApiResponse<Project>>(`/projects/${projectId}`, data)
    return response.data.data || null
}

export async function getProjectProgress(projectId: number): Promise<{ completed: number; total: number }> {
    // Assuming backend provides a specific endpoint for progress, 
    // or it returns it as part of project details. 
    // For this implementation, let's request a specific endpoint.
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
    role: 'OWNER' | 'EDITOR' | 'VIEWER'
}

export async function fetchProjectMembers(projectId: number): Promise<ProjectMember[]> {
    const response = await apiClient.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`)
    return response.data.data || []
}

export async function inviteMember(projectId: number, email: string): Promise<void> {
    await apiClient.post(`/projects/${projectId}/members`, { email })
}

export async function updateMemberRole(projectId: number, memberId: number, role: string): Promise<void> {
    await apiClient.patch(`/projects/${projectId}/members/${memberId}`, { role })
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
