export const DEFAULT_CLIP_SECONDS = 5

export function toDurationSeconds(duration?: number | null): number {
  if (typeof duration !== 'number' || !Number.isFinite(duration)) return 0
  if (duration >= 1000) {
    return Math.max(0, Math.round(duration / 1000))
  }
  return Math.max(0, Math.round(duration))
}

export function normalizeDurationSeconds(
  duration?: number | null,
  fallbackSeconds: number = DEFAULT_CLIP_SECONDS
): number {
  const normalized = toDurationSeconds(duration)
  return normalized > 0 ? normalized : fallbackSeconds
}
