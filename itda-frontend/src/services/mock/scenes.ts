import type { Scene, SceneStatus, CreateSceneRequest } from '../../types'

// Mock scenes data per project
const mockScenesData: Record<number, Scene[]> = {
  1: [
    {
      sceneId: 1,
      title: 'The Discovery',
      description: 'Mark walks through the red desert and spots a metallic glint in the distance.',
      order: 1,
      status: 'COMPLETED',
      thumbnailUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=200&auto=format',
    },
    {
      sceneId: 2,
      title: 'Entering the Structure',
      description: 'Inside the dark corridor, bioluminescent plants light up the path.',
      order: 2,
      status: 'IN_PROGRESS',
      thumbnailUrl: undefined,
    },
    {
      sceneId: 3,
      title: 'The Revelation',
      description: 'Mark discovers ancient alien technology that could save Earth.',
      order: 3,
      status: 'DRAFT',
      thumbnailUrl: undefined,
    },
  ],
  2: [
    {
      sceneId: 4,
      title: 'Night Market Chase',
      description: 'A neon-lit chase through the crowded streets of neo-Tokyo.',
      order: 1,
      status: 'IN_PROGRESS',
      thumbnailUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=200&auto=format',
    },
  ],
  3: [
    {
      sceneId: 5,
      title: 'Descent into the Abyss',
      description: 'The submarine begins its journey to the deepest part of the ocean.',
      order: 1,
      status: 'COMPLETED',
      thumbnailUrl: 'https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=200&auto=format',
    },
  ],
}

// ID counter for new scenes
let nextSceneId = 100

// Simulated API delay
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Get status badge config
export function getStatusConfig(status: SceneStatus): { label: string; variant: string; icon?: string } {
  const configs: Record<SceneStatus, { label: string; variant: string; icon?: string }> = {
    COMPLETED: { label: '완료', variant: 'success', icon: 'check' },
    IN_PROGRESS: { label: '진행 중', variant: 'info', icon: 'loader' },
    DRAFT: { label: '초안', variant: 'default' },
  }
  return configs[status]
}

// Mock API functions
export async function fetchScenesByProjectId(projectId: number): Promise<Scene[]> {
  await delay(300)
  return [...(mockScenesData[projectId] || [])].sort((a, b) => a.order - b.order)
}

export async function createScene(projectId: number, data: CreateSceneRequest): Promise<Scene> {
  await delay(500)
  const scenes = mockScenesData[projectId] || []
  const maxOrder = scenes.length > 0 ? Math.max(...scenes.map((s) => s.order)) : 0

  const newScene: Scene = {
    sceneId: nextSceneId++,
    title: data.title,
    description: data.description,
    order: maxOrder + 1,
    status: 'DRAFT',
    thumbnailUrl: undefined,
  }

  if (!mockScenesData[projectId]) {
    mockScenesData[projectId] = []
  }
  mockScenesData[projectId].push(newScene)

  return newScene
}

export async function updateScene(
  projectId: number,
  sceneId: number,
  data: Partial<Scene>
): Promise<Scene | null> {
  await delay(300)
  const scenes = mockScenesData[projectId]
  if (!scenes) return null

  const index = scenes.findIndex((s) => s.sceneId === sceneId)
  if (index === -1) return null

  const existingScene = scenes[index]
  if (!existingScene) return null

  const updatedScene: Scene = { ...existingScene, ...data }
  scenes[index] = updatedScene
  return updatedScene
}

export async function deleteScene(projectId: number, sceneId: number): Promise<boolean> {
  await delay(300)
  const scenes = mockScenesData[projectId]
  if (!scenes) return false

  const index = scenes.findIndex((s) => s.sceneId === sceneId)
  if (index === -1) return false

  scenes.splice(index, 1)
  // Reorder remaining scenes
  scenes.forEach((scene, idx) => {
    scene.order = idx + 1
  })

  return true
}

export async function reorderScenes(projectId: number, sceneIds: number[]): Promise<Scene[]> {
  await delay(200)
  const scenes = mockScenesData[projectId]
  if (!scenes) return []

  // Create a map for quick lookup
  const sceneMap = new Map(scenes.map((s) => [s.sceneId, s]))

  // Reorder based on provided order
  sceneIds.forEach((sceneId, index) => {
    const scene = sceneMap.get(sceneId)
    if (scene) {
      scene.order = index + 1
    }
  })

  return [...scenes].sort((a, b) => a.order - b.order)
}

// AI Scene Generation (mock)
export interface GenerateScenesRequest {
  genre: string
  mood: string
  sceneCount: number
  synopsis: string
}

export async function generateScenesWithAI(
  projectId: number,
  request: GenerateScenesRequest
): Promise<Scene[]> {
  // Simulate AI processing time
  await delay(2500)

  const generatedScenes: Scene[] = []
  const baseScenes = [
    { title: 'Opening Scene', description: 'The story begins with an establishing shot.' },
    { title: 'Rising Action', description: 'The main conflict is introduced.' },
    { title: 'Midpoint', description: 'A major turning point in the story.' },
    { title: 'Climax', description: 'The story reaches its peak intensity.' },
    { title: 'Resolution', description: 'The story concludes with a satisfying ending.' },
  ]

  const existingScenes = mockScenesData[projectId] || []
  let maxOrder = existingScenes.length > 0 ? Math.max(...existingScenes.map((s) => s.order)) : 0

  for (let i = 0; i < request.sceneCount; i++) {
    const baseScene = baseScenes[i % baseScenes.length] || { title: 'Scene', description: 'Scene description' }
    const newScene: Scene = {
      sceneId: nextSceneId++,
      title: `${baseScene.title} ${i + 1}`,
      description: `[${request.genre}/${request.mood}] ${baseScene.description}`,
      order: maxOrder + i + 1,
      status: 'DRAFT',
      thumbnailUrl: undefined,
    }
    generatedScenes.push(newScene)
  }

  if (!mockScenesData[projectId]) {
    mockScenesData[projectId] = []
  }
  mockScenesData[projectId].push(...generatedScenes)

  return generatedScenes
}
