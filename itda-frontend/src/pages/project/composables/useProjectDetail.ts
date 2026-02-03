import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, onBeforeRouteLeave } from 'vue-router';
import { useProjectStore } from '../../../stores/project';
import { useSceneStore } from '../../../stores/scene';
import { useObjectStore } from '../../../stores/object';
import { useUIStore } from '../../../stores/ui';
import { useScenarioStore } from '../../../stores/scenario';
import type { Scene, SceneStatus } from '../../../types/api/scenes';
import type { ObjectSheet } from '../../../types/api/objects';
import { acquireMediaLease, releaseMediaLease, type MediaUrlLease } from '../../../services/api/media';
import { fetchProjectTimeline } from '../../../services/api/timeline';
import {
  buildScenePreviewFromTimeline,
  type ScenePreview,
} from './scenePreviewMapper';

export type { ScenePreview, ScenePreviewClip } from './scenePreviewMapper';

export type ProjectTab = 'story' | 'scenes' | 'objects' | 'timeline' | 'settings';

export function useProjectDetail() {
  const route = useRoute();
  const projectStore = useProjectStore();
  const sceneStore = useSceneStore();
  const objectStore = useObjectStore();
  const uiStore = useUIStore();
  const scenarioStore = useScenarioStore();

  const activeTab = ref<ProjectTab>('story');
  const draggedScene = ref<Scene | null>(null);
  const scenePreviewMap = ref<Record<number, ScenePreview>>({});
  const previewLoadingMap = ref<Record<number, boolean>>({});
  const activePreview = ref<{ sceneId: number; clipIndex: number } | null>(null);
  const previewContentMap = ref<Record<string, string>>({});
  const previewLeaseMap = ref<Record<string, MediaUrlLease | null>>({});
  const previewTracks = ref<Record<number, HTMLDivElement | null>>({});
  const storyboardOpenMap = ref<Record<number, boolean>>({});

  const previewVisibleLimit = 6;

  const tabItems: { key: ProjectTab; label: string }[] = [
    { key: 'story', label: '스토리' },
    { key: 'scenes', label: '씬' },
    { key: 'objects', label: '오브젝트' },
  ];

  const projectId = computed(() => Number(route.params.id));
  const project = computed(() => projectStore.currentProject);
  const scenes = computed(() => sceneStore.orderedScenes);
  const objects = computed(() => objectStore.objects);
  const isSavingObject = computed(() => objectStore.isSaving);
  const isUpdatingObject = computed(() => objectStore.isUpdating);
  const editingObject = ref<ObjectSheet | null>(null);
  const sceneProgress = computed(() => sceneStore.progress);

  const isUntouchedProject = computed(() => {
    if (!project.value) return false;
    const isDefaultTitle = project.value.title?.trim() === '새 프로젝트';
    const isEmptyDescription = !(project.value.description && project.value.description.trim());
    const isEmptyGenre = !(project.value.genre && project.value.genre.trim());
    const hasNoScenes = scenes.value.length === 0;
    return isDefaultTitle && isEmptyDescription && isEmptyGenre && hasNoScenes;
  });

  const sceneStatusConfig: Record<SceneStatus, { label: string; variant: 'success' | 'info' | 'default' }> = {
    COMPLETED: { label: '완료', variant: 'success' },
    IN_PROGRESS: { label: '진행 중', variant: 'info' },
    DRAFT: { label: '초안', variant: 'default' },
  };

  const resolveSceneStatusConfig = (status: unknown) => {
    if (status === 'COMPLETED' || status === 'IN_PROGRESS' || status === 'DRAFT') {
      return sceneStatusConfig[status];
    }
    return sceneStatusConfig.DRAFT;
  };

  onMounted(async () => {
    if (projectId.value) {
      await Promise.all([
        projectStore.loadProject(projectId.value),
        sceneStore.loadScenes(projectId.value),
        objectStore.loadObjects(projectId.value),
      ]);

      scenarioStore.switchProject(projectId.value);

      // Call logic is handled in ProjectDetailPage.vue
      // collabStore.joinRoom(projectId.value);
      // collabStore.updateLocation('프로젝트 상세 페이지');
    }
  });

  onBeforeRouteLeave(async () => {
    if (project.value && isUntouchedProject.value) {
      await projectStore.moveToTrash(project.value.projectId);
    }
    projectStore.clearCurrentProject();
    revokePreviewContentUrls();
  });

  onUnmounted(() => {
    // collabStore.leaveRoom();
    revokePreviewContentUrls();
  });

  watch(
    () => route.params.id,
    async (newId) => {
      if (newId) {
        const id = Number(newId);
        scenePreviewMap.value = {};
        previewLoadingMap.value = {};
        revokePreviewContentUrls();

        scenarioStore.switchProject(id);

        await Promise.all([
          projectStore.loadProject(id),
          sceneStore.loadScenes(id),
        objectStore.loadObjects(id),
        ]);
      }
    }
  );

  watch(
    () => uiStore.activeModal,
    (modalId) => {
      if (modalId !== 'edit-object-modal') {
        editingObject.value = null;
      }
    }
  );

  const handleTabChange = (tab: string) => {
    activeTab.value = tab as typeof activeTab.value;
  };

  const getSceneEditLink = (scene: Scene) => ({
    name: 'scene-edit',
    params: {
      projectId: projectId.value,
      sceneId: scene.sceneId,
    },
  });

  const getScenePreview = (sceneId: number): ScenePreview => {
    return scenePreviewMap.value[sceneId] || { clips: [], totalDuration: 0 };
  };

  const isPreviewLoading = (sceneId: number): boolean =>
    Boolean(previewLoadingMap.value[sceneId]);

  const buildPreviewKey = (sceneId: number, clipIndex: number) => `${sceneId}-${clipIndex}`;

  function revokePreviewContentUrls() {
    Object.values(previewLeaseMap.value).forEach((lease) => {
      if (lease) {
        releaseMediaLease(lease);
      }
    });
    previewContentMap.value = {};
    previewLeaseMap.value = {};
  }

  const ensurePreviewContentUrl = async (sceneId: number, clipIndex: number) => {
    const preview = getScenePreview(sceneId);
    const clip = preview.clips[clipIndex];
    if (!clip?.contentUrl) return;
    const key = buildPreviewKey(sceneId, clipIndex);
    if (previewContentMap.value[key]) return;
    const lease = await acquireMediaLease(clip.contentUrl).catch(() => null);
    if (!lease) {
      previewContentMap.value = { ...previewContentMap.value, [key]: clip.contentUrl };
      previewLeaseMap.value = { ...previewLeaseMap.value, [key]: null };
      return;
    }
    previewContentMap.value = { ...previewContentMap.value, [key]: lease.url };
    previewLeaseMap.value = {
      ...previewLeaseMap.value,
      [key]: lease.releasable ? lease : null,
    };
  };

  const buildScenePreview = async (sceneId: number): Promise<ScenePreview> => {
    if (!projectId.value) return { clips: [], totalDuration: 0 };
    const timeline = await fetchProjectTimeline(projectId.value);
    return buildScenePreviewFromTimeline(sceneId, timeline.items);
  };

  const loadScenePreviews = async () => {
    if (!projectId.value || scenes.value.length === 0) {
      scenePreviewMap.value = {};
      return;
    }

    const previews: Record<number, ScenePreview> = { ...scenePreviewMap.value };
    const timeline = await fetchProjectTimeline(projectId.value);
    await Promise.all(
      scenes.value.map(async (scene) => {
        previewLoadingMap.value[scene.sceneId] = true;
        previews[scene.sceneId] = buildScenePreviewFromTimeline(scene.sceneId, timeline.items);
        previewLoadingMap.value[scene.sceneId] = false;
      })
    );
    scenePreviewMap.value = previews;
  };

  watch(
    [() => activeTab.value, scenes],
    ([tab]) => {
      if (tab === 'scenes') {
        loadScenePreviews();
      }
    }
  );

  const openPreview = (sceneId: number, clipIndex: number) => {
    activePreview.value = { sceneId, clipIndex };
    void ensurePreviewContentUrl(sceneId, clipIndex);
  };

  const closePreview = () => {
    activePreview.value = null;
  };

  const activePreviewClip = computed(() => {
    if (!activePreview.value) return null;
    const preview = getScenePreview(activePreview.value.sceneId);
    const clip = preview.clips[activePreview.value.clipIndex];
    if (!clip) return null;
    const key = buildPreviewKey(activePreview.value.sceneId, activePreview.value.clipIndex);
    const resolvedContentUrl = previewContentMap.value[key] ?? clip.contentUrl;
    return { ...clip, contentUrl: resolvedContentUrl };
  });

  const activePreviewScene = computed(() => {
    if (!activePreview.value) return null;
    return scenes.value.find((scene) => scene.sceneId === activePreview.value?.sceneId) || null;
  });

  const setPreviewTrackRef = (sceneId: number, el: HTMLDivElement | null) => {
    previewTracks.value[sceneId] = el;
  };

  const scrollPreview = (sceneId: number, direction: -1 | 1) => {
    const track = previewTracks.value[sceneId];
    if (!track) return;
    const amount = Math.max(180, track.clientWidth * 0.6);
    track.scrollBy({ left: direction * amount, behavior: 'smooth' });
  };

  const handlePreviewWheel = (sceneId: number, event: WheelEvent) => {
    const track = previewTracks.value[sceneId];
    if (!track) return;
    if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
      event.preventDefault();
      track.scrollBy({ left: event.deltaY, behavior: 'auto' });
    }
  };

  const getClipWidth = (duration: number): number =>
    Math.min(Math.max(duration * 20, 64), 220);

  const getOverflowCount = (sceneId: number): number => {
    const count = getScenePreview(sceneId).clips.length;
    return count > previewVisibleLimit ? count - previewVisibleLimit : 0;
  };

  const ensureScenePreview = async (sceneId: number) => {
    if (scenePreviewMap.value[sceneId]) return;
    previewLoadingMap.value[sceneId] = true;
    const preview = await buildScenePreview(sceneId);
    scenePreviewMap.value = {
      ...scenePreviewMap.value,
      [sceneId]: preview,
    };
    previewLoadingMap.value[sceneId] = false;
  };

  const toggleStoryboard = async (sceneId: number) => {
    const next = !storyboardOpenMap.value[sceneId];
    storyboardOpenMap.value = {
      ...storyboardOpenMap.value,
      [sceneId]: next,
    };
    if (next) {
      await ensureScenePreview(sceneId);
    }
  };

  const handleStoryboardWheel = (event: WheelEvent) => {
    const container = event.currentTarget as HTMLElement | null;
    if (!container) return;
    container.scrollLeft += event.deltaY + event.deltaX;
  };

  const handleDragStart = (scene: Scene) => {
    draggedScene.value = scene;
  };

  const handleDragEnd = async () => {
    if (draggedScene.value) {
      const sceneIds = scenes.value.map((s) => s.sceneId);
      await sceneStore.reorderScenes(sceneIds);
      uiStore.showToast({
        type: 'success',
        title: '순서 변경',
        message: '씬 순서가 변경되었습니다.',
      });
    }
    draggedScene.value = null;
  };

  const handleDragOver = (event: DragEvent, targetScene: Scene) => {
    event.preventDefault();
    if (!draggedScene.value || draggedScene.value.sceneId === targetScene.sceneId) return;

    const draggedIndex = scenes.value.findIndex((s) => s.sceneId === draggedScene.value?.sceneId);
    const targetIndex = scenes.value.findIndex((s) => s.sceneId === targetScene.sceneId);

    if (draggedIndex !== -1 && targetIndex !== -1) {
      const newScenes = [...scenes.value];
      newScenes.splice(draggedIndex, 1);
      newScenes.splice(targetIndex, 0, draggedScene.value);
      newScenes.forEach((s, idx) => {
        s.order = idx + 1;
      });
    }
  };

  const handleAddScene = async () => {
    const newScene = await sceneStore.addScene({
      title: `새 씬 ${scenes.value.length + 1}`,
      description: '',
    });
    if (newScene) {
      uiStore.showToast({
        type: 'success',
        title: '씬 추가',
        message: '새 씬이 추가되었습니다.',
      });
    }
  };

  const openAddObjectModal = () => {
    uiStore.openModal('add-object-modal');
  };

  const openEditObjectModal = (object: ObjectSheet) => {
    editingObject.value = object;
    uiStore.openModal('edit-object-modal');
  };

  const handleDeleteScene = async (scene: Scene): Promise<boolean> => {
    const success = await sceneStore.removeScene(scene.sceneId);
    if (success) {
      uiStore.showToast({
        type: 'success',
        title: '씬 삭제',
        message: `"${scene.title}" 씬이 삭제되었습니다.`,
      });
      return true;
    }
    uiStore.showToast({
      type: 'error',
      title: '씬 삭제 실패',
      message: '잠시 후 다시 시도해주세요.',
    });
    return false;
  };

  const handleAddObject = async (data: { name: string; type: ObjectSheet['type']; description: string; style: string; file: File }) => {
    const created = await objectStore.addObject(
      {
        name: data.name,
        type: data.type,
        description: data.description,
        style: data.style,
      },
      data.file
    );

    if (created) {
      uiStore.closeModal();
      uiStore.showToast({
        type: 'success',
        title: '오브젝트 생성 완료',
        message: `${data.name} 오브젝트가 추가되었습니다.`,
      });
    }
  };

  const handleUpdateObject = async (data: { objectId: number; name: string; type: ObjectSheet['type']; description: string; style: string; file?: File | null }) => {
    const updated = await objectStore.updateObject(
      data.objectId,
      {
        name: data.name,
        type: data.type,
        description: data.description,
        style: data.style,
      },
      data.file
    );
    if (updated) {
      uiStore.closeModal();
      uiStore.showToast({
        type: 'success',
        title: '오브젝트 수정 완료',
        message: `${data.name} 오브젝트가 수정되었습니다.`,
      });
    }
  };

  const handleDeleteObject = async (object: ObjectSheet) => {
    const success = await objectStore.removeObject(object.objectId);
    if (success) {
      uiStore.showToast({
        type: 'success',
        title: '오브젝트 삭제',
        message: `${object.name}이(가) 삭제되었습니다.`,
      });
    }
  };

  const handleDownloadObjectImage = async (object: ObjectSheet) => {
    const blob = await objectStore.downloadObjectImage(object.objectId);
    if (!blob) {
      uiStore.showToast({
        type: 'error',
        title: '다운로드 실패',
        message: '이미지를 다운로드할 수 없습니다.',
      });
      return;
    }
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${object.name || 'object'}.png`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  return {
    activeTab,
    tabItems,
    projectId,
    project,
    scenes,
    sceneProgress,
    objects,
    isSavingObject,
    isUpdatingObject,
    editingObject,
    scenarioStore,
    sceneStatusConfig,
    resolveSceneStatusConfig,
    handleTabChange,
    getSceneEditLink,
    getScenePreview,
    isPreviewLoading,
    loadScenePreviews,
    openPreview,
    closePreview,
    activePreviewClip,
    activePreviewScene,
    setPreviewTrackRef,
    scrollPreview,
    handlePreviewWheel,
    getClipWidth,
    getOverflowCount,
    toggleStoryboard,
    handleStoryboardWheel,
    storyboardOpenMap,
    handleDragStart,
    handleDragEnd,
    handleDragOver,
    handleAddScene,
    handleDeleteScene,
    openAddObjectModal,
    openEditObjectModal,
    handleAddObject,
    handleUpdateObject,
    handleDeleteObject,
    handleDownloadObjectImage,
  };
}
