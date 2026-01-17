<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, PlusCircle } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useSceneStore } from '../stores/scene'
import { useCharacterStore } from '../stores/character'
import { useUIStore } from '../stores/ui'
import type { ObjectSheet, Scene } from '../types'

import ProjectLayout from '../layouts/ProjectLayout.vue'
import Card from '../components/common/Card.vue'
import Button from '../components/common/Button.vue'
import SceneCard from '../components/project/SceneCard.vue'
import StoryPromptForm from '../components/project/StoryPromptForm.vue'
import CharacterCard from '../components/project/CharacterCard.vue'
import AddCharacterModal from '../components/project/AddCharacterModal.vue'

const route = useRoute()
const projectStore = useProjectStore()
const sceneStore = useSceneStore()
const characterStore = useCharacterStore()
const uiStore = useUIStore()

// State
const activeTab = ref<'story' | 'scenes' | 'characters' | 'timeline' | 'settings'>('story')
const draggedScene = ref<Scene | null>(null)

// Computed
const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)
const scenes = computed(() => sceneStore.orderedScenes)
const characters = computed(() => characterStore.characters)
const isGeneratingScenes = computed(() => sceneStore.isGenerating)
const isGeneratingCharacter = computed(() => characterStore.isGenerating)

// Load data on mount
onMounted(async () => {
  if (projectId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      characterStore.loadCharacters(projectId.value),
    ])
  }
})

// Watch for route changes
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      const id = Number(newId)
      await Promise.all([
        projectStore.loadProject(id),
        sceneStore.loadScenes(id),
        characterStore.loadCharacters(id),
      ])
    }
  }
)

// Tab change handler
const handleTabChange = (tab: string) => {
  activeTab.value = tab as typeof activeTab.value
}

// Scene generation handler
const handleGenerateScenes = async (data: {
  genre: string
  mood: string
  sceneCount: number
  synopsis: string
}) => {
  const generated = await sceneStore.generateScenes({
    genre: data.genre,
    mood: data.mood,
    sceneCount: data.sceneCount,
    synopsis: data.synopsis,
  })

  if (generated.length > 0) {
    uiStore.showToast({
      type: 'success',
      title: '씬 생성 완료',
      message: `${generated.length}개의 씬이 AI로 생성되었습니다.`,
    })
  }
}

// Scene drag & drop handlers
const handleDragStart = (scene: Scene) => {
  draggedScene.value = scene
}

const handleDragEnd = async () => {
  if (draggedScene.value) {
    const sceneIds = scenes.value.map((s) => s.sceneId)
    await sceneStore.reorderScenes(sceneIds)
    uiStore.showToast({
      type: 'success',
      title: '순서 변경',
      message: '씬 순서가 변경되었습니다.',
    })
  }
  draggedScene.value = null
}

const handleDragOver = (event: DragEvent, targetScene: Scene) => {
  event.preventDefault()
  if (!draggedScene.value || draggedScene.value.sceneId === targetScene.sceneId) return

  const draggedIndex = scenes.value.findIndex((s) => s.sceneId === draggedScene.value?.sceneId)
  const targetIndex = scenes.value.findIndex((s) => s.sceneId === targetScene.sceneId)

  if (draggedIndex !== -1 && targetIndex !== -1) {
    const newScenes = [...scenes.value]
    newScenes.splice(draggedIndex, 1)
    newScenes.splice(targetIndex, 0, draggedScene.value)
    // Update order in store temporarily (will be persisted on dragEnd)
    newScenes.forEach((s, idx) => {
      s.order = idx + 1
    })
  }
}

// Add new scene
const handleAddScene = async () => {
  const newScene = await sceneStore.addScene({
    title: `New Scene ${scenes.value.length + 1}`,
    description: '',
  })
  if (newScene) {
    uiStore.showToast({
      type: 'success',
      title: '씬 추가',
      message: '새 씬이 추가되었습니다.',
    })
  }
}

// Character handlers
const openAddCharacterModal = () => {
  uiStore.openModal('add-character-modal')
}

const handleAddCharacter = async (data: { name: string; description: string; style: string }) => {
  const newCharacter = await characterStore.generateCharacter({
    name: data.name,
    description: data.description,
    style: data.style,
  })

  if (newCharacter) {
    uiStore.closeModal()
    uiStore.showToast({
      type: 'success',
      title: '캐릭터 생성 완료',
      message: `${data.name} 캐릭터가 추가되었습니다.`,
    })
  }
}

const handleEditCharacter = (character: ObjectSheet) => {
  // TODO: Open edit modal
  console.log('Edit character:', character)
}

const handleDeleteCharacter = async (character: ObjectSheet) => {
  if (confirm(`"${character.name}" 캐릭터를 삭제하시겠습니까?`)) {
    const success = await characterStore.removeCharacter(character.objectId)
    if (success) {
      uiStore.showToast({
        type: 'success',
        title: '캐릭터 삭제',
        message: `${character.name}이(가) 삭제되었습니다.`,
      })
    }
  }
}
</script>

<template>
  <ProjectLayout
    :project="project"
    :active-tab="activeTab"
    :scene-count="scenes.length"
    :progress="sceneStore.progress"
    @tab-change="handleTabChange"
  >
    <div class="project-content">
      <!-- Tabs -->
      <div class="tabs">
        <button
          :class="['tab', { active: activeTab === 'story' }]"
          @click="activeTab = 'story'"
        >
          Story
        </button>
        <button
          :class="['tab', { active: activeTab === 'scenes' }]"
          @click="activeTab = 'scenes'"
        >
          Scenes
        </button>
        <button
          :class="['tab', { active: activeTab === 'characters' }]"
          @click="activeTab = 'characters'"
        >
          Characters
        </button>
      </div>

      <!-- Story Tab -->
      <div v-if="activeTab === 'story'" class="tab-content">
        <!-- Story Prompt Form -->
        <StoryPromptForm
          :is-generating="isGeneratingScenes"
          class="mb-6"
          @generate="handleGenerateScenes"
        />

        <!-- Scene List -->
        <div class="section-header">
          <h3 class="section-title">씬별 스토리</h3>
          <Button variant="secondary" size="sm" @click="handleAddScene">
            <Plus class="btn-icon" />
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
            />
          </div>

          <!-- Add Scene Button -->
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

      <!-- Scenes Tab -->
      <div v-if="activeTab === 'scenes'" class="tab-content">
        <div class="section-header">
          <h2 class="section-title">Scene Breakdown</h2>
          <Button variant="secondary">
            Generate with AI
          </Button>
        </div>
        <p class="text-muted">씬 관리는 Story 탭에서 진행됩니다.</p>
      </div>

      <!-- Characters Tab -->
      <div v-if="activeTab === 'characters'" class="tab-content">
        <div class="section-header">
          <h2 class="section-title">캐릭터</h2>
          <Button variant="primary" @click="openAddCharacterModal">
            <Plus class="btn-icon" />
            캐릭터 추가
          </Button>
        </div>

        <div class="character-grid">
          <CharacterCard
            v-for="character in characters"
            :key="character.objectId"
            :character="character"
            @edit="handleEditCharacter"
            @delete="handleDeleteCharacter"
          />

          <!-- Add Character Card -->
          <Card
            :dashed="true"
            :clickable="true"
            class="add-character-card"
            @click="openAddCharacterModal"
          >
            <div class="add-character-icon">
              <Plus class="add-icon" />
            </div>
            <span class="add-character-text">Add Character</span>
          </Card>
        </div>
      </div>
    </div>

    <!-- Character Modal -->
    <AddCharacterModal
      :is-generating="isGeneratingCharacter"
      @submit="handleAddCharacter"
    />
  </ProjectLayout>
</template>

<style scoped>
.project-content {
  max-width: 900px;
  margin: 0 auto;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--rose-100);
  padding-bottom: 0.5rem;
}

.tab {
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  color: var(--gray-500);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tab:hover {
  background: var(--rose-50);
  color: var(--gray-700);
}

.tab.active {
  background: var(--rose-100);
  color: var(--rose-600);
}

/* Section Header */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

/* Scene List */
.scene-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.add-scene-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  color: var(--gray-500);
  font-size: 0.875rem;
}

.add-icon {
  width: 20px;
  height: 20px;
  color: var(--rose-400);
}

/* Character Grid */
.character-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.add-character-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  text-align: center;
  min-height: 200px;
}

.add-character-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--rose-50);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-character-icon .add-icon {
  width: 24px;
  height: 24px;
}

.add-character-text {
  color: var(--gray-500);
  font-size: 0.875rem;
}

/* Utilities */
.mb-6 {
  margin-bottom: 1.5rem;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.text-muted {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.tab-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
