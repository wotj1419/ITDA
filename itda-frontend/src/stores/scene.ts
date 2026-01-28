import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Scene, CreateSceneRequest } from '../types/api/scenes'
import { normalizeScene } from '../types/mappers/scenes'
import {
  fetchScenes,
  createScene,
  createScenes,
  updateScene as updateSceneApi,
  deleteScene,
  reorderScenes as reorderScenesApi,
} from '../services/api/scenes'
import { useAsyncAction } from './helpers/useAsyncAction'

export interface GenerateScenesRequest {
  genre: string
  mood: string
  sceneCount: number
  synopsis: string
}

export const useSceneStore = defineStore('scene', () => {
  // State
  const scenes = ref<Scene[]>([])
  const { isLoading, error, run } = useAsyncAction()
  const isGenerating = ref(false)
  const currentProjectId = ref<number | null>(null)
  const completedCountByProject = ref<Record<number, number>>({})
  const COMPLETED_COUNT_STORAGE_KEY = 'completed_scene_count_by_project'

  try {
    const stored = localStorage.getItem(COMPLETED_COUNT_STORAGE_KEY)
    if (stored) {
      completedCountByProject.value = JSON.parse(stored)
    }
  } catch (e) {
    console.error('Failed to parse completed scene count cache', e)
  }

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

  function setCompletedCount(projectId: number, scenesForProject: Scene[]): void {
    const completed = scenesForProject.filter((scene) => scene.status === 'COMPLETED').length
    completedCountByProject.value[projectId] = completed
    try {
      localStorage.setItem(
        COMPLETED_COUNT_STORAGE_KEY,
        JSON.stringify(completedCountByProject.value)
      )
    } catch (e) {
      console.error('Failed to persist completed scene count cache', e)
    }
  }

  function getCompletedSceneCount(projectId: number): number {
    return completedCountByProject.value[projectId] ?? 0
  }

  // Actions
  async function loadScenes(projectId: number): Promise<void> {
    currentProjectId.value = projectId

    await run(async () => {
      const fetched = await fetchScenes(projectId)
      scenes.value = fetched.map((scene) => normalizeScene(scene))
      setCompletedCount(projectId, scenes.value)
    }, { errorMessage: 'Failed to load scenes' })
  }

  async function addScene(data: CreateSceneRequest): Promise<Scene | null> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return null
    }

    const newScene = await run(async () => {
      const created = await createScene(currentProjectId.value as number, data)
      return normalizeScene(created)
    }, { errorMessage: 'Failed to create scene' })
    if (newScene) {
      scenes.value.push(newScene)
      if (currentProjectId.value) {
        setCompletedCount(currentProjectId.value, scenes.value)
      }
    }
    return newScene
  }

  async function addScenes(dataList: CreateSceneRequest[]): Promise<Scene[]> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return []
    }

    const newScenes = await run(async () => {
      const created = await createScenes(currentProjectId.value as number, dataList)
      return created.map((scene) => normalizeScene(scene))
    }, { errorMessage: 'Failed to create scenes' })
    if (newScenes) {
      scenes.value.push(...newScenes)
      if (currentProjectId.value) {
        setCompletedCount(currentProjectId.value, scenes.value)
      }
      return newScenes
    }
    return []
  }

  async function updateScene(sceneId: number, data: Partial<Scene>): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    const updated = await run(() => updateSceneApi(sceneId, data), {
      loading: false,
      errorMessage: 'Failed to update scene',
    })
    if (updated) {
      const normalized = normalizeScene(updated)
      const index = scenes.value.findIndex((s) => s.sceneId === sceneId)
      if (index !== -1) {
        scenes.value[index] = normalized
      }
      if (currentProjectId.value) {
        setCompletedCount(currentProjectId.value, scenes.value)
      }
      return true
    }
    return false
  }

  async function removeScene(sceneId: number): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    const success = await run(async () => {
      await deleteScene(sceneId)
      scenes.value = scenes.value.filter((s) => s.sceneId !== sceneId)
      scenes.value.forEach((scene, idx) => {
        scene.order = idx + 1
      })
      if (currentProjectId.value) {
        setCompletedCount(currentProjectId.value, scenes.value)
      }
      return true
    }, { errorMessage: 'Failed to delete scene' })
    return Boolean(success)
  }

  async function reorderScenes(sceneIds: number[]): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    const success = await run(async () => {
      await reorderScenesApi(currentProjectId.value as number, sceneIds)
      scenes.value = [...scenes.value].map((scene) => ({
        ...scene,
        order: sceneIds.indexOf(scene.sceneId) + 1,
      }))
      return true
    }, { loading: false, errorMessage: 'Failed to reorder scenes' })
    return Boolean(success)
  }

  async function generateScenes(request: GenerateScenesRequest): Promise<Scene[]> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return []
    }

    isGenerating.value = true
    const result = await run(async () => {
      console.warn('generateScenes is not supported by the API yet', request)
      return []
    }, { loading: false, errorMessage: 'Failed to generate scenes' })
    isGenerating.value = false
    return result ?? []
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
    getCompletedSceneCount,
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
