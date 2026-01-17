import { onMounted, onUnmounted } from 'vue'
import { useUIStore } from '../stores/ui'

const SIDEBAR_SHORTCUT_SHOWN_KEY = 'sidebar-shortcut-shown'

/**
 * Composable for sidebar keyboard shortcut (Ctrl/Cmd + B)
 * - Registers global keyboard listener
 * - Shows one-time toast notification for discoverability
 */
export function useSidebarShortcut() {
    const uiStore = useUIStore()

    const handleKeydown = (e: KeyboardEvent) => {
        // Ctrl+B (Windows/Linux) or Cmd+B (Mac)
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
            // Don't trigger if user is typing in an input
            const target = e.target as HTMLElement
            if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
                return
            }

            e.preventDefault()
            uiStore.toggleSidebar()
        }
    }

    const showShortcutHint = () => {
        // Show hint only once per user
        if (localStorage.getItem(SIDEBAR_SHORTCUT_SHOWN_KEY)) {
            return
        }

        // Delay to show after page load
        setTimeout(() => {
            uiStore.showToast({
                type: 'info',
                title: 'Pro Tip',
                message: 'Ctrl+B로 사이드바를 빠르게 토글할 수 있어요!',
                duration: 5000,
            })
            localStorage.setItem(SIDEBAR_SHORTCUT_SHOWN_KEY, 'true')
        }, 2000)
    }

    onMounted(() => {
        window.addEventListener('keydown', handleKeydown)
        showShortcutHint()
    })

    onUnmounted(() => {
        window.removeEventListener('keydown', handleKeydown)
    })

    return {
        toggleSidebar: uiStore.toggleSidebar,
    }
}
