/**
 * Node Store - 노드 기반 씬 에디터의 상태 관리
 * @module stores/node
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Node, NodeSettings, TimelineClip } from '../types';
import {
    fetchNodesBySceneId as mockFetchNodes,
    updateNodeSettings as mockUpdateSettings,
    confirmVideoToTimeline as mockConfirmVideo,
    unconfirmVideoFromTimeline as mockUnconfirmVideo,
} from '../services/mock/nodes';

export const useNodeStore = defineStore('node', () => {
    // ==========================================================================
    // State
    // ==========================================================================

    /** 현재 씬의 노드 목록 */
    const nodes = ref<Node[]>([]);

    /** 선택된 노드 ID */
    const selectedNodeId = ref<number | null>(null);

    /** 로딩 상태 */
    const isLoading = ref(false);

    /** 에러 메시지 */
    const error = ref<string | null>(null);

    /** 현재 프로젝트 ID */
    const currentProjectId = ref<number | null>(null);

    /** 현재 씬 ID */
    const currentSceneId = ref<number | null>(null);

    // ==========================================================================
    // Getters
    // ==========================================================================

    /** 현재 선택된 노드 */
    const selectedNode = computed(() =>
        selectedNodeId.value
            ? nodes.value.find((n) => n.nodeId === selectedNodeId.value) || null
            : null
    );

    /** 마스터 노드 목록 */
    const masterNodes = computed(() =>
        nodes.value.filter((n) => n.type === 'MASTER')
    );

    /** 확정된 비디오 노드 목록 */
    const confirmedVideos = computed(() =>
        nodes.value.filter((n) => n.type === 'VIDEO' && n.isConfirmed)
    );

    /** 타임라인 클립 목록 (확정된 비디오 기반) */
    const timelineClips = computed<TimelineClip[]>(() =>
        confirmedVideos.value.map((node, index) => ({
            clipId: `clip-${node.nodeId}`,
            nodeId: node.nodeId,
            thumbnailUrl: node.thumbnailUrl || '',
            duration: node.settings?.duration || 4,
            order: index + 1,
            label: node.title,
        }))
    );

    /** 총 영상 길이 (초) */
    const totalDuration = computed(() =>
        timelineClips.value.reduce((sum, clip) => sum + clip.duration, 0)
    );

    // ==========================================================================
    // Actions
    // ==========================================================================

    /**
     * 특정 씬의 노드 목록 로드
     */
    async function loadNodes(projectId: number, sceneId: number): Promise<void> {
        isLoading.value = true;
        error.value = null;
        currentProjectId.value = projectId;
        currentSceneId.value = sceneId;

        try {
            nodes.value = await mockFetchNodes(projectId, sceneId);
        } catch (e) {
            error.value = 'Failed to load nodes';
            console.error(e);
        } finally {
            isLoading.value = false;
        }
    }

    /**
     * 노드 선택
     */
    function selectNode(nodeId: number | null): void {
        selectedNodeId.value = nodeId;
    }

    /**
     * 노드 설정 업데이트
     */
    async function updateSettings(
        nodeId: number,
        settings: Partial<NodeSettings>
    ): Promise<boolean> {
        if (!currentProjectId.value || !currentSceneId.value) return false;

        try {
            const updated = await mockUpdateSettings(
                currentProjectId.value,
                currentSceneId.value,
                nodeId,
                settings
            );
            if (updated) {
                const index = nodes.value.findIndex((n) => n.nodeId === nodeId);
                if (index !== -1) {
                    nodes.value[index] = updated;
                }
                return true;
            }
            return false;
        } catch (e) {
            console.error(e);
            return false;
        }
    }

    /**
     * 비디오 노드를 타임라인에 확정
     */
    async function confirmVideo(nodeId: number): Promise<boolean> {
        if (!currentProjectId.value || !currentSceneId.value) return false;

        try {
            const success = await mockConfirmVideo(
                currentProjectId.value,
                currentSceneId.value,
                nodeId
            );
            if (success) {
                const node = nodes.value.find((n) => n.nodeId === nodeId);
                if (node) node.isConfirmed = true;
            }
            return success;
        } catch (e) {
            console.error(e);
            return false;
        }
    }

    /**
     * 타임라인에서 비디오 노드 확정 취소
     */
    async function unconfirmVideo(nodeId: number): Promise<boolean> {
        if (!currentProjectId.value || !currentSceneId.value) return false;

        try {
            const success = await mockUnconfirmVideo(
                currentProjectId.value,
                currentSceneId.value,
                nodeId
            );
            if (success) {
                const node = nodes.value.find((n) => n.nodeId === nodeId);
                if (node) node.isConfirmed = false;
            }
            return success;
        } catch (e) {
            console.error(e);
            return false;
        }
    }

    /**
     * 노드 상태 초기화
     */
    function clearNodes(): void {
        nodes.value = [];
        selectedNodeId.value = null;
        currentProjectId.value = null;
        currentSceneId.value = null;
        error.value = null;
    }

    // ==========================================================================
    // Return
    // ==========================================================================

    return {
        // State
        nodes,
        selectedNodeId,
        isLoading,
        error,
        currentProjectId,
        currentSceneId,
        // Getters
        selectedNode,
        masterNodes,
        confirmedVideos,
        timelineClips,
        totalDuration,
        // Actions
        loadNodes,
        selectNode,
        updateSettings,
        confirmVideo,
        unconfirmVideo,
        clearNodes,
    };
});
