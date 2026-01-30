import { onMounted, onUnmounted, ref } from 'vue';

type HelpPopoverOptions = {
  storageKey?: string;
  defaultOpen?: boolean;
  openOnce?: boolean;
};

const SEEN_TOKEN = 'seen';

export function useHelpPopover(options: HelpPopoverOptions = {}) {
  const { storageKey, defaultOpen = false, openOnce = false } = options;
  const popoverRef = ref<HTMLElement | null>(null);
  const isOpen = ref(defaultOpen);

  const hasStorage = Boolean(storageKey && typeof window !== 'undefined');

  function readStorage(): string | null {
    if (!hasStorage || !storageKey) return null;
    try {
      return window.localStorage.getItem(storageKey);
    } catch {
      return null;
    }
  }

  function writeStorage(value: string): void {
    if (!hasStorage || !storageKey) return;
    try {
      window.localStorage.setItem(storageKey, value);
    } catch {
      // no-op: storage might be unavailable (private mode, quota, etc.)
    }
  }

  function syncStorage(next: boolean): void {
    if (!hasStorage) return;
    if (openOnce) {
      writeStorage(SEEN_TOKEN);
      return;
    }
    writeStorage(String(next));
  }

  function setOpen(next: boolean): void {
    isOpen.value = next;
    syncStorage(next);
  }

  function toggle(event?: MouseEvent): void {
    event?.stopPropagation();
    setOpen(!isOpen.value);
  }

  function close(): void {
    setOpen(false);
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
    const stored = readStorage();
    if (openOnce) {
      if (stored !== null) {
        isOpen.value = false;
      } else {
        isOpen.value = defaultOpen;
        if (defaultOpen && hasStorage) {
          writeStorage(SEEN_TOKEN);
        }
      }
    } else if (stored === 'true' || stored === 'false') {
      isOpen.value = stored === 'true';
    } else {
      isOpen.value = defaultOpen;
    }
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
