import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, onBeforeRouteLeave } from 'vue-router';
import { useProjectStore } from '../../../stores/project';
import { useSceneStore } from '../../../stores/scene';
import { useCharacterStore } from '../../../stores/character';
import { useUIStore } from '../../../stores/ui';
import { useCollabStore } from '../../../stores/collab';
import { useScenarioStore } from '../../../stores/scenario';
import type { Scene, SceneStatus } from '../../../types/api/scenes';
import type { ObjectSheet } from '../../../types/api/objects';
import { fetchProjectTimeline, type TimelineItem } from '../../../services/api/timeline';

export type ProjectTab = 'story' | 'scenes' | 'objects' | 'timeline' | 'settings';

export interface ScenePreviewClip {
  thumbnailUrl: string;
  duration: number;
  label?: string;
  contentUrl?: string;
}

export interface ScenePreview {
  clips: ScenePreviewClip[];
  totalDuration: number;
}

export function useProjectDetail() {
  const route = useRoute();
  const projectStore = useProjectStore();
  const sceneStore = useSceneStore();
  const characterStore = useCharacterStore();
  const uiStore = useUIStore();
  const collabStore = useCollabStore();
  const scenarioStore = useScenarioStore();

  const activeTab = ref<ProjectTab>('story');
  const draggedScene = ref<Scene | null>(null);
  const scenePreviewMap = ref<Record<number, ScenePreview>>({});
  const previewLoadingMap = ref<Record<number, boolean>>({});
  const activePreview = ref<{ sceneId: number; clipIndex: number } | null>(null);
  const previewTracks = ref<Record<number, HTMLDivElement | null>>({});
  const storyboardOpenMap = ref<Record<number, boolean>>({});

  const previewVisibleLimit = 6;

  const tabItems: { key: ProjectTab; label: string }[] = [
    { key: 'story', label: 'Story' },
    { key: 'scenes', label: 'Scenes' },
    { key: 'objects', label: 'Objects' },
  ];

  const projectId = computed(() => Number(route.params.id));
  const project = computed(() => projectStore.currentProject);
  const scenes = computed(() => sceneStore.orderedScenes);
  const characters = computed(() => characterStore.characters);
  const isGeneratingCharacter = computed(() => characterStore.isGenerating);
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
        characterStore.loadCharacters(projectId.value),
      ]);

      scenarioStore.switchProject(projectId.value);

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
  });

  onUnmounted(() => {
    // collabStore.leaveRoom();
  });

  watch(
    () => route.params.id,
    async (newId) => {
      if (newId) {
        const id = Number(newId);
        scenePreviewMap.value = {};
        previewLoadingMap.value = {};

        scenarioStore.switchProject(id);

        await Promise.all([
          projectStore.loadProject(id),
          sceneStore.loadScenes(id),
          characterStore.loadCharacters(id),
        ]);
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

  const buildScenePreviewFromTimeline = (sceneId: number, items: TimelineItem[]): ScenePreview => {
    const clips = items
      .filter((item) => item.sceneId === sceneId)
      .sort((a, b) => a.order - b.order)
      .map((item) => ({
        thumbnailUrl: item.url || '',
        duration: 4,
        label: `Video ${item.order}`,
        contentUrl: item.url || '',
      }));
    const totalDuration = clips.reduce((sum, clip) => sum + clip.duration, 0);
    return { clips, totalDuration };
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
  };

  const closePreview = () => {
    activePreview.value = null;
  };

  const activePreviewClip = computed(() => {
    if (!activePreview.value) return null;
    const preview = getScenePreview(activePreview.value.sceneId);
    return preview.clips[activePreview.value.clipIndex] || null;
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
      title: `New Scene ${scenes.value.length + 1}`,
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

  const openAddCharacterModal = () => {
    uiStore.openModal('add-character-modal');
  };

  const handleAddCharacter = async (data: { name: string; description: string; style: string }) => {
    const newCharacter = await characterStore.generateCharacter({
      name: data.name,
      description: data.description,
      style: data.style,
    });

    if (newCharacter) {
      uiStore.closeModal();
      uiStore.showToast({
        type: 'success',
        title: '캐릭터 생성 완료',
        message: `${data.name} 캐릭터가 추가되었습니다.`,
      });
    }
  };

  const handleEditCharacter = (character: ObjectSheet) => {
    console.log('Edit character:', character);
  };

  const handleDeleteCharacter = async (character: ObjectSheet) => {
    if (confirm(`"${character.name}" 캐릭터를 삭제하시겠습니까?`)) {
      const success = await characterStore.removeCharacter(character.objectId);
      if (success) {
        uiStore.showToast({
          type: 'success',
          title: '캐릭터 삭제',
          message: `${character.name}이(가) 삭제되었습니다.`,
        });
      }
    }
  };

  return {
    activeTab,
    tabItems,
    projectId,
    project,
    scenes,
    sceneProgress,
    characters,
    isGeneratingCharacter,
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
    openAddCharacterModal,
    handleAddCharacter,
    handleEditCharacter,
    handleDeleteCharacter,
  };
}
