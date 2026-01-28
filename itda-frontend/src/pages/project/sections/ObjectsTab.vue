<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ObjectSheet, ObjectType } from '../../../types/api/objects';
import ObjectCard from '../../../components/project/ObjectCard.vue';
import AddObjectModal from '../../../components/project/AddObjectModal.vue';
import EditObjectModal from '../../../components/project/EditObjectModal.vue';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import ConfirmModal from '../../../components/common/ConfirmModal.vue';
import { Plus } from 'lucide-vue-next';

interface Props {
  objects: ObjectSheet[];
  editingObject: ObjectSheet | null;
  isSavingObject: boolean;
  isUpdatingObject: boolean;
  openAddObjectModal: () => void;
  openEditObjectModal: (object: ObjectSheet) => void;
  handleAddObject: (data: { name: string; type: ObjectType; description: string; style: string; file: File }) => void | Promise<void>;
  handleUpdateObject: (data: { objectId: number; name: string; type: ObjectType; description: string; style: string; file?: File | null }) => void | Promise<void>;
  handleDeleteObject: (object: ObjectSheet) => void | Promise<void>;
  handleDownloadObjectImage: (object: ObjectSheet) => void | Promise<void>;
}

const props = defineProps<Props>();

const deleteTargetObject = ref<ObjectSheet | null>(null);

const deleteObjectMessage = computed(() => {
  if (!deleteTargetObject.value) return '';
  return `"${deleteTargetObject.value.name}" 오브젝트를 삭제하시겠습니까?`;
});

const requestDeleteObject = (object: ObjectSheet): void => {
  deleteTargetObject.value = object;
};

const closeDeleteObjectModal = (): void => {
  deleteTargetObject.value = null;
};

const confirmDeleteObject = async (): Promise<void> => {
  if (!deleteTargetObject.value) return;
  await props.handleDeleteObject(deleteTargetObject.value);
  deleteTargetObject.value = null;
};
</script>

<template>
  <div class="tab-content">
    <div class="section-header">
      <h2 class="section-title">오브젝트</h2>
      <Button variant="primary" @click="openAddObjectModal">
        <Plus class="icon-sm" />
        오브젝트 추가
      </Button>
    </div>

    <div class="object-grid">
      <ObjectCard
        v-for="object in objects"
        :key="object.objectId"
        :object="object"
        @edit="openEditObjectModal"
        @delete="requestDeleteObject"
        @download="handleDownloadObjectImage"
      />

      <Card
        :dashed="true"
        :clickable="true"
        class="add-object-card"
        @click="openAddObjectModal"
      >
        <div class="add-object-icon">
          <Plus class="add-icon" />
        </div>
        <span class="add-object-text">오브젝트 추가</span>
      </Card>
    </div>
  </div>

  <AddObjectModal
    :is-saving="isSavingObject"
    @submit="handleAddObject"
  />

  <EditObjectModal
    :object="editingObject"
    :is-updating="isUpdatingObject"
    @submit="handleUpdateObject"
  />

  <ConfirmModal
    :is-open="!!deleteTargetObject"
    title="오브젝트 삭제"
    :message="deleteObjectMessage"
    confirm-text="삭제"
    cancel-text="취소"
    :is-dangerous="true"
    @confirm="confirmDeleteObject"
    @cancel="closeDeleteObjectModal"
  />
</template>
