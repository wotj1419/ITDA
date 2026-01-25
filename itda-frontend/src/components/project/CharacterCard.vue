<script setup lang="ts">
import type { ObjectSheet } from '../../types/api/objects'
import Button from '../common/Button.vue'
import Card from '../common/Card.vue'

interface Props {
  character: ObjectSheet
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'edit', character: ObjectSheet): void
  (e: 'delete', character: ObjectSheet): void
}>()
</script>

<template>
  <Card class="character-card">
    <div class="character-avatar">
      <img
        v-if="character.sheetImageUrl"
        :src="character.sheetImageUrl"
        :alt="character.name"
        class="avatar-image"
      />
      <span v-else class="avatar-placeholder">
        {{ character.name.charAt(0).toUpperCase() }}
      </span>
    </div>

    <h4 class="character-name">{{ character.name }}</h4>
    <p class="character-description">{{ character.description }}</p>

    <div class="character-actions">
      <Button variant="secondary" size="sm" @click="emit('edit', character)">
        Edit
      </Button>
      <Button variant="ghost" size="sm" class="delete-btn" @click="emit('delete', character)">
        Delete
      </Button>
    </div>
  </Card>
</template>

<style scoped>
.character-card {
  text-align: center;
  padding: 1.5rem;
}

.character-avatar {
  width: 80px;
  height: 80px;
  margin: 0 auto 1rem;
  border-radius: 50%;
  overflow: hidden;
  background: var(--rose-100);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--rose-500);
}

.character-name {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--gray-900);
  margin: 0 0 0.25rem;
}

.character-description {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0 0 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  min-height: 2.8em;
}

.character-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

.delete-btn {
  color: var(--error);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}
</style>
