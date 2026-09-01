import type { CreateProjectRequest, Project, ProjectDetail, ProjectMember } from '../../types/api/projects'
import type { CreateSceneRequest, Scene } from '../../types/api/scenes'

export type StorageLike = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>

type DemoState = {
  projects: Project[]
  projectDetails: Record<number, ProjectDetail>
  scenesByProject: Record<number, Scene[]>
  favoriteIds: number[]
  nextProjectId: number
  nextSceneId: number
}

const STORAGE_KEY = 'itda.public-demo.v1'

const owner: ProjectMember = {
  userId: 1,
  email: 'portfolio@itda.kr',
  name: 'ITDA 포트폴리오',
  role: 'OWNER',
}

const sharedOwner: ProjectMember = {
  userId: 2,
  email: 'creative@itda.kr',
  name: '크리에이티브 팀',
  role: 'OWNER',
}

const createSeedScenes = (projectId: number, firstSceneId: number): Scene[] => [
  {
    sceneId: firstSceneId,
    title: '장면 1. 이야기의 시작',
    description: '주인공이 새로운 여정을 시작하며 이야기를 소개합니다.',
    order: 1,
    status: 'DRAFT',
  },
  {
    sceneId: firstSceneId + 1,
    title: '장면 2. 갈등의 발견',
    description: '주인공이 해결해야 할 문제와 마주합니다.',
    order: 2,
    status: 'DRAFT',
  },
  {
    sceneId: firstSceneId + 2,
    title: '장면 3. 새로운 결심',
    description: '주인공이 다음 단계로 나아갈 결심을 합니다.',
    order: 3,
    status: 'DRAFT',
  },
]

const createSeedState = (): DemoState => {
  const now = new Date().toISOString()
  const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
  const deletedAt = new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toISOString()
  const projects: Project[] = [
    {
      projectId: 101,
      title: '도시의 새벽',
      description: '새벽의 도심에서 시작되는 짧은 성장 이야기입니다.',
      genre: '드라마',
      role: 'OWNER',
      memberCount: 1,
      sceneCount: 3,
      updatedAt: now,
      createdAt: oneDayAgo,
      isDeleted: false,
    },
    {
      projectId: 102,
      title: '별빛 탐험대',
      description: '미지의 행성을 탐험하는 SF 어드벤처입니다.',
      genre: 'SF',
      role: 'OWNER',
      memberCount: 1,
      sceneCount: 3,
      updatedAt: oneDayAgo,
      createdAt: oneDayAgo,
      isDeleted: false,
    },
    {
      projectId: 103,
      title: '파도 너머',
      description: '바다를 배경으로 한 공동 제작 다큐멘터리입니다.',
      genre: '다큐멘터리',
      role: 'VIEWER',
      memberCount: 2,
      sceneCount: 3,
      updatedAt: oneDayAgo,
      createdAt: oneDayAgo,
      isDeleted: false,
    },
    {
      projectId: 104,
      title: '보관된 봄',
      description: '휴지통 화면을 위한 예시 프로젝트입니다.',
      genre: '로맨스',
      role: 'OWNER',
      memberCount: 1,
      sceneCount: 3,
      updatedAt: deletedAt,
      createdAt: oneDayAgo,
      isDeleted: true,
      deletedAt,
    },
  ]

  const projectDetails = Object.fromEntries(
    projects.map((project) => [
      project.projectId,
      {
        ...project,
        myRole: project.role,
        ownerId: project.role === 'OWNER' ? owner.userId : sharedOwner.userId,
        members: project.role === 'OWNER' ? [owner] : [sharedOwner, { ...owner, role: 'VIEWER' }],
      } satisfies ProjectDetail,
    ])
  ) as Record<number, ProjectDetail>

  return {
    projects,
    projectDetails,
    scenesByProject: {
      101: createSeedScenes(101, 1001),
      102: createSeedScenes(102, 1011),
      103: createSeedScenes(103, 1021),
      104: createSeedScenes(104, 1031),
    },
    favoriteIds: [101],
    nextProjectId: 1000,
    nextSceneId: 2000,
  }
}

const cloneProject = (project: Project): Project => ({ ...project })
const cloneScene = (scene: Scene): Scene => ({ ...scene })

const cloneDetail = (detail: ProjectDetail): ProjectDetail => ({
  ...detail,
  members: detail.members?.map((member) => ({ ...member })),
})

const readState = (storage: StorageLike): DemoState => {
  try {
    const raw = storage.getItem(STORAGE_KEY)
    if (!raw) return createSeedState()
    const parsed = JSON.parse(raw) as DemoState
    if (!Array.isArray(parsed.projects) || !parsed.projectDetails || !parsed.scenesByProject) {
      return createSeedState()
    }
    return parsed
  } catch {
    return createSeedState()
  }
}

const persist = (storage: StorageLike, state: DemoState): void => {
  try {
    storage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    // Demo mode remains usable if browser storage is unavailable.
  }
}

const syncProject = (state: DemoState, project: Project): void => {
  const index = state.projects.findIndex((item) => item.projectId === project.projectId)
  if (index !== -1) state.projects[index] = project
  const detail = state.projectDetails[project.projectId]
  if (detail) state.projectDetails[project.projectId] = { ...detail, ...project }
}

const updateSceneCount = (state: DemoState, projectId: number): void => {
  const project = state.projects.find((item) => item.projectId === projectId)
  if (!project) return
  syncProject(state, {
    ...project,
    sceneCount: state.scenesByProject[projectId]?.length ?? 0,
    updatedAt: new Date().toISOString(),
  })
}

const createGeneratedScenes = (state: DemoState): Scene[] => {
  const firstSceneId = state.nextSceneId
  state.nextSceneId += 3
  return createSeedScenes(0, firstSceneId)
}

export function createPublicDemoRepository(storage: StorageLike) {
  const state = readState(storage)

  const save = () => persist(storage, state)

  return {
    listProjects: (): Project[] => state.projects.filter((project) => !project.isDeleted).map(cloneProject),
    getProject: (projectId: number): ProjectDetail | null => {
      const project = state.projectDetails[projectId]
      return project && !project.isDeleted ? cloneDetail(project) : null
    },
    getProjectMembers: (projectId: number): ProjectMember[] =>
      state.projectDetails[projectId]?.members?.map((member) => ({ ...member })) ?? [],
    createProject: (data: CreateProjectRequest): Project => {
      const now = new Date().toISOString()
      const project: Project = {
        projectId: state.nextProjectId++,
        title: data.title.trim() || '새 프로젝트',
        description: data.description,
        genre: data.genre,
        role: 'OWNER',
        memberCount: 1,
        sceneCount: 3,
        updatedAt: now,
        createdAt: now,
        isDeleted: false,
      }
      state.projects.push(project)
      state.projectDetails[project.projectId] = {
        ...project,
        myRole: 'OWNER',
        ownerId: owner.userId,
        members: [{ ...owner }],
      }
      state.scenesByProject[project.projectId] = createGeneratedScenes(state)
      save()
      return cloneProject(project)
    },
    updateProject: (projectId: number, data: Partial<Project>): Project | null => {
      const project = state.projects.find((item) => item.projectId === projectId)
      if (!project || project.isDeleted) return null
      const updated = { ...project, ...data, updatedAt: new Date().toISOString() }
      syncProject(state, updated)
      save()
      return cloneProject(updated)
    },
    listScenes: (projectId: number): Scene[] =>
      [...(state.scenesByProject[projectId] ?? [])].sort((a, b) => a.order - b.order).map(cloneScene),
    createScene: (projectId: number, data: CreateSceneRequest): Scene => {
      const scenes = state.scenesByProject[projectId] ?? []
      const scene: Scene = {
        sceneId: state.nextSceneId++,
        title: data.title,
        description: data.description,
        order: scenes.length + 1,
        status: 'DRAFT',
      }
      state.scenesByProject[projectId] = [...scenes, scene]
      updateSceneCount(state, projectId)
      save()
      return cloneScene(scene)
    },
    updateScene: (projectId: number, sceneId: number, data: Partial<Scene>): Scene | null => {
      const scenes = state.scenesByProject[projectId] ?? []
      const index = scenes.findIndex((scene) => scene.sceneId === sceneId)
      if (index === -1) return null
      const existing = scenes[index]
      if (!existing) return null
      const updated = { ...existing, ...data }
      scenes[index] = updated
      updateSceneCount(state, projectId)
      save()
      return cloneScene(updated)
    },
    removeScene: (projectId: number, sceneId: number): boolean => {
      const scenes = state.scenesByProject[projectId] ?? []
      const next = scenes.filter((scene) => scene.sceneId !== sceneId).map((scene, index) => ({ ...scene, order: index + 1 }))
      if (next.length === scenes.length) return false
      state.scenesByProject[projectId] = next
      updateSceneCount(state, projectId)
      save()
      return true
    },
    reorderScenes: (projectId: number, sceneIds: number[]): Scene[] => {
      const scenes = state.scenesByProject[projectId] ?? []
      const byId = new Map(scenes.map((scene) => [scene.sceneId, scene]))
      state.scenesByProject[projectId] = sceneIds
        .map((sceneId, index) => {
          const scene = byId.get(sceneId)
          return scene ? { ...scene, order: index + 1 } : null
        })
        .filter((scene): scene is Scene => scene !== null)
      updateSceneCount(state, projectId)
      save()
      return state.scenesByProject[projectId].map(cloneScene)
    },
    getDeletedProjects: (): Project[] => state.projects.filter((project) => project.isDeleted).map(cloneProject),
    moveToTrash: (projectId: number): boolean => {
      const project = state.projects.find((item) => item.projectId === projectId)
      if (!project || project.isDeleted) return false
      syncProject(state, { ...project, isDeleted: true, deletedAt: new Date().toISOString() })
      save()
      return true
    },
    restoreProject: (projectId: number): boolean => {
      const project = state.projects.find((item) => item.projectId === projectId)
      if (!project || !project.isDeleted) return false
      syncProject(state, { ...project, isDeleted: false, deletedAt: undefined, updatedAt: new Date().toISOString() })
      save()
      return true
    },
    deleteProjectPermanently: (projectId: number): boolean => {
      const index = state.projects.findIndex((item) => item.projectId === projectId)
      if (index === -1) return false
      state.projects.splice(index, 1)
      delete state.projectDetails[projectId]
      delete state.scenesByProject[projectId]
      state.favoriteIds = state.favoriteIds.filter((id) => id !== projectId)
      save()
      return true
    },
    isFavorite: (projectId: number): boolean => state.favoriteIds.includes(projectId),
    toggleFavorite: (projectId: number): boolean => {
      state.favoriteIds = state.favoriteIds.includes(projectId)
        ? state.favoriteIds.filter((id) => id !== projectId)
        : [...state.favoriteIds, projectId]
      save()
      return state.favoriteIds.includes(projectId)
    },
  }
}

const fallbackValues = new Map<string, string>()

const fallbackStorage: StorageLike = {
  getItem: (key) => fallbackValues.get(key) ?? null,
  setItem: (key, value) => fallbackValues.set(key, value),
  removeItem: (key) => fallbackValues.delete(key),
}

const getDefaultStorage = (): StorageLike => {
  if (typeof window === 'undefined') return fallbackStorage
  try {
    window.localStorage.getItem(STORAGE_KEY)
    return window.localStorage
  } catch {
    return fallbackStorage
  }
}

export const publicDemoRepository = createPublicDemoRepository(getDefaultStorage())
