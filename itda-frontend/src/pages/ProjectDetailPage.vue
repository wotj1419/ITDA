<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Plus, PlusCircle, Sparkles, Play, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useProjectStore } from '../stores/project'
import { useSceneStore } from '../stores/scene'
import { useCharacterStore } from '../stores/character'
import { useUIStore } from '../stores/ui'
import { useCollabStore } from '../stores/collab'
import { useScenarioStore } from '../stores/scenario'
import type { ObjectSheet, Scene, SceneStatus } from '../types'
import { fetchNodesBySceneId } from '../services/mock/nodes'

import ProjectLayout from '../layouts/ProjectLayout.vue'
import Card from '../components/common/Card.vue'
import Button from '../components/common/Button.vue'
import Badge from '../components/common/Badge.vue'
import SceneCard from '../components/project/SceneCard.vue'
import ScenarioDrawer from '../components/scenario/ScenarioDrawer.vue'
import CharacterCard from '../components/project/CharacterCard.vue'
import AddCharacterModal from '../components/project/AddCharacterModal.vue'

const route = useRoute()
const projectStore = useProjectStore()
const sceneStore = useSceneStore()
const characterStore = useCharacterStore()
const uiStore = useUIStore()
const collabStore = useCollabStore()
const scenarioStore = useScenarioStore()

// State
type ProjectTab = 'story' | 'scenes' | 'objects' | 'timeline' | 'settings'

interface ScenePreviewClip {
  thumbnailUrl: string
  duration: number
  label?: string
  contentUrl?: string
}

interface ScenePreview {
  clips: ScenePreviewClip[]
  totalDuration: number
}

const activeTab = ref<ProjectTab>('story')
const draggedScene = ref<Scene | null>(null)
const scenePreviewMap = ref<Record<number, ScenePreview>>({})
const previewLoadingMap = ref<Record<number, boolean>>({})
const activePreview = ref<{ sceneId: number; clipIndex: number } | null>(null)
const previewTracks = ref<Record<number, HTMLDivElement | null>>({})
const storyboardOpenMap = ref<Record<number, boolean>>({})

const previewVisibleLimit = 6

const tabItems: { key: ProjectTab; label: string }[] = [
  { key: 'story', label: 'Story' },
  { key: 'scenes', label: 'Scenes' },
  { key: 'objects', label: 'Objects' }, // PRD v2.5: 캐릭터 -> 오브젝트로 명칭 변경
]

// Computed
const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)
const scenes = computed(() => sceneStore.orderedScenes)
const characters = computed(() => characterStore.characters)
const isGeneratingCharacter = computed(() => characterStore.isGenerating)

const sceneStatusConfig: Record<SceneStatus, { label: string; variant: 'success' | 'info' | 'default' }> = {
  COMPLETED: { label: '완료', variant: 'success' },
  IN_PROGRESS: { label: '진행 중', variant: 'info' },
  DRAFT: { label: '초안', variant: 'default' },
}

// Load data on mount
onMounted(async () => {
  if (projectId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      characterStore.loadCharacters(projectId.value),
    ])
    
    // 협업 방 입장
    collabStore.joinRoom(projectId.value)
    collabStore.updateLocation('프로젝트 상세 페이지')
  }

})

onUnmounted(() => {
  // 페이지 이탈 시 협업 방 퇴장
  collabStore.leaveRoom()
})

// Watch for route changes
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      const id = Number(newId)
      scenePreviewMap.value = {}
      previewLoadingMap.value = {}
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

const getSceneEditLink = (scene: Scene) => ({
  name: 'scene-edit',
  params: {
    projectId: projectId.value,
    sceneId: scene.sceneId,
  },
})

const getScenePreview = (sceneId: number): ScenePreview => {
  return scenePreviewMap.value[sceneId] || { clips: [], totalDuration: 0 }
}

const isPreviewLoading = (sceneId: number): boolean =>
  Boolean(previewLoadingMap.value[sceneId])

const buildScenePreview = async (sceneId: number): Promise<ScenePreview> => {
  if (!projectId.value) return { clips: [], totalDuration: 0 }
  const nodes = await fetchNodesBySceneId(projectId.value, sceneId)
  const clips = nodes
    .filter((node) => node.type === 'VIDEO' && node.isConfirmed)
    .map((node) => ({
      thumbnailUrl: node.thumbnailUrl || '',
      duration: node.settings?.duration || 4,
      label: node.title,
      contentUrl: node.contentUrl || '',
    }))
  const totalDuration = clips.reduce((sum, clip) => sum + clip.duration, 0)
  return { clips, totalDuration }
}

const loadScenePreviews = async () => {
  if (!projectId.value || scenes.value.length === 0) {
    scenePreviewMap.value = {}
    return
  }

  const previews: Record<number, ScenePreview> = { ...scenePreviewMap.value }
  await Promise.all(
    scenes.value.map(async (scene) => {
      previewLoadingMap.value[scene.sceneId] = true
      previews[scene.sceneId] = await buildScenePreview(scene.sceneId)
      previewLoadingMap.value[scene.sceneId] = false
    })
  )
  scenePreviewMap.value = previews
}

watch(
  [() => activeTab.value, scenes],
  ([tab]) => {
    if (tab === 'scenes') {
      loadScenePreviews()
    }
  }
)

const openPreview = (sceneId: number, clipIndex: number) => {
  activePreview.value = { sceneId, clipIndex }
}

const closePreview = () => {
  activePreview.value = null
}

const activePreviewClip = computed(() => {
  if (!activePreview.value) return null
  const preview = getScenePreview(activePreview.value.sceneId)
  return preview.clips[activePreview.value.clipIndex] || null
})

const activePreviewScene = computed(() => {
  if (!activePreview.value) return null
  return scenes.value.find((scene) => scene.sceneId === activePreview.value?.sceneId) || null
})

const setPreviewTrackRef = (sceneId: number, el: HTMLDivElement | null) => {
  previewTracks.value[sceneId] = el
}

const scrollPreview = (sceneId: number, direction: -1 | 1) => {
  const track = previewTracks.value[sceneId]
  if (!track) return
  const amount = Math.max(180, track.clientWidth * 0.6)
  track.scrollBy({ left: direction * amount, behavior: 'smooth' })
}

const handlePreviewWheel = (sceneId: number, event: WheelEvent) => {
  const track = previewTracks.value[sceneId]
  if (!track) return
  if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
    event.preventDefault()
    track.scrollBy({ left: event.deltaY, behavior: 'auto' })
  }
}

const getClipWidth = (duration: number): number =>
  Math.min(Math.max(duration * 20, 64), 220)

const getOverflowCount = (sceneId: number): number => {
  const count = getScenePreview(sceneId).clips.length
  return count > previewVisibleLimit ? count - previewVisibleLimit : 0
}

const ensureScenePreview = async (sceneId: number) => {
  if (scenePreviewMap.value[sceneId]) return
  previewLoadingMap.value[sceneId] = true
  const preview = await buildScenePreview(sceneId)
  scenePreviewMap.value = {
    ...scenePreviewMap.value,
    [sceneId]: preview,
  }
  previewLoadingMap.value[sceneId] = false
}

const toggleStoryboard = async (sceneId: number) => {
  const next = !storyboardOpenMap.value[sceneId]
  storyboardOpenMap.value = {
    ...storyboardOpenMap.value,
    [sceneId]: next,
  }
  if (next) {
    await ensureScenePreview(sceneId)
  }
}

const handleStoryboardWheel = (event: WheelEvent) => {
  const container = event.currentTarget as HTMLElement | null
  if (!container) return
  container.scrollLeft += event.deltaY + event.deltaX
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
          v-for="tab in tabItems"
          :key="tab.key"
          :class="['tab', { active: activeTab === tab.key }]"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Story Tab -->
      <div v-if="activeTab === 'story'" class="tab-content">
        <!-- AI Scenario Generation Button -->
        <div class="scenario-trigger mb-6">
          <Button variant="primary" @click="scenarioStore.openDrawer()">
            <Sparkles class="icon-sm" />
            AI 시나리오 생성
          </Button>
          <p class="scenario-hint">AI가 장르, 분위기를 바탕으로 씬별 스토리를 자동 생성합니다.</p>
        </div>

        <!-- Scenario Drawer (responsive sidebar/modal) -->
        <ScenarioDrawer />

        <!-- Scene List -->
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
          <h2 class="section-title">Scene Preview</h2>
          <div class="section-actions">
            <RouterLink
              :to="{ name: 'timeline', params: { id: projectId } }"
              custom
              v-slot="{ navigate }"
            >
              <Button variant="secondary" size="sm" @click="navigate">
                Full Timeline
              </Button>
            </RouterLink>
          </div>
        </div>
        <p class="text-muted">
          씬별로 확정된 영상을 미리 확인하고 빠르게 편집 화면으로 이동할 수 있습니다.
        </p>

        <div v-if="scenes.length === 0" class="empty-state">
          아직 생성된 씬이 없습니다.
        </div>

        <div v-else class="scene-preview-list">
          <Card v-for="scene in scenes" :key="scene.sceneId" class="scene-preview-card">
            <div class="scene-preview-row">
              <div class="preview-info">
                <div class="preview-header">
                  <Badge variant="default" size="sm">SCENE {{ scene.order }}</Badge>
                  <Badge
                    :variant="sceneStatusConfig[scene.status].variant"
                    size="sm"
                    class="status-badge"
                  >
                    {{ sceneStatusConfig[scene.status].label }}
                  </Badge>
                </div>
                <h4 class="preview-title">{{ scene.title }}</h4>
                <p v-if="scene.description" class="preview-description">
                  {{ scene.description }}
                </p>
                <div class="preview-meta">
                  <span>클립 {{ getScenePreview(scene.sceneId).clips.length }}개</span>
                  <span>{{ getScenePreview(scene.sceneId).totalDuration }}s</span>
                </div>
                <div class="preview-actions">
                  <RouterLink :to="getSceneEditLink(scene)" custom v-slot="{ navigate }">
                    <Button variant="secondary" size="sm" @click="navigate">
                      씬 편집
                    </Button>
                  </RouterLink>
                  <RouterLink
                    :to="{ name: 'timeline', params: { id: projectId }, query: { sceneId: scene.sceneId } }"
                    custom
                    v-slot="{ navigate }"
                  >
                    <Button variant="primary" size="sm" @click="navigate">
                      Scene Timeline
                    </Button>
                  </RouterLink>
                </div>
              </div>

              <div class="preview-media">
                <div v-if="isPreviewLoading(scene.sceneId)" class="preview-loading">
                  미리보기를 불러오는 중...
                </div>
                <template v-else>
                  <div
                    v-if="getScenePreview(scene.sceneId).clips.length > 0"
                    class="preview-strip"
                    :ref="(el) => setPreviewTrackRef(scene.sceneId, el as HTMLDivElement | null)"
                    @wheel="(event) => handlePreviewWheel(scene.sceneId, event)"
                  >
                    <button
                      v-for="(clip, index) in getScenePreview(scene.sceneId).clips"
                      :key="`${scene.sceneId}-clip-${index}`"
                      class="preview-thumb"
                      type="button"
                      :aria-label="clip.label || scene.title"
                      :style="{ width: `${getClipWidth(clip.duration)}px` }"
                      @click="openPreview(scene.sceneId, index)"
                    >
                      <img
                        :src="clip.thumbnailUrl || scene.thumbnailUrl"
                        :alt="clip.label || scene.title"
                      />
                      <span class="preview-duration">{{ clip.duration }}s</span>
                      <span class="preview-play">
                        <Play class="icon-sm" />
                      </span>
                    </button>
                  </div>
                  <div v-else class="preview-empty">
                    <img
                      v-if="scene.thumbnailUrl"
                      :src="scene.thumbnailUrl"
                      :alt="scene.title"
                    />
                    <span v-else>확정된 영상이 없습니다</span>
                  </div>
                </template>

                <button
                  v-if="getScenePreview(scene.sceneId).clips.length > 0"
                  type="button"
                  class="preview-scroll-btn left"
                  aria-label="Scroll left"
                  @click="scrollPreview(scene.sceneId, -1)"
                >
                  <ChevronLeft class="icon-sm" />
                </button>
                <button
                  v-if="getScenePreview(scene.sceneId).clips.length > 0"
                  type="button"
                  class="preview-scroll-btn right"
                  aria-label="Scroll right"
                  @click="scrollPreview(scene.sceneId, 1)"
                >
                  <ChevronRight class="icon-sm" />
                </button>
                <div
                  v-if="getOverflowCount(scene.sceneId) > 0"
                  class="preview-overflow"
                >
                  +{{ getOverflowCount(scene.sceneId) }}
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- Objects Tab -->
      <div v-if="activeTab === 'objects'" class="tab-content">
        <div class="section-header">
          <h2 class="section-title">오브젝트</h2>
          <Button variant="primary" @click="openAddCharacterModal">
            <Plus class="icon-sm" />
            오브젝트 추가
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

    <!-- Preview Modal -->
    <div v-if="activePreviewClip" class="preview-modal" @click.self="closePreview">
      <div class="preview-modal-content">
        <div class="preview-modal-header">
          <div>
            <p class="preview-modal-title">
              {{ activePreviewScene?.title || 'Scene Preview' }}
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
            <span v-else>영상 미리보기를 준비 중입니다.</span>
          </div>
        </div>
      </div>
    </div>
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

.section-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Scene List */
.scene-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.storyboard-wrap {
  margin-top: 0.75rem;
}

.storyboard-strip {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 0.25rem;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.storyboard-item {
  flex: 0 0 auto;
  width: 120px;
  border: 1px solid var(--rose-100);
  border-radius: 10px;
  overflow: hidden;
  background: white;
  padding: 0;
  cursor: pointer;
  scroll-snap-align: start;
}

.storyboard-thumb {
  width: 100%;
  height: 72px;
  object-fit: cover;
  display: block;
}

.storyboard-label {
  display: block;
  padding: 0.375rem 0.5rem;
  font-size: 0.6875rem;
  color: var(--gray-600);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.storyboard-empty {
  font-size: 0.75rem;
  color: var(--gray-500);
  padding: 0.5rem 0;
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

/* Scene Preview */
.scene-preview-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.scene-preview-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.scene-preview-row {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 1fr;
  gap: 1.5rem;
  align-items: center;
}

.preview-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.preview-media {
  position: relative;
  background: var(--rose-50);
  border-radius: 12px;
  padding: 0.75rem 1.25rem;
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  overflow: hidden;
}

.preview-strip {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  overflow-x: auto;
  padding-bottom: 0.25rem;
  width: 100%;
  scroll-behavior: smooth;
}

.preview-thumb {
  position: relative;
  height: 54px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  font-size: 0.625rem;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.75);
  color: white;
  padding: 1px 4px;
  border-radius: 4px;
}

.preview-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: rgba(15, 23, 42, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.preview-thumb:hover .preview-play {
  opacity: 1;
}

.preview-scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: white;
  color: var(--gray-600);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  cursor: pointer;
}

.preview-scroll-btn.left {
  left: 8px;
}

.preview-scroll-btn.right {
  right: 8px;
}

.preview-media:hover .preview-scroll-btn {
  opacity: 1;
}

.preview-overflow {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--rose-500);
  background: white;
  border: 1px dashed var(--rose-200);
  border-radius: 999px;
  padding: 0.125rem 0.5rem;
}

.preview-empty {
  width: 100%;
  height: 72px;
  border-radius: 10px;
  border: 1px dashed var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  font-size: 0.75rem;
  background: white;
}

.preview-empty img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.preview-loading {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.preview-body {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.preview-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--gray-900);
}

.preview-description {
  margin: 0;
  color: var(--gray-500);
  font-size: 0.8125rem;
  line-height: 1.4;
}

.preview-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.preview-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.empty-state {
  border: 1px dashed var(--rose-200);
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
  color: var(--gray-500);
  background: var(--rose-50);
  margin-bottom: 1rem;
}

@media (max-width: 960px) {
  .scene-preview-row {
    grid-template-columns: 1fr;
  }
}

/* Preview Modal */
.preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.preview-modal-content {
  width: min(880px, 100%);
  background: white;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
}

.preview-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.preview-modal-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
}

.preview-modal-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--gray-500);
}

.preview-modal-close {
  border: none;
  background: var(--rose-50);
  color: var(--gray-600);
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.preview-modal-close:hover {
  background: var(--rose-100);
  color: var(--rose-500);
}

.preview-modal-body {
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-modal-body video {
  width: 100%;
  max-height: 60vh;
  border-radius: 12px;
  background: black;
}

.preview-modal-empty {
  width: 100%;
  min-height: 240px;
  border-radius: 12px;
  border: 1px dashed var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.75rem;
  color: var(--gray-500);
}

.preview-modal-empty img {
  max-width: 100%;
  border-radius: 12px;
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

/* Scenario Trigger */
.scenario-trigger {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1.25rem;
  background: linear-gradient(135deg, var(--rose-50) 0%, white 100%);
  border: 1px solid var(--rose-100);
  border-radius: 12px;
}

.scenario-hint {
  font-size: 0.8125rem;
  color: var(--gray-500);
  margin: 0;
}

/* Utilities */
.mb-6 {
  margin-bottom: 1.5rem;
}

/* Uses global .icon-sm from base.css */

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
