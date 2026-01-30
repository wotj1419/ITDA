import { onMounted, onUnmounted, ref } from 'vue';

export function useHelpPopover() {
  const popoverRef = ref<HTMLElement | null>(null);
  const isOpen = ref(false);

  function toggle(event?: MouseEvent): void {
    event?.stopPropagation();
    isOpen.value = !isOpen.value;
  }

  function close(): void {
    isOpen.value = false;
  }

  function handleDocumentClick(event: MouseEvent): void {
    if (!isOpen.value) return;
    const target = event.target as Node | null;
    if (!popoverRef.value || !target) return;
    if (!popoverRef.value.contains(target)) {
      close();
    }
  }

  function handleDocumentKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && isOpen.value) {
      close();
    }
  }

  onMounted(() => {
    document.addEventListener('click', handleDocumentClick);
    document.addEventListener('keydown', handleDocumentKeydown);
  });

  onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
    document.removeEventListener('keydown', handleDocumentKeydown);
  });

  return {
    popoverRef,
    isOpen,
    toggle,
    close,
  };
}
