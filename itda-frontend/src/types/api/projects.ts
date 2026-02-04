export type ProjectRole = 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER';

export interface Project {
  projectId: number;
  title: string;
  description?: string;
  genre?: string;
  thumbnailUrl?: string;
  previewType?: 'PROJECT_MERGE' | 'SCENE_MERGE' | 'CLIP' | null;
  previewThumbnailUrl?: string | null;
  previewVideoUrl?: string | null;
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
  members?: ProjectMember[];
}

export interface ProjectMember {
  userId: number;
  email: string;
  name: string;
  role: ProjectRole;
  profileImageUrl?: string | null;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  genre?: string;
}
