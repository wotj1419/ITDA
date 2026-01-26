import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, ProjectDetail, CreateProjectRequest } from '../types/api/projects'
import {
  fetchProjects,
  fetchProjectById,
  createProject,
  deleteProject,
  updateProject as updateProjectApi,
} from '../services/api/projects'
import { useAsyncAction } from './helpers/useAsyncAction'

export const useProjectStore = defineStore('project', () => {
  // State
  const projects = ref<Project[]>([])
  const currentProject = ref<ProjectDetail | null>(null)
  const { isLoading, error, run } = useAsyncAction()
  const favoriteIds = ref<Set<number>>(new Set([1, 2])) // Mock default favorites

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

  async function loadProjects(): Promise<void> {
    await run(async () => {
      projects.value = await fetchProjects()
    }, { errorMessage: 'Failed to load projects' })
  }

  async function loadProject(projectId: number): Promise<void> {
    const result = await run(() => fetchProjectById(projectId), {
      errorMessage: 'Failed to load project',
    })
    currentProject.value = result
    if (!result && !error.value) {
      error.value = 'Project not found'
    }
  }

  async function addProject(data: CreateProjectRequest): Promise<Project | null> {
    const newProject = await run(() => createProject(data), {
      errorMessage: 'Failed to create project',
    })
    if (newProject) {
      projects.value.push(newProject)
    }
    return newProject
  }

  async function moveToTrash(projectId: number): Promise<boolean> {
    const result = await run(async () => {
      await deleteProject(projectId)
      projects.value = projects.value.filter((p) => p.projectId !== projectId)
      if (currentProject.value?.projectId === projectId) {
        currentProject.value = null
      }
      return true
    }, { errorMessage: 'Failed to move project to trash' })
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
    // Actions
    isFavorite,
    toggleFavorite,
    loadProjects,
    loadProject,
    addProject,
    moveToTrash,
    updateProject,
    clearCurrentProject,
    getDeletedProjects,
    restoreProject,
    permanentDeleteProject,
  }
})
