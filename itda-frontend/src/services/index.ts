import { useMock } from './config'

// Mock Services
import * as mockProjects from './mock/projects'
import * as mockAuth from './mock/auth'
import * as mockCharacters from './mock/characters' // Maps to objects
import * as mockScenes from './mock/scenes'
import * as mockNodes from './mock/nodes' // Maps to nodes
import * as mockTimeline from './mock/timeline'
import * as mockAi from './mock/ai'

// API Services
import * as apiProjects from './api/projects'
import * as apiAuth from './api/auth'
import * as apiObjects from './api/objects'
import * as apiScenes from './api/scenes'
import * as apiNodes from './api/nodes'
import * as apiTimeline from './api/timeline'
import * as apiAi from './api/ai'

// Export aggregated services
export const projectService = useMock ? mockProjects : apiProjects

// Auth and AI services export objects, so we need to export the object itself.
// mockAuth exports { mockAuthService }, apiAuth exports { apiAuthService }
export const authService = useMock ? mockAuth.mockAuthService : apiAuth.apiAuthService

export const objectService = useMock ? mockCharacters : apiObjects // Character & Object mapped
export const sceneService = useMock ? mockScenes : apiScenes
export const nodeService = useMock ? mockNodes : apiNodes

export const timelineService = useMock ? {
    // Fallback if needed, but mockTimeline has functions exported directly
    ...mockTimeline
} as any : apiTimeline

// AI Service exports object
export const aiService = useMock ? mockAi.mockAiService : apiAi.apiAiService
