export type ProjectRole = 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER';

export interface Project {
  projectId: number;
  title: string;
  description?: string;
  genre?: string;
  thumbnailUrl?: string;
  role: ProjectRole;
  memberCount: number;
  sceneCount: number;
  updatedAt: string;
  createdAt?: string;
  isDeleted?: boolean;
  deletedAt?: string;
}

export interface ProjectListItem extends Project {
  // Same as Project for now
}

export interface ProjectDetail extends Project {
  myRole: ProjectRole;
  ownerId: number;
  members: ProjectMember[];
}

export interface ProjectMember {
  userId: number;
  email: string;
  name: string;
  role: ProjectRole;
  profileImage?: string;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  genre?: string;
}
