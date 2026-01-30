import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../../stores/auth'

export interface ProjectEventMessage {
    event: string
    data: unknown
}

export interface ProjectEventTarget {
    type?: string
    id?: number
}

export interface ProjectEventPayload {
    jobId?: number
    type?: string
    status?: string
    target?: ProjectEventTarget
    resultUrl?: string
}

type Listener = (message: ProjectEventMessage) => void

type Connection = {
    client: Client
    listeners: Set<Listener>
    subscription: StompSubscription | null
}

const connections = new Map<number, Connection>()

function parseMessage(message: IMessage): ProjectEventMessage | null {
    try {
        const parsed = JSON.parse(message.body)
        const event = parsed?.event ?? parsed?.type
        if (!event) return null
        return { event, data: parsed?.data ?? parsed }
    } catch (error) {
        console.error('Failed to parse WS message', error)
        return null
    }
}

function createConnection(projectId: number): Connection {
    const listeners = new Set<Listener>()
    let subscription: StompSubscription | null = null

    const client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        onConnect: () => {
            if (subscription) return
            subscription = client.subscribe(`/topic/projects/${projectId}`, (message) => {
                const parsed = parseMessage(message)
                if (!parsed) return
                listeners.forEach((listener) => listener(parsed))
            })
        },
        onStompError: (frame) => {
            console.error('STOMP error', frame.headers['message'], frame.body)
        },
    })

    client.beforeConnect = () => {
        const authStore = useAuthStore()
        const token = authStore.accessToken
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
    }

    return { client, listeners, subscription }
}

export function subscribeProjectEvents(projectId: number, listener: Listener): () => void {
    let connection = connections.get(projectId)
    if (!connection) {
        connection = createConnection(projectId)
        connections.set(projectId, connection)
        connection.client.activate()
    }

    connection.listeners.add(listener)

    return () => {
        const current = connections.get(projectId)
        if (!current) return
        current.listeners.delete(listener)
        if (current.listeners.size === 0) {
            if (current.subscription) {
                current.subscription.unsubscribe()
                current.subscription = null
            }
            current.client.deactivate()
            connections.delete(projectId)
        }
    }
}
