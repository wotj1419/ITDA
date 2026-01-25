<script setup lang="ts">
import type { ObjectSheet } from '../../../types/api/objects';
import CharacterCard from '../../../components/project/CharacterCard.vue';
import AddCharacterModal from '../../../components/project/AddCharacterModal.vue';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import { Plus } from 'lucide-vue-next';

interface Props {
  characters: ObjectSheet[];
  isGeneratingCharacter: boolean;
  openAddCharacterModal: () => void;
  handleAddCharacter: (data: { name: string; description: string; style: string }) => void | Promise<void>;
  handleEditCharacter: (character: ObjectSheet) => void;
  handleDeleteCharacter: (character: ObjectSheet) => void | Promise<void>;
}

defineProps<Props>();
</script>

<template>
  <div class="tab-content">
    <div class="section-header">
      <h2 class="section-title">오브젝트</h2>
      <Button variant="primary" @click="openAddCharacterModal">
        <Plus class="icon-sm" />
        오브젝트 추가
      </Button>
    </div>

    <div class="character-grid">
      <CharacterCard
        v-for="character in characters"
        :key="character.objectId"
        :character="character"
        @edit="handleEditCharacter"
        @delete="handleDeleteCharacter"
      />

      <Card
        :dashed="true"
        :clickable="true"
        class="add-character-card"
        @click="openAddCharacterModal"
      >
        <div class="add-character-icon">
          <Plus class="add-icon" />
        </div>
        <span class="add-character-text">Add Character</span>
      </Card>
    </div>
  </div>

  <AddCharacterModal
    :is-generating="isGeneratingCharacter"
    @submit="handleAddCharacter"
  />
</template>
