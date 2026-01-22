/**
 * Scene Node Store - Vue Flow 노드 기반 씬 에디터 상태 관리
 * @module stores/sceneNode
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 5
 */
import { defineStore } from 'pinia';
import { ref, computed, watch } from 'vue';
import type { Node, Edge } from '@vue-flow/core';
import {
    NodeType,
    JobStatus,
    PromptStatus,
    VALID_CONNECTIONS,
    NODE_HEIGHTS,
    NODE_WIDTHS,
    type BaseNodeData,
    type AnyNodeData,
    type SceneHeaderNodeData,
    type MasterImageNodeData,
    type StoryboardGridNodeData,
    type ShotNodeData,
    type VideoNodeData,
} from '../types/node';
import { generateMockSceneNodes, generateSimpleMockNodes } from '../services/mock/sceneNodes';

// =============================================================================
// Helper Functions
// =============================================================================

function generateId(): string {
    return `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

function createBaseNodeData(
    id: string,
    type: NodeType,
    parentNodeId: string | null = null,
    version: number = 1
): BaseNodeData {
    const now = new Date().toISOString();
    return {
        id,
        type,
        jobStatus: null,
        promptStatus: PromptStatus.DRAFT,
        createdAt: now,
        updatedAt: now,
        versionGroupId: id,
        version,
        parentNodeId,
        isCollapsed: false,
        childCount: 0,
    };
}

function getDefaultNodeDimensions(type: NodeType): { width: number; height: number } {
    return {
        width: NODE_WIDTHS[type] ?? 200,
        height: NODE_HEIGHTS[type] ?? 150,
    };
}

type EdgeMeta = {
    isTransition: boolean;
    isConfirmed: boolean;
};

// =============================================================================
// Type-safe node creation
// =============================================================================

type SceneNode = Node<AnyNodeData>;
type NodePositionSnapshot = Array<{
    id: string;
    position: { x: number; y: number };
    dimensions?: { width: number | string; height: number | string };
}>;

const MAX_POSITION_HISTORY = 20;

function buildPositionSnapshot(nodes: SceneNode[]): NodePositionSnapshot {
    return nodes.map((node) => ({
        id: node.id,
        position: { x: node.position.x, y: node.position.y },
        dimensions: {
            width: node.style?.width ?? '',
            height: node.style?.height ?? '',
        },
    }));
}

function snapshotsEqual(
    a: NodePositionSnapshot,
    b: NodePositionSnapshot
): boolean {
    if (a.length !== b.length) return false;
    const positions = new Map(a.map((item) => [item.id, item]));
    return b.every((item) => {
        const existing = positions.get(item.id);
        return (
            existing !== undefined &&
            existing.position.x === item.position.x &&
            existing.position.y === item.position.y &&
            existing.dimensions?.width === item.dimensions?.width &&
            existing.dimensions?.height === item.dimensions?.height
        );
    });
}

// =============================================================================
// Store
// =============================================================================

export const useSceneNodeStore = defineStore('sceneNode', () => {
    // ==========================================================================
    // State
    // ==========================================================================

    const nodes = ref<SceneNode[]>([]);
    const edges = ref<Edge[]>([]);
    const selectedNodeId = ref<string | null>(null);
    const positionHistory = ref<NodePositionSnapshot[]>([]);

    // end shot 선택 모드 (트랜지션 영상용)
    const selectionMode = ref<'none' | 'selectEndShot'>('none');
    const endShotTargetVideoId = ref<string | null>(null);

    const sceneId = ref<string | null>(null);

    // 로딩 상태
    const isLoading = ref(false);
    const isSaving = ref(false);

    // ==========================================================================
    // Getters
    // ==========================================================================

    const selectedNode = computed(() =>
        nodes.value.find((n) => n.id === selectedNodeId.value) || null
    );

    const nodesByType = computed(() => (type: NodeType) =>
        nodes.value.filter((n) => n.data?.type === type)
    );

    const activeMaster = computed(() =>
        nodes.value.find(
            (n) =>
                n.data?.type === NodeType.MASTER_IMAGE &&
                (n.data as MasterImageNodeData).isActive
        ) || null
    );

    const confirmedVideos = computed(() =>
        nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.VIDEO &&
                (n.data as VideoNodeData).isConfirmed
        )
    );

    const childNodes = computed(() => (parentId: string) =>
        edges.value
            .filter((e) => e.source === parentId)
            .map((e) => nodes.value.find((n) => n.id === e.target))
            .filter(Boolean) as SceneNode[]
    );

    // ==========================================================================
    // Actions - Persistence
    // ==========================================================================

    function getStorageKey(id: string): string {
        return `scene-nodes-${id}`;
    }

    function saveToLocalStorage(): void {
        if (!sceneId.value) return;
        isSaving.value = true;
        try {
            const data = {
                nodes: nodes.value,
                edges: edges.value,
                updatedAt: new Date().toISOString(),
            };
            localStorage.setItem(getStorageKey(sceneId.value), JSON.stringify(data));
        } catch (e) {
            console.error('Failed to save scene nodes:', e);
        } finally {
            setTimeout(() => {
                isSaving.value = false;
            }, 500);
        }
    }

    // 간단한 디바운스 처리
    let saveTimeout: ReturnType<typeof setTimeout> | null = null;
    function debouncedSave() {
        if (saveTimeout) clearTimeout(saveTimeout);
        saveTimeout = setTimeout(() => {
            saveToLocalStorage();
        }, 1000); // 1초 후 저장
    }

    // 노드/엣지 변경 감지하여 자동 저장
    // (로드 중이나 초기화 중에는 불필요하게 저장되지 않도록 주의)
    watch(
        [nodes, edges],
        () => {
            if (!isLoading.value && sceneId.value) {
                debouncedSave();
            }
        },
        { deep: true }
    );

    // ==========================================================================
    // Actions - Load
    // ==========================================================================

    async function loadSceneNodes(sceneIdParam: string): Promise<void> {
        isLoading.value = true;
        try {
            sceneId.value = sceneIdParam;

            // 1. LocalStorage 확인
            const savedData = localStorage.getItem(getStorageKey(sceneIdParam));
            if (savedData) {
                try {
                    const parsed = JSON.parse(savedData);
                    // 데이터 유효성 체크 대략적으로 (nodes 배열 존재 여부)
                    if (Array.isArray(parsed.nodes)) {
                        nodes.value = parsed.nodes;
                        edges.value = parsed.edges || [];
                        console.log('Restored nodes from localStorage');
                        return; // 저장된 데이터가 있으면 여기서 종료
                    }
                } catch (e) {
                    console.error('Failed to parse saved scene nodes:', e);
                }
            }

            // 2. 저장된 데이터가 없으면 Mock 데이터 또는 초기상태 시작
            // nodes.value = [];
            // edges.value = [];
            // positionHistory.value = [];

            // (기존 초기화 로직 유지)
            // Mock: 빈 상태에서 시작
            nodes.value = [];
            edges.value = [];

            // 씬 헤더 노드 자동 생성
            ensureSceneHeaderNode();
            // 마스터 노드 자동 생성
            ensureActiveMasterNode();

            // 엣지 파생
            edges.value = deriveEdges();
        } finally {
            isLoading.value = false;
        }
    }

    function ensureSceneHeaderNode(): void {
        const hasHeader = nodes.value.some(
            (n) => n.data?.type === NodeType.SCENE_HEADER
        );
        if (!hasHeader && sceneId.value) {
            const { width, height } = getDefaultNodeDimensions(NodeType.SCENE_HEADER);
            const headerData: SceneHeaderNodeData = {
                ...createBaseNodeData(generateId(), NodeType.SCENE_HEADER),
                type: NodeType.SCENE_HEADER,
                sceneId: sceneId.value,
                title: '새 씬',
                description: '씬 설명을 입력하세요.',
                sceneOrder: 1,
            };

            const newNode: SceneNode = {
                id: headerData.id,
                type: 'sceneHeader',
                position: { x: 0, y: 0 },
                width,
                height,
                data: headerData,
            };
            nodes.value.push(newNode);
        }
    }

    function ensureActiveMasterNode(): void {
        const hasMaster = nodes.value.some(
            (n) => n.data?.type === NodeType.MASTER_IMAGE
        );
        if (!hasMaster) {
            const headerNode = nodes.value.find(
                (n) => n.data?.type === NodeType.SCENE_HEADER
            );
            if (headerNode) {
                addMasterImageNode(headerNode.id, true);
            }
        }
    }

    // ==========================================================================
    // Actions - Add Nodes
    // ==========================================================================

    function addMasterImageNode(
        parentNodeId: string,
        isActive: boolean = false
    ): SceneNode | null {
        // 마스터 최대 3개 제한
        const masterCount = nodes.value.filter(
            (n) => n.data?.type === NodeType.MASTER_IMAGE
        ).length;
        if (masterCount >= 3) return null;

        const id = generateId();
        const data: MasterImageNodeData = {
            ...createBaseNodeData(id, NodeType.MASTER_IMAGE, parentNodeId),
            type: NodeType.MASTER_IMAGE,
            sceneId: sceneId.value || '',
            isActive,
            imageUrl: null,
            thumbnailUrl: null,
            prompt: '',
            style: '',
            timeOfDay: '',
            mood: '',
            objectIds: [],
        };

        const newNode: SceneNode = {
            id,
            type: 'masterImage',
            position: { x: 0, y: 0 },
            ...getDefaultNodeDimensions(NodeType.MASTER_IMAGE),
            data,
        };

        nodes.value.push(newNode);
        addEdge(parentNodeId, id);
        return newNode;
    }

    function addStoryboardGridNode(parentNodeId: string): SceneNode | null {
        if (!canConnect(parentNodeId, NodeType.STORYBOARD_GRID)) return null;

        const id = generateId();
        const data: StoryboardGridNodeData = {
            ...createBaseNodeData(id, NodeType.STORYBOARD_GRID, parentNodeId),
            type: NodeType.STORYBOARD_GRID,
            imageUrl: null,
            thumbnailUrl: null,
            prompt: '',
            layout: '2x3',
            shotTypes: [],
            compositionHint: '',
        };

        const newNode: SceneNode = {
            id,
            type: 'storyboardGrid',
            position: { x: 0, y: 0 },
            ...getDefaultNodeDimensions(NodeType.STORYBOARD_GRID),
            data,
        };

        nodes.value.push(newNode);
        addEdge(parentNodeId, id);
        return newNode;
    }

    function addShotNode(parentNodeId: string, gridCellIndex?: number): SceneNode | null {
        if (!canConnect(parentNodeId, NodeType.SHOT)) return null;

        const nextGridIndex =
            gridCellIndex ??
            nodes.value.filter(
                (n) =>
                    n.data?.type === NodeType.SHOT &&
                    n.data.parentNodeId === parentNodeId
            ).length;

        const id = generateId();
        const data: ShotNodeData = {
            ...createBaseNodeData(id, NodeType.SHOT, parentNodeId),
            type: NodeType.SHOT,
            imageUrl: null,
            thumbnailUrl: null,
            prompt: '',
            gridCellIndex: nextGridIndex,
            shotType: '',
            expression: '',
            additionalDetail: '',
        };

        const newNode: SceneNode = {
            id,
            type: 'shot',
            position: { x: 0, y: 0 },
            ...getDefaultNodeDimensions(NodeType.SHOT),
            data,
        };

        nodes.value.push(newNode);
        addEdge(parentNodeId, id);
        return newNode;
    }

    function addVideoNode(parentNodeId: string): SceneNode | null {
        if (!canConnect(parentNodeId, NodeType.VIDEO)) return null;

        const id = generateId();
        const data: VideoNodeData = {
            ...createBaseNodeData(id, NodeType.VIDEO, parentNodeId),
            type: NodeType.VIDEO,
            startShotId: parentNodeId,
            endShotId: null,
            videoUrl: null,
            thumbnailUrl: null,
            duration: 5,
            isConfirmed: false,
            prompt: '',
            cameraMotion: 'static',
            motionDescription: '',
        };

        const newNode: SceneNode = {
            id,
            type: 'video',
            position: { x: 0, y: 0 },
            ...getDefaultNodeDimensions(NodeType.VIDEO),
            data,
        };

        nodes.value.push(newNode);
        addEdge(parentNodeId, id);
        return newNode;
    }

    // ==========================================================================
    // Actions - Update / Delete
    // ==========================================================================

    function updateNode(nodeId: string, updates: Partial<AnyNodeData>): void {
        const nodeIndex = nodes.value.findIndex((n) => n.id === nodeId);
        const targetNode = nodeIndex !== -1 ? nodes.value[nodeIndex] : undefined;
        if (targetNode && targetNode.data) {
            targetNode.data = {
                ...targetNode.data,
                ...updates,
                updatedAt: new Date().toISOString(),
            } as AnyNodeData;
        }
    }

    function deleteNode(nodeId: string): void {
        const targetNode = nodes.value.find((n) => n.id === nodeId);
        if (
            !targetNode ||
            targetNode.data?.type === NodeType.SCENE_HEADER ||
            targetNode.data?.type === NodeType.MASTER_IMAGE
        )
            return;

        // 하위 노드 재귀 삭제
        const descendants = getDescendantIds(nodeId);
        const toDelete = [nodeId, ...descendants];

        nodes.value = nodes.value.filter((n) => !toDelete.includes(n.id));
        edges.value = edges.value.filter(
            (e) => !toDelete.includes(e.source) && !toDelete.includes(e.target)
        );

        // 선택 해제
        if (selectedNodeId.value && toDelete.includes(selectedNodeId.value)) {
            selectedNodeId.value = null;
        }
    }

    function getDescendantIds(nodeId: string): string[] {
        const directChildren = edges.value
            .filter((e) => e.source === nodeId)
            .map((e) => e.target);

        return directChildren.flatMap((childId) => [
            childId,
            ...getDescendantIds(childId),
        ]);
    }

    // ==========================================================================
    // Actions - Position History
    // ==========================================================================

    function pushPositionSnapshot(): void {
        if (!nodes.value.length) return;
        const snapshot = buildPositionSnapshot(nodes.value);
        const lastSnapshot =
            positionHistory.value[positionHistory.value.length - 1];
        if (lastSnapshot && snapshotsEqual(lastSnapshot, snapshot)) return;

        positionHistory.value.push(snapshot);
        if (positionHistory.value.length > MAX_POSITION_HISTORY) {
            positionHistory.value.shift();
        }
    }

    function undoLastMove(): void {
        const snapshot = positionHistory.value.pop();
        if (!snapshot) return;

        snapshot.forEach((item) => {
            const node = nodes.value.find((n) => n.id === item.id);
            if (node) {
                node.position = { ...item.position };
                if (item.dimensions) {
                    node.style = {
                        ...node.style,
                        width: item.dimensions.width,
                        height: item.dimensions.height,
                    };
                }
            }
        });
    }

    // ==========================================================================
    // Actions - Edges
    // ==========================================================================

    function getEdgeMeta(sourceId: string, targetId: string): EdgeMeta {
        const targetNode = nodes.value.find((n) => n.id === targetId);
        const isTransition =
            targetNode?.data?.type === NodeType.VIDEO &&
            (targetNode.data as VideoNodeData).endShotId === sourceId;
        const isConfirmed =
            targetNode?.data?.type === NodeType.VIDEO &&
            (targetNode.data as VideoNodeData).isConfirmed;

        return { isTransition, isConfirmed };
    }

    function getEdgeClass(meta: EdgeMeta): string | undefined {
        const classes: string[] = [];
        if (meta.isTransition) classes.push('transition');
        if (meta.isConfirmed) classes.push('confirmed');
        return classes.length ? classes.join(' ') : undefined;
    }

    function buildEdge(
        sourceId: string,
        targetId: string,
        options: { sourceHandle?: string; targetHandle?: string } = {}
    ): Edge {
        const edgeId = `edge-${sourceId}-${targetId}`;
        const meta = getEdgeMeta(sourceId, targetId);
        return {
            id: edgeId,
            source: sourceId,
            target: targetId,
            type: 'smoothstep',
            sourceHandle: options.sourceHandle,
            targetHandle: options.targetHandle,
            data: { isTransition: meta.isTransition, isConfirmed: meta.isConfirmed },
            class: getEdgeClass(meta),
        };
    }

    function addEdge(
        sourceId: string,
        targetId: string,
        options: { sourceHandle?: string; targetHandle?: string } = {}
    ): void {
        const edgeId = `edge-${sourceId}-${targetId}`;
        const nextEdge = buildEdge(sourceId, targetId, options);
        const existingIndex = edges.value.findIndex((e) => e.id === edgeId);
        if (existingIndex >= 0) {
            edges.value[existingIndex] = { ...edges.value[existingIndex], ...nextEdge };
            return;
        }
        edges.value.push(nextEdge);
    }

    function deriveEdges(): Edge[] {
        const edgeMap = new Map<string, Edge>();

        nodes.value
            .filter((n) => n.data?.parentNodeId)
            .forEach((n) => {
                const edge = buildEdge(n.data!.parentNodeId as string, n.id);
                edgeMap.set(edge.id, edge);
            });

        nodes.value
            .filter((n) => n.data?.type === NodeType.VIDEO)
            .forEach((n) => {
                const videoData = n.data as VideoNodeData;
                if (!videoData.endShotId) return;
                const edge = buildEdge(videoData.endShotId, n.id, {
                    targetHandle: 'end-shot',
                });
                edgeMap.set(edge.id, edge);
            });

        return Array.from(edgeMap.values());
    }

    function canConnect(sourceId: string, targetType: NodeType): boolean {
        const sourceNode = nodes.value.find((n) => n.id === sourceId);
        if (!sourceNode || !sourceNode.data) return false;
        return VALID_CONNECTIONS[sourceNode.data.type]?.includes(targetType) ?? false;
    }

    // ==========================================================================
    // Actions - Selection
    // ==========================================================================

    function selectNode(nodeId: string | null): void {
        selectedNodeId.value = nodeId;
    }

    // ==========================================================================
    // Actions - Active Master
    // ==========================================================================

    function setActiveMaster(masterId: string): void {
        nodes.value.forEach((n) => {
            if (n.data?.type === NodeType.MASTER_IMAGE) {
                (n.data as MasterImageNodeData).isActive = n.id === masterId;
            }
        });
    }

    // ==========================================================================
    // Actions - Video Confirm
    // ==========================================================================

    function toggleVideoConfirm(videoId: string): void {
        const node = nodes.value.find((n) => n.id === videoId);
        if (
            node?.data &&
            node.data.type === NodeType.VIDEO &&
            node.data.jobStatus === JobStatus.SUCCEEDED
        ) {
            const parentShotId = node.data.parentNodeId;
            // 같은 부모 샷의 다른 영상들 확정 해제
            nodes.value.forEach((n) => {
                if (
                    n.data?.type === NodeType.VIDEO &&
                    n.data.parentNodeId === parentShotId
                ) {
                    (n.data as VideoNodeData).isConfirmed =
                        n.id === videoId ? !(n.data as VideoNodeData).isConfirmed : false;
                }
            });
        }
        syncEdgeMeta();
    }

    // ==========================================================================
    // Actions - Collapse
    // ==========================================================================

    function toggleCollapse(nodeId: string): void {
        const node = nodes.value.find((n) => n.id === nodeId);
        if (node?.data) {
            node.data.isCollapsed = !node.data.isCollapsed;
            if (node.data.isCollapsed) {
                node.data.childCount = getDescendantIds(nodeId).length;
            }
            syncHiddenByCollapse();
        }
    }

    function syncHiddenByCollapse(): void {
        nodes.value.forEach((n) => {
            n.hidden = hasCollapsedAncestor(n.id);
        });
    }

    function hasCollapsedAncestor(nodeId: string): boolean {
        const node = nodes.value.find((n) => n.id === nodeId);
        const parentId = node?.data?.parentNodeId;
        if (!parentId) return false;
        const parent = nodes.value.find((n) => n.id === parentId);
        if (!parent?.data) return false;
        return parent.data.isCollapsed || hasCollapsedAncestor(parentId);
    }

    // ==========================================================================
    // Actions - End Shot Selection
    // ==========================================================================

    function startSelectEndShot(videoId: string): void {
        selectionMode.value = 'selectEndShot';
        endShotTargetVideoId.value = videoId;
    }

    function setEndShot(shotId: string): void {
        if (selectionMode.value !== 'selectEndShot' || !endShotTargetVideoId.value)
            return;

        const videoNode = nodes.value.find(
            (n) => n.id === endShotTargetVideoId.value
        );
        if (videoNode?.data?.type === NodeType.VIDEO) {
            const videoData = videoNode.data as VideoNodeData;
            // 기존 end shot 엣지 제거
            if (videoData.endShotId) {
                edges.value = edges.value.filter(
                    (e) =>
                        !(
                            e.source === videoData.endShotId &&
                            e.target === endShotTargetVideoId.value
                        )
                );
            }
            // 새 end shot 설정
            videoData.endShotId = shotId;
            addEdge(shotId, endShotTargetVideoId.value, { targetHandle: 'end-shot' });
        }

        selectionMode.value = 'none';
        endShotTargetVideoId.value = null;
        syncEdgeMeta();
    }

    function cancelSelectEndShot(): void {
        selectionMode.value = 'none';
        endShotTargetVideoId.value = null;
    }

    function clearEndShot(videoId: string): void {
        const videoNode = nodes.value.find((n) => n.id === videoId);
        if (videoNode?.data?.type !== NodeType.VIDEO) return;

        const videoData = videoNode.data as VideoNodeData;
        if (!videoData.endShotId) return;

        edges.value = edges.value.filter(
            (e) => !(e.source === videoData.endShotId && e.target === videoId)
        );
        videoData.endShotId = null;
        syncEdgeMeta();
    }

    function syncEdgeMeta(): void {
        edges.value = edges.value.map((edge) => {
            const meta = getEdgeMeta(edge.source, edge.target);
            const next: Edge = {
                ...edge,
                data: { ...(edge.data || {}), ...meta },
                class: getEdgeClass(meta),
            };

            if (meta.isTransition) {
                next.targetHandle = 'end-shot';
            } else if (next.targetHandle === 'end-shot') {
                delete (next as { targetHandle?: string }).targetHandle;
            }

            return next;
        });
    }

    // ==========================================================================
    // Actions - Mock Data
    // ==========================================================================

    /**
     * Mock 데이터 로드 (개발/테스트용)
     * @param sceneIdParam Scene ID
     * @param simple true면 간단한 구조, false면 전체 워크플로우
     */
    function loadMockSceneNodes(sceneIdParam: string, simple = false): void {
        isLoading.value = true;
        try {
            sceneId.value = sceneIdParam;
            const mockData = simple
                ? generateSimpleMockNodes(sceneIdParam)
                : generateMockSceneNodes(sceneIdParam);
            nodes.value = mockData.nodes;
            edges.value = mockData.edges;
            positionHistory.value = [];
        } finally {
            isLoading.value = false;
        }
    }

    // ==========================================================================
    // Actions - Clear
    // ==========================================================================

    function clearNodes(): void {
        nodes.value = [];
        edges.value = [];
        positionHistory.value = [];
        selectedNodeId.value = null;
        sceneId.value = null;
    }

    // ==========================================================================
    // Return
    // ==========================================================================

    return {
        // State
        nodes,
        edges,
        selectedNodeId,
        selectionMode,
        endShotTargetVideoId,
        sceneId,
        isLoading,
        isSaving,

        // Getters
        selectedNode,
        nodesByType,
        activeMaster,
        confirmedVideos,
        childNodes,

        // Actions - Load
        loadSceneNodes,
        loadMockSceneNodes,

        // Actions - Add
        addMasterImageNode,
        addStoryboardGridNode,
        addShotNode,
        addVideoNode,

        // Actions - Update/Delete
        updateNode,
        deleteNode,
        pushPositionSnapshot,
        undoLastMove,

        // Actions - Edges
        addEdge,
        deriveEdges,
        canConnect,

        // Actions - Selection
        selectNode,

        // Actions - Master
        setActiveMaster,

        // Actions - Video
        toggleVideoConfirm,

        // Actions - Collapse
        toggleCollapse,

        // Actions - End Shot
        startSelectEndShot,
        setEndShot,
        cancelSelectEndShot,
        clearEndShot,

        // Actions - Clear
        clearNodes,
    };
});
