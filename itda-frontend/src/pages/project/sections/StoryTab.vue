<script setup lang="ts">
import type { Scene } from '../../../types/api/scenes';
import type { ScenePreview } from '../composables/useProjectDetail';
import ScenarioDrawer from '../../../components/scenario/ScenarioDrawer.vue';
import SceneCard from '../../../components/project/SceneCard.vue';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import { Plus, PlusCircle, Play, Sparkles } from 'lucide-vue-next';

interface Props {
  scenes: Scene[];
  projectId: number;
  storyboardOpenMap: Record<number, boolean>;
  getScenePreview: (sceneId: number) => ScenePreview;
  isPreviewLoading: (sceneId: number) => boolean;
  toggleStoryboard: (sceneId: number) => void | Promise<void>;
  handleStoryboardWheel: (event: WheelEvent) => void;
  handleAddScene: () => void | Promise<void>;
  handleDragStart: (scene: Scene) => void;
  handleDragEnd: () => void | Promise<void>;
  handleDragOver: (event: DragEvent, targetScene: Scene) => void;
  onOpenScenario: () => void;
}

defineProps<Props>();
</script>

<template>
  <div class="tab-content">
    <div class="scenario-trigger mb-6">
      <Button variant="primary" @click="onOpenScenario">
        <Sparkles class="icon-sm" />
        AI 시나리오 생성
      </Button>
      <p class="scenario-hint">AI가 장르, 분위기를 바탕으로 씬별 스토리를 자동 생성합니다.</p>
    </div>

    <ScenarioDrawer />

    <div class="section-header">
      <h3 class="section-title">Scenes</h3>
      <Button variant="secondary" size="sm" @click="handleAddScene">
        <Plus class="icon-sm" />
        Add Scene
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

          <template #extra-content>
            <div
              v-show="storyboardOpenMap[scene.sceneId]"
              class="storyboard-wrap"
            >
              <div class="storyboard-strip" @wheel.prevent="handleStoryboardWheel">
                <template v-if="isPreviewLoading(scene.sceneId)">
                  <div class="storyboard-empty">영상 불러오는 중...</div>
                </template>
                <template v-else-if="getScenePreview(scene.sceneId).clips.length === 0">
                  <div class="storyboard-empty">생성된 영상이 없습니다.</div>
                </template>
                <button
                  v-for="(clip, index) in getScenePreview(scene.sceneId).clips"
                  :key="`${scene.sceneId}-storyboard-${index}`"
                  type="button"
                  class="storyboard-item"
                >
                  <img
                    class="storyboard-thumb"
                    :src="clip.thumbnailUrl || scene.thumbnailUrl"
                    :alt="clip.label || scene.title"
                  />
                  <span class="storyboard-label">{{ clip.label || scene.title }}</span>
                </button>
              </div>
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
        Add New Scene
      </Card>
    </div>
  </div>
</template>
