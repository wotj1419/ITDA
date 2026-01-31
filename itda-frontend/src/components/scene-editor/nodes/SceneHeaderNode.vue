<script setup lang="ts">
/**
 * SceneHeaderNode - 씬 헤더 노드
 * 읽기 전용 메타 노드, 씬 진입 시 자동 생성
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3.3
 */
import { computed, ref, watch, nextTick, onMounted } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import { NodeResizer } from '@vue-flow/node-resizer';
import { getNodeMinSize, NODE_RESIZER_STYLE } from '../../../utils/nodeUi';
import type { SceneHeaderNodeData } from '../../../types/ui/sceneNodes';
import { BookOpen, Plus } from 'lucide-vue-next';
import { useSceneNodeStore } from '../../../stores/sceneNode';

// =============================================================================
// Props
// =============================================================================

interface Props {
  id: string;
  data: SceneHeaderNodeData;
  selected?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'add-child'): void;
}>();

const store = useSceneNodeStore();
const nodeStyle = NODE_RESIZER_STYLE;
const { minWidth, minHeight } = getNodeMinSize(props.data.type);
const nodeRef = ref<HTMLElement | null>(null);

// =============================================================================
// Computed
// =============================================================================

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--header',
  { 'node-glass--selected': props.selected },
]);

function handleAddChild(event: Event) {
  event.stopPropagation();
  emit('add-child');
}

function syncNodeHeight(): void {
  const nodeEl = nodeRef.value;
  if (!nodeEl) return;
  const headerEl = nodeEl.querySelector('.node-glass__header') as HTMLElement | null;
  const bodyEl = nodeEl.querySelector('.node-glass__body') as HTMLElement | null;
  const headerHeight = headerEl?.offsetHeight ?? 0;
  const bodyHeight = bodyEl?.scrollHeight ?? 0;
  const requiredHeight = Math.max(minHeight, Math.ceil(headerHeight + bodyHeight));
  const node = store.nodes.find((item) => item.id === props.id);
  if (!node) return;
  const currentHeightRaw = (node.style as Record<string, unknown> | undefined)?.height ?? node.height ?? 0;
  const currentHeight = Number(String(currentHeightRaw).replace('px', ''));
  if (Number.isFinite(currentHeight) && requiredHeight <= currentHeight + 4) return;
  node.height = requiredHeight;
  node.style = { ...(node.style ?? {}), height: `${requiredHeight}px` };
  store.persistNodePositions();
}

onMounted(() => {
  nextTick(syncNodeHeight);
});

watch(
  () => props.data.description,
  () => {
    nextTick(syncNodeHeight);
  }
);
</script>

<template>
  <div ref="nodeRef" :class="nodeClasses" :style="nodeStyle">
    <NodeResizer
      :min-width="minWidth"
      :min-height="minHeight"
      :is-visible="props.selected"
      @resize-start="store.pushPositionSnapshot()"
      @resize-end="store.persistNodePositions()"
    />
    <div v-if="props.selected" class="node-resizer-outline" />
    <!-- Header -->
    <div class="node-glass__header">
      <div class="node-glass__header-left">
        <div class="node-glass__icon-box node-glass__icon-box--header">
          <BookOpen class="node-glass__icon" />
        </div>
        <div class="node-glass__title-group">
          <span class="node-glass__title">
            씬 {{ data.sceneOrder }}: {{ data.title }}
          </span>
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="node-glass__body">
      <p class="node-glass__description">
        {{ data.description || '설명 없음' }}
      </p>
    </div>

    <!-- Source Handle (bottom) -->
    <Handle
      type="source"
      :position="Position.Bottom"
      class="node-glass__handle"
    />
    <!-- Header -->
    <button
      class="node-glass__add-btn"
      title="마스터 이미지 추가"
      @click="handleAddChild"
    >
      <Plus :size="32" />
    </button>
  </div>
</template>
