import { computed } from 'vue';
import type { Ref } from 'vue';
import type { VideoNodeData } from '../types/ui/sceneNodes';
import { useSceneNodeStore } from '../stores/sceneNode';

export function useSceneEditorTimeline(sceneId: Ref<string>, nodeStore = useSceneNodeStore()) {
  const timelineClips = computed(() => {
    return nodeStore.confirmedVideos.map((n, index) => {
      const data = n.data as VideoNodeData;
      const order = data.timelineOrder ?? index + 1;
      return {
        clipId: n.id,
        nodeId: n.id,
        sceneId: Number(sceneId.value) || undefined,
        sourceNodeId: n.id,
        thumbnailUrl: data.thumbnailUrl || '',
        videoUrl: data.videoUrl || undefined,
        duration: data.duration || 5,
        order,
        label: `영상 ${data.version || 1}`,
      };
    });
  });

  const totalDuration = computed(() =>
    timelineClips.value.reduce((sum, clip) => sum + clip.duration, 0)
  );

  return { timelineClips, totalDuration };
}
