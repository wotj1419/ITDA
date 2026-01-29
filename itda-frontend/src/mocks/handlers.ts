import { http, HttpResponse } from 'msw'
import type {
    ApiResponse,

    ProjectListItem,
    TimelineClip,
    Scene,
    LoginResponse,
    User
} from '../types'

// Mock Data
const MOCK_USER: User = {
    userId: 1,
    email: 'user@example.com',
    name: 'Mock User',
    profileImage: 'https://placehold.co/100'
}

// Mock Data
const MOCK_PROJECTS: ProjectListItem[] = [
    {
        projectId: 101,
        title: '화성 브이로그',
        thumbnailUrl: 'https://placehold.co/600x400/2a2a2a/FFF?text=Mars+Vlog',
        role: 'OWNER',
        memberCount: 3,
        sceneCount: 5,
        updatedAt: new Date().toISOString(),
        createdAt: '2026-01-15T10:00:00Z',
        description: '화성에서 살아남기 위한 일상 기록',
        genre: 'SF'
    },
    {
        projectId: 102,
        title: 'Deep Ocean Mystery',
        thumbnailUrl: 'https://placehold.co/600x400/003366/FFF?text=Ocean',
        role: 'EDITOR',
        memberCount: 5,
        sceneCount: 2,
        updatedAt: new Date(Date.now() - 86400000).toISOString(),
        createdAt: '2026-01-20T15:30:00Z',
        description: '심해 탐사 다큐멘터리',
        genre: 'DOCUMENTARY'
    }
]

const MOCK_TIMELINE: TimelineClip[] = [
    {
        clipId: 'clip-1',
        nodeId: 1001,
        sceneId: 201,
        thumbnailUrl: 'https://placehold.co/300x200/333/FFF?text=Scene+1',
        duration: 5,
        order: 1,
        label: 'Scene 1: Intro'
    },
    {
        clipId: 'clip-2',
        nodeId: 1002,
        sceneId: 202,
        thumbnailUrl: 'https://placehold.co/300x200/444/FFF?text=Scene+2',
        duration: 3,
        order: 2,
        label: 'Scene 2: Exploration'
    },
    {
        clipId: 'clip-3',
        nodeId: 1003,
        sceneId: 203,
        thumbnailUrl: 'https://placehold.co/300x200/555/FFF?text=Scene+3',
        duration: 4,
        order: 3,
        label: 'Scene 3: Discovery'
    }
]

const MOCK_SCENES: Scene[] = [
    {
        sceneId: 201,
        title: 'Intro: The Arrival',
        description: 'Landing on Mars base',
        order: 1,
        status: 'COMPLETED',
        thumbnailUrl: 'https://placehold.co/300x200/333/FFF?text=Scene+1'
    },
    {
        sceneId: 202,
        title: 'Exploration',
        description: 'Walking on the surface',
        order: 2,
        status: 'IN_PROGRESS',
        thumbnailUrl: 'https://placehold.co/300x200/444/FFF?text=Scene+2'
    },
    {
        sceneId: 203,
        title: 'Discovery',
        description: 'Finding an ancient artifact',
        order: 3,
        status: 'DRAFT',
        thumbnailUrl: 'https://placehold.co/300x200/555/FFF?text=Scene+3'
    }
]

// Common Base URL handling
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
// Helper to strip trailing slash if present in env, though usually handled by client
const getPath = (path: string) => {
    // If BASE_URL is just /api/v1 (relative) or http://... (absolute), MSW handles it.
    // We'll use the full URL if it's absolute, or relative properly.
    // Ideally, we can just match against the path suffix if we want to be loose,
    // but strict matching is better.
    return `${BASE_URL}${path}`
}

export const handlers = [
    // ---------------------------------------------------------
    // Auth APIs
    // ---------------------------------------------------------

    // POST /auth/signup
    http.post(getPath('/auth/signup'), async () => {
        return HttpResponse.json<ApiResponse<{ userId: number; email: string; name: string }>>({
            code: 'SUCCESS',
            message: 'User registered successfully',
            data: {
                userId: MOCK_USER.userId,
                email: MOCK_USER.email,
                name: MOCK_USER.name
            }
        })
    }),

    // POST /auth/login
    http.post(getPath('/auth/login'), async () => {
        return HttpResponse.json<ApiResponse<LoginResponse>>({
            code: 'SUCCESS',
            message: 'Login successful',
            data: {
                accessToken: 'mock-access-token-' + Date.now(),
                tokenType: 'Bearer',
                expiresIn: 3600,
                user: MOCK_USER
            }
        })
    }),

    // GET /auth/me
    http.get(getPath('/auth/me'), () => {
        return HttpResponse.json<ApiResponse<User>>({
            code: 'SUCCESS',
            data: MOCK_USER
        })
    }),

    // ---------------------------------------------------------
    // Project APIs
    // ---------------------------------------------------------

    // GET /projects - List Projects
    http.get(getPath('/projects'), () => {
        return HttpResponse.json<ApiResponse<{ items: ProjectListItem[], page: number, size: number, total: number }>>({
            code: 'SUCCESS',
            message: 'Projects retrieved successfully',
            data: {
                items: MOCK_PROJECTS,
                page: 0,
                size: 20,
                total: MOCK_PROJECTS.length
            }
        })
    }),

    // POST /projects - Create Project (Fix for 401 error)
    http.post(getPath('/projects'), async ({ request }) => {
        const body = await request.json() as { title: string, description?: string, genre?: string }
        const newProject: ProjectListItem = {
            projectId: Date.now(),
            title: body.title,
            description: body.description || '',
            genre: body.genre || 'DRAMA',
            role: 'OWNER',
            memberCount: 1,
            sceneCount: 0,
            updatedAt: new Date().toISOString(),
            createdAt: new Date().toISOString(),
            thumbnailUrl: 'https://placehold.co/600x400/333/FFF?text=New+Project'
        }

        // Add to mock data store (optional, for this session)
        MOCK_PROJECTS.unshift(newProject)

        return HttpResponse.json<ApiResponse<ProjectListItem>>({
            code: 'SUCCESS',
            message: 'Project created',
            data: newProject
        })
    }),

    // GET /projects/:id - Project Detail
    http.get(getPath('/projects/:id'), ({ params }) => {
        const { id } = params
        const project = MOCK_PROJECTS.find(p => p.projectId === Number(id))

        if (!project) {
            return HttpResponse.json({
                code: 'PROJECT_NOT_FOUND',
                message: 'Project not found'
            }, { status: 404 })
        }

        return HttpResponse.json<ApiResponse<ProjectListItem>>({
            code: 'SUCCESS',
            data: {
                ...project,
                myRole: 'OWNER',
                ownerId: 1,
                members: []
            } as any // Cast for ProjectDetail vs ProjectListItem mix
        })
    }),

    // ---------------------------------------------------------
    // Timeline APIs
    // ---------------------------------------------------------

    // GET /projects/:id/timeline
    http.get(getPath('/projects/:id/timeline'), () => {
        return HttpResponse.json<ApiResponse<TimelineClip[]>>({
            code: 'SUCCESS',
            data: MOCK_TIMELINE
        })
    }),

    // GET /scenes/:id/timeline
    http.get(getPath('/scenes/:id/timeline'), () => {
        // Return a subset or different mock for scene timeline
        // For simplicity, returning a single clip corresponding to scene
        return HttpResponse.json<ApiResponse<TimelineClip[]>>({
            code: 'SUCCESS',
            data: [MOCK_TIMELINE[0] as TimelineClip]
        })
    }),

    // ---------------------------------------------------------
    // Scene APIs
    // ---------------------------------------------------------

    // GET /projects/:id/scenes
    http.get(getPath('/projects/:id/scenes'), () => {
        return HttpResponse.json<ApiResponse<Scene[]>>({
            code: 'SUCCESS',
            data: MOCK_SCENES
        })
    }),

    // POST /projects/:id/scenes
    http.post(getPath('/projects/:id/scenes'), async ({ request }) => {
        const body = await request.json() as { title: string, description: string }
        const newScene: Scene = {
            sceneId: Date.now(),
            title: body.title,
            description: body.description,
            order: MOCK_SCENES.length + 1,
            status: 'DRAFT',
            thumbnailUrl: 'https://placehold.co/300x200'
        }

        return HttpResponse.json<ApiResponse<Scene>>({
            code: 'SUCCESS',
            message: 'Scene created',
            data: newScene
        })
    }),

    // ---------------------------------------------------------
    // AI APIs
    // ---------------------------------------------------------

    // POST /ai/prompts/generate
    http.post(getPath('/ai/prompts/generate'), async () => {
        // Simulate AI delay
        await new Promise(resolve => setTimeout(resolve, 1000))

        return HttpResponse.json<ApiResponse<{ prompt: string; negativePrompt?: string }>>({
            code: 'SUCCESS',
            message: 'Prompt generated successfully',
            data: {
                prompt: "Cinematic wide shot of a futuristic Mars base cafeteria, soft morning light, peaceful atmosphere, highly detailed, 8k resolution",
                negativePrompt: "blurry, low quality, distorted, ugly"
            }
        })
    })
]
