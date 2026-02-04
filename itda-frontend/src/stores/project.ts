import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, ProjectDetail, CreateProjectRequest, ProjectRole } from '../types/api/projects'
import {
  fetchProjects,
  fetchProjectById,
  fetchProjectMembers,
  createProject,
  deleteProject,
  leaveProject,
  updateProject as updateProjectApi,
} from '../services/api/projects'
import { useAsyncAction } from './helpers/useAsyncAction'

export const useProjectStore = defineStore('project', () => {
  // State
  const projects = ref<Project[]>([])
  const currentProject = ref<ProjectDetail | null>(null)
  const { isLoading, error, run } = useAsyncAction()
  const favoriteIds = ref<Set<number>>(new Set([1, 2])) // Mock default favorites
  const highlightedProjectId = ref<number | null>(null)
  let highlightTimeout: ReturnType<typeof setTimeout> | null = null

  // Local Storage for Last Accessed Time
  const lastAccessedMap = ref<Record<number, number>>({})

  // Initialize from localStorage
  try {
    const stored = localStorage.getItem('project_last_accessed')
    if (stored) {
      lastAccessedMap.value = JSON.parse(stored)
    }
  } catch (e) {
    console.error('Failed to parse last accessed projects', e)
  }

  // Getters
  const projectCount = computed(() => projects.value.length)

  const favoriteProjects = computed(() =>
    projects.value.filter((p) => favoriteIds.value.has(p.projectId))
  )

  const recentProjects = computed(() =>
    [...projects.value]
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      .slice(0, 2)
  )

  const sortedProjects = computed(() => {
    return [...projects.value].sort((a, b) => {
      const timeA = lastAccessedMap.value[a.projectId] || 0
      const timeB = lastAccessedMap.value[b.projectId] || 0

      // 1. Sort by last accessed time (descending)
      if (timeA !== timeB) {
        return timeB - timeA
      }

      // 2. If never accessed (Time=0) or equal, sort by updatedAt (descending)
      const dateA = new Date(a.updatedAt).getTime()
      const dateB = new Date(b.updatedAt).getTime()
      return dateB - dateA
    })
  })

  // Actions
  function isFavorite(projectId: number): boolean {
    return favoriteIds.value.has(projectId)
  }

  function toggleFavorite(projectId: number): void {
    if (favoriteIds.value.has(projectId)) {
      favoriteIds.value.delete(projectId)
    } else {
      favoriteIds.value.add(projectId)
    }
  }

  function touchProject(projectId: number): void {
    const now = Date.now()
    lastAccessedMap.value[projectId] = now
    const updatedAt = new Date(now).toISOString()

    const index = projects.value.findIndex((p) => p.projectId === projectId)
    if (index > -1) {
      const existing = projects.value[index]
      if (existing) {
        projects.value[index] = {
          ...existing,
          updatedAt,
        }
      }
    }

    if (currentProject.value?.projectId === projectId) {
      currentProject.value = {
        ...currentProject.value,
        updatedAt,
      }
    }

    // Persist to localStorage
    try {
      localStorage.setItem('project_last_accessed', JSON.stringify(lastAccessedMap.value))
    } catch (e) {
      console.error('Failed to save last accessed projects', e)
    }
  }

  function highlightProject(projectId: number): void {
    highlightedProjectId.value = projectId

    if (highlightTimeout) {
      clearTimeout(highlightTimeout)
    }

    highlightTimeout = setTimeout(() => {
      if (highlightedProjectId.value === projectId) {
        highlightedProjectId.value = null
      }
    }, 1400)
  }

  async function loadProjects(): Promise<void> {
    await run(async () => {
      const fetched = await fetchProjects()
      projects.value = fetched.map((project) => {
        const lastAccessed = lastAccessedMap.value[project.projectId]
        if (lastAccessed) {
          const updatedAt = new Date(project.updatedAt).getTime()
          if (lastAccessed > updatedAt) {
            return {
              ...project,
              updatedAt: new Date(lastAccessed).toISOString(),
            }
          }
        }
        return project
      })
    }, { errorMessage: 'Failed to load projects' })
  }

  async function loadProject(projectId: number): Promise<void> {
    const result = await run(() => fetchProjectById(projectId), {
      errorMessage: 'Failed to load project',
    })
    currentProject.value = result
    if (result) {
      // update access time when loading detail
      touchProject(projectId)
    }
    if (!result && !error.value) {
      error.value = 'Project not found'
    }
  }

  async function loadProjectMembers(projectId: number): Promise<void> {
    try {
      const members = await fetchProjectMembers(projectId)
      const memberCount = members.length
      if (currentProject.value?.projectId === projectId) {
        currentProject.value = {
          ...currentProject.value,
          members,
          memberCount,
        }
      }
      const index = projects.value.findIndex((p) => p.projectId === projectId)
      if (index > -1) {
        const existing = projects.value[index]
        if (existing) {
          projects.value[index] = {
            ...existing,
            memberCount,
          }
        }
      }
    } catch (err) {
      console.error('Failed to load project members', err)
    }
  }

  async function addProject(data: CreateProjectRequest): Promise<Project | null> {
    const newProject = await run(() => createProject(data), {
      errorMessage: 'Failed to create project',
    })
    if (newProject) {
      projects.value.push(newProject)
      touchProject(newProject.projectId) // New project is accessed
    }
    return newProject
  }

  function resolveProjectRole(project: Project | ProjectDetail | null): ProjectRole | null {
    if (!project) return null
    if ('myRole' in project && project.myRole) {
      return project.myRole
    }
    return project.role ?? null
  }

  async function moveToTrash(projectId: number): Promise<boolean> {
    const project =
      projects.value.find((item) => item.projectId === projectId) ??
      (currentProject.value?.projectId === projectId ? currentProject.value : null)
    const role = resolveProjectRole(project)
    const shouldLeave = role !== null && role !== 'OWNER'
    const errorMessage = shouldLeave ? 'Failed to leave project' : 'Failed to move project to trash'

    const result = await run(async () => {
      if (shouldLeave) {
        await leaveProject(projectId)
      } else {
        await deleteProject(projectId)
      }
      projects.value = projects.value.filter((p) => p.projectId !== projectId)
      if (currentProject.value?.projectId === projectId) {
        currentProject.value = null
      }
      return true
    }, { errorMessage })
    return Boolean(result)
  }

  async function updateProject(projectId: number, data: Partial<Project>): Promise<Project | null> {
    const existing =
      currentProject.value?.projectId === projectId
        ? currentProject.value
        : projects.value.find((p) => p.projectId === projectId)

    const payload = {
      title: data.title ?? existing?.title ?? '',
      description: data.description ?? existing?.description ?? '',
      genre: data.genre ?? existing?.genre ?? '',
    }

    const updatedProject = await run(() => updateProjectApi(projectId, payload), {
      errorMessage: 'Failed to update project',
    })
    if (updatedProject) {
      const index = projects.value.findIndex((p) => p.projectId === projectId)
      if (index > -1) {
        projects.value[index] = updatedProject
      }

      if (currentProject.value?.projectId === projectId) {
        currentProject.value = {
          ...currentProject.value,
          ...updatedProject,
        }
      }
      touchProject(projectId) // Updated project is accessed
    }
    return updatedProject
  }

  async function getDeletedProjects(): Promise<Project[]> {
    const result = await run(async () => [], { errorMessage: 'Failed to load deleted projects' })
    return result ?? []
  }

  async function restoreProject(_projectId: number): Promise<void> {
    await loadProjects()
  }

  async function permanentDeleteProject(projectId: number): Promise<void> {
    await run(() => deleteProject(projectId), { errorMessage: 'Failed to permanently delete project' })
  }

  async function inviteMember(projectId: number, email: string, role: 'ADMIN' | 'EDITOR' | 'VIEWER'): Promise<void> {
    await run(async () => {
      // Dynamic import to avoid circular dependency if any, though explicit import is better if safe
      const api = await import('../services/api/projects')
      await api.inviteMember(projectId, email, role)
      await loadProjectMembers(projectId)
    }, { errorMessage: 'Failed to invite member' })
  }

  async function updateMemberRole(projectId: number, userId: number, role: 'ADMIN' | 'EDITOR' | 'VIEWER'): Promise<void> {
    await run(async () => {
      const api = await import('../services/api/projects')
      await api.updateMemberRole(projectId, userId, role)
      await loadProjectMembers(projectId)
    }, { errorMessage: 'Failed to update member role' })
  }

  async function removeMember(projectId: number, userId: number): Promise<void> {
    await run(async () => {
      const api = await import('../services/api/projects')
      await api.removeMember(projectId, userId)
      await loadProjectMembers(projectId)
    }, { errorMessage: 'Failed to remove member' })
  }

  function clearCurrentProject(): void {
    currentProject.value = null
  }

  return {
    // State
    projects,
    currentProject,
    isLoading,
    error,
    // Getters
    projectCount,
    favoriteProjects,
    recentProjects,
    sortedProjects,
    highlightedProjectId,
    // Actions
    isFavorite,
    toggleFavorite,
    touchProject,
    highlightProject,
    loadProjects,
    loadProject,
    addProject,
    moveToTrash,
    updateProject,
    clearCurrentProject,
    getDeletedProjects,
    restoreProject,
    permanentDeleteProject,
    inviteMember,
    updateMemberRole,
    removeMember,
    loadProjectMembers,
  }
})
