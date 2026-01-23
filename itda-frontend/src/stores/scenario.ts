import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

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
    const isGenerating = ref(false)
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

    // Mock AI Generation Functions
    const generatePrompt = async (): Promise<void> => {
        isGenerating.value = true

        // Simulate AI generation delay
        await new Promise(resolve => setTimeout(resolve, 1500))

        const genreLabels: Record<string, string> = {
            fantasy: '판타지',
            sf: 'SF',
            romance: '로맨스',
            action: '액션',
            thriller: '스릴러',
            comedy: '코미디',
        }

        const moodLabels: Record<string, string> = {
            epic: '서사적인',
            bright: '밝은',
            dark: '어두운',
            hopeful: '희망적인',
            tense: '긴장감 있는',
            comic: '유쾌한',
        }

        const genre = genreLabels[input.value.genre] || input.value.genre
        const mood = moodLabels[input.value.mood] || input.value.mood

        // Generate mock prompt based on input
        prompt.value = {
            text: `${mood} 분위기의 ${genre} 단편 영화. ${input.value.keywords ? `"${input.value.keywords}"를 중심 테마로 ` : ''}${input.value.sceneCount}개의 씬으로 구성된 약 1분 길이의 영상입니다.${input.value.characterHints ? ` 주인공은 ${input.value.characterHints}.` : ''}${input.value.backgroundHints ? ` 배경은 ${input.value.backgroundHints}.` : ''}`,
            status: 'draft',
        }

        isGenerating.value = false
        nextStep()
    }

    const approvePrompt = () => {
        prompt.value.status = 'approved'
        generatePlot()
    }

    const regeneratePrompt = async () => {
        prompt.value.status = 'draft'
        isGenerating.value = true
        await new Promise(resolve => setTimeout(resolve, 1500))

        // Slightly different mock prompt
        prompt.value.text = prompt.value.text + ' 감정선이 섬세하게 표현됩니다.'
        isGenerating.value = false
    }

    const generatePlot = async (): Promise<void> => {
        isGenerating.value = true

        await new Promise(resolve => setTimeout(resolve, 2000))

        // Generate mock plot based on prompt
        plot.value = {
            text: `[줄거리]\n\n${prompt.value.text.includes('SF') || prompt.value.text.includes('sf')
                ? '미래의 화성 탐사 기지. 홀로 남겨진 우주인 민준은 3개월째 지구와의 교신이 끊긴 채 버티고 있다. 매일 고장난 통신 장비를 수리하며 희망을 잃지 않는다. 마침내 통신이 복구되는 순간, 딸의 목소리를 듣고 살아야 할 이유를 되찾는다.'
                : '이야기의 주인공은 평범한 일상 속에서 특별한 변화를 맞이한다. 처음에는 혼란스럽지만, 점차 자신만의 길을 찾아가며 성장해나간다. 마지막에는 중요한 깨달음을 얻고 새로운 시작을 맞이한다.'}`,
            status: 'draft',
        }

        isGenerating.value = false
        nextStep()
    }

    const approvePlot = () => {
        plot.value.status = 'approved'
        generateScenes()
    }

    const regeneratePlot = async () => {
        plot.value.status = 'draft'
        isGenerating.value = true
        await new Promise(resolve => setTimeout(resolve, 2000))

        plot.value.text = plot.value.text + '\n\n결말에서 예상치 못한 반전이 펼쳐진다.'
        isGenerating.value = false
    }

    const generateScenes = async (): Promise<void> => {
        isGenerating.value = true

        await new Promise(resolve => setTimeout(resolve, 2000))

        // Generate mock scenes based on plot
        const sceneTemplates = [
            { title: '시작: 일상의 평화', description: '주인공의 평범한 일상이 소개된다. 아침 햇살이 비치는 공간에서 하루가 시작된다.' },
            { title: '변화의 조짐', description: '무언가 달라지기 시작한다. 주인공은 미묘한 변화를 감지하고 호기심을 느낀다.' },
            { title: '갈등과 도전', description: '본격적인 갈등이 시작된다. 주인공은 어려움에 직면하지만 포기하지 않는다.' },
            { title: '전환점', description: '중요한 전환점이 찾아온다. 새로운 관점이나 도움을 통해 희망이 보인다.' },
            { title: '클라이맥스', description: '모든 것이 정점에 달한다. 가장 큰 도전을 맞이하고 결정적인 순간이 펼쳐진다.' },
            { title: '해결과 성장', description: '갈등이 해결되고 주인공은 성장한다. 새로운 시작을 암시하며 마무리된다.' },
            { title: '에필로그', description: '이후의 이야기를 간략히 보여준다. 평온하지만 달라진 일상이 펼쳐진다.' },
        ]

        scenes.value = sceneTemplates.slice(0, input.value.sceneCount).map((template, index) => ({
            id: index + 1,
            order: index + 1,
            title: template.title,
            description: template.description,
        }))

        isGenerating.value = false
        nextStep()
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
        isGenerating.value = true
        await new Promise(resolve => setTimeout(resolve, 1000))

        const scene = scenes.value.find(s => s.id === id)
        if (scene) {
            scene.description += ' (새롭게 생성된 내용)'
            saveCurrentState()
        }

        isGenerating.value = false
    }

    const addScene = () => {
        const newId = Math.max(...scenes.value.map(s => s.id), 0) + 1
        scenes.value.push({
            id: newId,
            order: scenes.value.length + 1,
            title: `새 씬 ${newId}`,
            description: '새로운 씬 설명을 입력하세요.',
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

