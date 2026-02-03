<script setup lang="ts">
import { computed, ref } from 'vue'
import { useUIStore } from '../../stores/ui'
import { useProjectStore } from '../../stores/project'
import { useInviteStore } from '../../stores/invites'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'
import CustomSelect from '../common/CustomSelect.vue'

const MODAL_ID = 'share-project'

interface Props {
  projectId: number | null
}

import { useAuthStore } from '../../stores/auth'

const props = defineProps<Props>()
const uiStore = useUIStore()
const projectStore = useProjectStore()
const inviteStore = useInviteStore()
const authStore = useAuthStore()

const inviteEmail = ref('')
const inviteRole = ref<'admin' | 'editor' | 'viewer'>('admin')
const isSending = ref(false)



const inviteOptions = [
  { label: '전체 허용', value: 'admin' },
  { label: '편집 허용', value: 'editor' },
  { label: '읽기 허용', value: 'viewer' },
]

import { watch } from 'vue'

watch(
  () => uiStore.activeModal,
  async (newId) => {
    if (newId === MODAL_ID) {
      inviteEmail.value = ''
      inviteRole.value = 'admin'
      if (props.projectId) {
        await projectStore.loadProjectMembers(props.projectId)
      }
    }
  }
)

async function handleInvite() {
  if (!props.projectId || !inviteEmail.value.trim()) return
  
  // 1. Email Format Validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(inviteEmail.value.trim())) {
    uiStore.showToast({ 
      type: 'warning', 
      title: '입력 오류', 
      message: '유효한 이메일 주소를 입력해주세요.' 
    })
    return
  }

  isSending.value = true
  try {
    const roleUpper = inviteRole.value.toUpperCase() as 'ADMIN' | 'EDITOR' | 'VIEWER'
    await inviteStore.sendInvite({
      projectId: props.projectId,
      email: inviteEmail.value.trim(),
      role: roleUpper,
    })

    uiStore.showToast({
      type: 'success',
      title: '초대 요청 전송',
      message: '상대의 알림에서 수락해야만 프로젝트에 추가됩니다.',
    })
    inviteEmail.value = ''
  } catch (err: any) {
    console.error(err)
    const status = err.response?.status
    const code = err.response?.data?.code
    if (status === 404 || code === 'USER_NOT_FOUND') {
      uiStore.showToast({ 
        type: 'error', 
        title: '사용자 없음', 
        message: '가입되지 않은 이메일입니다.' 
      })
    } else if (status === 409 || code === 'INVITE_ALREADY_EXISTS') {
      uiStore.showToast({ 
        type: 'warning', 
        title: '이미 초대함', 
        message: '해당 이메일로 보낸 초대가 이미 존재합니다.' 
      })
    } else {
      uiStore.showToast({
        type: 'error',
        title: '초대 요청 실패',
        message: '초대 요청을 전송하지 못했습니다.',
      })
    }
  } finally {
    isSending.value = false
  }
}

const members = computed(() => projectStore.currentProject?.members || [])

const normalizedMembers = computed(() => {
  const currentUser = authStore.user
  const currentUserId = currentUser?.id 
  
  // 1. Existing members mapping
  let list = members.value.map((member) => {
    const isMe = member.userId === currentUserId
    // Backend returns 'OWNER'/'EDITOR'/'VIEWER' uppercase, convert to lowercase for UI logic if needed, 
    // but the select values are 'owner'/'editor'/'viewer' lowercase.
    // The role from backend is string, so we lowercase it.
    const roleLower = (member.role?.toLowerCase() || 'viewer') as 'owner' | 'admin' | 'editor' | 'viewer'

    return {
      userId: member.userId,
      name: isMe ? `${member.name} (나)` : (member.name || member.email || 'User'),
      email: member.email,
      role: roleLower,
    }
  })

  // 2. If current user is logged in BUT not in the list (and is owner), force add them...
  if (currentUser && currentUserId && !list.find(m => m.userId === currentUserId)) {
     const isOwner = projectStore.currentProject?.ownerId === currentUserId
     // If I'm owner, show 'owner', otherwise default to 'editor'
     const myRole = isOwner ? 'owner' : 'editor' 
     
     list.unshift({
       userId: currentUserId,
       name: `${currentUser.name} (나)`,
       email: currentUser.email,
       role: myRole,
     })
  }
  
  return list
})

const currentUserRole = computed(() => {
  const me = normalizedMembers.value.find((m) => m.userId === authStore.user?.id)
  return me?.role ?? 'viewer'
})

const isOwnerUser = computed(() => currentUserRole.value === 'owner')
const isAdminUser = computed(() => currentUserRole.value === 'admin')

const canManageRoles = computed(() => {
  const currentUserId = authStore.user?.id
  if (!currentUserId) return false
  const ownerId = projectStore.currentProject?.ownerId
  if (ownerId === currentUserId) return true
  return ['owner', 'admin'].includes(currentUserRole.value)
})

const roleLabelMap: Record<'owner' | 'admin' | 'editor' | 'viewer', string> = {
  owner: '전체 허용',
  admin: '전체 허용',
  editor: '편집 허용',
  viewer: '읽기 허용',
}

const removeTitle = '멤버 제거'

async function updateMemberRole(userId: number, role: 'owner' | 'admin' | 'editor' | 'viewer') {
  if (!props.projectId) return
  if (isAdminUser.value && (role === 'admin' || role === 'owner')) return
  // Convert UI role (lowercase) to API role (uppercase)
  // 'owner' cannot be set via this API usually, only ADMIN/EDITOR/VIEWER changes for members.
  // Assuming frontend prevents changing TO owner via disabled option/logic.
  const apiRole = role.toUpperCase() as 'ADMIN' | 'EDITOR' | 'VIEWER'
  
  try {
    await projectStore.updateMemberRole(props.projectId, userId, apiRole)
    uiStore.showToast({
      type: 'success',
      title: '권한 변경',
      message: '권한이 변경되었습니다.',
    })
  } catch (err) {
    uiStore.showToast({
      type: 'error',
      title: '권한 변경 실패',
      message: '권한을 변경하지 못했습니다.',
    })
  }
}

const canEditMemberRole = (member: { userId: number; role: 'owner' | 'admin' | 'editor' | 'viewer' }) => {
  if (!canManageRoles.value) return false
  if (member.role === 'owner') return false
  if (member.userId === authStore.user?.id) return false
  if (isOwnerUser.value) return true
  if (isAdminUser.value) return ['editor', 'viewer'].includes(member.role)
  return false
}

const canRemoveMember = (member: { userId: number; role: 'owner' | 'admin' | 'editor' | 'viewer' }) => {
  if (!canManageRoles.value) return false
  if (member.role === 'owner') return false
  if (member.userId === authStore.user?.id) return false
  if (isOwnerUser.value) return true
  if (isAdminUser.value) return ['editor', 'viewer'].includes(member.role)
  return false
}

const roleOptionsForMember = () => {
  if (isAdminUser.value) {
    return inviteOptions.filter((option) => option.value !== 'admin')
  }
  return inviteOptions
}

async function removeMember(userId: number) {
  if (!props.projectId) return
  
  // 멤버 이름 가져오기
  const member = normalizedMembers.value.find(m => m.userId === userId)
  const memberName = member?.name || '멤버'

  try {
    await projectStore.removeMember(props.projectId, userId)
    uiStore.showToast({
      type: 'success',
      title: '멤버 제외 완료',
      message: `${memberName}님을 프로젝트에서 내보냈습니다.`,
    })
  } catch (err) {
    uiStore.showToast({
      type: 'error',
      title: '멤버 제외 실패',
      message: `${memberName}님을 내보내지 못했습니다. 다시 시도해주세요.`,
    })
  }
}
// 귀여운 동물 이모지 목록
const AVATAR_EMOJIS = [
  '🐱', '🐶', '🐰', '🦊', '🐻', '🐼', '🐨', '🦁',
  '🐯', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🦄',
  '🐹', '🐝', '🦋', '🐢', '🐙', '🦀', '🐳', '🦩',
]

// userId 기반으로 이모지 선택
function getEmoji(userId?: number, name?: string): string {
  if (userId !== undefined && userId > 0) {
    const index = (userId - 1) % AVATAR_EMOJIS.length
    return AVATAR_EMOJIS[index] ?? '🐱'
  }
  if (!name) return AVATAR_EMOJIS[0] ?? '🐱'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i)
    hash = hash & hash
  }
  const index = Math.abs(hash) % AVATAR_EMOJIS.length
  return AVATAR_EMOJIS[index] ?? '🐱'
}

const getMemberEmoji = (userId: number, name?: string) => getEmoji(userId, name)
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="프로젝트 공유" size="md">
    <div class="share-modal">
      <div class="section">
        <div class="section-title">접근 권한이 있는 사용자</div>
        <div class="member-list member-list--large">
          <div v-if="normalizedMembers.length === 0" class="empty-row">
            아직 참여자가 없습니다.
          </div>
          <div v-for="member in normalizedMembers" :key="member.userId" class="member-row member-row--large">
            <div class="member-avatar member-avatar--large">
              <span class="avatar-emoji">{{ getMemberEmoji(member.userId, member.name) }}</span>
            </div>
            <div class="member-info">
              <div class="member-name member-name--large">{{ member.name }}</div>
              <div class="member-email member-email--large">{{ member.email }}</div>
            </div>
            <div class="member-role">
              <CustomSelect
                v-if="canEditMemberRole(member)"
                :model-value="member.role"
                :options="roleOptionsForMember()"
                class="member-role-select"
                @update:model-value="(val) => updateMemberRole(member.userId, val as any)"
              />
              <span v-else class="role-badge">{{ roleLabelMap[member.role] || roleLabelMap.viewer }}</span>
              
              <button 
                v-if="canRemoveMember(member)"
                class="remove-btn"
                :title="removeTitle"
                @click="removeMember(member.userId)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">이메일로 초대하기</div>
        <div class="invite-form">
          <input 
            v-model="inviteEmail" 
            class="invite-input" 
            placeholder="your@email.com"
            @keyup.enter="handleInvite"
          />
          <CustomSelect 
            v-model="inviteRole" 
            :options="inviteOptions"
            class="custom-select-override"
          />
          <Button :disabled="isSending || !inviteEmail" @click="handleInvite">
            {{ isSending ? '보내는 중...' : '초대' }}
          </Button>
        </div>
      </div>
    </div>
  </ModalBase>
</template>

<style scoped>
.share-modal {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
  letter-spacing: 0.02em;
}

.share-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.share-input {
  flex: 1;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 12px;
  font-size: 0.875rem;
  background: white;
  box-shadow: inset 0 0 0 1px rgba(255, 133, 161, 0.05);
}

.role-pill {
  background: var(--rose-50);
  border: 1px solid var(--rose-200);
  border-radius: 999px;
  padding: 0 0.25rem;
}

.role-select {
  border: none;
  background: transparent;
  padding: 0.45rem 0.75rem;
  font-size: 0.8rem;
  color: var(--rose-600);
  font-weight: 600;
  appearance: none;
  cursor: pointer;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border: 1px solid var(--rose-100);
  border-radius: 14px;
  padding: 0.75rem;
  background: white;
  /* Add min-height to balance the list if it has few items */
  min-height: 200px;
  /* 4명까지 보이고 그 이상은 스크롤 */
  max-height: 320px;
  overflow-y: auto;
  /* Custom Scrollbar for Webkit */
  scrollbar-width: thin;
  scrollbar-color: var(--rose-200) transparent;
}

.member-list::-webkit-scrollbar {
  width: 6px;
}

.member-list::-webkit-scrollbar-track {
  background: transparent;
}

.member-list::-webkit-scrollbar-thumb {
  background-color: var(--rose-200);
  border-radius: 20px;
}

.member-list--large {
  padding: 0.75rem;
  gap: 0.5rem;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.member-row--large {
  padding: 0.5rem;
  gap: 0.75rem;
}

.member-row:hover {
  background: var(--rose-50);
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--rose-100);
  color: var(--rose-600);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  text-transform: uppercase;
}

.member-avatar--large {
  width: 40px; /* Reduced from 44px */
  height: 40px;
  font-size: 0.95rem; /* Slightly smaller font */
}

.avatar-emoji {
  font-size: 1.2rem;
  line-height: 1;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-900);
}

.member-name--large {
  font-size: 0.9rem; /* Reduced from 0.95rem */
}

.member-email {
  font-size: 0.75rem;
  color: var(--gray-400);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-email--large {
  font-size: 0.75rem; /* Reduced from 0.8rem */
}

.member-role {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.role-select--large {
  font-size: 0.8rem;
  padding: 0.35rem 0.75rem; /* Reduced padding */
  background: var(--rose-50);
  border-radius: 8px;
  border: 1px solid var(--rose-200);
  text-align: center;
  text-align-last: center;
}

.remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 0.25rem;
}

.remove-btn:hover {
  background: var(--rose-50);
  color: var(--rose-500);
}

.invite-form {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.invite-input {
  flex: 1;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.2s;
}

.invite-input:focus {
  border-color: var(--rose-400);
}

.invite-role-select {
  padding: 0.6rem 2rem 0.6rem 1rem; /* Extra padding for arrow */
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
  background-color: white;
  color: var(--gray-700);
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%23fb7185' stroke-width='2'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
  background-size: 1rem;
}

.empty-row {
  font-size: 0.75rem;
  color: var(--gray-400);
  padding: 0.5rem 0;
  text-align: center;
}

.role-badge {
  display: inline-block;
  padding: 0.6rem 0.75rem;
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: 8px;
  border: 1px solid var(--rose-200);
}

.custom-select-override {
  width: 120px !important;
}

.member-role-select {
  width: 100px !important;
}
</style>
