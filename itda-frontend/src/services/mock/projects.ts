import type { Project, ProjectDetail, ProjectMember } from '../../types'

// Mock project members
const mockMembers: Record<number, ProjectMember[]> = {
  1: [
    {
      userId: 1,
      email: 'minjun@example.com',
      name: 'Minjun Kim',
      role: 'OWNER',
      profileImage: 'https://i.pravatar.cc/150?u=a',
    },
    {
      userId: 2,
      email: 'sujin@example.com',
      name: 'Sujin Lee',
      role: 'EDITOR',
      profileImage: 'https://i.pravatar.cc/150?u=b',
    },
  ],
  2: [
    {
      userId: 1,
      email: 'minjun@example.com',
      name: 'Minjun Kim',
      role: 'OWNER',
      profileImage: 'https://i.pravatar.cc/150?u=a',
    },
  ],
  3: [
    {
      userId: 3,
      email: 'yuna@example.com',
      name: 'Yuna Park',
      role: 'OWNER',
      profileImage: 'https://i.pravatar.cc/150?u=c',
    },
    {
      userId: 1,
      email: 'minjun@example.com',
      name: 'Minjun Kim',
      role: 'VIEWER',
      profileImage: 'https://i.pravatar.cc/150?u=a',
    },
  ],
}

// Mock projects data
export const mockProjects: Project[] = [
  {
    projectId: 1,
    title: 'The Martian Red',
    description: 'A survival story on Mars. The protagonist finds an ancient ruin.',
    genre: 'Sci-Fi',
    thumbnailUrl: 'https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format',
    role: 'OWNER',
    memberCount: 2,
    sceneCount: 5,
    updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), // 1 week ago
  },
  {
    projectId: 2,
    title: 'Neon Dreams',
    description: 'Cyberpunk thriller set in neo-Tokyo.',
    genre: 'Draft',
    thumbnailUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format',
    role: 'OWNER',
    memberCount: 1,
    sceneCount: 5,
    updatedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // yesterday
    createdAt: new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString(), // 2 weeks ago
  },
  {
    projectId: 3,
    title: 'Ocean Depths',
    description: 'Deep sea exploration documentary with mysterious creatures.',
    genre: 'Documentary',
    thumbnailUrl: 'https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=600&auto=format',
    role: 'VIEWER',
    memberCount: 2,
    sceneCount: 8,
    updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(), // 3 days ago
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(), // 1 month ago
  },
]

// Mock project details
export const mockProjectDetails: Record<number, ProjectDetail> = {
  1: {
    projectId: 1,
    title: 'The Martian Red',
    description: 'A survival story on Mars. The protagonist finds an ancient ruin.',
    genre: 'Sci-Fi',
    thumbnailUrl: 'https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format',
    role: 'OWNER',
    memberCount: 2,
    sceneCount: 5,
    updatedAt: mockProjects[0]?.updatedAt ?? new Date().toISOString(),
    myRole: 'OWNER',
    ownerId: 1,
    members: mockMembers[1] ?? [],
  },
  2: {
    projectId: 2,
    title: 'Neon Dreams',
    description: 'Cyberpunk thriller set in neo-Tokyo.',
    genre: 'Draft',
    thumbnailUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format',
    role: 'OWNER',
    memberCount: 1,
    sceneCount: 5,
    updatedAt: mockProjects[1]?.updatedAt ?? new Date().toISOString(),
    myRole: 'OWNER',
    ownerId: 1,
    members: mockMembers[2] ?? [],
  },
  3: {
    projectId: 3,
    title: 'Ocean Depths',
    description: 'Deep sea exploration documentary with mysterious creatures.',
    genre: 'Documentary',
    thumbnailUrl: 'https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=600&auto=format',
    role: 'VIEWER',
    memberCount: 2,
    sceneCount: 8,
    updatedAt: mockProjects[2]?.updatedAt ?? new Date().toISOString(),
    myRole: 'VIEWER',
    ownerId: 3,
    members: mockMembers[3] ?? [],
  },
}

// Helper functions
export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMins < 60) {
    return `${diffMins}m ago`
  } else if (diffHours < 24) {
    return `${diffHours}h ago`
  } else if (diffDays === 1) {
    return 'yesterday'
  } else if (diffDays < 7) {
    return `${diffDays}d ago`
  } else {
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  }
}

export function getProjectProgress(projectId: number): { completed: number; total: number } {
  // Mock progress data
  const progressMap: Record<number, { completed: number; total: number }> = {
    1: { completed: 3, total: 5 },
    2: { completed: 1, total: 5 },
    3: { completed: 6, total: 8 },
  }
  return progressMap[projectId] || { completed: 0, total: 0 }
}

// Simulated API delay
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Mock API functions
export async function fetchProjects(): Promise<Project[]> {
  await delay(300)
  return [...mockProjects]
}

export async function fetchProjectById(projectId: number): Promise<ProjectDetail | null> {
  await delay(300)
  return mockProjectDetails[projectId] || null
}

export async function createProject(data: { title: string; description?: string; genre?: string }): Promise<Project> {
  await delay(500)
  const newProject: Project = {
    projectId: Date.now(),
    title: data.title,
    description: data.description,
    genre: data.genre,
    thumbnailUrl: undefined,
    role: 'OWNER',
    memberCount: 1,
    sceneCount: 0,
    updatedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  }
  mockProjects.unshift(newProject)

  // Also add to mockProjectDetails so loadProject works
  mockProjectDetails[newProject.projectId] = {
    ...newProject,
    myRole: 'OWNER',
    ownerId: 1, // Mock user ID
    members: [{
      userId: 1,
      email: 'minjun@example.com',
      name: 'Minjun Kim',
      role: 'OWNER',
      profileImage: 'https://i.pravatar.cc/150?u=user123',
    }],
  }

  return newProject
}

export async function deleteProject(projectId: number): Promise<void> {
  await delay(300)
  const index = mockProjects.findIndex((p) => p.projectId === projectId)
  if (index > -1) {
    mockProjects.splice(index, 1)
  }
}
