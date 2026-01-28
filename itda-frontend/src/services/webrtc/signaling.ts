/**
 * Collab WebSocket Client (v0)
 *
 * - Raw WebSocket: /ws/room/{roomId}
 * - Handles: webrtc signaling / chat / presence / cursor
 */

export type CollabWsMessageType =
  | 'hello'
  | 'welcome'
  | 'presence.join'
  | 'presence.leave'
  | 'presence.update'
  | 'chat.send'
  | 'chat.message'
  | 'cursor.move'
  | 'cursor.update'
  | 'webrtc.offer'
  | 'webrtc.answer'
  | 'webrtc.ice'
  | 'ping'
  | 'pong'
  | 'error'

export interface CollabWsSender {
  clientId: string
  name?: string
}

export interface CollabWsMessage<T = unknown> {
  v: number
  type: CollabWsMessageType
  targetId?: string
  sender?: CollabWsSender
  data?: T
  ts?: number
}

export interface CollabWsCallbacks {
  onMessage: (message: CollabWsMessage) => void
  onConnected: () => void
  onDisconnected: () => void
  onError: (error: Error) => void
}

const buildWsUrl = (roomId: string): string => {
  const base = (import.meta.env.VITE_WS_BASE_URL as string | undefined) ?? ''
  if (base) {
    return `${base.replace(/\/$/, '')}/ws/room/${encodeURIComponent(roomId)}`
  }

  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = import.meta.env.DEV ? 'localhost:8080' : window.location.host
  return `${proto}://${host}/ws/room/${encodeURIComponent(roomId)}`
}

class CollabWsClient {
  private ws: WebSocket | null = null
  private callbacks: CollabWsCallbacks | null = null

  connect(roomId: string, callbacks: CollabWsCallbacks): void {
    this.disconnect()
    this.callbacks = callbacks

    const url = buildWsUrl(roomId)
    const ws = new WebSocket(url)
    this.ws = ws

    ws.onopen = () => {
      callbacks.onConnected()
    }

    ws.onmessage = (evt) => {
      try {
        const parsed = JSON.parse(String(evt.data)) as CollabWsMessage
        callbacks.onMessage(parsed)
      } catch (e) {
        callbacks.onError(e instanceof Error ? e : new Error('Failed to parse WS message'))
      }
    }

    ws.onclose = () => {
      callbacks.onDisconnected()
    }

    ws.onerror = () => {
      callbacks.onError(new Error('WebSocket error'))
    }
  }

  disconnect(): void {
    if (this.ws) {
      try {
        this.ws.close()
      } catch {
        // ignore
      }
    }
    this.ws = null
    this.callbacks = null
  }

  send(type: CollabWsMessageType, data?: unknown, targetId?: string): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return

    const msg: CollabWsMessage = {
      v: 0,
      type,
      data,
      ...(targetId ? { targetId } : {}),
    }
    this.ws.send(JSON.stringify(msg))
  }

  getConnectionStatus(): boolean {
    return !!this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

export const signalingService = new CollabWsClient()
