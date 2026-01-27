<script setup lang="ts">
import { computed, ref } from 'vue'
import { useUIStore } from '../../stores/ui'
import { useProjectStore } from '../../stores/project'
import { inviteMember } from '../../services/api/projects'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'

const MODAL_ID = 'share-project'

interface Props {
  projectId: number | null
}

const props = defineProps<Props>()
const uiStore = useUIStore()
const projectStore = useProjectStore()

const inviteEmail = ref('')
const isSending = ref(false)

const shareUrl = computed(() => {
  if (!props.projectId) return ''
  return `${window.location.origin}/projects/${props.projectId}`
})

async function handleCopy() {
  if (!shareUrl.value) return
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    uiStore.showToast({ type: 'success', title: 'Link copied', message: 'Share link copied.' })
  } catch {
    uiStore.showToast({ type: 'error', title: 'Copy failed', message: 'Please copy manually.' })
  }
}

async function handleInvite() {
  if (!props.projectId || !inviteEmail.value.trim()) return
  isSending.value = true
  try {
    await inviteMember(props.projectId, inviteEmail.value.trim())
    uiStore.showToast({ type: 'success', title: 'Invite sent', message: 'Member invited.' })
    inviteEmail.value = ''
    await projectStore.loadProject(props.projectId)
  } catch {
    uiStore.showToast({ type: 'error', title: 'Invite failed', message: 'Could not invite member.' })
  } finally {
    isSending.value = false
  }
}
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="Share Project" size="md">
    <div class="share-modal">
      <div class="section">
        <div class="section-title">Share Link</div>
        <div class="share-row">
          <input class="share-input" :value="shareUrl" readonly />
          <Button variant="secondary" @click="handleCopy">Copy</Button>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Invite by Email</div>
        <div class="share-row">
          <input
            v-model="inviteEmail"
            class="share-input"
            type="email"
            placeholder="user@example.com"
          />
          <Button variant="primary" :disabled="isSending" @click="handleInvite">
            {{ isSending ? 'Sending...' : 'Send' }}
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
  gap: 1rem;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.share-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.share-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>
