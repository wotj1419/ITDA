import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ProjectInvite } from '../types/api/invites'
import {
  acceptProjectInvite,
  createProjectInvite,
  declineProjectInvite,
  fetchProjectInvites,
} from '../services/api/invites'

export const useInviteStore = defineStore('invites', () => {
  const invites = ref<ProjectInvite[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const pendingInvites = computed(() =>
    invites.value.filter((invite) => invite.status === 'PENDING')
  )

  const acceptedInvites = computed(() =>
    invites.value.filter((invite) => invite.status === 'ACCEPTED')
  )

  const unreadCount = computed(() => pendingInvites.value.length)

  const loadInvites = async () => {
    isLoading.value = true
    error.value = null
    try {
      invites.value = await fetchProjectInvites()
    } catch (err) {
      console.error('Failed to load invites', err)
      error.value = 'Failed to load invites'
      invites.value = []
    } finally {
      isLoading.value = false
    }
  }

  const sendInvite = async (payload: { projectId: number; email: string; role: 'ADMIN' | 'EDITOR' | 'VIEWER' }) => {
    const invite = await createProjectInvite(payload.projectId, payload.email, payload.role)
    invites.value = [invite, ...invites.value]
    return invite
  }

  const acceptInvite = async (inviteId: number) => {
    await acceptProjectInvite(inviteId)
    invites.value = invites.value.map((invite) =>
      invite.inviteId === inviteId ? { ...invite, status: 'ACCEPTED' } : invite
    )
  }

  const declineInvite = async (inviteId: number) => {
    await declineProjectInvite(inviteId)
    invites.value = invites.value.map((invite) =>
      invite.inviteId === inviteId ? { ...invite, status: 'DECLINED' } : invite
    )
  }

  return {
    invites,
    pendingInvites,
    acceptedInvites,
    unreadCount,
    isLoading,
    error,
    loadInvites,
    sendInvite,
    acceptInvite,
    declineInvite,
  }
})
