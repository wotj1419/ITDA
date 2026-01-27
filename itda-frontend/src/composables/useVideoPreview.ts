
export function useVideoPreview() {
  function isVideo(url?: string): boolean {
    if (!url) return false
    const lower = url.toLowerCase()
    return (
      lower.startsWith('blob:') ||
      lower.startsWith('data:video') ||
      lower.endsWith('.mp4') ||
      lower.endsWith('.webm') ||
      lower.endsWith('.mov') ||
      lower.endsWith('/content')
    )
  }

  function playVideoPreview(event: MouseEvent) {
    const video = event.target as HTMLVideoElement
    if (video && video.paused) {
      video.play().catch(() => {
        // Auto-play might be blocked
      })
    }
  }

  function stopVideoPreview(event: MouseEvent) {
    const video = event.target as HTMLVideoElement
    if (video) {
      video.pause()
      video.currentTime = 0
    }
  }

  return {
    isVideo,
    playVideoPreview,
    stopVideoPreview
  }
}
