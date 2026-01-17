<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  clickable?: boolean
  padding?: boolean
  dashed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  clickable: false,
  padding: true,
  dashed: false,
})

const classes = computed(() => [
  'card',
  {
    'card-clickable': props.clickable,
    'card-no-padding': !props.padding,
    'card-dashed': props.dashed,
  },
])
</script>

<template>
  <div :class="classes">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 16px;
  padding: 1.25rem;
  transition: all 0.2s ease;
}

.card-clickable {
  cursor: pointer;
}

.card-clickable:hover {
  border-color: var(--rose-200);
  box-shadow: 0 8px 24px -4px rgba(244, 63, 94, 0.12);
  transform: translateY(-2px);
}

.card-no-padding {
  padding: 0;
}

.card-dashed {
  border-style: dashed;
  border-color: var(--rose-200);
}

.card-dashed:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}
</style>
