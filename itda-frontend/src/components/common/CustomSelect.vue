<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: string | number
  options: Option[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'change', value: string | number): void
}>()

const isOpen = ref(false)
const selectRef = ref<HTMLElement | null>(null)
const selectedLabel = ref('')
const dropdownStyle = ref({})

watch(() => props.modelValue, updateSelectedLabel, { immediate: true })

function updateSelectedLabel() {
  const found = props.options.find(o => o.value === props.modelValue)
  selectedLabel.value = found ? found.label : ''
}

async function toggle() {
  if (isOpen.value) {
    isOpen.value = false
    return
  }
  
  isOpen.value = true
  await nextTick()
  updatePosition()
}

function updatePosition() {
  if (!selectRef.value || !isOpen.value) return
  
  const rect = selectRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
    zIndex: 9999
  }
}

function select(option: Option) {
  emit('update:modelValue', option.value)
  emit('change', option.value)
  isOpen.value = false
}

function close(e: MouseEvent) {
  // If clicking outside the select trigger AND outside the dropdown options
  // Note: Since options are teleported, we can't just check selectRef.contains(target)
  // We need to check if the click target is inside the dropdown menu manually or rely on a different logic.
  // Ideally, clicking anywhere else closes it.
  
  if (selectRef.value && selectRef.value.contains(e.target as Node)) {
    // Clicked on the trigger, toggle handles this
    return
  }
  
  // Checking if clicked inside the teleported dropdown content is harder without a ref to it, 
  // but usually clicking an option calls select() which closes it.
  // Clicking generic background should close it.
  // We can add a simple check: if the event target is inside .custom-select-options (we add a class / ref)
  const target = e.target as HTMLElement
  if (target.closest('.custom-select-dropdown')) {
    return
  }

  isOpen.value = false
}

function handleScroll() {
  if (isOpen.value) {
    updatePosition()
    // Optional: close on scroll to avoid detached menu
    // isOpen.value = false 
  }
}

function handleResize() {
  if (isOpen.value) {
    updatePosition()
  }
}

onMounted(() => {
  document.addEventListener('click', close)
  window.addEventListener('scroll', handleScroll, true) // capture param for scroll in elements
  window.addEventListener('resize', handleResize)
  updateSelectedLabel()
})

onUnmounted(() => {
  document.removeEventListener('click', close)
  window.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="select" ref="selectRef" :class="{ open: isOpen }">
    <div class="selected" @click="toggle">
      <span class="text">{{ selectedLabel }}</span>
      <svg
        class="arrow"
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
      >
        <path d="M7 10l5 5 5-5z" />
      </svg>
    </div>

    <Teleport to="body">
      <div 
        v-if="isOpen"
        class="custom-select-dropdown options" 
        :style="dropdownStyle"
      >
        <div 
          v-for="option in options" 
          :key="option.value" 
          class="option"
          :class="{ active: option.value === modelValue }"
          @click.stop="select(option)"
        >
          {{ option.label }}
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.select {
  width: 110px; /* Adapted width */
  cursor: pointer;
  position: relative;
  transition: 300ms;
  color: var(--gray-700);
  font-size: 0.875rem;
  z-index: 10;
}

.selected {
  background-color: white; /* Adapted from dark #2a2f3b */
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px; /* Adapted radius */
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.2s;
}

.selected:hover {
  border-color: var(--rose-400);
}

.arrow {
  height: 16px;
  width: 16px;
  fill: var(--rose-400); /* Adapted fill */
  transition: 300ms;
  transform: rotate(0deg);
}

/* Open State Animations */
.select.open .arrow {
  transform: rotate(180deg);
}

.custom-select-dropdown {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  padding: 0.25rem;
  background-color: white;
  border: 1px solid var(--rose-200);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  box-sizing: border-box;
  
  /* Initial state for animation */
  opacity: 0;
  transform: translateY(-10px);
  animation: slideDown 0.2s forwards;
}

@keyframes slideDown {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.option {
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  transition: 200ms;
  background-color: transparent;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--gray-700);
}

.option:hover {
  background-color: var(--rose-50);
}

.option.active {
  color: var(--rose-600);
  font-weight: 600;
  background-color: var(--rose-50);
}

.text {
  font-weight: 500;
  white-space: nowrap;
}
</style>
