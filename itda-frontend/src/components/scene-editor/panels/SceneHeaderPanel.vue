<script setup lang="ts">
/**
 * SceneHeaderPanel - 씬 헤더 패널
 * 읽기 전용 + 편집 모드 지원
 */
import { ref, computed } from 'vue';
import type { Node } from '@vue-flow/core';
import type { SceneHeaderNodeData } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { BookOpen, Hash, Type, Text, Edit2, ExternalLink } from 'lucide-vue-next';

interface Props {
  node: Node<SceneHeaderNodeData>;
}

const props = defineProps<Props>();

const nodeStore = useSceneNodeStore();
const isEditing = ref(false);

const form = ref({
  title: '',
  description: '',
});

const data = computed(() => props.node.data as SceneHeaderNodeData | undefined);

function startEdit(): void {
  if (!data.value) return;
  form.value.title = data.value.title || '';
  form.value.description = data.value.description || '';
  isEditing.value = true;
}

function cancelEdit(): void {
  isEditing.value = false;
}

function saveEdit(): void {
  nodeStore.updateNode(props.node.id, {
    title: form.value.title,
    description: form.value.description,
  });
  isEditing.value = false;
}
</script>

<template>
  <BasePanel title="씬 정보" :icon="BookOpen">
    <template v-if="data">
      <!-- Read Mode -->
      <template v-if="!isEditing">
        <div class="panel-section">
          <label class="panel-label">
            <Hash class="panel-label-icon" />
            씬 번호
          </label>
          <p class="panel-value">{{ data.sceneOrder }}</p>
        </div>
        <div class="panel-section">
          <label class="panel-label">
            <Type class="panel-label-icon" />
            제목
          </label>
          <p class="panel-value">{{ data.title }}</p>
        </div>
        <div class="panel-section">
          <label class="panel-label">
            <Text class="panel-label-icon" />
            설명
          </label>
          <p class="panel-value panel-value--multiline">{{ data.description || '설명 없음' }}</p>
        </div>
        <div class="panel-actions">
          <button class="panel-btn panel-btn--secondary" @click="startEdit">
            <Edit2 class="panel-btn-icon" />
            편집
          </button>
          <button class="panel-btn panel-btn--text">
            <ExternalLink class="panel-btn-icon" />
            프로젝트 상세로 이동
          </button>
        </div>
      </template>

      <!-- Edit Mode -->
      <template v-else>
        <div class="panel-section">
          <label class="panel-label">
            <Type class="panel-label-icon" />
            제목
          </label>
          <input v-model="form.title" class="panel-input" placeholder="씬 제목" />
        </div>
        <div class="panel-section">
          <label class="panel-label">
            <Text class="panel-label-icon" />
            설명
          </label>
          <textarea v-model="form.description" class="panel-textarea" rows="4" placeholder="씬 설명"></textarea>
        </div>
      </template>
    </template>

    <template v-if="isEditing" #footer>
      <div class="panel-footer-actions">
        <button class="panel-btn panel-btn--secondary" @click="cancelEdit">취소</button>
        <button class="panel-btn panel-btn--primary" @click="saveEdit">저장</button>
      </div>
    </template>
  </BasePanel>
</template>

