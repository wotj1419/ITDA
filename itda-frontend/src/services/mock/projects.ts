import type { Project, ProjectDetail, ProjectMember } from '../../types'

// 1. Mock 데이터 정의 (기존과 동일)
const mockMembers: Record<number, ProjectMember[]> = {
  1: [
    { userId: 1, email: 'minjun@example.com', name: 'Minjun Kim', role: 'OWNER', profileImage: 'https://i.pravatar.cc/150?u=a' },
    { userId: 2, email: 'sujin@example.com', name: 'Sujin Lee', role: 'EDITOR', profileImage: 'https://i.pravatar.cc/150?u=b' },
  ],
  2: [
    { userId: 1, email: 'minjun@example.com', name: 'Minjun Kim', role: 'OWNER', profileImage: 'https://i.pravatar.cc/150?u=a' },
  ],
  3: [
    { userId: 3, email: 'yuna@example.com', name: 'Yuna Park', role: 'OWNER', profileImage: 'https://i.pravatar.cc/150?u=c' },
    { userId: 1, email: 'minjun@example.com', name: 'Minjun Kim', role: 'VIEWER', profileImage: 'https://i.pravatar.cc/150?u=a' },
  ],
}

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
    updatedAt: new Date(Date.now() - 10 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
    isDeleted: false,
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
    updatedAt: new Date(Date.now() - 50 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString(),
    isDeleted: false,
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
    updatedAt: new Date(Date.now() - 90 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    isDeleted: false,
  },
]

export const mockProjectDetails: Record<number, ProjectDetail> = {
  1: {
    ...mockProjects[0]!, // !를 써서 초기 데이터 존재 보장
    myRole: 'OWNER',
    ownerId: 1,
    members: mockMembers[1] ?? [],
  },
  2: {
    ...mockProjects[1]!,
    myRole: 'OWNER',
    ownerId: 1,
    members: mockMembers[2] ?? [],
  },
  3: {
    ...mockProjects[2]!,
    myRole: 'VIEWER',
    ownerId: 3,
    members: mockMembers[3] ?? [],
  },
}

// 2. 유틸리티 및 API 함수들
import { formatRelativeTime } from '../../utils/date'
export { formatRelativeTime }

import { getSceneProgress } from './scenes'
export function getProjectProgress(projectId: number) {
  return getSceneProgress(projectId)
}

const delay = (_ms: number) => new Promise((resolve) => setTimeout(resolve, 50))

export async function fetchProjects(): Promise<Project[]> {
  await delay(300)
  return mockProjects.filter(p => !p.isDeleted)
}

export async function fetchProjectById(projectId: number): Promise<ProjectDetail | null> {
  await delay(300)
  const project = mockProjectDetails[projectId]
  if (!project || project.isDeleted) return null
  return project
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
    isDeleted: false,
  }
  mockProjects.push(newProject)

  mockProjectDetails[newProject.projectId] = {
    ...newProject,
    myRole: 'OWNER',
    ownerId: 1,
    members: [{ userId: 1, email: 'minjun@example.com', name: 'Minjun Kim', role: 'OWNER', profileImage: 'https://i.pravatar.cc/150?u=user123' }],
  }

  return newProject
}

export async function deleteProject(projectId: number): Promise<void> {
  await delay(300)
  // Soft delete
  const now = new Date().toISOString()
  const project = mockProjects.find((p) => p.projectId === projectId)
  if (project) {
    project.isDeleted = true
    project.deletedAt = now
  }

  const detail = mockProjectDetails[projectId]
  if (detail) {
    detail.isDeleted = true
    detail.deletedAt = now
  }
}

export async function fetchDeletedProjects(): Promise<Project[]> {
  await delay(300)
  return mockProjects.filter(p => p.isDeleted)
}

export async function restoreProject(projectId: number): Promise<void> {
  await delay(300)
  const project = mockProjects.find(p => p.projectId === projectId)
  if (project) {
    project.isDeleted = false
    project.deletedAt = undefined
  }

  const detail = mockProjectDetails[projectId]
  if (detail) {
    detail.isDeleted = false
    detail.deletedAt = undefined
  }
}

export async function hardDeleteProject(projectId: number): Promise<void> {
  await delay(300)
  const index = mockProjects.findIndex(p => p.projectId === projectId)
  if (index > -1) {
    mockProjects.splice(index, 1)
  }
  delete mockProjectDetails[projectId]
}

/**
 * 프로젝트 업데이트 함수 (수정된 핵심 로직)
 */
export async function updateProject(projectId: number, data: Partial<Project>): Promise<Project | null> {
  await delay(300)
  const projectIndex = mockProjects.findIndex((p) => p.projectId === projectId)

  // 1. 인덱스가 유효한지 확인
  if (projectIndex > -1) {
    const targetProject = mockProjects[projectIndex]

    // 2. 가드 클로즈: TypeScript가 targetProject가 존재함을 확신하게 함
    if (!targetProject) return null

    // 3. 업데이트 데이터 생성
    const updatedDate = new Date().toISOString()
    const updatedProject: Project = {
      ...targetProject,
      ...data,
      updatedAt: updatedDate
    }

    // 4. 원본 배열(List) 업데이트
    mockProjects[projectIndex] = updatedProject

    // 5. 상세 정보(Detail) 업데이트 (동기화)
    if (mockProjectDetails[projectId]) {
      mockProjectDetails[projectId] = {
        ...mockProjectDetails[projectId],
        ...data,
        updatedAt: updatedDate // 리스트와 동일한 시간 적용
      }
    }

    return updatedProject
  }

  return null
}