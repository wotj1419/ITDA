export function formatRelativeTime(date: string | Date, now = new Date()): string {
    const d = new Date(date)
    const diffMs = now.getTime() - d.getTime()
    const diffMins = Math.floor(diffMs / (1000 * 60))
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffMins < 1) {
        return 'just now'
    } else if (diffMins < 60) {
        return `${diffMins}m ago`
    } else if (diffHours < 24) {
        return `${diffHours}h ago`
    } else if (diffDays === 1) {
        return 'yesterday'
    } else if (diffDays < 7) {
        return `${diffDays}d ago`
    } else {
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    }
}
