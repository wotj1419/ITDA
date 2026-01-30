import { useUIStore } from '../stores/ui'

type GenerationKind = 'image' | 'grid' | 'shot' | 'video' | 'prompt' | 'scenario_prompt' | 'plot' | 'scenes'
type GenerationResult = 'success' | 'error'
type GenerationErrorOptions = {
  reason?: string
  advice?: string
}

const labelMap: Record<GenerationKind, string> = {
  image: '이미지',
  grid: '그리드',
  shot: '샷',
  video: '영상',
  prompt: '프롬프트',
  scenario_prompt: '시나리오 프롬프트',
  plot: '줄거리',
  scenes: '씬',
}

export function useGenerationToast() {
  const uiStore = useUIStore()

  const resolvePosition = (_kind: GenerationKind): 'top-right' => 'top-right'

  const startGenerationToast = (kind: GenerationKind, _eta?: string | null): string => {
    const label = labelMap[kind]
    return uiStore.showToast({
      type: 'progress',
      title: `${label} 생성중`,
      position: resolvePosition(kind),
      autoClose: false,
    })
  }

  const finishGenerationToast = (
    toastId: string,
    kind: GenerationKind,
    result: GenerationResult,
    options: GenerationErrorOptions = {}
  ): void => {
    const label = labelMap[kind]
    uiStore.removeToast(toastId)
    const isError = result === 'error'
    const reason = options.reason?.trim() || '알 수 없는 오류'
    const advice =
      options.advice?.trim() || '잠시 후 다시 시도하거나 프롬프트를 간단히 수정해 주세요.'

    uiStore.showToast({
      type: result,
      title: result === 'success' ? `${label} 생성 완료` : `${label} 생성 실패`,
      message: isError
        ? `원인: ${reason}\n권고: ${advice}`
        : `${label} 생성이 완료되었습니다.`,
      position: resolvePosition(kind),
      autoClose: !isError,
      duration: isError ? 0 : undefined,
    })
  }

  return {
    startGenerationToast,
    finishGenerationToast,
  }
}
