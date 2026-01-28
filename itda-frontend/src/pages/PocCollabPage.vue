<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useCollabStore } from '../stores/collab'

const collabStore = useCollabStore()

const room = ref('project-poc')
const name = ref(localStorage.getItem('collab.displayName') || '')
const messageInput = ref('')
const areaRef = ref<HTMLDivElement | null>(null)

const isConnected = computed(() => collabStore.status === 'connected')

const connect = async () => {
  await collabStore.joinRoomByRoomId(room.value.trim() || 'project-poc', { name: name.value })
}

const disconnect = () => {
  collabStore.leaveRoom()
}

const send = () => {
  collabStore.sendMessage(messageInput.value)
  messageInput.value = ''
}

const onMove = (e: MouseEvent) => {
  const el = areaRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  collabStore.updateCursor(x, y)
}

onBeforeUnmount(() => {
  // PoC page 이동 시 연결 정리(원하면 유지로 변경 가능)
  collabStore.leaveRoom()
})
</script>

<template>
  <div class="poc">
    <h1>Collab PoC (W5)</h1>

    <div class="controls">
      <label>
        Room
        <input v-model="room" />
      </label>
      <label>
        Name
        <input v-model="name" />
      </label>

      <button v-if="!isConnected" @click="connect">Connect</button>
      <button v-else @click="disconnect">Disconnect</button>

      <div class="status">
        <div>status: {{ collabStore.status }}</div>
        <div>clientId: {{ collabStore.clientId }}</div>
      </div>
    </div>

    <div ref="areaRef" class="area" @mousemove="onMove">
      <div class="area-title">Move mouse here (broadcast cursor)</div>
      <div
        v-for="p in collabStore.participants"
        :key="p.odps"
        class="cursor"
        :style="{ left: (p.cursor?.x ?? -9999) + 'px', top: (p.cursor?.y ?? -9999) + 'px' }"
      >
        <span class="cursor-dot" />
        <span class="cursor-name">{{ p.name }}</span>
      </div>
    </div>

    <div class="grid">
      <section class="panel">
        <h2>Participants</h2>
        <ul>
          <li>
            <strong>ME</strong> ({{ collabStore.displayName }}) - muted: {{ collabStore.isMuted }}
          </li>
          <li v-for="p in collabStore.participants" :key="p.odps">
            {{ p.name }} - muted: {{ p.isMuted ?? '?' }} - location: {{ p.currentLocation ?? '' }}
          </li>
        </ul>
      </section>

      <section class="panel">
        <h2>Chat</h2>
        <div class="chat">
          <div v-for="m in collabStore.messages" :key="m.messageId" class="msg">
            <span class="who">{{ m.senderName }}</span>
            <span class="txt">{{ m.content }}</span>
          </div>
        </div>
        <form class="chat-input" @submit.prevent="send">
          <input v-model="messageInput" placeholder="type message..." />
          <button type="submit">Send</button>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.poc {
  padding: 24px;
}

.controls {
  display: flex;
  align-items: end;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #444;
}

input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 8px;
  min-width: 220px;
}

button {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}

.status {
  font-size: 12px;
  color: #666;
}

.area {
  position: relative;
  height: 360px;
  border: 1px dashed #bbb;
  border-radius: 16px;
  margin: 16px 0;
  overflow: hidden;
  background: #fafafa;
}

.area-title {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 12px;
  color: #666;
}

.cursor {
  position: absolute;
  transform: translate(8px, 8px);
  pointer-events: none;
}

.cursor-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  background: #ff3b81;
  margin-right: 6px;
}

.cursor-name {
  font-size: 11px;
  color: #222;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 2px 8px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.chat {
  height: 220px;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 10px;
  overflow: auto;
  background: #fafafa;
}

.msg {
  display: flex;
  gap: 8px;
  padding: 4px 0;
}

.who {
  font-weight: 600;
  white-space: nowrap;
}

.chat-input {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.chat-input input {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

