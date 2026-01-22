<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import DefaultLayout from '../layouts/DefaultLayout.vue'

const router = useRouter()

// Mock Form Data
const form = ref({
  name: 'Minjun Kim',
  jobTitle: 'Filmmaker & AI Artist',
  bio: 'AI 기술을 활용해 상상을 현실로 만드는 영화 제작자입니다. 주로 SF와 판타지 장르를 다루며, 새로운 시각적 경험을 탐구합니다.',
  email: 'minjun@example.com',
  website: 'https://minjun.art',
  location: 'Seoul, South Korea',
})

const handleSave = () => {
  // Save logic here (mock)
  router.push('/profile')
}

const handleCancel = () => {
  router.back()
}
</script>

<template>
  <DefaultLayout>
    <!-- Header moved closer to left/top -->
    <div class="edit-page-header">
      <button class="btn btn-ghost" @click="handleCancel">
        <ArrowLeft class="w-4 h-4" />
        Back
      </button>
    </div>

    <div class="profile-container" style="margin-top: 0;">
      <div class="content-wrapper">
        <div class="flex items-center justify-between mb-4">
          <h1 class="h2">Edit Profile</h1>
        </div>

        <div class="card p-6">
          <form @submit.prevent="handleSave">
            <!-- Avatar -->
            <div class="form-group mb-6">
              <label class="block text-sm font-semibold mb-2">Profile Photo</label>
              <div class="flex items-center gap-4">
                <div
                  class="avatar-preview"
                  style="background-image: url('https://i.pravatar.cc/150?u=user123')"
                ></div>
                <div>
                  <button type="button" class="btn btn-secondary btn-sm mb-2">Change Photo</button>
                  <div class="text-xs text-muted">JPG, GIF or PNG. Max size of 800K</div>
                </div>
              </div>
            </div>

            <!-- Name -->
            <div class="grid grid-cols-2 gap-4 mb-4">
              <div class="form-group">
                <label class="form-label required">Display Name</label>
                <input type="text" class="form-input" v-model="form.name" required />
              </div>
              <div class="form-group">
                <label class="form-label">Job Title</label>
                <input type="text" class="form-input" v-model="form.jobTitle" />
              </div>
            </div>

            <!-- Bio -->
            <div class="form-group mb-4">
              <label class="form-label">Bio</label>
              <textarea
                class="form-input form-textarea"
                rows="4"
                v-model="form.bio"
              ></textarea>
              <div class="text-xs text-right text-muted mt-1">{{ form.bio.length }} / 200</div>
            </div>

            <!-- Contact Info -->
            <h3 class="h4 mb-4 mt-6 contact-header">Contact Information</h3>

            <div class="grid grid-cols-2 gap-4 mb-4">
              <div class="form-group">
                <label class="form-label required">Email</label>
                <input
                  type="email"
                  class="form-input"
                  v-model="form.email"
                  readonly
                  style="background: var(--gray-50);"
                />
              </div>
              <div class="form-group">
                <label class="form-label">Website</label>
                <input type="url" class="form-input" v-model="form.website" />
              </div>
            </div>

            <div class="form-group mb-8">
              <label class="form-label">Location</label>
              <input type="text" class="form-input" v-model="form.location" />
            </div>

            <!-- Buttons -->
            <div class="form-actions">
              <button type="button" class="btn btn-ghost" @click="handleCancel">Cancel</button>
              <button type="submit" class="btn btn-primary">Save Changes</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.edit-page-header {
  padding: 0 1rem 1rem 1rem;
  margin-left: -2rem; /* Align to the screen edge/sidebar edge */
}

.content-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gray-900);
}

.h4 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
}

.card {
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 16px;
}

.p-6 {
  padding: 1.5rem;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-position: center;
  background-size: cover;
  border: 2px solid var(--rose-100);
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-700);
  margin-bottom: 0.5rem;
}

.form-label.required::after {
  content: '*';
  color: var(--rose-500);
  margin-left: 0.25rem;
}

.form-input {
  padding: 0.75rem 1rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-900);
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: var(--rose-400);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

.form-textarea {
  resize: vertical;
}

.grid {
  display: grid;
  gap: 1rem;
}

.grid-cols-2 {
  grid-template-columns: repeat(2, 1fr);
}

.contact-header {
  border-bottom: 1px solid var(--rose-100);
  padding-bottom: 0.5rem;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--rose-100);
}

.text-muted {
  color: var(--gray-500);
}

.text-sm {
  font-size: 0.875rem;
}

.text-xs {
  font-size: 0.75rem;
}

.block {
  display: block;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.mb-4 {
  margin-bottom: 1rem;
}

.mb-6 {
  margin-bottom: 1.5rem;
}

.mb-8 {
  margin-bottom: 2rem;
}

.mt-1 {
  margin-top: 0.25rem;
}

.mt-6 {
  margin-top: 1.5rem;
}
</style>
