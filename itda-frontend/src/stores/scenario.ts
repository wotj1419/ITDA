import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
    generateScenarioPrompt,
    generateScenarioPlot,
    generateScenarioScenes,
    updateScenarioPrompt,
    updateScenarioPlot,
} from '../services/api/scenario'
import { useAsyncAction } from './helpers/useAsyncAction'

// Types
export interface ScenarioInput {
    genre: string
    mood: string
    sceneCount: number
    keywords: string
    characterHints: string
    backgroundHints: string
    referenceStyle: string
}

export interface ScenarioPrompt {
    text: string
    status: 'draft' | 'approved'
}

export interface ScenarioPlot {
    text: string
    status: 'draft' | 'approved'
}

export interface ScenarioScene {
    id: number
    order: number
    title: string
    description: string
}

export type ScenarioStep = 1 | 2 | 3 | 4

export const useScenarioStore = defineStore('scenario', () => {
    // State
    const isDrawerOpen = ref(false)
    const currentStep = ref<ScenarioStep>(1)
    const { isLoading: isGenerating, error, run } = useAsyncAction()
    const activeProjectId = ref<number | null>(null)
    const projectStates = ref<Record<number, {
        currentStep: ScenarioStep
        input: ScenarioInput
        prompt: ScenarioPrompt
        plot: ScenarioPlot
        scenes: ScenarioScene[]
    }>>({})

    const input = ref<ScenarioInput>({
        genre: '',
        mood: '',
        sceneCount: 5,
        keywords: '',
        characterHints: '',
        backgroundHints: '',
        referenceStyle: '',
    })

    const prompt = ref<ScenarioPrompt>({
        text: '',
        status: 'draft',
    })

    const plot = ref<ScenarioPlot>({
        text: '',
        status: 'draft',
    })

    const scenes = ref<ScenarioScene[]>([])

    // Computed
    const canGoNext = computed(() => {
        switch (currentStep.value) {
            case 1:
                return input.value.genre && input.value.mood
            case 2:
                return prompt.value.status === 'approved'
            case 3:
                return plot.value.status === 'approved'
            case 4:
                return scenes.value.length > 0
            default:
                return false
        }
    })

    const stepLabels = ['입력', '프롬프트', '줄거리', '씬']

    const toStatus = (status?: string) =>
        status?.toLowerCase() === 'approved' ? 'approved' : 'draft'

    // Actions
    const openDrawer = () => {
        isDrawerOpen.value = true
    }

    const closeDrawer = () => {
        isDrawerOpen.value = false
    }

    const resetWizard = () => {
        currentStep.value = 1
        input.value = {
            genre: '',
            mood: '',
            sceneCount: 5,
            keywords: '',
            characterHints: '',
            backgroundHints: '',
            referenceStyle: '',
        }
        prompt.value = { text: '', status: 'draft' }
        plot.value = { text: '', status: 'draft' }
        scenes.value = []
    }

    const saveCurrentState = () => {
        if (activeProjectId.value) {
            projectStates.value[activeProjectId.value] = {
                currentStep: currentStep.value,
                input: JSON.parse(JSON.stringify(input.value)),
                prompt: JSON.parse(JSON.stringify(prompt.value)),
                plot: JSON.parse(JSON.stringify(plot.value)),
                scenes: JSON.parse(JSON.stringify(scenes.value)),
            }
        }
    }

    const switchProject = (projectId: number) => {
        // Close drawer to prevent confusion
        isDrawerOpen.value = false

        // Save current project state if exists
        if (activeProjectId.value && activeProjectId.value !== projectId) {
            saveCurrentState()
        }

        activeProjectId.value = projectId

        // Restore or reset
        if (projectStates.value[projectId]) {
            const state = projectStates.value[projectId]
            currentStep.value = state.currentStep
            input.value = JSON.parse(JSON.stringify(state.input))
            prompt.value = JSON.parse(JSON.stringify(state.prompt))
            plot.value = JSON.parse(JSON.stringify(state.plot))
            scenes.value = JSON.parse(JSON.stringify(state.scenes))
        } else {
            resetWizard()
        }
    }

    const goToStep = (step: ScenarioStep) => {
        currentStep.value = step
        saveCurrentState()
    }

    const nextStep = () => {
        if (currentStep.value < 4) {
            currentStep.value = (currentStep.value + 1) as ScenarioStep
            saveCurrentState()
        }
    }

    const prevStep = () => {
        if (currentStep.value > 1) {
            currentStep.value = (currentStep.value - 1) as ScenarioStep
            saveCurrentState()
        }
    }

    const generatePrompt = async (): Promise<void> => {
        if (!activeProjectId.value) return
        await run(async () => {
            const keywords = input.value.keywords
                ? input.value.keywords.split(',').map((k) => k.trim()).filter(Boolean)
                : []
            const promptData = await generateScenarioPrompt(activeProjectId.value as number, {
                genre: input.value.genre,
                mood: input.value.mood,
                sceneCount: input.value.sceneCount,
                keywords,
                characterHints: input.value.characterHints || undefined,
                backgroundHints: input.value.backgroundHints || undefined,
                referenceStyle: input.value.referenceStyle || undefined,
            })

            prompt.value = {
                text: promptData.text || '',
                status: toStatus(promptData.status),
            }

            nextStep()
        }, { errorMessage: 'Failed to generate prompt' })
    }

    const approvePrompt = async () => {
        if (!activeProjectId.value) return
        await run(async () => {
            await updateScenarioPrompt(activeProjectId.value as number, prompt.value.text, 'APPROVED')
            prompt.value.status = 'approved'
            await generatePlot()
        }, { errorMessage: 'Failed to approve prompt' })
    }

    const regeneratePrompt = async () => {
        if (!activeProjectId.value) return
        await run(async () => {
            const keywords = input.value.keywords
                ? input.value.keywords.split(',').map((k) => k.trim()).filter(Boolean)
                : []
            const promptData = await generateScenarioPrompt(activeProjectId.value as number, {
                genre: input.value.genre,
                mood: input.value.mood,
                sceneCount: input.value.sceneCount,
                keywords,
                characterHints: input.value.characterHints || undefined,
                backgroundHints: input.value.backgroundHints || undefined,
                referenceStyle: input.value.referenceStyle || undefined,
            })
            prompt.value.text = promptData.text || ''
            prompt.value.status = toStatus(promptData.status)
        }, { errorMessage: 'Failed to regenerate prompt' })
    }

    const generatePlot = async (): Promise<void> => {
        if (!activeProjectId.value) return
        await run(async () => {
            const plotData = await generateScenarioPlot(activeProjectId.value as number)
            plot.value = {
                text: plotData.text || '',
                status: toStatus(plotData.status),
            }
            nextStep()
        }, { errorMessage: 'Failed to generate plot' })
    }

    const approvePlot = () => {
        if (!activeProjectId.value) return
        run(async () => {
            await updateScenarioPlot(activeProjectId.value as number, plot.value.text, 'APPROVED')
            plot.value.status = 'approved'
            await generateScenes()
        }, { errorMessage: 'Failed to approve plot' })
    }

    const regeneratePlot = async () => {
        if (!activeProjectId.value) return
        await run(async () => {
            const plotData = await generateScenarioPlot(activeProjectId.value as number)
            plot.value.text = plotData.text || ''
            plot.value.status = toStatus(plotData.status)
        }, { errorMessage: 'Failed to regenerate plot' })
    }

    const generateScenes = async (): Promise<void> => {
        if (!activeProjectId.value) return
        await run(async () => {
            const response = await generateScenarioScenes(activeProjectId.value as number)
            scenes.value = response.scenes.map((scene) => ({
                id: scene.sceneId,
                order: scene.order,
                title: scene.title,
                description: scene.description,
            }))
            nextStep()
        }, { errorMessage: 'Failed to generate scenes' })
    }

    const updateScene = (id: number, updates: Partial<Pick<ScenarioScene, 'title' | 'description'>>) => {
        const scene = scenes.value.find(s => s.id === id)
        if (scene) {
            Object.assign(scene, updates)
            saveCurrentState()
        }
    }

    const reorderScenes = (orderedIds: number[]) => {
        const newScenes = orderedIds.map((id, index) => {
            const scene = scenes.value.find(s => s.id === id)!
            return { ...scene, order: index + 1 }
        })
        scenes.value = newScenes
        saveCurrentState()
    }

    const regenerateScene = async (id: number) => {
        await run(async () => {
            await new Promise((resolve) => setTimeout(resolve, 1000))

            const scene = scenes.value.find(s => s.id === id)
            if (scene) {
                scene.description += ' (새롭게 생성된 내용)'
                saveCurrentState()
            }
        }, { errorMessage: 'Failed to regenerate scene' })
    }

    const addScene = () => {
        const newId = Math.max(...scenes.value.map(s => s.id), 0) + 1
        scenes.value.push({
            id: newId,
            order: scenes.value.length + 1,
            title: '', // 빈 문자열로 시작
            description: '', // 빈 문자열로 시작
        })
        saveCurrentState()
    }

    const removeScene = (id: number) => {
        const index = scenes.value.findIndex(s => s.id === id)
        if (index !== -1) {
            scenes.value.splice(index, 1)
            // Reorder remaining scenes
            scenes.value.forEach((scene, idx) => {
                scene.order = idx + 1
            })
            saveCurrentState()
        }
    }

    return {
        // State
        isDrawerOpen,
        currentStep,
        isGenerating,
        error,
        activeProjectId,
        input,
        prompt,
        plot,
        scenes,
        // Computed
        canGoNext,
        stepLabels,
        // Actions
        openDrawer,
        closeDrawer,
        resetWizard,
        switchProject,
        goToStep,
        nextStep,
        prevStep,
        generatePrompt,
        approvePrompt,
        regeneratePrompt,
        generatePlot,
        approvePlot,
        regeneratePlot,
        generateScenes,
        updateScene,
        reorderScenes,
        regenerateScene,
        addScene,
        removeScene,
    }
})

