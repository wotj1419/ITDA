<script setup lang="ts">
import type { Scene } from '../../../types/api/scenes';
import type { ProjectDetail } from '../../../types/api/projects';
import type { ScenePreview, ScenePreviewClip } from '../composables/useProjectDetail';
import ScenarioDrawer from '../../../components/scenario/ScenarioDrawer.vue';
import SceneCard from '../../../components/project/SceneCard.vue';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import ConfirmModal from '../../../components/common/ConfirmModal.vue';
import { Plus, PlusCircle, Play, Sparkles, X } from 'lucide-vue-next';
import { computed, ref, watch } from 'vue';
import { useProjectStore } from '../../../stores/project';
import { useUIStore } from '../../../stores/ui';
import { useScenarioStore } from '../../../stores/scenario';

interface Props {
  scenes: Scene[];
  project: ProjectDetail | null;
  projectId: number;
  storyboardOpenMap: Record<number, boolean>;
  getScenePreview: (sceneId: number) => ScenePreview;
  isPreviewLoading: (sceneId: number) => boolean;
  openPreview: (sceneId: number, clipIndex: number) => void;
  closePreview: () => void;
  activePreviewClip: ScenePreviewClip | null;
  activePreviewScene: Scene | null;
  toggleStoryboard: (sceneId: number) => void | Promise<void>;
  handleStoryboardWheel: (event: WheelEvent) => void;
  handleAddScene: () => void | Promise<void>;
  handleDragStart: (scene: Scene) => void;
  handleDragEnd: () => void | Promise<void>;
  handleDragOver: (event: DragEvent, targetScene: Scene) => void;
  handleDeleteScene: (scene: Scene) => void | Promise<boolean>;
  onOpenScenario: () => void;
}


const previewVideoRefs = ref<Record<string, HTMLVideoElement | null>>({});

const getPreviewKey = (sceneId: number, index: number) => `${sceneId}-${index}`;

const setPreviewVideoRef = (
  sceneId: number,
  index: number,
  el: HTMLVideoElement | null
): void => {
  previewVideoRefs.value[getPreviewKey(sceneId, index)] = el;
};

const playPreviewVideo = (sceneId: number, index: number): void => {
  const video = previewVideoRefs.value[getPreviewKey(sceneId, index)];
  if (!video) return;
  video.currentTime = 0;
  void video.play().catch(() => {});
};

const pausePreviewVideo = (sceneId: number, index: number): void => {
  const video = previewVideoRefs.value[getPreviewKey(sceneId, index)];
  if (!video) return;
  video.pause();
  video.currentTime = 0;
};

const props = defineProps<Props>();
const projectStore = useProjectStore();
const uiStore = useUIStore();
const scenarioStore = useScenarioStore();

const localTitle = ref('');
const localDescription = ref('');
const isSavingProject = ref(false);
const deleteTargetScene = ref<Scene | null>(null);

const labels = {
  defaultTitle: '\uC0C8 \uD504\uB85C\uC81D\uD2B8',
  titleRequired: '\uD504\uB85C\uC81D\uD2B8 \uC81C\uBAA9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.',
  saveFailTitle: '\uD504\uB85C\uC81D\uD2B8 \uC800\uC7A5 \uC2E4\uD328',
  saveFailMessage: '\uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574\uC8FC\uC138\uC694.',
  infoTitle: '\uD504\uB85C\uC81D\uD2B8 \uAE30\uBCF8 \uC815\uBCF4',
  infoSubtitle: '\uD504\uB85C\uC81D\uD2B8 \uC81C\uBAA9\uACFC \uC124\uBA85\uC744 \uBA3C\uC800 \uC785\uB825\uD574\uC8FC\uC138\uC694.',
  titleLabel: '\uD504\uB85C\uC81D\uD2B8 \uC81C\uBAA9',
  descLabel: '\uD504\uB85C\uC81D\uD2B8 \uC124\uBA85',
  titlePlaceholder: '\uD504\uB85C\uC81D\uD2B8 \uC81C\uBAA9\uC744 \uC785\uB825\uD558\uC138\uC694',
  descPlaceholder: '\uD504\uB85C\uC81D\uD2B8\uC5D0 \uB300\uD55C \uAC04\uB2E8\uD55C \uC124\uBA85\uC744 \uC785\uB825\uD558\uC138\uC694',
  saving: '\uC800\uC7A5 \uC911...',
  scenarioButton: 'AI \uC2DC\uB098\uB9AC\uC624 \uC0DD\uC131',
  scenarioHint: 'AI\uAC00 \uC7A5\uB974, \uBD84\uC704\uAE30\uB97C \uBC14\uD0D5\uC73C\uB85C \uC52C\uBCC4 \uC2A4\uD1A0\uB9AC\uB97C \uC790\uB3D9 \uC0DD\uC131\uD569\uB2C8\uB2E4.',
};

const isInitialProject = computed(() => {
  if (!props.project) return false;
  const isDefaultTitle = props.project.title?.trim() === labels.defaultTitle;
  const isEmptyDescription = !(props.project.description && props.project.description.trim());
  const hasNoScenes = props.scenes.length === 0;
  return isDefaultTitle && isEmptyDescription && hasNoScenes;
});

watch(
  () => props.project,
  (project) => {
    if (!project) return;
    localTitle.value = project.title || '';
    localDescription.value = project.description || '';
  },
  { immediate: true }
);

const hasProjectChanges = computed(() => {
  if (!props.project) return false;
  const trimmedTitle = localTitle.value.trim();
  const trimmedDescription = localDescription.value.trim();
  return (
    trimmedTitle !== (props.project.title || '') ||
    trimmedDescription !== (props.project.description || '')
  );
});

const saveProjectInfo = async (genre: string): Promise<boolean> => {
  if (!props.project) return false;
  const trimmedTitle = localTitle.value.trim();
  const trimmedDescription = localDescription.value.trim();

  if (!genre) {
    return false;
  }

  if (!trimmedTitle) {
    uiStore.showToast({
      type: 'error',
      title: '프로젝트 제목을 입력해주세요.',
    });
    localTitle.value = props.project.title || labels.defaultTitle;
    return false;
  }

  const unchanged =
    trimmedTitle === (props.project.title || '') &&
    trimmedDescription === (props.project.description || '');

  if (unchanged) return true;

  isSavingProject.value = true;
  const updated = await projectStore.updateProject(props.project.projectId, {
    title: trimmedTitle,
    description: trimmedDescription,
    genre,
  });
  isSavingProject.value = false;

  if (!updated) {
    uiStore.showToast({
      type: 'error',
      title: '프로젝트 저장 실패',
      message: '잠시 후 다시 시도해주세요.',
    });
    return false;
  }
  return true;
};

const openDeleteSceneModal = (scene: Scene) => {
  deleteTargetScene.value = scene;
};

const closeDeleteSceneModal = () => {
  deleteTargetScene.value = null;
};

const confirmDeleteScene = async () => {
  if (!deleteTargetScene.value) return;
  const target = deleteTargetScene.value;
  deleteTargetScene.value = null;
  await props.handleDeleteScene(target);
};

const deleteSceneMessage = computed(() => {
  if (!deleteTargetScene.value) return '';
  return `"${deleteTargetScene.value.title}" 씬을 삭제하시겠어요? 이 작업은 되돌릴 수 없습니다.`;
});

const handleOpenScenario = async () => {
  if (!props.project) {
    props.onOpenScenario();
    return;
  }
  if (isSavingProject.value) return;

  const trimmedTitle = localTitle.value.trim();
  const trimmedDescription = localDescription.value.trim();

  if (!trimmedTitle) {
    uiStore.showToast({
      type: 'error',
      title: labels.titleRequired,
    });
    return;
  }

  const shouldSave = isInitialProject.value || hasProjectChanges.value;
  const hasGenre = Boolean(props.project.genre && props.project.genre.trim());

  if (shouldSave && hasGenre) {
    const ok = await saveProjectInfo(props.project.genre as string);
    if (ok) {
      props.onOpenScenario();
    }
    return;
  }

  if (shouldSave && !hasGenre && props.project.projectId) {
    scenarioStore.setPendingProjectInfo(
      props.project.projectId,
      trimmedTitle,
      trimmedDescription
    );
  }

  props.onOpenScenario();
};

</script>

<template>
  <div class="tab-content">
    <Card class="project-info-card">
      <div class="info-header">
        <h3 class="info-title">{{ labels.infoTitle }}</h3>
        <p class="info-subtitle">{{ labels.infoSubtitle }}</p>
      </div>

      <div class="form-group">
        <label class="form-label required">{{ labels.titleLabel }}</label>
        <input
          v-model="localTitle"
          type="text"
          class="form-input"
          :placeholder="labels.titlePlaceholder"
        />
      </div>

      <div class="form-group">
        <label class="form-label">{{ labels.descLabel }}</label>
        <textarea
          v-model="localDescription"
          class="form-input form-textarea"
          :placeholder="labels.descPlaceholder"
          rows="3"
        ></textarea>
      </div>

      <p v-if="isSavingProject" class="save-status">{{ labels.saving }}</p>
    </Card>

    <div class="scenario-trigger mb-6">
      <Button variant="primary" @click="handleOpenScenario">
        <Sparkles class="icon-sm" />
        {{ labels.scenarioButton }}
      </Button>
      <p class="scenario-hint">{{ labels.scenarioHint }}</p>
    </div>

    <ScenarioDrawer />

    <div class="section-header">
      <h3 class="section-title">씬 리스트</h3>
      <Button variant="secondary" size="sm" @click="handleAddScene">
        <Plus class="icon-sm" />
        씬 추가
      </Button>
    </div>

    <div class="scene-list">
      <div
        v-for="scene in scenes"
        :key="scene.sceneId"
        @dragstart="handleDragStart(scene)"
        @dragend="handleDragEnd"
        @dragover="(e) => handleDragOver(e, scene)"
      >
        <SceneCard
          :scene="scene"
          :project-id="projectId"
          :draggable="true"
          :show-thumbnail="false"
          @delete="openDeleteSceneModal"
        >
          <template #actions-left>
            <Button
              variant="secondary"
              size="sm"
              :disabled="isPreviewLoading(scene.sceneId)"
              @click="toggleStoryboard(scene.sceneId)"
            >
              <Play class="icon-sm" />
              미리보기
            </Button>
          </template>

          <template #extra-inside>
            <div
              v-show="storyboardOpenMap[scene.sceneId]"
              class="storyboard-wrap"
            >
              <template v-if="isPreviewLoading(scene.sceneId)">
                <div class="storyboard-empty">영상 불러오는 중..</div>
              </template>
              <template v-else-if="getScenePreview(scene.sceneId).clips.length === 0">
                <div class="storyboard-empty">
                  <img src="/icon.png" alt="아직 미리보기가 없어요" class="storyboard-empty-icon" />
                  <span>생성된 영상이 없습니다.</span>
                </div>
              </template>
              <template v-else>
                <div class="storyboard-strip" @wheel.prevent="handleStoryboardWheel">
                  <button
                    v-for="(clip, index) in getScenePreview(scene.sceneId).clips"
                    :key="`${scene.sceneId}-storyboard-${index}`"
                    type="button"
                    class="storyboard-item"
                    @mouseenter="playPreviewVideo(scene.sceneId, index)"
                    @mouseleave="pausePreviewVideo(scene.sceneId, index)"
                    @focus="playPreviewVideo(scene.sceneId, index)"
                    @blur="pausePreviewVideo(scene.sceneId, index)"
                    @click="pausePreviewVideo(scene.sceneId, index); openPreview(scene.sceneId, index)"
                  >
                    <div class="storyboard-media">
                      <img
                        class="storyboard-thumb"
                        :src="clip.thumbnailUrl || scene.thumbnailUrl"
                        :alt="clip.label || scene.title"
                      />
                      <video
                        v-if="clip.contentUrl"
                        class="storyboard-video"
                        :src="clip.contentUrl"
                        muted
                        playsinline
                        loop
                        preload="metadata"
                        :ref="(el) => setPreviewVideoRef(scene.sceneId, index, el as HTMLVideoElement | null)"
                      />
                    </div>
                    <span class="storyboard-label">{{ clip.label || scene.title }}</span>
                  </button>
                </div>
</template>
            </div>
          </template>
        </SceneCard>
      </div>

      <Card
        :dashed="true"
        :clickable="true"
        class="add-scene-card"
        @click="handleAddScene"
      >
        <PlusCircle class="add-icon" />
        씬 추가
      </Card>
    </div>
  </div>


  <div v-if="activePreviewClip" class="preview-modal" @click.self="closePreview">
    <div class="preview-modal-content">
      <div class="preview-modal-header">
        <div>
          <p class="preview-modal-title">
            {{ activePreviewScene?.title || '씬 미리보기' }}
          </p>
          <p v-if="activePreviewClip.label" class="preview-modal-subtitle">
            {{ activePreviewClip.label }}
          </p>
        </div>
        <button type="button" class="preview-modal-close" @click="closePreview">
          <X class="icon-sm" />
        </button>
      </div>
      <div class="preview-modal-body">
        <video
          v-if="activePreviewClip.contentUrl"
          :src="activePreviewClip.contentUrl"
          controls
          autoplay
        />
        <div v-else class="preview-modal-empty">
          <img
            v-if="activePreviewClip.thumbnailUrl"
            :src="activePreviewClip.thumbnailUrl"
            :alt="activePreviewClip.label || 'preview'"
          />
          <template v-else>
            <img src="/icon.png" alt="아직 미리보기가 없어요" class="preview-modal-empty-icon" />
            <span>?? ????? ?? ????.</span>
          </template>
        </div>
      </div>
    </div>
  </div>

  <ConfirmModal
    :is-open="!!deleteTargetScene"
    title="씬 삭제"
    :message="deleteSceneMessage"
    confirm-text="삭제"
    cancel-text="취소"
    :is-dangerous="true"
    @confirm="confirmDeleteScene"
    @cancel="closeDeleteSceneModal"
  />
</template>

<style scoped>
.storyboard-media {
  position: relative;
  width: 100%;
  height: 72px;
}

.storyboard-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: opacity 0.2s ease;
}

.storyboard-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.storyboard-item:hover .storyboard-video,
.storyboard-item:focus-visible .storyboard-video {
  opacity: 1;
}

.storyboard-item:hover .storyboard-thumb,
.storyboard-item:focus-visible .storyboard-thumb {
  opacity: 0;
}

.project-info-card {
  margin-bottom: 1.5rem;
}

.info-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 1rem;
}

.info-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
}

.info-subtitle {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--gray-500);
}

.save-status {
  margin: 0;
  font-size: 0.75rem;
  color: var(--gray-500);
}
</style>
