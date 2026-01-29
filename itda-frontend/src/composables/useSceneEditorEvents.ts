import { useSceneNodeStore } from '../stores/sceneNode';
import { useUIStore } from '../stores/ui';

interface SceneEditorEventOptions {
  nodeStore?: ReturnType<typeof useSceneNodeStore>;
  uiStore?: ReturnType<typeof useUIStore>;
  nodeDeleteModalId: string;
  requestDeleteNode: (nodeId: string) => void;
}

export function useSceneEditorEvents({
  nodeStore = useSceneNodeStore(),
  uiStore = useUIStore(),
  nodeDeleteModalId,
  requestDeleteNode,
}: SceneEditorEventOptions) {
  function isEditableTarget(target: EventTarget | null): boolean {
    const element = target as HTMLElement | null;
    if (!element) return false;
    const tagName = element.tagName;
    return tagName === 'INPUT' || tagName === 'TEXTAREA' || element.isContentEditable;
  }

  function handleEditorKeydown(event: KeyboardEvent): void {
    if (isEditableTarget(event.target)) return;

    const key = event.key.toLowerCase();
    if (key === 'delete' || key === 'backspace') {
      if (uiStore.activeModal === nodeDeleteModalId) return;
      const selectedId = nodeStore.selectedNodeId;
      if (selectedId) {
        event.preventDefault();
        requestDeleteNode(selectedId);
      }
      return;
    }

    if ((event.ctrlKey || event.metaKey) && key === 'z') {
      event.preventDefault();
      nodeStore.undoLastMove();
    }
  }

  function handleBeforeUnload(): void {
    nodeStore.flushPersistNodePositions();
    nodeStore.flushSave();
  }

  return {
    handleBeforeUnload,
    handleEditorKeydown,
  };
}
