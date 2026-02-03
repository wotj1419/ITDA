<script setup lang="ts">
import type { Scene } from '../../../types/api/scenes';
import type { ProjectDetail } from '../../../types/api/projects';
import type { ScenePreview, ScenePreviewClip } from '../composables/useProjectDetail';
import ScenarioDrawer from '../../../components/scenario/ScenarioDrawer.vue';
import SceneCard from '../../../components/project/SceneCard.vue';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import ConfirmModal from '../../../components/common/ConfirmModal.vue';
import LazyVideo from '../../../components/media/LazyVideo.vue';
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
  scenarioTitle: 'AI\uB85C \uC2DC\uB098\uB9AC\uC624 \uB9CC\uB4E4\uAE30',
  scenarioHint: '\uC7A5\uB974\uC640 \uBD84\uC704\uAE30\uB97C \uC120\uD0DD\uD558\uBA74, \uC52C \uAD6C\uC131\uBD80\uD130 \uC2A4\uD1A0\uB9AC\uAE4C\uC9C0 \uC790\uB3D9\uC73C\uB85C \uC81C\uC548\uD574\uB4DC\uB824\uC694.',
  scenarioButton: '\uC0DD\uC131\uD558\uAE30',
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
    <div class="story-hero">
      <Card class="scenario-card premium-card scenario-main-card">
        <div class="scenario-card-header">
          <div class="scenario-icon">
            <Sparkles class="icon-md" />
          </div>
          <div>
            <p class="scenario-eyebrow">AI Studio</p>
            <h3 class="scenario-title">{{ labels.scenarioTitle }}</h3>
          </div>
        </div>
        <p class="scenario-description">{{ labels.scenarioHint }}</p>
        <Button variant="primary" class="scenario-btn" @click="handleOpenScenario">
          <Sparkles class="icon-sm" />
          {{ labels.scenarioButton }}
        </Button>
      </Card>
    </div>

    <ScenarioDrawer />

    <div class="section-header story-section-header">
      <div class="section-title-wrap">
        <h3 class="section-title story-section-title">씬 리스트</h3>
        <span class="section-count">{{ scenes.length }}</span>
      </div>
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
                    @click="openPreview(scene.sceneId, index)"
                  >
                    <div class="storyboard-media">
                      <template v-if="clip.contentUrl">
                        <img
                          class="storyboard-thumb"
                          :src="clip.thumbnailUrl || scene.thumbnailUrl || '/icon.png'"
                          :alt="clip.label || scene.title"
                        />
                        <LazyVideo
                          class="storyboard-video"
                          :src="clip.contentUrl"
                          :poster="clip.thumbnailUrl || scene.thumbnailUrl || '/icon.png'"
                          :play-on-hover="true"
                        />
                      </template>
                      <img
                        v-else
                        class="storyboard-thumb"
                        :src="clip.thumbnailUrl || scene.thumbnailUrl || '/icon.png'"
                        :alt="clip.label || scene.title"
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
.story-hero {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.premium-card {
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(255, 232, 242, 0.75);
  box-shadow: 0 16px 40px -28px rgba(255, 133, 161, 0.35);
  backdrop-filter: blur(18px);
}

.premium-card:hover {
  transform: none;
  box-shadow: 0 18px 40px -28px rgba(255, 133, 161, 0.35);
  border-color: rgba(255, 232, 242, 0.9);
}

.scenario-card {
  position: relative;
  overflow: hidden;
  padding: 2.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  min-height: 280px;
  align-items: center;
  text-align: center;
}

.scenario-card::after {
  content: none;
}

.scenario-card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  position: relative;
  z-index: 1;
}

.scenario-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: white;
  border: 1px solid rgba(255, 232, 242, 0.8);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--rose-500);
  box-shadow: 0 10px 24px rgba(255, 133, 161, 0.15);
}

.scenario-eyebrow {
  margin: 0;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.24em;
  color: var(--gray-400);
  font-weight: 800;
}

.scenario-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--gray-900);
}

.scenario-description {
  margin: 0;
  font-size: 0.9rem;
  color: var(--gray-500);
  line-height: 1.5;
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.scenario-btn.btn-primary {
  align-self: center;
  padding: 0.75rem 1.5rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  box-shadow: 0 12px 24px rgba(255, 133, 161, 0.25);
  position: relative;
  z-index: 1;
}

.scenario-btn.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(255, 133, 161, 0.3);
}

.scenario-main-card {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
}

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

.story-section-header {
  margin-top: 2rem;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.story-section-title {
  font-weight: 700;
}

.section-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 0.5rem;
  border-radius: 999px;
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.65rem;
  font-weight: 700;
}

.scene-list {
  margin-top: 1.5rem;
}


.add-scene-card {
  border-style: dashed;
  border-color: var(--rose-200);
  border-radius: 24px;
  padding: 1.75rem;
  gap: 0.75rem;
  background: rgba(255, 255, 255, 0.7);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.add-scene-card:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
  transform: translateY(-2px);
}

@media (max-width: 960px) {
  .story-hero {
    gap: 1rem;
  }
}
</style>
