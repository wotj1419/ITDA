import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ObjectSheet, CreateObjectRequest } from '../types'
import { objectService } from '../services'
// Keep type import if needed
import type { GenerateCharacterRequest } from '../services/mock/characters'

export const useCharacterStore = defineStore('character', () => {
  // State
  const characters = ref<ObjectSheet[]>([])
  const isLoading = ref(false)
  const isGenerating = ref(false)
  const error = ref<string | null>(null)
  const currentProjectId = ref<number | null>(null)

  // Getters
  const characterCount = computed(() => characters.value.length)

  // Actions
  async function loadCharacters(projectId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    currentProjectId.value = projectId

    try {
      characters.value = await objectService.fetchCharactersByProjectId(projectId)
    } catch (e) {
      error.value = 'Failed to load characters'
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }

  async function addCharacter(data: CreateObjectRequest): Promise<ObjectSheet | null> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return null
    }

    isLoading.value = true
    error.value = null

    try {
      const newCharacter = await objectService.createCharacter(currentProjectId.value, data)
      characters.value.push(newCharacter)
      return newCharacter
    } catch (e) {
      error.value = 'Failed to create character'
      console.error(e)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function updateCharacter(characterId: number, data: Partial<ObjectSheet>): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    try {
      const updated = await objectService.updateCharacter(currentProjectId.value, characterId, data)
      if (updated) {
        const index = characters.value.findIndex((c) => c.objectId === characterId)
        if (index !== -1) {
          characters.value[index] = updated
        }
        return true
      }
      return false
    } catch (e) {
      error.value = 'Failed to update character'
      console.error(e)
      return false
    }
  }

  async function removeCharacter(characterId: number): Promise<boolean> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return false
    }

    isLoading.value = true
    error.value = null

    try {
      const success = await objectService.deleteCharacter(currentProjectId.value, characterId)
      if (success) {
        characters.value = characters.value.filter((c) => c.objectId !== characterId)
      }
      return success
    } catch (e) {
      error.value = 'Failed to delete character'
      console.error(e)
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function generateCharacter(request: GenerateCharacterRequest): Promise<ObjectSheet | null> {
    if (!currentProjectId.value) {
      error.value = 'No project selected'
      return null
    }

    isGenerating.value = true
    error.value = null

    try {
      const generated = await objectService.generateCharacterWithAI(currentProjectId.value, request)
      characters.value.push(generated)
      return generated
    } catch (e) {
      error.value = 'Failed to generate character'
      console.error(e)
      return null
    } finally {
      isGenerating.value = false
    }
  }

  function clearCharacters(): void {
    characters.value = []
    currentProjectId.value = null
    error.value = null
  }

  return {
    // State
    characters,
    isLoading,
    isGenerating,
    error,
    currentProjectId,
    // Getters
    characterCount,
    // Actions
    loadCharacters,
    addCharacter,
    updateCharacter,
    removeCharacter,
    generateCharacter,
    clearCharacters,
  }
})
