import apiClient from './client'
import type { ApiResponse } from '../../types/api/common'

export interface ChatMessageSender {
  userId: number
  name: string
  profileImageUrl?: string | null
}

export interface ChatMessage {
  messageId: number
  sender: ChatMessageSender
  content: string
  type: 'TEXT' | string
  createdAt: string
}

export interface ChatMessagePage {
  items: ChatMessage[]
  hasMore: boolean
}

export async function fetchChatMessages(projectId: number, size = 50, before?: number): Promise<ChatMessagePage> {
  const params: Record<string, string | number> = { size }
  if (before) params.before = before
  const response = await apiClient.get<ApiResponse<ChatMessagePage>>(`/projects/${projectId}/chat/messages`, { params })
  return response.data.data || { items: [], hasMore: false }
}
