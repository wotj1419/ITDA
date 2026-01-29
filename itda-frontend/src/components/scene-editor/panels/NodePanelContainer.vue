<script setup lang="ts">
/**
 * NodePanelContainer - 동적 패널 라우팅 컨테이너
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.1
 */
import { computed, inject, nextTick, provide, ref, watch } from 'vue';
import { panelRegistry } from './index';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { NodeType } from '../../../types/ui/sceneNodes';
import type { AnyNodeData } from '../../../types/ui/sceneNodes';

// =============================================================================
// Store
// =============================================================================

const nodeStore = useSceneNodeStore();
const requestDeleteNode = inject<((nodeId: string) => void) | null>('nodeDeleteRequest', null);

// =============================================================================
// Computed
// =============================================================================

const selectedNode = computed(() => nodeStore.selectedNode);
const panelRef = ref<HTMLElement | null>(null);

const panelComponent = computed(() => {
  if (!selectedNode.value) return null;
  const nodeType = (selectedNode.value.data as AnyNodeData).type;
  return panelRegistry[nodeType] || null;
});

const canDeleteSelected = computed(
  () => selectedNode.value?.data?.type !== NodeType.SCENE_HEADER
);

// =============================================================================
// Methods
// =============================================================================

function handleClose(): void {
  nodeStore.selectNode(null);
}

function handleDelete(): void {
  const nodeId = selectedNode.value?.id;
  if (!nodeId) return;
  if (requestDeleteNode) {
    requestDeleteNode(nodeId);
    return;
  }
  nodeStore.deleteNode(nodeId);
}

provide('nodePanelClose', handleClose);
provide('nodePanelDelete', handleDelete);
provide('nodePanelCanDelete', canDeleteSelected);

// Scroll panel content to top when switching nodes (all node types)
watch(
  () => selectedNode.value?.id,
  async (nextId, prevId) => {
    if (!nextId || nextId === prevId) return;
    await nextTick();
    const panelEl = panelRef.value;
    if (!panelEl) return;
    const contentEl = panelEl.querySelector<HTMLElement>('.base-panel__content');
    if (contentEl) {
      contentEl.scrollTop = 0;
      return;
    }
    panelEl.scrollTop = 0;
  }
);
</script>

<template>
  <Transition name="slide">
    <aside v-if="selectedNode" ref="panelRef" class="node-panel">
      <component
        v-if="panelComponent"
        :is="panelComponent"
        :key="selectedNode.id"
        :node="selectedNode"
        @close="handleClose"
      />
      
      <div v-else class="node-panel__empty">
        <p>패널을 찾을 수 없습니다.</p>
      </div>
    </aside>
  </Transition>
</template>

<style scoped>
/* ==========================================================================
   Animations
   ========================================================================== */

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.4s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ==========================================================================
   Panel Styles
   ========================================================================== */
.node-panel {
  width: 380px;
  height: 100%;
  background: white;
  border-left: 1px solid #F3F4F6;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
  z-index: 10;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
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
