<script setup lang="ts">
import { computed } from 'vue'
import { useUIStore } from '../../stores/ui'
import ModalBase from '../common/ModalBase.vue'
import Button from '../common/Button.vue'

const MODAL_ID = 'node-delete-confirm'

const props = defineProps<{
  nodeId: string | null
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const uiStore = useUIStore()

const canConfirm = computed(() => Boolean(props.nodeId))

const handleConfirm = () => {
  emit('confirm')
  uiStore.closeModal()
}

const handleCancel = () => {
  emit('cancel')
  uiStore.closeModal()
}

const handleClose = () => {
  emit('cancel')
}
</script>

<template>
  <ModalBase :modal-id="MODAL_ID" title="삭제 경고" @close="handleClose">
    <p class="delete-warning">
      이 노드를 삭제하면 하위 노드들도 함께 삭제됩니다. 계속 하시겠습니까?
    </p>

    <template #footer>
      <Button variant="secondary" @click="handleCancel">
        취소
      </Button>
      <Button variant="danger" :disabled="!canConfirm" @click="handleConfirm">
        삭제
      </Button>
    </template>
  </ModalBase>
</template>

<style scoped>
.delete-warning {
  font-size: 0.9375rem;
  color: var(--gray-700);
  line-height: 1.5;
}
</style>
