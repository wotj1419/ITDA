import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useProjectStore } from './project'
import { useUIStore } from './ui'
import {
    generateScenarioPrompt,
    generateScenarioPlot,
    generateScenarioScenes,
    updateScenarioPrompt,
    updateScenarioPlot,
} from '../services/api/scenario'
import { useAsyncAction } from './helpers/useAsyncAction'
import { useGenerationToast } from '../composables/useGenerationToast'

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
    const projectStore = useProjectStore()
    const uiStore = useUIStore()
    const activeProjectId = ref<number | null>(null)
    const projectStates = ref<Record<number, {
        currentStep: ScenarioStep
        input: ScenarioInput
        prompt: ScenarioPrompt
        plot: ScenarioPlot
        scenes: ScenarioScene[]
    }>>({})

    const pendingProjectInfo = ref<Record<number, { title: string; description: string }>>({})

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
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        const { startGenerationToast, finishGenerationToast } = useGenerationToast()
        const toastId = startGenerationToast('scenario_prompt')

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
            finishGenerationToast(toastId, 'scenario_prompt', 'success')
        }, {
            errorMessage: 'Failed to generate prompt',
            onError: (err) => {
                const message = err instanceof Error ? err.message : '알 수 없는 오류'
                finishGenerationToast(toastId, 'scenario_prompt', 'error', { reason: message })
                return message
            }
        })
    }

    const approvePrompt = async () => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        // approvePrompt triggers plot generation
        await run(async () => {
            await updateScenarioPrompt(activeProjectId.value as number, prompt.value.text, 'APPROVED')
            prompt.value.status = 'approved'

            // Calls generatePlot internally, but we want to track it here or let generatePlot handle it?
            // Original code: await generatePlot()
            // To avoid double toasts if generatePlot also had toasts, we should be careful.
            // But generatePlot is exposed as an action too.
            // Let's modify generatePlot instead to handle its own toast, and just await it here?
            // Actually, existing generatePlot is just an action.
            // If we modify generatePlot to have toast, then calling it here will show toast.
            // BUT: approvePrompt does MORE than just generatePlot (it updates prompt status first).
            // Users perceives this as "Approved and Next" which generates plot.
            // So toast for 'plot' generation is appropriate here if we wrap the whole flow or just the generation part.
            // Let's rely on generatePlot having its own toast if we modify it, 
            // OR we wrap the whole thing here.
            // The request says "approve and next button... also shows popup".
            // Since generatePlot is called, let's add toast INSIDE generatePlot, 
            // and maybe a separate small toast or just relying on generatePlot is enough?
            // Wait, approvePrompt calls generatePlot. if generatePlot has toast, it will show.
            // Let's just modify generatePlot and generateScenes to have toasts.
            // And generatePrompt.

            await generatePlot()
        }, { errorMessage: 'Failed to approve prompt' })
        // Note: if generatePlot fails, run catches it. 
        // We will implement toast inside generatePlot so we don't duplicate logic.
    }

    const regeneratePrompt = async () => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        const { startGenerationToast, finishGenerationToast } = useGenerationToast()
        const toastId = startGenerationToast('scenario_prompt')

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
            finishGenerationToast(toastId, 'scenario_prompt', 'success')
        }, {
            errorMessage: 'Failed to regenerate prompt',
            onError: (err) => {
                const message = err instanceof Error ? err.message : '알 수 없는 오류'
                finishGenerationToast(toastId, 'scenario_prompt', 'error', { reason: message })
                return message
            }
        })
    }

    const generatePlot = async (): Promise<void> => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        const { startGenerationToast, finishGenerationToast } = useGenerationToast()
        const toastId = startGenerationToast('plot')

        await run(async () => {
            const plotData = await generateScenarioPlot(activeProjectId.value as number)
            plot.value = {
                text: plotData.text || '',
                status: toStatus(plotData.status),
            }
            nextStep()
            finishGenerationToast(toastId, 'plot', 'success')
        }, {
            errorMessage: 'Failed to generate plot',
            onError: (err) => {
                const message = err instanceof Error ? err.message : '알 수 없는 오류'
                finishGenerationToast(toastId, 'plot', 'error', { reason: message })
                return message
            }
        })
    }

    const approvePlot = () => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        // approvePlot calls generateScenes
        run(async () => {
            await updateScenarioPlot(activeProjectId.value as number, plot.value.text, 'APPROVED')
            plot.value.status = 'approved'
            await generateScenes()
        }, { errorMessage: 'Failed to approve plot' })
    }

    const regeneratePlot = async () => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        const { startGenerationToast, finishGenerationToast } = useGenerationToast()
        const toastId = startGenerationToast('plot')

        await run(async () => {
            const plotData = await generateScenarioPlot(activeProjectId.value as number)
            plot.value.text = plotData.text || ''
            plot.value.status = toStatus(plotData.status)
            finishGenerationToast(toastId, 'plot', 'success')
        }, {
            errorMessage: 'Failed to regenerate plot',
            onError: (err) => {
                const message = err instanceof Error ? err.message : '알 수 없는 오류'
                finishGenerationToast(toastId, 'plot', 'error', { reason: message })
                return message
            }
        })
    }

    const generateScenes = async (): Promise<void> => {
        if (isGenerating.value) return
        if (!activeProjectId.value) return
        const { startGenerationToast, finishGenerationToast } = useGenerationToast()
        const toastId = startGenerationToast('scenes')

        await run(async () => {
            const response = await generateScenarioScenes(activeProjectId.value as number)
            scenes.value = response.scenes.map((scene) => ({
                id: scene.sceneId,
                order: scene.order,
                title: scene.title,
                description: scene.description,
            }))
            nextStep()
            finishGenerationToast(toastId, 'scenes', 'success')
        }, {
            errorMessage: 'Failed to generate scenes',
            onError: (err) => {
                const message = err instanceof Error ? err.message : '알 수 없는 오류'
                finishGenerationToast(toastId, 'scenes', 'error', { reason: message })
                return message
            }
        })
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
        if (isGenerating.value) return
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

    
    const setPendingProjectInfo = (projectId: number, title: string, description: string) => {
        pendingProjectInfo.value[projectId] = { title, description }
    }

    const clearPendingProjectInfo = (projectId: number) => {
        delete pendingProjectInfo.value[projectId]
    }

    const setGenre = async (genre: string) => {
        input.value.genre = genre
        if (!activeProjectId.value) return
        const pending = pendingProjectInfo.value[activeProjectId.value]
        if (!pending) return
        const updated = await projectStore.updateProject(activeProjectId.value, {
            title: pending.title,
            description: pending.description,
            genre,
        })
        if (updated) {
            clearPendingProjectInfo(activeProjectId.value)
        } else {
            uiStore.showToast({
                type: 'error',
                title: '프로젝트 저장 실패',
                message: '잠시 후 다시 시도해주세요.',
            })
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
        setPendingProjectInfo,
        clearPendingProjectInfo,
        setGenre,
    }
})
