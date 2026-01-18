<script setup lang="ts">
/**
 * NodePanelContainer - 동적 패널 라우팅 컨테이너
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.1
 */
import { computed } from 'vue';
import { panelRegistry } from './index';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import type { AnyNodeData } from '../../../types/node';
import { X } from 'lucide-vue-next';

// =============================================================================
// Store
// =============================================================================

const nodeStore = useSceneNodeStore();

// =============================================================================
// Computed
// =============================================================================

const selectedNode = computed(() => nodeStore.selectedNode);

const panelComponent = computed(() => {
  if (!selectedNode.value) return null;
  const nodeType = (selectedNode.value.data as AnyNodeData).type;
  return panelRegistry[nodeType] || null;
});

// =============================================================================
// Methods
// =============================================================================

function handleClose(): void {
  nodeStore.selectNode(null);
}
</script>

<template>
  <aside v-if="selectedNode" class="node-panel">
    <div class="node-panel__header">
      <button class="node-panel__close" @click="handleClose" title="닫기">
        <X class="node-panel__close-icon" />
      </button>
    </div>
    
    <component
      v-if="panelComponent"
      :is="panelComponent"
      :node="selectedNode"
      @close="handleClose"
    />
    
    <div v-else class="node-panel__empty">
      <p>패널을 찾을 수 없습니다.</p>
    </div>
  </aside>
</template>

<style scoped>
.node-panel {
  width: 380px;
  height: 100%;
  background: white;
  border-left: 1px solid var(--rose-100, #FFF0F5);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(255, 133, 161, 0.1);
  overflow: hidden;
}

.node-panel__header {
  display: flex;
  justify-content: flex-end;
  padding: 0.75rem 1rem;
  background: var(--rose-50, #FFFAFC);
  border-bottom: 1px solid var(--rose-100, #FFF0F5);
}

.node-panel__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: white;
  border: 1px solid var(--gray-200, #E5E7EB);
  border-radius: 0.5rem;
  cursor: pointer;
  color: var(--gray-500, #6B7280);
  transition: all 0.2s ease;
}

.node-panel__close:hover {
  background: var(--gray-50, #FAFAFA);
  border-color: var(--gray-300, #D1D5DB);
  color: var(--gray-700, #374151);
}

.node-panel__close-icon {
  width: 20px;
  height: 20px;
}

.node-panel__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  color: var(--gray-500, #6B7280);
}
</style>
