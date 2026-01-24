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
import {
    fetchSceneNodes,
    createNode as apiCreateNode,
    updateNode as apiUpdateNode,
    deleteNode as apiDeleteNode,
    confirmNode as apiConfirmNode,
    unconfirmNode as apiUnconfirmNode,
    activateMaster as apiActivateMaster,
    type NodeSummary,
    type ApiNodeType,
} from '../services/api/nodes';
import {
    subscribeProjectEvents,
    type ProjectEventMessage,
    type ProjectEventPayload,
} from '../services/ws/projectEvents';
import { useSceneStore } from './scene';

// =============================================================================
// Helper Functions
// =============================================================================

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

const API_TO_UI_NODE_TYPE: Record<ApiNodeType, NodeType> = {
    SCENE_HEADER: NodeType.SCENE_HEADER,
    MASTER: NodeType.MASTER_IMAGE,
    GRID: NodeType.STORYBOARD_GRID,
    SHOT: NodeType.SHOT,
    VIDEO: NodeType.VIDEO,
};

const UI_TO_API_NODE_TYPE: Record<NodeType, ApiNodeType> = {
    [NodeType.SCENE_HEADER]: 'SCENE_HEADER',
    [NodeType.MASTER_IMAGE]: 'MASTER',
    [NodeType.STORYBOARD_GRID]: 'GRID',
    [NodeType.SHOT]: 'SHOT',
    [NodeType.VIDEO]: 'VIDEO',
};

function toJobStatus(status?: string | null): JobStatus | null {
    if (!status) return null;
    const normalized = status.toUpperCase();
    switch (normalized) {
        case 'PENDING':
            return JobStatus.PENDING;
        case 'RUNNING':
            return JobStatus.RUNNING;
        case 'SUCCEEDED':
            return JobStatus.SUCCEEDED;
        case 'FAILED':
            return JobStatus.FAILED;
        default:
            return null;
    }
}

function toFiniteNumber(value: string | number): number | null {
    const numeric = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(numeric) ? numeric : null;
}

function buildNodeSettings(data: AnyNodeData): Record<string, unknown> {
    switch (data.type) {
        case NodeType.MASTER_IMAGE:
            return {
                style: data.style,
                timeOfDay: data.timeOfDay,
                mood: data.mood,
                objectIds: data.objectIds,
            };
        case NodeType.STORYBOARD_GRID:
            return {
                layout: data.layout,
                shotTypes: data.shotTypes,
                compositionHint: data.compositionHint,
            };
        case NodeType.SHOT:
            return {
                gridCellIndex: data.gridCellIndex,
                shotTypes: data.shotTypes,
                shotType: data.shotType,
                expression: data.expression,
                additionalDetail: data.additionalDetail,
            };
        case NodeType.VIDEO:
            return {
                startShotId: data.startShotId,
                endShotId: data.endShotId,
                cameraMotion: data.cameraMotion,
                motionDescription: data.motionDescription,
                duration: data.duration,
                timelineOrder: data.timelineOrder,
            };
        default:
            return {};
    }
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

function applyNodeResultUrl(node: SceneNode, url: string): void {
    if (!node.data) return;
    if (node.data.type === NodeType.VIDEO) {
        const videoData = node.data as VideoNodeData;
        videoData.videoUrl = url;
        videoData.thumbnailUrl = url;
    } else if (node.data.type === NodeType.MASTER_IMAGE) {
        const masterData = node.data as MasterImageNodeData;
        masterData.imageUrl = url;
        masterData.thumbnailUrl = url;
    } else if (node.data.type === NodeType.STORYBOARD_GRID) {
        const gridData = node.data as StoryboardGridNodeData;
        gridData.imageUrl = url;
        gridData.thumbnailUrl = url;
    } else if (node.data.type === NodeType.SHOT) {
        const shotData = node.data as ShotNodeData;
        shotData.imageUrl = url;
        shotData.thumbnailUrl = url;
    }
}

function createSceneNodeFromApi(
    node: NodeSummary,
    sceneId: string,
    sceneInfo?: { title: string; description: string; order: number }
): SceneNode {
    const uiType = API_TO_UI_NODE_TYPE[node.type];
    const id = String(node.nodeId);
    const fallbackHeaderId = String(-Number(sceneId));
    const resolvedParentId = node.parentNodeId
        ? String(node.parentNodeId)
        : uiType === NodeType.MASTER_IMAGE
            ? fallbackHeaderId
            : null;
    const base = createBaseNodeData(id, uiType, resolvedParentId);
    base.jobStatus = toJobStatus(node.status ?? null);
    base.parentNodeId = resolvedParentId;

    let data: AnyNodeData;
    switch (uiType) {
        case NodeType.SCENE_HEADER:
            data = {
                ...base,
                type: NodeType.SCENE_HEADER,
                sceneId,
                title: node.title || sceneInfo?.title || '새 씬',
                description: node.description || sceneInfo?.description || '',
                sceneOrder: sceneInfo?.order || 1,
            };
            break;
        case NodeType.MASTER_IMAGE:
            data = {
                ...base,
                type: NodeType.MASTER_IMAGE,
                sceneId,
                isActive: !!node.isActive,
                imageUrl: node.contentUrl || null,
                thumbnailUrl: node.contentUrl || null,
                prompt: '',
                style: '',
                timeOfDay: '',
                mood: '',
                objectIds: [],
            };
            break;
        case NodeType.STORYBOARD_GRID:
            data = {
                ...base,
                type: NodeType.STORYBOARD_GRID,
                imageUrl: node.contentUrl || null,
                thumbnailUrl: node.contentUrl || null,
                prompt: '',
                layout: '2x2',
                shotTypes: [],
                compositionHint: '',
            };
            break;
        case NodeType.SHOT:
            data = {
                ...base,
                type: NodeType.SHOT,
                imageUrl: node.contentUrl || null,
                thumbnailUrl: node.contentUrl || null,
                prompt: '',
                gridCellIndex: 0,
                shotTypes: [],
                shotType: '',
                expression: '',
                additionalDetail: '',
            };
            break;
        case NodeType.VIDEO:
        default:
            data = {
                ...base,
                type: NodeType.VIDEO,
                startShotId: base.parentNodeId || '',
                endShotId: null,
                videoUrl: node.contentUrl || null,
                thumbnailUrl: node.contentUrl || null,
                duration: 5,
                isConfirmed: !!node.isConfirmed,
                prompt: '',
                cameraMotion: 'staticCamera',
                motionDescription: '',
                timelineOrder: undefined,
            };
            break;
    }

    const { width, height } = getDefaultNodeDimensions(uiType);
    return {
        id,
        type: uiType,
        position: {
            x: node.position?.x ?? 0,
            y: node.position?.y ?? 0,
        },
        width,
        height,
        data,
    };
}

function buildPositionSnapshot(nodes: SceneNode[]): NodePositionSnapshot {
    return nodes.map((node) => {
        const style = typeof node.style === 'object' && node.style !== null ? node.style : {};
        return {
            id: node.id,
            position: { x: node.position.x, y: node.position.y },
            dimensions: {
                width: ('width' in style ? style.width : '') ?? '',
                height: ('height' in style ? style.height : '') ?? '',
            },
        };
    });
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

    const sceneStore = useSceneStore();

    const nodes = ref<SceneNode[]>([]);
    const edges = ref<Edge[]>([]);

    async function ensureSceneInProgress() {
        if (isLoading.value) return; // 로딩 중에는 상태 변경 안 함

        if (sceneId.value) {
            // We need to find the scene object from the store
            const scene = sceneStore.scenes.find(s => s.sceneId === Number(sceneId.value));
            if (scene && scene.status === 'DRAFT') {
                await sceneStore.updateScene(scene.sceneId, { status: 'IN_PROGRESS' });
            }
        }
    }
    const selectedNodeId = ref<string | null>(null);
    const positionHistory = ref<NodePositionSnapshot[]>([]);

    // end shot 선택 모드 (트랜지션 영상용)
    const selectionMode = ref<'none' | 'selectEndShot'>('none');
    const endShotTargetVideoId = ref<string | null>(null);

    const sceneId = ref<string | null>(null);

    // 로딩 상태
    const isLoading = ref(false);
    const isSaving = ref(false);

    let unsubscribeProjectEvents: (() => void) | null = null;

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

    const confirmedVideos = computed(() => {
        const videos = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.VIDEO &&
                (n.data as VideoNodeData).isConfirmed
        );
        return [...videos].sort((a, b) => {
            const orderA = (a.data as VideoNodeData).timelineOrder ?? Number.MAX_SAFE_INTEGER;
            const orderB = (b.data as VideoNodeData).timelineOrder ?? Number.MAX_SAFE_INTEGER;
            if (orderA !== orderB) return orderA - orderB;
            const createdA = a.data?.createdAt ?? '';
            const createdB = b.data?.createdAt ?? '';
            return createdA.localeCompare(createdB);
        });
    });

    const childNodes = computed(() => (parentId: string) =>
        edges.value
            .filter((e) => e.source === parentId)
            .map((e) => nodes.value.find((n) => n.id === e.target))
            .filter(Boolean) as SceneNode[]
    );

    const handleProjectEvent = (message: ProjectEventMessage) => {
        const payload = message.data as ProjectEventPayload;

        if (!payload?.type || payload.target?.type !== 'NODE') return;

        const nodeId = payload.target.id;
        if (!nodeId) return;

        const targetNode = nodes.value.find((n) => n.id === String(nodeId));
        if (!targetNode?.data) return;

        const nextStatus = payload.status
            ? toJobStatus(payload.status)
            : message.event === 'job.failed'
                ? JobStatus.FAILED
                : JobStatus.SUCCEEDED;

        targetNode.data.jobStatus = nextStatus;

        if (message.event === 'job.done' || payload.status === 'SUCCEEDED') {
            applyNodeResultUrl(targetNode, payload.resultUrl || '');
        }
    };

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

    async function loadSceneNodes(
        sceneIdParam: string,
        sceneInfo?: { title: string; description: string; order: number }
    ): Promise<void> {
        isLoading.value = true;
        try {
            sceneId.value = sceneIdParam;
            nodes.value = [];
            edges.value = [];

            const numericSceneId = toFiniteNumber(sceneIdParam);
            if (numericSceneId === null) return;

            const apiNodes = await fetchSceneNodes(numericSceneId);
            nodes.value = apiNodes.map((node) =>
                createSceneNodeFromApi(node, sceneIdParam, sceneInfo)
            );

            if (!nodes.value.find((n) => n.data?.type === NodeType.SCENE_HEADER)) {
                ensureSceneHeaderNode(sceneInfo);
            }

            await ensureActiveMasterNode();
            edges.value = deriveEdges();
            ensureTimelineOrder();

            const projectId = sceneStore.currentProjectId;
            if (projectId) {
                if (unsubscribeProjectEvents) {
                    unsubscribeProjectEvents();
                }
                unsubscribeProjectEvents = subscribeProjectEvents(projectId, handleProjectEvent);
            }
        } catch (error) {
            console.error('Failed to load scene nodes:', error);
        } finally {
            isLoading.value = false;
        }
    }

    function ensureSceneHeaderNode(
        sceneInfo?: { title: string; description: string; order: number }
    ): void {
        const hasHeader = nodes.value.some(
            (n) => n.data?.type === NodeType.SCENE_HEADER
        );
        if (!hasHeader && sceneId.value) {
            const { width, height } = getDefaultNodeDimensions(NodeType.SCENE_HEADER);
            const headerId = String(-Number(sceneId.value));
            const headerData: SceneHeaderNodeData = {
                ...createBaseNodeData(headerId, NodeType.SCENE_HEADER),
                type: NodeType.SCENE_HEADER,
                sceneId: sceneId.value,
                title: sceneInfo?.title || '새 씬',
                description: sceneInfo?.description || '씬 설명을 입력하세요.',
                sceneOrder: sceneInfo?.order || 1,
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

    async function ensureActiveMasterNode(): Promise<void> {
        const hasMaster = nodes.value.some(
            (n) => n.data?.type === NodeType.MASTER_IMAGE
        );
        if (!hasMaster) {
            const headerNode = nodes.value.find(
                (n) => n.data?.type === NodeType.SCENE_HEADER
            );
            if (headerNode) {
                await addMasterImageNode(headerNode.id, true);
            }
        }
    }

    // ==========================================================================
    // Actions - Add Nodes
    // ==========================================================================

    async function addMasterImageNode(
        parentNodeId: string,
        isActive: boolean = false
    ): Promise<SceneNode | null> {
        // 마스터 최대 3개 제한
        const masterCount = nodes.value.filter(
            (n) => n.data?.type === NodeType.MASTER_IMAGE
        ).length;
        if (masterCount >= 3) return null;
        const nextVersion = masterCount + 1;
        if (!sceneId.value) return null;
        const sceneNumericId = toFiniteNumber(sceneId.value);
        if (sceneNumericId === null) return null;

        let apiNodeId: number;
        try {
            apiNodeId = await apiCreateNode(sceneNumericId, {
                nodeType: UI_TO_API_NODE_TYPE[NodeType.MASTER_IMAGE],
                parentNodeId: null,
            });
            if (isActive) {
                await apiActivateMaster(apiNodeId);
            }
        } catch (error) {
            console.error('Failed to create master node:', error);
            return null;
        }

        const id = String(apiNodeId);
        const data: MasterImageNodeData = {
            ...createBaseNodeData(id, NodeType.MASTER_IMAGE, parentNodeId, nextVersion),
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
        ensureSceneInProgress();
        return newNode;
    }

    async function addStoryboardGridNode(parentNodeId: string): Promise<SceneNode | null> {
        if (!canConnect(parentNodeId, NodeType.STORYBOARD_GRID)) return null;

        const gridCount = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.STORYBOARD_GRID &&
                n.data.parentNodeId === parentNodeId
        ).length;
        const nextVersion = gridCount + 1;

        if (!sceneId.value) return null;
        const sceneNumericId = toFiniteNumber(sceneId.value);
        if (sceneNumericId === null) return null;

        let apiNodeId: number;
        try {
            apiNodeId = await apiCreateNode(sceneNumericId, {
                nodeType: UI_TO_API_NODE_TYPE[NodeType.STORYBOARD_GRID],
                parentNodeId: Number(parentNodeId),
            });
        } catch (error) {
            console.error('Failed to create grid node:', error);
            return null;
        }

        const id = String(apiNodeId);
        const data: StoryboardGridNodeData = {
            ...createBaseNodeData(id, NodeType.STORYBOARD_GRID, parentNodeId, nextVersion),
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
        ensureSceneInProgress();
        return newNode;
    }

    async function addShotNode(parentNodeId: string, gridCellIndex?: number): Promise<SceneNode | null> {
        if (!canConnect(parentNodeId, NodeType.SHOT)) return null;

        const existingShots = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.SHOT &&
                n.data.parentNodeId === parentNodeId
        );
        const nextGridIndex = gridCellIndex ?? existingShots.length;
        const nextVersion = existingShots.length + 1;

        if (!sceneId.value) return null;
        const sceneNumericId = toFiniteNumber(sceneId.value);
        if (sceneNumericId === null) return null;

        let apiNodeId: number;
        try {
            apiNodeId = await apiCreateNode(sceneNumericId, {
                nodeType: UI_TO_API_NODE_TYPE[NodeType.SHOT],
                parentNodeId: Number(parentNodeId),
            });
        } catch (error) {
            console.error('Failed to create shot node:', error);
            return null;
        }

        const id = String(apiNodeId);
        const data: ShotNodeData = {
            ...createBaseNodeData(id, NodeType.SHOT, parentNodeId, nextVersion),
            type: NodeType.SHOT,
            imageUrl: null,
            thumbnailUrl: null,
            prompt: '',
            gridCellIndex: nextGridIndex,
            shotTypes: [],
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
        ensureSceneInProgress();
        return newNode;
    }

    async function addVideoNode(parentNodeId: string): Promise<SceneNode | null> {
        if (!canConnect(parentNodeId, NodeType.VIDEO)) return null;

        const videoCount = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.VIDEO &&
                n.data.parentNodeId === parentNodeId
        ).length;
        const nextVersion = videoCount + 1;

        if (!sceneId.value) return null;
        const sceneNumericId = toFiniteNumber(sceneId.value);
        if (sceneNumericId === null) return null;

        let apiNodeId: number;
        try {
            apiNodeId = await apiCreateNode(sceneNumericId, {
                nodeType: UI_TO_API_NODE_TYPE[NodeType.VIDEO],
                parentNodeId: Number(parentNodeId),
            });
        } catch (error) {
            console.error('Failed to create video node:', error);
            return null;
        }

        const id = String(apiNodeId);
        const data: VideoNodeData = {
            ...createBaseNodeData(id, NodeType.VIDEO, parentNodeId, nextVersion),
            type: NodeType.VIDEO,
            startShotId: parentNodeId,
            endShotId: null,
            videoUrl: null,
            thumbnailUrl: null,
            duration: 5,
            isConfirmed: false,
            prompt: '',
            cameraMotion: 'staticCamera',
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
        ensureSceneInProgress();
        return newNode;
    }

    // ==========================================================================
    // Actions - Update / Delete
    // ==========================================================================

    async function updateNode(nodeId: string, updates: Partial<AnyNodeData>): Promise<void> {
        const nodeIndex = nodes.value.findIndex((n) => n.id === nodeId);
        const targetNode = nodeIndex !== -1 ? nodes.value[nodeIndex] : undefined;
        if (targetNode && targetNode.data) {
            const nextData = {
                ...targetNode.data,
                ...updates,
                updatedAt: new Date().toISOString(),
            } as AnyNodeData;
            targetNode.data = nextData;
            ensureSceneInProgress();

            const numericNodeId = toFiniteNumber(nodeId);
            if (numericNodeId !== null && nextData.type !== NodeType.SCENE_HEADER) {
                try {
                    await apiUpdateNode(numericNodeId, {
                        prompt: 'prompt' in nextData ? nextData.prompt : undefined,
                        settings: buildNodeSettings(nextData),
                    });
                } catch (error) {
                    console.error('Failed to update node:', error);
                }
            }
        }
    }

    async function deleteNode(nodeId: string): Promise<void> {
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
        ensureSceneInProgress();

        const numericNodeId = toFiniteNumber(nodeId);
        if (numericNodeId !== null) {
            try {
                await apiDeleteNode(numericNodeId);
            } catch (error) {
                console.error('Failed to delete node:', error);
            }
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

    function hasDescendants(nodeId: string): boolean {
        return getDescendantIds(nodeId).length > 0;
    }

    function canDeleteNode(nodeId: string): boolean {
        const targetNode = nodes.value.find((n) => n.id === nodeId);
        if (!targetNode?.data) return false;
        return (
            targetNode.data.type !== NodeType.SCENE_HEADER &&
            targetNode.data.type !== NodeType.MASTER_IMAGE
        );
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
        ensureSceneInProgress();
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

    async function setActiveMaster(masterId: string): Promise<void> {
        let shouldSyncCollapse = false;
        nodes.value.forEach((n) => {
            if (n.data?.type === NodeType.MASTER_IMAGE) {
                const masterData = n.data as MasterImageNodeData;
                const isActive = n.id === masterId;
                masterData.isActive = isActive;
                if (isActive && masterData.isCollapsed) {
                    masterData.isCollapsed = false;
                    shouldSyncCollapse = true;
                }
            }
        });
        if (shouldSyncCollapse) {
            syncHiddenByCollapse();
        }
        ensureSceneInProgress();

        const numericId = toFiniteNumber(masterId);
        if (numericId !== null) {
            try {
                await apiActivateMaster(numericId);
            } catch (error) {
                console.error('Failed to activate master:', error);
            }
        }
    }

    // ==========================================================================
    // Actions - Video Confirm
    // ==========================================================================

    async function toggleVideoConfirm(videoId: string): Promise<void> {
        const node = nodes.value.find((n) => n.id === videoId);
        if (
            node?.data &&
            node.data.type === NodeType.VIDEO &&
            node.data.jobStatus === JobStatus.SUCCEEDED
        ) {
            const parentShotId = node.data.parentNodeId;
            const shouldConfirm = !(node.data as VideoNodeData).isConfirmed;
            const nextOrder = shouldConfirm ? getNextTimelineOrder() : undefined;
            // 같은 부모 샷의 다른 영상들 확정 해제
            for (const n of nodes.value) {
                if (
                    n.data?.type === NodeType.VIDEO &&
                    n.data.parentNodeId === parentShotId
                ) {
                    const videoData = n.data as VideoNodeData;
                    if (n.id === videoId) {
                        videoData.isConfirmed = shouldConfirm;
                        videoData.timelineOrder = shouldConfirm ? nextOrder : undefined;
                        videoData.updatedAt = new Date().toISOString();
                        const numericId = toFiniteNumber(videoId);
                        if (numericId !== null) {
                            try {
                                if (shouldConfirm) {
                                    await apiConfirmNode(numericId);
                                } else {
                                    await apiUnconfirmNode(numericId);
                                }
                            } catch (error) {
                                console.error('Failed to update confirm state:', error);
                            }
                        }
                    } else {
                        videoData.isConfirmed = false;
                        videoData.timelineOrder = undefined;
                        videoData.updatedAt = new Date().toISOString();
                        const numericId = toFiniteNumber(n.id);
                        if (numericId !== null) {
                            try {
                                await apiUnconfirmNode(numericId);
                            } catch (error) {
                                console.error('Failed to unconfirm video:', error);
                            }
                        }
                    }
                }
            }
        }
        syncEdgeMeta();
    }

    function getNextTimelineOrder(): number {
        return nodes.value.reduce((max, node) => {
            if (node.data?.type !== NodeType.VIDEO) return max;
            const videoData = node.data as VideoNodeData;
            if (!videoData.isConfirmed) return max;
            return Math.max(max, videoData.timelineOrder ?? 0);
        }, 0) + 1;
    }

    function ensureTimelineOrder(): void {
        const confirmed = nodes.value
            .filter(
                (n) =>
                    n.data?.type === NodeType.VIDEO &&
                    (n.data as VideoNodeData).isConfirmed
            )
            .sort((a, b) => {
                const createdA = a.data?.createdAt ?? '';
                const createdB = b.data?.createdAt ?? '';
                return createdA.localeCompare(createdB);
            });
        let maxOrder = confirmed.reduce((max, node) => {
            const order = (node.data as VideoNodeData).timelineOrder ?? 0;
            return Math.max(max, order);
        }, 0);
        confirmed.forEach((node) => {
            const videoData = node.data as VideoNodeData;
            if (!videoData.timelineOrder) {
                maxOrder += 1;
                videoData.timelineOrder = maxOrder;
            }
        });
    }

    function updateTimelineOrder(orderedIds: string[]): void {
        const orderedSet = new Set(orderedIds);
        let order = 1;

        orderedIds.forEach((id) => {
            const node = nodes.value.find((n) => n.id === id);
            if (node?.data?.type === NodeType.VIDEO) {
                const videoData = node.data as VideoNodeData;
                if (videoData.isConfirmed) {
                    videoData.timelineOrder = order;
                    videoData.updatedAt = new Date().toISOString();
                    order += 1;
                }
            }
        });

        const remaining = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.VIDEO &&
                (n.data as VideoNodeData).isConfirmed &&
                !orderedSet.has(n.id)
        );
        remaining.forEach((node) => {
            const videoData = node.data as VideoNodeData;
            videoData.timelineOrder = order;
            videoData.updatedAt = new Date().toISOString();
            order += 1;
        });
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

    function getMasterAncestor(nodeId: string): SceneNode | null {
        const startNode = nodes.value.find((n) => n.id === nodeId);
        let parentId = startNode?.data?.parentNodeId ?? null;
        const visited = new Set<string>();
        while (parentId) {
            if (visited.has(parentId)) return null;
            visited.add(parentId);
            const parent = nodes.value.find((n) => n.id === parentId);
            if (!parent?.data) return null;
            if (parent.data.type === NodeType.MASTER_IMAGE) {
                return parent;
            }
            parentId = parent.data.parentNodeId;
        }
        return null;
    }

    function isUnderInactiveMaster(nodeId: string): boolean {
        const masterNode = getMasterAncestor(nodeId);
        if (!masterNode?.data || masterNode.data.type !== NodeType.MASTER_IMAGE) {
            return false;
        }
        return !(masterNode.data as MasterImageNodeData).isActive;
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
        ensureSceneInProgress();
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
        if (unsubscribeProjectEvents) {
            unsubscribeProjectEvents();
            unsubscribeProjectEvents = null;
        }
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
        hasDescendants,
        canDeleteNode,
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
        updateTimelineOrder,

        // Actions - Collapse
        toggleCollapse,
        isUnderInactiveMaster,

        // Actions - End Shot
        startSelectEndShot,
        setEndShot,
        cancelSelectEndShot,
        clearEndShot,

        // Actions - Clear
        clearNodes,
    };
});
