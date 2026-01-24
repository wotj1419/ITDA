import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, ProjectDetail, CreateProjectRequest } from '../types'
import { projectService } from '../services'

export const useProjectStore = defineStore('project', () => {
  // State
  const projects = ref<Project[]>([])
  const currentProject = ref<ProjectDetail | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
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
    isLoading.value = true
    error.value = null

    try {
      projects.value = await projectService.fetchProjects()
    } catch (e) {
      error.value = 'Failed to load projects'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function loadProject(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      currentProject.value = await projectService.fetchProjectById(projectId)
      if (!currentProject.value) {
        error.value = 'Project not found'
      }
    } catch (e) {
      error.value = 'Failed to load project'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function addProject(data: CreateProjectRequest): Promise<Project | null> {
    isLoading.value = true
    error.value = null

    try {
      const newProject = await projectService.createProject(data)
      projects.value.unshift(newProject)
      return newProject
    } catch (e) {
      error.value = 'Failed to create project'
      console.error(e)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function moveToTrash(projectId: number): Promise<boolean> {
    isLoading.value = true
    error.value = null

    try {
      await projectService.deleteProject(projectId)
      projects.value = projects.value.filter((p) => p.projectId !== projectId)
      if (currentProject.value?.projectId === projectId) {
        currentProject.value = null
      }
      return true
    } catch (e) {
      error.value = 'Failed to move project to trash'
      console.error(e)
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function updateProject(projectId: number, data: Partial<Project>): Promise<Project | null> {
    isLoading.value = true
    error.value = null

    try {
      const updatedProject = await projectService.updateProject(projectId, data)
      if (updatedProject) {
        // Update item in projects list
        const index = projects.value.findIndex((p) => p.projectId === projectId)
        if (index > -1) {
          projects.value[index] = updatedProject
        }

        // Update currentProject if relevant
        if (currentProject.value?.projectId === projectId) {
          currentProject.value = {
            ...currentProject.value,
            ...updatedProject
          }
        }
      }
      return updatedProject
    } catch (e) {
      error.value = 'Failed to update project'
      console.error(e)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function getDeletedProjects(): Promise<Project[]> {
    isLoading.value = true
    error.value = null
    try {
      const { fetchDeletedProjects } = await import('../services/mock/projects')
      return await fetchDeletedProjects()
    } catch (e) {
      console.error(e)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function restoreProject(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const { restoreProject: mockRestore } = await import('../services/mock/projects')
      await mockRestore(projectId)
      // Reload projects to reflect restoration if we are in main list, 
      // but usually we just want to update local state if we had it.
      // Since we filter in loadProjects, reloading is safest.
      await loadProjects()
    } catch (e) {
      error.value = 'Failed to restore project'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function permanentDeleteProject(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const { hardDeleteProject } = await import('../services/mock/projects')
      await hardDeleteProject(projectId)
    } catch (e) {
      error.value = 'Failed to permanently delete project'
      console.error(e)
    } finally {
      isLoading.value = false
    }
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
