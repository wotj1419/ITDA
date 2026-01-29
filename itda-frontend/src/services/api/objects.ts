import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { CreateObjectRequest, ObjectSheet, UpdateObjectRequest } from '../../types/api/objects'

const BASE_URL = '/projects'
const OBJECT_URL = '/objects'

export async function fetchObjectsByProjectId(projectId: number): Promise<ObjectSheet[]> {
    const response = await apiClient.get<ApiResponse<ObjectSheet[]>>(`${BASE_URL}/${projectId}/objects`)
    return response.data.data || []
}

export async function createObject(
    projectId: number,
    data: CreateObjectRequest,
    file: File
): Promise<ObjectSheet> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('name', data.name)
    formData.append('type', data.type)
    formData.append('description', data.description)
    if (data.style) {
        formData.append('style', data.style)
    }

    const response = await apiClient.post<ApiResponse<ObjectSheet>>(
        `${BASE_URL}/${projectId}/objects`,
        formData
    )
    if (!response.data.data) {
        throw new Error('Failed to create object')
    }
    return response.data.data
}

export async function fetchObjectById(objectId: number): Promise<ObjectSheet> {
    const response = await apiClient.get<ApiResponse<ObjectSheet>>(`${OBJECT_URL}/${objectId}`)
    if (!response.data.data) {
        throw new Error('Object not found')
    }
    return response.data.data
}

export async function updateObject(
    objectId: number,
    data: UpdateObjectRequest
): Promise<ObjectSheet | null> {
    const response = await apiClient.put<ApiResponse<ObjectSheet>>(`${OBJECT_URL}/${objectId}`, data)
    return response.data.data || null
}

export async function replaceObjectImage(objectId: number, file: File): Promise<ObjectSheet> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.patch<ApiResponse<ObjectSheet>>(
        `${OBJECT_URL}/${objectId}/image`,
        formData
    )
    if (!response.data.data) {
        throw new Error('Failed to replace object image')
    }
    return response.data.data
}

export async function downloadObjectImage(objectId: number): Promise<Blob> {
    const response = await apiClient.get(`${OBJECT_URL}/${objectId}/image`, { responseType: 'blob' })
    return response.data
}

export async function deleteObject(objectId: number): Promise<boolean> {
    await apiClient.delete<ApiResponse<void>>(`${OBJECT_URL}/${objectId}`)
    return true
}
