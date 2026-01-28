import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ObjectSheet, CreateObjectRequest, UpdateObjectRequest } from '../types/api/objects'
import { objectService } from '../services'

export const useObjectStore = defineStore('object', () => {
  const objects = ref<ObjectSheet[]>([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const isUpdating = ref(false)
  const isDeleting = ref(false)
  const error = ref<string | null>(null)
  const currentProjectId = ref<number | null>(null)

  const objectCount = computed(() => objects.value.length)

  async function loadObjects(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    currentProjectId.value = projectId
    try {
      objects.value = await objectService.fetchObjectsByProjectId(projectId)
    } catch (e) {
      error.value = 'Failed to load objects'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function addObject(data: CreateObjectRequest, file: File): Promise<ObjectSheet | null> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return null
    }
    isSaving.value = true
    error.value = null
    try {
      const created = await objectService.createObject(currentProjectId.value, data, file)
      objects.value.push(created)
      return created
    } catch (e) {
      error.value = 'Failed to create object'
      console.error(e)
      return null
    } finally {
      isSaving.value = false
    }
  }

  async function updateObject(
    objectId: number,
    data: UpdateObjectRequest,
    file?: File | null
  ): Promise<ObjectSheet | null> {
    isUpdating.value = true
    error.value = null
    try {
      const updated = await objectService.updateObject(objectId, data)
      if (!updated) {
        throw new Error('Failed to update object')
      }
      let finalObject = updated
      if (file) {
        finalObject = await objectService.replaceObjectImage(objectId, file)
      }
      if (finalObject) {
        const index = objects.value.findIndex((o) => o.objectId === objectId)
        if (index !== -1) {
          objects.value[index] = finalObject
        }
      }
      return finalObject
    } catch (e) {
      error.value = 'Failed to update object'
      console.error(e)
      return null
    } finally {
      isUpdating.value = false
    }
  }

  async function removeObject(objectId: number): Promise<boolean> {
    isDeleting.value = true
    error.value = null
    try {
      const success = await objectService.deleteObject(objectId)
      if (success) {
        objects.value = objects.value.filter((o) => o.objectId !== objectId)
      }
      return success
    } catch (e) {
      error.value = 'Failed to delete object'
      console.error(e)
      return false
    } finally {
      isDeleting.value = false
    }
  }

  async function downloadObjectImage(objectId: number): Promise<Blob | null> {
    try {
      return await objectService.downloadObjectImage(objectId)
    } catch (e) {
      error.value = 'Failed to download object image'
      console.error(e)
      return null
    }
  }

  function clearObjects(): void {
    objects.value = []
    currentProjectId.value = null
    error.value = null
  }

  return {
    objects,
    isLoading,
    isSaving,
    isUpdating,
    isDeleting,
    error,
    currentProjectId,
    objectCount,
    loadObjects,
    addObject,
    updateObject,
    removeObject,
    downloadObjectImage,
    clearObjects,
  }
})
