<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCollabStore } from '../../stores/collab'
import AvatarGroup from '../common/AvatarGroup.vue'
import type { CollabParticipant } from '../../types/ui/collab'

const collabStore = useCollabStore()
const router = useRouter()
const route = useRoute()

const projectId = computed(() => {
  const id = route.params.projectId ?? route.params.id
  return id ? Number(id) : null
})

const presenceList = computed(() => {
  const map = new Map<string, CollabParticipant & { isMe?: boolean }>()
  const local = collabStore.localParticipant
  if (local?.odps) {
    map.set(local.odps, { ...local, isMe: true })
  }
  collabStore.participants.forEach((p) => {
    if (!map.has(p.odps)) {
      map.set(p.odps, { ...p, isMe: false })
    }
  })
  return Array.from(map.values())
})

const avatars = computed(() =>
  presenceList.value.map((p) => ({
    src: p.avatarUrl || '',
    fallback: p.name?.[0]?.toUpperCase() || '?',
    alt: p.name,
    title: `${p.name || 'Guest'}: ${locationLabel(p)}`,
    onClick: p.isMe ? undefined : () => followMember(p),
  }))
)

const isConnected = computed(() => collabStore.isConnected)

const locationLabel = (member: CollabParticipant) => {
  const location = member.currentLocation
  switch (location) {
    case 'PROJECT_LIST':
      return '프로젝트 목록'
    case 'SCENE_LIST':
      return '씬 목록'
    case 'SCENE_EDIT':
      return member.sceneId ? `Scene ${member.sceneId} 편집 중` : '씬 편집 중'
    case 'TIMELINE':
      return '타임라인'
    default:
      return 'Online'
  }
}

const followMember = (member: CollabParticipant) => {
  const pid = projectId.value
  if (!pid) return

  switch (member.currentLocation) {
    case 'PROJECT_LIST':
      router.push({ name: 'dashboard' })
      break
    case 'SCENE_EDIT':
      if (member.sceneId) {
        router.push({ name: 'scene-edit', params: { projectId: pid, sceneId: member.sceneId } })
      } else {
        router.push({ name: 'project-detail', params: { id: pid } })
      }
      break
    case 'TIMELINE':
      router.push({ name: 'timeline', params: { id: pid } })
      break
    case 'SCENE_LIST':
    default:
      router.push({ name: 'project-detail', params: { id: pid } })
      break
  }
}
</script>

<template>
  <div class="presence">
    <div class="section-label">ONLINE NOW</div>
    <div class="online-users">
      <AvatarGroup :avatars="avatars" :max="3" size="sm" />
    </div>

    <div v-if="!isConnected" class="presence-empty">Not connected</div>
    <div v-else-if="presenceList.length === 0" class="presence-empty">No one online</div>
    <div v-else class="presence-list">
      <div
        v-for="member in presenceList"
        :key="member.odps"
        class="presence-item"
      >
        <span class="presence-name">
          {{ member.name || 'Guest' }}
          <span v-if="member.isMe" class="presence-me">(ME)</span>
        </span>
        <span class="presence-location">
          {{ locationLabel(member) }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.presence {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.section-label {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.25rem;
}

.online-users {
  margin-bottom: 0.25rem;
}

.presence-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.presence-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-600);
}

.presence-name {
  font-weight: 600;
  color: var(--gray-800);
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.presence-me {
  font-weight: 600;
  color: var(--rose-500);
  font-size: 0.7rem;
}

.presence-location {
  color: var(--gray-500);
  font-size: 0.7rem;
  white-space: nowrap;
}

.presence-empty {
  font-size: 0.75rem;
  color: var(--gray-400);
}
</style>
