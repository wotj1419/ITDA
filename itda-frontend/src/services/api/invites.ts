import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'
import type { ProjectInvite } from '../../types/api/invites'

// NOTE: API 스펙 확정 전 임시 엔드포인트.
// 백엔드 확정 시 경로/필드만 맞추면 바로 연결됩니다.

export async function fetchProjectInvites(): Promise<ProjectInvite[]> {
  const response = await apiClient.get<ApiResponse<ProjectInvite[]>>('/invites')
  return response.data.data || []
}

export async function createProjectInvite(
  projectId: number,
  email: string,
  role: 'ADMIN' | 'EDITOR' | 'VIEWER'
): Promise<ProjectInvite> {
  const response = await apiClient.post<ApiResponse<ProjectInvite>>(`/projects/${projectId}/invites`, {
    email,
    role,
  })
  if (!response.data.data) {
    throw new Error('Failed to create invite')
  }
  return response.data.data
}

export async function acceptProjectInvite(inviteId: number): Promise<void> {
  await apiClient.post(`/invites/${inviteId}/accept`)
}

export async function declineProjectInvite(inviteId: number): Promise<void> {
  await apiClient.post(`/invites/${inviteId}/decline`)
}
