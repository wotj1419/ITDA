import apiClient from './client'
import type { Project, ProjectDetail, CreateProjectRequest, ApiResponse } from '../../types'

export async function fetchProjects(): Promise<Project[]> {
    const response = await apiClient.get<ApiResponse<Project[]>>('/projects')
    return response.data.data || []
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
    const response = await apiClient.patch<ApiResponse<Project>>(`/projects/${projectId}`, data)
    return response.data.data || null
}

export async function getProjectProgress(projectId: number): Promise<{ completed: number; total: number }> {
    // Assuming backend provides a specific endpoint for progress, 
    // or it returns it as part of project details. 
    // For this implementation, let's request a specific endpoint.
    const response = await apiClient.get<ApiResponse<{ completed: number; total: number }>>(`/projects/${projectId}/progress`)
    return response.data.data || { completed: 0, total: 0 }
}
