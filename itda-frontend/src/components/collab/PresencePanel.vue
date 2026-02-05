<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCollabStore } from '../../stores/collab'
import { useSceneStore } from '../../stores/scene'
import Avatar from '../common/Avatar.vue'
import type { CollabParticipant } from '../../types/ui/collab'

const collabStore = useCollabStore()
const sceneStore = useSceneStore()
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

const isConnected = computed(() => collabStore.isConnected)

const sceneLabelById = computed(() => {
  const map = new Map<number, string>()
  sceneStore.scenes.forEach((scene) => {
    const order = scene.order ?? scene.sceneId
    const title = scene.title || ''
    const label = title ? `씬 ${order}: ${title}` : `씬 ${order}`
    map.set(scene.sceneId, label)
  })
  return map
})


const locationLabel = (member: CollabParticipant) => {
  const location = member.currentLocation
  switch (location) {
    case 'PROJECT_LIST':
      return '프로젝트 목록'
    case 'SCENE_LIST':
      return '씬 목록'
    case 'SCENE_EDIT':
      if (member.sceneId) {
        const label = sceneLabelById.value.get(member.sceneId)
        return label ? `${label} 편집 중` : `Scene ${member.sceneId} 편집 중`
      }
      return '씬 편집 중'
    case 'TIMELINE':
      if (member.sceneId) {
        const label = sceneLabelById.value.get(member.sceneId)
        return label ? `${label} 타임라인` : `Scene ${member.sceneId} 타임라인`
      }
      return '전체 타임라인'
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
      if (member.sceneId) {
        router.push({ name: 'scene-timeline', params: { id: pid, sceneId: member.sceneId } })
      } else {
        router.push({ name: 'timeline', params: { id: pid } })
      }
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
    <div class="section-label">현재 접속 중</div>

    <div v-if="!isConnected" class="presence-empty">Not connected</div>
    <div v-else-if="presenceList.length === 0" class="presence-empty">No one online</div>
    <div v-else class="presence-list">
      <div
        v-for="member in presenceList"
        :key="member.odps"
        class="presence-item"
      >
        <button
          class="presence-avatar-btn"
          type="button"
          :disabled="member.isMe"
          :aria-label="member.isMe ? `${member.name || 'Guest'} (ME)` : `Follow ${member.name || 'Guest'}`"
          @click="!member.isMe && followMember(member)"
        >
          <Avatar
            class="presence-avatar"
            :src="member.avatarUrl || ''"
            :alt="member.name || 'Guest'"
            :user-id="parseInt(member.odps, 10) || 0"
            size="sm"
          />
        </button>
        <div class="presence-text">
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
  </div>
</template>

<style scoped>
.presence {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.section-label {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.25rem;
}

.presence-list {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  max-height: 180px;
  overflow-y: auto;
}

.presence-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 0.5rem;
  padding: 0.15rem 0;
  font-size: 0.75rem;
  color: var(--gray-600);
}

.presence-avatar-btn {
  border: none;
  background: transparent;
  padding: 2px;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: visible;
  position: relative;
}

.presence-avatar-btn:disabled {
  cursor: default;
}

.presence-avatar-btn:not(:disabled):hover .presence-avatar {
  box-shadow: 0 0 0 2px rgba(255, 133, 161, 0.35);
}

.presence-avatar-btn:focus-visible .presence-avatar {
  outline: 2px solid rgba(255, 133, 161, 0.5);
  outline-offset: 2px;
}


.presence-text {
  display: flex;
  flex-direction: column;
  gap: 0.05rem;
  min-width: 0;
}

.presence-name {
  font-weight: 600;
  color: var(--gray-800);
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  min-width: 0;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.72rem;
}

.presence-me {
  font-weight: 600;
  color: var(--rose-500);
  font-size: 0.7rem;
}

.presence-location {
  color: var(--gray-500);
  font-size: 0.65rem;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
}

.presence-empty {
  font-size: 0.75rem;
  color: var(--gray-400);
}
</style>
