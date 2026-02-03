export type InviteStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED'

export interface ProjectInvite {
  inviteId: number
  projectId: number
  projectTitle: string
  projectGenre?: string
  projectThumbnailUrl?: string
  senderName?: string
  senderEmail?: string
  receiverEmail: string
  createdAt: string
  status: InviteStatus
}
