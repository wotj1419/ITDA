<script setup lang="ts">
import { useCollabStore } from '../../stores/collab'
import { computed } from 'vue'

const collabStore = useCollabStore()

const activeCursors = computed(() => {
    const toScreen = collabStore.flowToScreenCoordinate
    if (!toScreen) return []
    return Array.from(collabStore.cursors.entries())
        .filter(([userId]) => userId !== collabStore.localParticipant.odps)
        .map(([userId, cursor]) => {
            const participant = collabStore.participants.find(p => p.odps === userId);
            const screen = toScreen({ x: cursor.x, y: cursor.y })
            if (!Number.isFinite(screen.x) || !Number.isFinite(screen.y)) {
                return null
            }
            return {
                userId,
                left: screen.x,
                top: screen.y,
                color: cursor.color,
                name: participant?.name || 'Unknown'
            }
        })
        .filter((item): item is { userId: string; left: number; top: number; color: string; name: string } => !!item)
})
</script>

<template>
  <div class="cursor-overlay">
    <div 
        v-for="cursor in activeCursors" 
        :key="cursor.userId"
        class="remote-cursor"
        :style="{ left: `${cursor.left}px`, top: `${cursor.top}px` }"
    >
        <!-- Standard cursor SVG icon -->
        <svg 
            width="24" 
            height="24" 
            viewBox="0 0 24 24" 
            fill="none" 
            xmlns="http://www.w3.org/2000/svg"
            class="cursor-icon"
        >
            <path 
                d="M5.65376 12.3673H5.46026L5.31717 12.4976L0.500002 16.8829L0.500002 1.19138L11.7841 12.3673L5.65376 12.3673Z" 
                :fill="cursor.color" 
                :stroke="cursor.color"
                stroke-width="1"
            />
        </svg>
        
        <!-- Name Tag -->
        <div class="cursor-label" :style="{ backgroundColor: cursor.color }">
            {{ cursor.name }}
        </div>
    </div>
  </div>
</template>

<style scoped>
.cursor-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    pointer-events: none; /* Let clicks pass through */
    z-index: 9998; /* Below control bar but above content */
    overflow: hidden;
}

.remote-cursor {
    position: absolute;
    transition: left 0.1s linear, top 0.1s linear; /* Smooth movement */
    will-change: left, top;
}

.cursor-icon {
    display: block;
    filter: drop-shadow(0 1px 2px rgba(0,0,0,0.3));
}

.cursor-label {
    position: absolute;
    left: 14px;
    top: 14px;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: bold;
    color: white;
    white-space: nowrap;
    box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
</style>
