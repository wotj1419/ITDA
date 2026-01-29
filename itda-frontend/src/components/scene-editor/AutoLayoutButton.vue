<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import autolayoutIcon from '../../assets/autolayout.svg';

interface Props {
  isPanelOpen?: boolean;
  bottomOffset?: number;
  leftOffset?: number;
  extraBottom?: number;
  label?: string;
  alignToControls?: boolean;
  gapAboveControls?: number;
}

const props = withDefaults(defineProps<Props>(), {
  isPanelOpen: false,
  bottomOffset: 80,
  leftOffset: 104,
  extraBottom: 96,
  label: '레이아웃 정렬',
  alignToControls: true,
  gapAboveControls: 12,
});

const emit = defineEmits<{
  (e: 'click'): void;
}>();

const buttonRef = ref<HTMLButtonElement | null>(null);
const resolvedStyle = ref<Record<string, string>>({});

const fallbackStyle = computed<Record<string, string>>(() => ({
  '--bottom': `${props.bottomOffset}px`,
  '--left': `${props.leftOffset}px`,
  '--extra-bottom': `${props.extraBottom}px`,
}));

function clampHorizontal(left: number, width: number, container: HTMLElement | null): number {
  if (!container) return Math.max(left, 12);
  const containerRect = container.getBoundingClientRect();
  const minLeft = 12;
  const maxLeft = containerRect.width - width - 12;
  return Math.min(Math.max(left, minLeft), Math.max(maxLeft, minLeft));
}

function clampVertical(bottom: number, height: number, container: HTMLElement | null): number {
  if (!container) return Math.max(bottom, 12);
  const containerRect = container.getBoundingClientRect();
  const minBottom = 12;
  const maxBottom = containerRect.height - height - 12;
  return Math.min(Math.max(bottom, minBottom), Math.max(maxBottom, minBottom));
}

function computeButtonStyle(): Record<string, string> {
  const button = buttonRef.value;
  const container = button?.offsetParent as HTMLElement | null;
  const buttonWidth = button?.offsetWidth ?? 138;
  const buttonHeight = button?.offsetHeight ?? 44;
  let left = props.leftOffset;
  let bottom = props.bottomOffset + props.extraBottom;

  if (props.alignToControls) {
    const controls = document.querySelector('.vue-flow__controls') as HTMLElement | null;
    if (controls && container) {
      const controlsRect = controls.getBoundingClientRect();
      const containerRect = container.getBoundingClientRect();
      left = controlsRect.left - containerRect.left + (controlsRect.width - buttonWidth) / 2;
      bottom = containerRect.bottom - controlsRect.top + props.gapAboveControls;
    }
  }

  left = clampHorizontal(left, buttonWidth, container);
  bottom = clampVertical(bottom, buttonHeight, container);

  return {
    '--left': `${Math.max(Math.round(left), 12)}px`,
    '--bottom': `${Math.round(bottom)}px`,
    '--extra-bottom': '0px',
  };
}

async function syncPositions(): Promise<void> {
  resolvedStyle.value = props.alignToControls ? computeButtonStyle() : fallbackStyle.value;
  await nextTick();
}

onMounted(async () => {
  await nextTick();
  await syncPositions();
  window.addEventListener('resize', syncPositions);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncPositions);
});

watch(
  () => [
    props.isPanelOpen,
    props.bottomOffset,
    props.leftOffset,
    props.extraBottom,
    props.alignToControls,
  ],
  async () => {
    await nextTick();
    await syncPositions();
  }
);
</script>

<template>
  <button
    class="auto-layout-btn"
    :class="{ 'panel-open': props.isPanelOpen }"
    :style="resolvedStyle"
    type="button"
    ref="buttonRef"
    @click="emit('click')"
  >
    <span class="icon-wrap">
      <img class="svgIcon icon-default" :src="autolayoutIcon" alt="Auto layout" />
      <img class="svgIcon icon-refresh" :src="autolayoutIcon" alt="" aria-hidden="true" />
    </span>
    <span class="auto-layout-label">{{ props.label }}</span>
  </button>
</template>

<style scoped>
.auto-layout-btn {
  --btn-height: 44px;
  --icon-size: 18px;
  --bottom: 80px;
  --left: 104px;
  --extra-bottom: 96px;

  min-width: 138px;
  height: var(--btn-height);
  padding: 0 14px;
  border-radius: 9999px;
  background: #ffffff;
  border: 1px solid var(--gray-100, #F3F4F6);
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: var(--left);
  bottom: calc(var(--bottom) + var(--extra-bottom));
  overflow: hidden;
  color: var(--gray-500, #6B7280);
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  z-index: 50;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.auto-layout-btn.panel-open {
  opacity: 0.92;
}

.icon-wrap {
  position: absolute;
  left: 14px;
  top: 50%;
  width: var(--icon-size);
  height: var(--icon-size);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transform: translateY(-50%);
}

.svgIcon {
  width: var(--icon-size);
  height: var(--icon-size);
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  transition: opacity 0.2s ease;
  display: block;
  object-fit: contain;
}

.auto-layout-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  position: absolute;
  left: 50%;
  top: 50%;
  line-height: 1;
  text-align: center;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.auto-layout-btn:hover {
  transform: translateY(-2px);
  color: var(--rose-500, #FF4D8D);
  border-color: var(--rose-200, #FFE8F2);
}

.auto-layout-btn:hover .icon-refresh {
  opacity: 1;
  transform: translate(-50%, -50%) rotate(180deg);
}

.auto-layout-btn:hover .icon-default {
  opacity: 0;
}

.auto-layout-btn:active {
  transform: translateY(0);
  box-shadow: none;
}
</style>
