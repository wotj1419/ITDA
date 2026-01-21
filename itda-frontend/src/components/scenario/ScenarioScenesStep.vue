<script setup lang="ts">
import { ref } from 'vue'
import { Film, Plus, Trash2, RefreshCw, GripVertical, Check, Info } from 'lucide-vue-next'
import { useScenarioStore, type ScenarioScene } from '../../stores/scenario'
import { useSceneStore } from '../../stores/scene'
import { useUIStore } from '../../stores/ui'
import Button from '../common/Button.vue'

const scenarioStore = useScenarioStore()
const sceneStore = useSceneStore()
const uiStore = useUIStore()

const editingSceneId = ref<number | null>(null)
const draggedId = ref<number | null>(null)

const startEdit = (scene: ScenarioScene) => {
  editingSceneId.value = scene.id
}

const saveEdit = () => {
  editingSceneId.value = null
}

const handleDragStart = (scene: ScenarioScene) => {
  draggedId.value = scene.id
}

const handleDragOver = (event: DragEvent, targetScene: ScenarioScene) => {
  event.preventDefault()
  if (!draggedId.value || draggedId.value === targetScene.id) return

  const draggedIndex = scenarioStore.scenes.findIndex((s) => s.id === draggedId.value)
  const targetIndex = scenarioStore.scenes.findIndex((s) => s.id === targetScene.id)

  if (draggedIndex !== -1 && targetIndex !== -1) {
    const newOrder = scenarioStore.scenes.map((s) => s.id)
    newOrder.splice(draggedIndex, 1)
    newOrder.splice(targetIndex, 0, draggedId.value!)
    scenarioStore.reorderScenes(newOrder)
  }
}

const handleDragEnd = () => {
  draggedId.value = null
}

const handleApplyToProject = async () => {
  // Generate scenes to SceneStore
  for (const scene of scenarioStore.scenes) {
    await sceneStore.addScene({
      title: scene.title,
      description: scene.description,
    })
  }

  uiStore.showToast({
    type: 'success',
    title: '씬 생성 완료',
    message: `${scenarioStore.scenes.length}개의 씬이 프로젝트에 추가되었습니다.`,
  })

  scenarioStore.closeDrawer()
  scenarioStore.resetWizard()
}
</script>

<template>
  <div class="scenes-step">
    <div class="step-header">
      <Film class="header-icon" />
      <h3 class="header-title">씬별 스토리</h3>
    </div>
    <p class="step-description">
      씬 순서를 변경하거나 개별 씬을 편집하세요. 드래그하여 순서를 변경할 수 있습니다.
    </p>

    <!-- Scene List -->
    <div class="scene-list">
      <div
        v-for="scene in scenarioStore.scenes"
        :key="scene.id"
        :class="['scene-item', { dragging: draggedId === scene.id }]"
        draggable="true"
        @dragstart="handleDragStart(scene)"
        @dragover="(e) => handleDragOver(e, scene)"
        @dragend="handleDragEnd"
      >
        <!-- Drag Handle -->
        <div class="drag-handle">
          <GripVertical class="grip-icon" />
        </div>

        <!-- Scene Content -->
        <div class="scene-content">
          <div class="scene-header">
            <span class="scene-number">씬 {{ scene.order }}</span>
            <div class="scene-actions">
              <button
                v-if="editingSceneId !== scene.id"
                class="action-btn"
                title="재생성"
                :disabled="scenarioStore.isGenerating"
                @click="scenarioStore.regenerateScene(scene.id)"
              >
                <RefreshCw class="icon-sm" />
              </button>
              <button
                class="action-btn danger"
                title="삭제"
                @click="scenarioStore.removeScene(scene.id)"
              >
                <Trash2 class="icon-sm" />
              </button>
            </div>
          </div>

          <!-- Edit Mode -->
          <template v-if="editingSceneId === scene.id">
            <input
              v-model="scene.title"
              class="scene-title-input"
              placeholder="씬 제목"
            />
            <textarea
              v-model="scene.description"
              class="scene-desc-input"
              rows="3"
              placeholder="씬 설명"
            />
            <Button variant="primary" size="sm" @click="saveEdit">
              저장
            </Button>
          </template>

          <!-- View Mode -->
          <template v-else>
            <h4 class="scene-title" @click="startEdit(scene)">
              {{ scene.title }}
            </h4>
            <p class="scene-description" @click="startEdit(scene)">
              {{ scene.description }}
            </p>
          </template>
        </div>
      </div>

      <!-- Add Scene Button -->
      <button class="add-scene-btn" @click="scenarioStore.addScene">
        <Plus class="icon-sm" />
        씬 추가
      </button>
    </div>

    <!-- Apply Button -->
    <div class="step-footer">
      <Button
        variant="primary"
        size="lg"
        :disabled="scenarioStore.scenes.length === 0"
        @click="handleApplyToProject"
      >
        <Check class="icon-sm" />
        프로젝트에 적용 ({{ scenarioStore.scenes.length }}개 씬)
      </Button>
    </div>

    <p class="step-hint">
      <Info class="hint-icon" /> 적용 후에도 Story 탭에서 개별 씬을 수정할 수 있습니다.
    </p>
  </div>
</template>

<style scoped>
.scenes-step {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  width: 24px;
  height: 24px;
  color: var(--rose-500);
}

.header-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.step-description {
  color: var(--gray-500);
  font-size: 0.875rem;
  margin: 0;
}

.scene-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 0.25rem;
}

.scene-item {
  display: flex;
  gap: 0.75rem;
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 10px;
  padding: 0.875rem;
  transition: all 0.2s ease;
}

.scene-item:hover {
  border-color: var(--rose-200);
  box-shadow: 0 2px 8px rgba(255, 133, 161, 0.08);
}

.scene-item.dragging {
  opacity: 0.5;
  border-color: var(--rose-300);
}

.drag-handle {
  display: flex;
  align-items: flex-start;
  padding-top: 0.25rem;
  cursor: grab;
  color: var(--gray-400);
}

.drag-handle:active {
  cursor: grabbing;
}

.grip-icon {
  width: 16px;
  height: 16px;
}

.scene-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.scene-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.scene-number {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
  text-transform: uppercase;
}

.scene-actions {
  display: flex;
  gap: 0.25rem;
}

.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: var(--gray-400);
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: var(--gray-100);
  color: var(--gray-600);
}

.action-btn.danger:hover {
  background: var(--rose-50);
  color: var(--rose-500);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.scene-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
  cursor: pointer;
}

.scene-title:hover {
  color: var(--rose-600);
}

.scene-description {
  font-size: 0.8125rem;
  color: var(--gray-600);
  line-height: 1.5;
  margin: 0;
  cursor: pointer;
}

.scene-description:hover {
  color: var(--gray-700);
}

.scene-title-input,
.scene-desc-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--gray-200);
  border-radius: 6px;
  font-size: 0.875rem;
  font-family: inherit;
}

.scene-title-input:focus,
.scene-desc-input:focus {
  outline: none;
  border-color: var(--rose-300);
}

.scene-desc-input {
  resize: vertical;
  min-height: 60px;
}

.add-scene-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--rose-50);
  border: 2px dashed var(--rose-200);
  border-radius: 10px;
  color: var(--rose-500);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-scene-btn:hover {
  background: var(--rose-100);
  border-color: var(--rose-300);
}

.step-footer {
  margin-top: 0.5rem;
}

.step-hint {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

.hint-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* Uses global .icon-sm from base.css */
</style>
