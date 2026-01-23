import apiClient from './client'
import type { ApiResponse } from '../../types'

export interface PresignedUrlResponse {
    uploadUrl: string
    fileId: string
    key: string
}

export interface CompleteUploadRequest {
    fileId: string
    key: string // specific to S3/Cloud storage usually
}

export interface FileInfo {
    fileId: string
    url: string
    name: string
    type: string
}

export async function getPresignedUrl(filename: string, fileType: string): Promise<PresignedUrlResponse> {
    const response = await apiClient.post<ApiResponse<PresignedUrlResponse>>('/files/presign', { filename, fileType })
    if (!response.data.data) throw new Error('Failed to get presigned URL')
    return response.data.data
}

export async function completeUpload(data: CompleteUploadRequest): Promise<void> {
    await apiClient.post('/files/complete', data)
}

export async function getFileInfo(fileId: string): Promise<FileInfo> {
    const response = await apiClient.get<ApiResponse<FileInfo>>(`/files/${fileId}`)
    if (!response.data.data) throw new Error('File not found')
    return response.data.data
}
