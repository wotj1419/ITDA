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
import { useCollabStore } from '../../../stores/collab';

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
const collabStore = useCollabStore();
const nodeStyle = NODE_RESIZER_STYLE;
const { minWidth, minHeight } = getNodeMinSize(props.data.type);
const nodeRef = ref<HTMLElement | null>(null);
const headerRef = ref<HTMLElement | null>(null);
const bodyRef = ref<HTMLElement | null>(null);
const descriptionRef = ref<HTMLElement | null>(null);

// =============================================================================
// Computed
// =============================================================================

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--header',
  {
    'node-glass--selected': props.selected,
    'node-glass--locked': collabStore.isNodeLockedByOther(props.id),
  },
]);

// 노드 잠금 사용자의 커서 색상으로 테두리 스타일 적용
const lockStyle = computed(() => {
  const lockColor = collabStore.getNodeLockColor(props.id);
  if (!lockColor) return {};
  return {
    '--node-lock-color': lockColor,
    borderColor: lockColor,
    boxShadow: `0 0 0 3px ${lockColor}33`,
  };
});

function handleAddChild(event: Event) {
  event.stopPropagation();
  emit('add-child');
}

function syncNodeHeight(): void {
  const nodeEl = nodeRef.value;
  const headerEl = headerRef.value;
  const bodyEl = bodyRef.value;
  const descriptionEl = descriptionRef.value;
  if (!nodeEl || !headerEl || !descriptionEl) return;
  const bodyStyle = bodyEl ? getComputedStyle(bodyEl) : null;
  const paddingTop = bodyStyle ? Number.parseFloat(bodyStyle.paddingTop) || 0 : 0;
  const paddingBottom = bodyStyle ? Number.parseFloat(bodyStyle.paddingBottom) || 0 : 0;
  const requiredHeight = Math.max(
    minHeight,
    Math.ceil(headerEl.offsetHeight + paddingTop + paddingBottom + descriptionEl.scrollHeight)
  );
  const node = store.nodes.find((item) => item.id === props.id);
  if (!node) return;
  const currentHeightRaw = (node.style as Record<string, unknown> | undefined)?.height ?? node.height ?? 0;
  const currentHeight = Number(String(currentHeightRaw).replace('px', ''));
  if (Number.isFinite(currentHeight) && Math.abs(requiredHeight - currentHeight) <= 2) return;
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

watch(
  () => props.data.title,
  () => {
    nextTick(syncNodeHeight);
  }
);
</script>

<template>
  <div ref="nodeRef" :class="nodeClasses" :style="{ ...nodeStyle, ...lockStyle }">
    <NodeResizer
      :min-width="minWidth"
      :min-height="minHeight"
      :is-visible="props.selected"
      @resize-start="store.pushPositionSnapshot()"
      @resize-end="store.persistNodePositions()"
    />
    <div v-if="props.selected" class="node-resizer-outline" />
    <!-- Header -->
    <div ref="headerRef" class="node-glass__header">
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
    <div ref="bodyRef" class="node-glass__body">
      <p ref="descriptionRef" class="node-glass__description">
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
