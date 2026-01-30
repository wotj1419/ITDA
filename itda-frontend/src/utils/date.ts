export function formatRelativeTime(date: string | Date, now = new Date()): string {
    const d = new Date(date)
    const diffMs = now.getTime() - d.getTime()
    const diffMins = Math.floor(diffMs / (1000 * 60))
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffMins < 1) {
        return '방금 수정했어요'
    } else if (diffMins < 60) {
        return `${diffMins}분 전에 수정했어요`
    } else if (diffHours < 24) {
        return `${diffHours}시간 전에 수정했어요`
    } else if (diffDays === 1) {
        return '어제 수정했어요'
    } else if (diffDays < 7) {
        return `${diffDays}일 전에 수정했어요`
    } else {
        return `${d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })} 수정했어요`
    }
}
