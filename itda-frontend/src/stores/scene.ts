import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Scene, CreateSceneRequest } from '../types'
import { sceneService } from '../services'
import type { GenerateScenesRequest } from '../services/mock/scenes' // Keep type import if needed, or move to types

export const useSceneStore = defineStore('scene', () => {
  // State
  const scenes = ref<Scene[]>([])
  const isLoading = ref(false)
  const isGenerating = ref(false)
  const error = ref<string | null>(null)
  const currentProjectId = ref<number | null>(null)

  // Getters
  const sceneCount = computed(() => scenes.value.length)

  const completedScenes = computed(() =>
    scenes.value.filter((s) => s.status === 'COMPLETED')
  )

  const orderedScenes = computed(() =>
    [...scenes.value].sort((a, b) => a.order - b.order)
  )

  const progress = computed(() => ({
    completed: completedScenes.value.length,
    total: scenes.value.length,
    percentage: scenes.value.length > 0
      ? Math.round((completedScenes.value.length / scenes.value.length) * 100)
      : 0,
  }))

  // Actions
  async function loadScenes(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    currentProjectId.value = projectId

    try {
      const scenesData = await sceneService.fetchScenesByProjectId(projectId)
      scenes.value = scenesData
    } catch (e) {
      error.value = 'Failed to load scenes'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function addScene(data: CreateSceneRequest): Promise<Scene | null> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return null
    }

    isLoading.value = true
    error.value = null

    try {
      const newScene = await sceneService.createScene(currentProjectId.value, data)
      scenes.value.push(newScene)
      return newScene
    } catch (e) {
      error.value = 'Failed to create scene'
      console.error(e)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function addScenes(dataList: CreateSceneRequest[]): Promise<Scene[]> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return []
    }

    isLoading.value = true
    error.value = null

    try {
      const newScenes = await sceneService.createScenes(currentProjectId.value, dataList)
      scenes.value.push(...newScenes)
      return newScenes
    } catch (e) {
      error.value = 'Failed to create scenes'
      console.error(e)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function updateScene(sceneId: number, data: Partial<Scene>): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    try {
      const updated = await sceneService.updateScene(currentProjectId.value, sceneId, data)
      if (updated) {
        const index = scenes.value.findIndex((s) => s.sceneId === sceneId)
        if (index !== -1) {
          scenes.value[index] = updated
        }
        return true
      }
      return false
    } catch (e) {
      error.value = 'Failed to update scene'
      console.error(e)
      return false
    }
  }

  async function removeScene(sceneId: number): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    isLoading.value = true
    error.value = null

    try {
      const success = await sceneService.deleteScene(currentProjectId.value, sceneId)
      if (success) {
        scenes.value = scenes.value.filter((s) => s.sceneId !== sceneId)
        // Re-order remaining scenes
        scenes.value.forEach((scene, idx) => {
          scene.order = idx + 1
        })
      }
      return success
    } catch (e) {
      error.value = 'Failed to delete scene'
      console.error(e)
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function reorderScenes(sceneIds: number[]): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    try {
      const reordered = await sceneService.reorderScenes(currentProjectId.value, sceneIds)
      scenes.value = reordered
      return true
    } catch (e) {
      error.value = 'Failed to reorder scenes'
      console.error(e)
      return false
    }
  }

  async function generateScenes(request: GenerateScenesRequest): Promise<Scene[]> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return []
    }

    isGenerating.value = true
    error.value = null

    try {
      const generated = await sceneService.generateScenesWithAI(currentProjectId.value, request)
      scenes.value.push(...generated)
      return generated
    } catch (e) {
      error.value = 'Failed to generate scenes'
      console.error(e)
      return []
    } finally {
      isGenerating.value = false
    }
  }

  function clearScenes(): void {
    scenes.value = []
    currentProjectId.value = null
    error.value = null
  }

  return {
    // State
    scenes,
    isLoading,
    isGenerating,
    error,
    currentProjectId,
    // Getters
    sceneCount,
    completedScenes,
    orderedScenes,
    progress,
    // Actions
    loadScenes,
    addScene,
    addScenes,
    updateScene,
    removeScene,
    reorderScenes,
    generateScenes,
    clearScenes,
  }
})
