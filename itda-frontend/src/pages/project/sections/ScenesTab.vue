<script setup lang="ts">
import { RouterLink, useRouter, type RouteLocationRaw } from 'vue-router';
import type { Scene } from '../../../types/api/scenes';
import type { ScenePreview, ScenePreviewClip } from '../composables/useProjectDetail';
import Card from '../../../components/common/Card.vue';
import Button from '../../../components/common/Button.vue';
import Badge from '../../../components/common/Badge.vue';
import AvatarGroup from '../../../components/common/AvatarGroup.vue';
import { useCollabStore } from '../../../stores/collab';
import type { CollabParticipant } from '../../../types/ui/collab';
import { Play, ChevronLeft, ChevronRight, X } from 'lucide-vue-next';

interface StatusConfig {
  label: string;
  variant: 'success' | 'info' | 'default';
}

interface Props {
  scenes: Scene[];
  projectId: number;
  resolveSceneStatusConfig: (status: unknown) => StatusConfig;
  getScenePreview: (sceneId: number) => ScenePreview;
  isPreviewLoading: (sceneId: number) => boolean;
  openPreview: (sceneId: number, clipIndex: number) => void;
  closePreview: () => void;
  activePreviewClip: ScenePreviewClip | null;
  activePreviewScene: Scene | null;
  setPreviewTrackRef: (sceneId: number, el: HTMLDivElement | null) => void;
  scrollPreview: (sceneId: number, direction: -1 | 1) => void;
  handlePreviewWheel: (sceneId: number, event: WheelEvent) => void;
  getClipWidth: (duration: number) => number;
  getOverflowCount: (sceneId: number) => number;
  getSceneEditLink: (scene: Scene) => RouteLocationRaw;
}

const props = defineProps<Props>();
const collabStore = useCollabStore();
const router = useRouter();

const getScenePresence = (sceneId: number) => {
  const list: (CollabParticipant & { isMe?: boolean })[] = [];
  const local = collabStore.localParticipant;
  if (local.currentLocation === 'SCENE_EDIT' && local.sceneId === sceneId) {
    list.push({ ...local, isMe: true });
  }
  collabStore.participants.forEach((p) => {
    if (p.currentLocation === 'SCENE_EDIT' && p.sceneId === sceneId) {
      list.push({ ...p, isMe: false });
    }
  });
  return list;
};

const getScenePresenceAvatars = (sceneId: number) =>
  getScenePresence(sceneId).map((p) => ({
    src: p.avatarUrl || '',
    fallback: p.name?.[0]?.toUpperCase() || '?',
    alt: p.name,
    title: `${p.name || 'Guest'}: Scene ${sceneId} 편집 중`,
    onClick: p.isMe ? undefined : () => router.push({ name: 'scene-edit', params: { projectId: props.projectId, sceneId } }),
  }));
</script>

<template>
  <div class="tab-content">
    <div class="section-header">
      <h2 class="section-title">씬 미리보기</h2>
      <div class="section-actions">
        <RouterLink
          :to="{ name: 'timeline', params: { id: projectId } }"
          custom
          v-slot="{ navigate }"
        >
          <Button variant="secondary" size="sm" @click="navigate">
            전체 타임라인
          </Button>
        </RouterLink>
      </div>
    </div>
    <p class="text-muted">
      씬별로 확정된 영상을 미리 확인하고 빠르게 편집 화면으로 이동할 수 있습니다.
    </p>

    <div v-if="scenes.length === 0" class="empty-state">
      아직 생성된 씬이 없습니다.
    </div>

    <div v-else class="scene-preview-list">
      <Card v-for="scene in scenes" :key="scene.sceneId" class="scene-preview-card">
        <div class="scene-preview-row">
          <div class="preview-info">
            <div class="preview-header">
              <Badge variant="default" size="sm">씬 {{ scene.order }}</Badge>
              <Badge
                :variant="resolveSceneStatusConfig(scene.status).variant"
                size="sm"
                class="status-badge"
              >
                {{ resolveSceneStatusConfig(scene.status).label }}
              </Badge>
              <div v-if="getScenePresence(scene.sceneId).length > 0" class="scene-presence">
                <AvatarGroup :avatars="getScenePresenceAvatars(scene.sceneId)" :max="3" size="sm" />
              </div>
            </div>
            <h4 class="preview-title">{{ scene.title }}</h4>
            <p v-if="scene.description" class="preview-description">
              {{ scene.description }}
            </p>
            <div class="preview-meta">
              <span>클립 {{ getScenePreview(scene.sceneId).clips.length }}개</span>
              <span>{{ getScenePreview(scene.sceneId).totalDuration }}s</span>
            </div>
            <div class="preview-actions">
              <RouterLink :to="getSceneEditLink(scene)" custom v-slot="{ navigate }">
                <Button variant="secondary" size="sm" @click="navigate">
                  씬 편집
                </Button>
              </RouterLink>
              <RouterLink
                :to="{ name: 'timeline', params: { id: projectId }, query: { sceneId: scene.sceneId } }"
                custom
                v-slot="{ navigate }"
              >
                <Button variant="primary" size="sm" @click="navigate">
                  씬 타임라인
                </Button>
              </RouterLink>
            </div>
          </div>

          <div class="preview-media">
            <div v-if="isPreviewLoading(scene.sceneId)" class="preview-loading">
              미리보기를 불러오는 중...
            </div>
            <template v-else>
              <div
                v-if="getScenePreview(scene.sceneId).clips.length > 0"
                class="preview-strip"
                :ref="(el) => setPreviewTrackRef(scene.sceneId, el as HTMLDivElement | null)"
                @wheel="(event) => handlePreviewWheel(scene.sceneId, event)"
              >
                <button
                  v-for="(clip, index) in getScenePreview(scene.sceneId).clips"
                  :key="`${scene.sceneId}-clip-${index}`"
                  class="preview-thumb"
                  type="button"
                  :aria-label="clip.label || scene.title"
                  :style="{ width: `${getClipWidth(clip.duration)}px` }"
                  @click="openPreview(scene.sceneId, index)"
                >
                  <img
                    :src="clip.thumbnailUrl || scene.thumbnailUrl"
                    :alt="clip.label || scene.title"
                  />
                  <span class="preview-duration">{{ clip.duration }}s</span>
                  <span class="preview-play">
                    <Play class="icon-sm" />
                  </span>
                </button>
              </div>
              <div v-else class="preview-empty">
                <img
                  v-if="scene.thumbnailUrl"
                  :src="scene.thumbnailUrl"
                  :alt="scene.title"
                />
                <template v-else>
                  <img src="/icon.png" alt="No Preview" class="preview-empty-icon" />
                  <span>아직 미리보기가 없습니다.</span>
                </template>
              </div>
            </template>

            <button
              v-if="getScenePreview(scene.sceneId).clips.length > 0"
              type="button"
              class="preview-scroll-btn left"
              aria-label="Scroll left"
              @click="scrollPreview(scene.sceneId, -1)"
            >
              <ChevronLeft class="icon-sm" />
            </button>
            <button
              v-if="getScenePreview(scene.sceneId).clips.length > 0"
              type="button"
              class="preview-scroll-btn right"
              aria-label="Scroll right"
              @click="scrollPreview(scene.sceneId, 1)"
            >
              <ChevronRight class="icon-sm" />
            </button>
            <div
              v-if="getOverflowCount(scene.sceneId) > 0"
              class="preview-overflow"
            >
              +{{ getOverflowCount(scene.sceneId) }}
            </div>
          </div>
        </div>
      </Card>
    </div>
  </div>

  <div v-if="activePreviewClip" class="preview-modal" @click.self="closePreview">
    <div class="preview-modal-content">
      <div class="preview-modal-header">
        <div>
          <p class="preview-modal-title">
            {{ activePreviewScene?.title || '씬 미리보기' }}
          </p>
          <p v-if="activePreviewClip.label" class="preview-modal-subtitle">
            {{ activePreviewClip.label }}
          </p>
        </div>
        <button type="button" class="preview-modal-close" @click="closePreview">
          <X class="icon-sm" />
        </button>
      </div>
      <div class="preview-modal-body">
        <video
          v-if="activePreviewClip.contentUrl"
          :src="activePreviewClip.contentUrl"
          controls
          autoplay
        />
        <div v-else class="preview-modal-empty">
          <img
            v-if="activePreviewClip.thumbnailUrl"
            :src="activePreviewClip.thumbnailUrl"
            :alt="activePreviewClip.label || 'preview'"
          />
          <template v-else>
            <img src="/icon.png" alt="No Preview" class="preview-modal-empty-icon" />
            <span>미리보기를 불러올 수 없습니다.</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.scene-presence {
  margin-left: auto;
}

.scene-presence :deep(.avatar-group) {
  align-items: center;
}
</style>
