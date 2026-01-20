<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { Edit2, MapPin, Link as LinkIcon, Mail, Film, Users, Heart, Eye } from 'lucide-vue-next'
import DefaultLayout from '../layouts/DefaultLayout.vue'

// Mock Data
const user = {
  name: 'Minjun Kim',
  email: 'minjun@example.com',
  jobTitle: 'Filmmaker & AI Artist',
  bio: 'AI 기술을 활용해 상상을 현실로 만드는 영화 제작자입니다. 주로 SF와 판타지 장르를 다루며, 새로운 시각적 경험을 탐구합니다.',
  location: 'Seoul, South Korea',
  website: 'minjun.art',
  avatarUrl: 'https://i.pravatar.cc/150?u=user123',
  stats: {
    projects: 12,
    followers: '1.5k',
    following: 840,
  },
}

const publicProjects = [
  {
    id: 1,
    title: 'The Martian Red',
    genre: 'Sci-Fi',
    description: 'A survival story on Mars. The protagonist finds an ancient ruin.',
    likes: 342,
    views: '1.2k',
    timeAgo: '2h ago',
    thumbnailUrl: 'https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format',
    duration: '00:45',
    badgeVariant: 'rose',
  },
  {
    id: 2,
    title: 'Neon Dreams',
    genre: 'Draft',
    description: 'Cyberpunk thriller set in neo-Tokyo.',
    likes: 156,
    views: 890,
    timeAgo: '1 day ago',
    thumbnailUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format',
    duration: '',
    badgeVariant: 'default',
  },
  {
    id: 3,
    title: 'Ocean Mystery',
    genre: 'Docu',
    description: 'Exploring the deep sea.',
    likes: 52,
    views: 230,
    timeAgo: '3 days ago',
    thumbnailUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format',
    duration: '',
    badgeVariant: 'default',
  },
]
</script>

<template>
  <DefaultLayout>
    <div class="profile-container">
      <!-- Profile Header Card -->
      <div class="card profile-header-card">
        <div
          class="profile-avatar"
          :style="{ backgroundImage: `url(${user.avatarUrl})` }"
        ></div>

        <div class="profile-info">
          <div class="profile-header-row">
            <div>
              <h1 class="profile-name">{{ user.name }}</h1>
              <p class="profile-job">{{ user.jobTitle }}</p>
            </div>
            <RouterLink to="/profile/edit" class="btn btn-secondary">
              <Edit2 class="w-4 h-4" />
              프로필 수정
            </RouterLink>
          </div>

          <p class="profile-bio">
            {{ user.bio }}
          </p>

          <div class="profile-meta">
            <div class="profile-meta-item">
              <MapPin class="w-4 h-4" />
              {{ user.location }}
            </div>
            <div class="profile-meta-item">
              <LinkIcon class="w-4 h-4" />
              <a :href="`https://${user.website}`" target="_blank" class="link-underline">{{ user.website }}</a>
            </div>
            <div class="profile-meta-item">
              <Mail class="w-4 h-4" />
              {{ user.email }}
            </div>
          </div>
        </div>
      </div>

      <!-- Stats Row -->
      <div class="stats-row">
        <div class="card stat-card">
          <div class="stat-icon-wrapper">
            <Film class="w-6 h-6" />
          </div>
          <div>
            <div class="stat-value">{{ user.stats.projects }}</div>
            <div class="stat-label">Projects</div>
          </div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon-wrapper">
            <Users class="w-6 h-6" />
          </div>
          <div>
            <div class="stat-value">{{ user.stats.followers }}</div>
            <div class="stat-label">Followers</div>
          </div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon-wrapper">
            <Heart class="w-6 h-6" />
          </div>
          <div>
            <div class="stat-value">{{ user.stats.following }}</div>
            <div class="stat-label">Following</div>
          </div>
        </div>
      </div>

      <!-- Projects Section -->
      <section>
        <div class="section-header">
          <h2 class="h3">Public Projects</h2>
          <div class="flex gap-2">
            <select class="form-select" style="width: auto; padding-right: 2rem;">
              <option>Latest</option>
              <option>Popular</option>
            </select>
          </div>
        </div>

        <div class="projects-grid">
          <RouterLink
            v-for="project in publicProjects"
            :key="project.id"
            :to="`/projects/${project.id}`"
            class="card card-clickable project-card"
          >
            <div
              class="card-thumbnail"
              :style="{ backgroundImage: `url(${project.thumbnailUrl})` }"
            >
              <div v-if="project.duration" class="duration-badge">
                {{ project.duration }}
              </div>
            </div>
            <div class="p-4">
              <div class="flex items-center justify-between mb-2">
                <h3 class="project-title">{{ project.title }}</h3>
                <span class="badge" :class="`badge-${project.badgeVariant}`">{{ project.genre }}</span>
              </div>
              <p class="project-desc">{{ project.description }}</p>
              <div class="project-footer">
                <div class="project-stats">
                  <Heart class="w-3 h-3" /> {{ project.likes }}
                  <Eye class="w-3 h-3 ml-2" /> {{ project.views }}
                </div>
                <span class="project-time">{{ project.timeAgo }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </section>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.profile-header-card {
  margin-bottom: 2rem;
  display: flex;
  align-items: flex-start;
  gap: 2rem;
  padding: 2rem;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background-position: center;
  background-size: cover;
  border: 4px solid var(--rose-50);
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
}

.profile-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.profile-name {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
  color: var(--gray-900);
}

.profile-job {
  font-size: 1.125rem;
  color: var(--gray-500);
}

.profile-bio {
  color: var(--gray-600);
  max-width: 600px;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.profile-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--gray-500);
}

.profile-meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.link-underline {
  text-decoration: underline;
  color: inherit;
}

/* Stats */
.stats-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  flex: 1;
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  background: var(--rose-50);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rose-500);
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gray-900);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

/* Projects */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.project-card {
  padding: 0;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
}

.card-thumbnail {
  aspect-ratio: 16/9;
  background-position: center;
  background-size: cover;
  position: relative;
}

.duration-badge {
  position: absolute;
  bottom: 0.5rem;
  right: 0.5rem;
  background: rgba(0, 0, 0, 0.6);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.625rem;
  color: white;
}

.project-title {
  font-weight: 600;
  font-size: 1rem;
  color: var(--gray-900);
  margin: 0;
}

.project-desc {
  font-size: 0.875rem;
  color: var(--gray-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.75rem;
}

.project-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.project-stats {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.project-time {
  font-size: 0.75rem;
  color: var(--gray-500);
}

/* Utils */
.h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--gray-900);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.form-select {
  padding: 0.5rem 2rem 0.5rem 1rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  background: white;
  font-size: 0.875rem;
  color: var(--gray-700);
}

.badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-rose {
  background: var(--rose-100);
  color: var(--rose-600);
}

.badge-default {
  background: var(--gray-100);
  color: var(--gray-600);
}
</style>
