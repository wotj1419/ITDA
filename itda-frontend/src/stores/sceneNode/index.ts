/**
 * Scene Node Store - Vue Flow 노드 기반 씬 에디터 상태 관리
 * @module stores/sceneNode
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 5
 */
import { defineStore } from 'pinia';
import { ref, computed, watch } from 'vue';
import type { Edge } from '@vue-flow/core';
import {
    NodeType,
    JobStatus,
    GenerationState,
    type AnyNodeData,
    type SceneHeaderNodeData,
    type MasterImageNodeData,
    type StoryboardGridNodeData,
    type ShotNodeData,
    type VideoNodeData,
} from '../../types/ui/sceneNodes';
import { generateMockSceneNodes, generateSimpleMockNodes } from '../../services/mock/sceneNodes';
import {
    fetchSceneNodes,
    fetchNodeDetail,
    createNode as apiCreateNode,
    updateNode as apiUpdateNode,
    deleteNode as apiDeleteNode,
    confirmNode as apiConfirmNode,
    unconfirmNode as apiUnconfirmNode,
    activateMaster as apiActivateMaster,
    updateNodePositions as apiUpdateNodePositions,
} from '../../services/api/nodes';
import {
    subscribeProjectEvents,
    type ProjectEventMessage,
    type ProjectEventPayload,
} from '../../services/ws/projectEvents';
import { useSceneStore } from '../scene';
import { useAuthStore } from '../auth';
import { useTimelineStore } from '../timeline';
import type { SceneNode, NodePositionSnapshot } from './types';
import { buildPositionSnapshot, snapshotsEqual } from './history';
import { buildEdge, deriveEdges, canConnect, syncEdgeMeta } from './edges';
import {
    applyNodeResultUrl,
    buildNodeSettings,
    createBaseNodeData,
    createSceneNodeFromApi,
    getDefaultNodeDimensions,
    toFiniteNumber,
    toJobStatus,
    UI_TO_API_NODE_TYPE,
} from './mappers';
import {
    mapShotTypeKeysToLabels,
    normalizeCameraMotionValue,
    resolveExpressionLabel,
    resolveFilmLookLabel,
    resolveMoodLabel,
    resolveShotTypeLabel,
    resolveStyleLabel,
    resolveTimeOfDayLabel,
} from '../../utils/nodeSettings';
import {
    acquireMediaLease,
    releaseMediaLease,
    type MediaUrlLease,
} from '../../services/api/media';
import { resolveApiUrl, isApiResourceUrl } from '../../services/api/urls';
import { SHOT_FALLBACK_THUMBNAIL } from '../../utils/fallbacks';
import {
    DEFAULT_GRID_LAYOUT,
    DEFAULT_GRID_SHOT_TYPES,
    DEFAULT_MASTER_FILM_LOOK,
    DEFAULT_MASTER_MOOD,
    DEFAULT_MASTER_STYLE,
    DEFAULT_MASTER_TIME_OF_DAY,
    DEFAULT_VIDEO_ASPECT_RATIO,
    DEFAULT_VIDEO_CAMERA_MOTION,
    DEFAULT_VIDEO_DURATION,
    normalizeAspectRatio,
} from '../../utils/nodeDefaults';

const MAX_POSITION_HISTORY = 20;
const NODE_MEDIA_HYDRATION_CONCURRENCY = 6;
type NodeMediaLeaseField = 'videoUrl' | 'thumbnailUrl' | 'imageUrl';

// =============================================================================
// Store
// =============================================================================

export const useSceneNodeStore = defineStore('sceneNode', () => {
    // ==========================================================================
    // State
    // ==========================================================================

    const sceneStore = useSceneStore();
    const authStore = useAuthStore();
    const timelineStore = useTimelineStore();

    const nodes = ref<SceneNode[]>([]);
    const edges = ref<Edge[]>([]);

    async function ensureSceneInProgress() {
        if (isLoading.value) return; // 로딩 중에는 상태 변경 안 함
        // status 업데이트는 백엔드 지원 전까지 비활성화
    }
    const selectedNodeId = ref<string | null>(null);
    const positionHistory = ref<NodePositionSnapshot[]>([]);
    const hydratedNodeIds = ref<Set<string>>(new Set());
    const hydrationRequests = new Map<string, Promise<void>>();
    const nodeMediaLeaseMap = new Map<string, MediaUrlLease>();

    // end shot 선택 모드 (트랜지션 영상용)
    const selectionMode = ref<'none' | 'selectEndShot'>('none');
    const endShotTargetVideoId = ref<string | null>(null);

    const sceneId = ref<string | null>(null);
    const sceneInfoRef = ref<{ title: string; description: string; order: number } | null>(null);

    // 로딩 상태
    const isLoading = ref(false);
    const isSaving = ref(false);

    let unsubscribeProjectEvents: (() => void) | null = null;
    let nodeSyncTimeout: ReturnType<typeof setTimeout> | null = null;

    const getNodeMediaLeaseKey = (nodeId: string, field: NodeMediaLeaseField) =>
        `${nodeId}:${field}`;

    const releaseNodeMediaLease = (nodeId: string, field: NodeMediaLeaseField): void => {
        const key = getNodeMediaLeaseKey(nodeId, field);
        const lease = nodeMediaLeaseMap.get(key);
        if (!lease) return;
        releaseMediaLease(lease);
        nodeMediaLeaseMap.delete(key);
    };

    const setNodeMediaLease = (
        nodeId: string,
        field: NodeMediaLeaseField,
        lease: MediaUrlLease | null
    ): void => {
        releaseNodeMediaLease(nodeId, field);
        if (!lease || !lease.releasable) return;
        nodeMediaLeaseMap.set(getNodeMediaLeaseKey(nodeId, field), lease);
    };

    const releaseNodeLeases = (nodeId: string): void => {
        releaseNodeMediaLease(nodeId, 'videoUrl');
        releaseNodeMediaLease(nodeId, 'thumbnailUrl');
        releaseNodeMediaLease(nodeId, 'imageUrl');
    };

    const releaseAllNodeMediaLeases = (): void => {
        nodeMediaLeaseMap.forEach((lease) => {
            releaseMediaLease(lease);
        });
        nodeMediaLeaseMap.clear();
    };

    const resolveNodeMediaUrl = async (
        nodeId: string,
        field: NodeMediaLeaseField,
        url?: string | null
    ): Promise<string | null> => {
        releaseNodeMediaLease(nodeId, field);
        const resolvedUrl = resolveApiUrl(url);
        if (!resolvedUrl) return null;
        const lease = await acquireMediaLease(resolvedUrl).catch(() => null);
        if (!lease) return null;
        setNodeMediaLease(nodeId, field, lease.releasable ? lease : null);
        return lease.url;
    };

    const refreshCurrentSceneTimeline = async (): Promise<void> => {
        const projectId = sceneStore.currentProjectId;
        const currentSceneId = sceneId.value ? toFiniteNumber(sceneId.value) : null;
        if (!projectId || currentSceneId === null) return;
        await timelineStore.loadClips(projectId, currentSceneId, {
            hydrateDurations: false,
        });
    };

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

    const videoDurationCache = new Map<string, number>();
    const videoDurationInflight = new Map<string, Promise<number | null>>();

    const readDurationFromUrl = (url: string): Promise<number | null> =>
        new Promise((resolve) => {
            const video = document.createElement('video');
            let settled = false;
            const timeoutId = setTimeout(() => {
                if (settled) return;
                settled = true;
                resolve(null);
            }, 8000);

            const cleanup = () => {
                clearTimeout(timeoutId);
                video.removeAttribute('src');
                video.load();
            };

            video.preload = 'metadata';
            video.muted = true;
            video.playsInline = true;
            video.onloadedmetadata = () => {
                if (settled) return;
                settled = true;
                cleanup();
                const duration = Number.isFinite(video.duration) ? Math.round(video.duration) : null;
                resolve(duration);
            };
            video.onerror = () => {
                if (settled) return;
                settled = true;
                cleanup();
                resolve(null);
            };
            video.src = url;
        });

    const fetchVideoDuration = async (url: string): Promise<number | null> => {
        const cached = videoDurationCache.get(url);
        if (cached) return Promise.resolve(cached);

        const inflight = videoDurationInflight.get(url);
        if (inflight) return inflight;

        const task = (async (): Promise<number | null> => {
            const resolved = resolveApiUrl(url) ?? url;
            if (resolved.startsWith('blob:') || resolved.startsWith('data:')) {
                const duration = await readDurationFromUrl(resolved);
                if (duration && duration > 0) {
                    videoDurationCache.set(url, duration);
                }
                return duration;
            }

            let duration = await readDurationFromUrl(resolved);

            if (!duration && isApiResourceUrl(url)) {
                const lease = await acquireMediaLease(url).catch(() => null);
                if (lease) {
                    duration = await readDurationFromUrl(lease.url);
                    releaseMediaLease(lease);
                }
            }

            if (duration && duration > 0) {
                videoDurationCache.set(url, duration);
                return duration;
            }

            return null;
        })();

        videoDurationInflight.set(url, task);
        try {
            return await task;
        } finally {
            videoDurationInflight.delete(url);
        }
    };

    const updateVideoDurationForNode = async (node: SceneNode): Promise<void> => {
        if (!node.data || node.data.type !== NodeType.VIDEO) return;
        const data = node.data as VideoNodeData;
        if (!data.videoUrl) return;
        const duration = await fetchVideoDuration(data.videoUrl);
        if (duration && duration !== data.duration) {
            data.duration = duration;
            data.updatedAt = new Date().toISOString();
        }
    };

    const hydrateVideoDurations = async (): Promise<void> => {
        const targets = nodes.value.filter(
            (node) => node.data?.type === NodeType.VIDEO && (node.data as VideoNodeData).videoUrl
        );
        await Promise.all(targets.map((node) => updateVideoDurationForNode(node)));
    };

    const isMissingVideoThumbnail = (video: VideoNodeData): boolean =>
        !video.thumbnailUrl || video.thumbnailUrl === SHOT_FALLBACK_THUMBNAIL;

    const canUseShotThumbnail = (video: VideoNodeData): boolean =>
        video.jobStatus === JobStatus.SUCCEEDED &&
        video.generationState !== GenerationState.REQUESTED;

    const getShotThumbnail = (shotNode: SceneNode | undefined): string | null => {
        if (!shotNode?.data || shotNode.data.type !== NodeType.SHOT) return null;
        const shot = shotNode.data as ShotNodeData;
        return shot.thumbnailUrl || shot.imageUrl || null;
    };

    const syncVideoThumbnailsFromShots = (targetShotIds?: Set<string>): void => {
        if (!nodes.value.length) return;
        const nodeLookup = new Map<string, SceneNode>(
            nodes.value.map((node) => [String(node.id), node])
        );

        nodes.value.forEach((node) => {
            if (!node.data || node.data.type !== NodeType.VIDEO) return;
            const video = node.data as VideoNodeData;
            if (!canUseShotThumbnail(video)) return;
            if (!isMissingVideoThumbnail(video)) return;

            const shotId = video.startShotId || video.parentNodeId;
            if (!shotId) return;
            if (targetShotIds && !targetShotIds.has(String(shotId))) return;

            const shotNode = nodeLookup.get(String(shotId));
            const shotThumbnail = getShotThumbnail(shotNode);
            if (!shotThumbnail) return;
            video.thumbnailUrl = shotThumbnail;
        });
    };

    const childNodes = computed(() => (parentId: string) =>
        edges.value
            .filter((e) => e.source === parentId)
            .map((e) => nodes.value.find((n) => n.id === e.target))
            .filter(Boolean) as SceneNode[]
    );

    const scheduleNodeSync = (targetSceneId: number) => {
        if (!sceneId.value) return;
        const currentSceneId = toFiniteNumber(sceneId.value);
        if (currentSceneId === null || currentSceneId !== targetSceneId) return;

        if (nodeSyncTimeout) clearTimeout(nodeSyncTimeout);
        nodeSyncTimeout = setTimeout(() => {
            nodeSyncTimeout = null;
            if (!sceneId.value || isLoading.value) return;
            void loadSceneNodes(sceneId.value, sceneInfoRef.value ?? undefined, { preserveSelection: true });
        }, 200);
    };

    const handleProjectEvent = (message: ProjectEventMessage) => {
        const payload = message.data as ProjectEventPayload;

        if (message.event === 'node.changed') {
            const targetSceneId =
                payload?.sceneId != null ? toFiniteNumber(payload.sceneId) : null;
            if (targetSceneId === null) return;

            const actorId = payload.actorId ?? null;
            const localUserId = authStore.user?.id ?? null;
            if (actorId != null && localUserId != null && actorId === localUserId) return;

            if (payload.action === 'POSITION' && Array.isArray(payload.positions)) {
                payload.positions.forEach((position) => {
                    const nodeId = position?.nodeId != null ? String(position.nodeId) : null;
                    if (!nodeId) return;
                    const x = Number(position.x);
                    const y = Number(position.y);
                    if (!Number.isFinite(x) || !Number.isFinite(y)) return;
                    applyRemoteNodeMove(nodeId, x, y);
                });
                return;
            }

            scheduleNodeSync(targetSceneId);
            return;
        }

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
        if (nextStatus === JobStatus.SUCCEEDED) {
            targetNode.data.generationState = null;
        } else if (nextStatus === JobStatus.FAILED) {
            targetNode.data.generationState = GenerationState.FAILED;
        }

        if (nextStatus === JobStatus.FAILED && targetNode.data.type === NodeType.SHOT) {
            const hasUrl = Boolean(
                (targetNode.data as ShotNodeData).thumbnailUrl ||
                (targetNode.data as ShotNodeData).imageUrl
            );
            if (!hasUrl) {
                const shotData = targetNode.data as ShotNodeData;
                setNodeMediaLease(targetNode.id, 'imageUrl', null);
                shotData.imageUrl = SHOT_FALLBACK_THUMBNAIL;
                shotData.thumbnailUrl = SHOT_FALLBACK_THUMBNAIL;
            }
        }

        if (message.event === 'job.done' || payload.status === 'SUCCEEDED') {
            void (async () => {
                let resolvedUrl: string | null = null;
                let resolvedLease: MediaUrlLease | null = null;
                if (payload.resultUrl) {
                    const lease = await acquireMediaLease(payload.resultUrl).catch(() => null);
                    if (lease) {
                        resolvedUrl = lease.url;
                        resolvedLease = lease.releasable ? lease : null;
                    } else {
                        resolvedUrl = resolveApiUrl(payload.resultUrl);
                    }
                }
                applyNodeResultUrl(targetNode, resolvedUrl ?? '');
                if (targetNode.data?.type === NodeType.VIDEO) {
                    setNodeMediaLease(targetNode.id, 'videoUrl', resolvedLease);
                } else if (
                    targetNode.data?.type === NodeType.MASTER_IMAGE ||
                    targetNode.data?.type === NodeType.STORYBOARD_GRID ||
                    targetNode.data?.type === NodeType.SHOT
                ) {
                    setNodeMediaLease(targetNode.id, 'imageUrl', resolvedLease);
                }
                await updateVideoDurationForNode(targetNode);
                if (targetNode.data?.type === NodeType.SHOT) {
                    syncVideoThumbnailsFromShots(new Set([targetNode.id]));
                } else if (targetNode.data?.type === NodeType.VIDEO) {
                    const videoData = targetNode.data as VideoNodeData;
                    const shotId = videoData.startShotId || videoData.parentNodeId;
                    if (shotId) {
                        syncVideoThumbnailsFromShots(new Set([String(shotId)]));
                    }
                }
            })();
        }
    };

    function reindexNodeVersions(): void {
        const groups = new Map<string, SceneNode[]>();

        for (const node of nodes.value) {
            if (!node.data) continue;
            if (node.data.type === NodeType.SCENE_HEADER) continue;
            const parentId = node.data.parentNodeId ?? '';
            const key = `${node.data.type}::${parentId}`;
            const group = groups.get(key);
            if (group) {
                group.push(node);
            } else {
                groups.set(key, [node]);
            }
        }

        const compareNodeId = (a: SceneNode, b: SceneNode): number => {
            const aNum = toFiniteNumber(a.id);
            const bNum = toFiniteNumber(b.id);
            if (aNum !== null && bNum !== null) return aNum - bNum;
            if (aNum !== null) return -1;
            if (bNum !== null) return 1;
            return String(a.id).localeCompare(String(b.id));
        };

        for (const group of groups.values()) {
            group.sort(compareNodeId);
            group.forEach((node, index) => {
                if (!node.data) return;
                node.data.version = index + 1;
            });
        }
    }

    function getNextVersion(type: NodeType, parentNodeId: string | null): number {
        const siblings = nodes.value.filter(
            (n) => n.data?.type === type && (n.data.parentNodeId ?? null) === parentNodeId
        );
        const maxVersion = siblings.reduce((max, node) => {
            const version = node.data?.version ?? 0;
            return version > max ? version : max;
        }, 0);
        return maxVersion + 1;
    }

    // ==========================================================================
    // Actions - Persistence
    // ==========================================================================

    function getStorageKey(id: string): string {
        return `scene-nodes-${id}`;
    }

    function loadFromLocalStorage(id: string): { nodes: SceneNode[]; edges: Edge[]; updatedAt?: string } | null {
        try {
            const raw = localStorage.getItem(getStorageKey(id));
            if (!raw) return null;
            const parsed = JSON.parse(raw) as { nodes?: SceneNode[]; edges?: Edge[]; updatedAt?: string } | null;
            if (!parsed?.nodes || !parsed?.edges) return null;
            if (!Array.isArray(parsed.nodes) || !Array.isArray(parsed.edges)) return null;
            return { nodes: parsed.nodes, edges: parsed.edges, updatedAt: parsed.updatedAt };
        } catch {
            return null;
        }
    }

    function restoreNodeLayoutFromStorage(loadedNodes: SceneNode[], storedNodes: SceneNode[]): void {
        if (!loadedNodes.length || !storedNodes.length) return;

        const storedById = new Map<string, SceneNode>(storedNodes.map((node) => [String(node.id), node]));

        loadedNodes.forEach((node) => {
            const stored = storedById.get(String(node.id));
            if (!stored) return;

            const hasApiPosition =
                Number.isFinite(node.position?.x) &&
                Number.isFinite(node.position?.y) &&
                (node.position.x !== 0 || node.position.y !== 0);

            if (!hasApiPosition && stored.position) {
                const x = Number((stored.position as { x?: unknown }).x);
                const y = Number((stored.position as { y?: unknown }).y);
                if (Number.isFinite(x) && Number.isFinite(y)) {
                    node.position = { x, y };
                }
            }

            const storedWidth = Number((stored as { width?: unknown }).width);
            const storedHeight = Number((stored as { height?: unknown }).height);
            if (Number.isFinite(storedWidth) && storedWidth > 0) {
                node.width = storedWidth;
            }
            if (Number.isFinite(storedHeight) && storedHeight > 0) {
                node.height = storedHeight;
            }

            const storedStyle = (stored as { style?: unknown }).style;
            if (storedStyle && typeof storedStyle === 'object') {
                const styleObj = storedStyle as Record<string, unknown>;
                const nextStyle = { ...(node.style ?? {}) } as SceneNode['style'];
                if (styleObj.width !== undefined) {
                    (nextStyle as Record<string, unknown>).width = styleObj.width;
                }
                if (styleObj.height !== undefined) {
                    (nextStyle as Record<string, unknown>).height = styleObj.height;
                }
                node.style = nextStyle;
            }
        });
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

    function flushSave(): void {
        if (!sceneId.value) return;
        if (saveTimeout) {
            clearTimeout(saveTimeout);
            saveTimeout = null;
        }
        saveToLocalStorage();
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

    function resetInteractionState(): void {
        selectedNodeId.value = null;
        selectionMode.value = 'none';
        endShotTargetVideoId.value = null;
    }

    function resetHydrationState(): void {
        hydratedNodeIds.value.clear();
        hydrationRequests.clear();
        releaseAllNodeMediaLeases();
    }

    async function loadSceneNodes(
        sceneIdParam: string,
        sceneInfo?: { title: string; description: string; order: number },
        options?: { preserveSelection?: boolean }
    ): Promise<void> {
        const preserveSelection = Boolean(options?.preserveSelection);
        const previousSelectedId = preserveSelection ? selectedNodeId.value : null;
        const previousNodes = nodes.value;
        isLoading.value = true;
        try {
            sceneId.value = sceneIdParam;
            sceneInfoRef.value = sceneInfo ?? null;
            nodes.value = [];
            edges.value = [];
            positionHistory.value = [];
            resetInteractionState();
            resetHydrationState();

            const numericSceneId = toFiniteNumber(sceneIdParam);
            if (numericSceneId === null) return;

            const storedState = loadFromLocalStorage(sceneIdParam);
            const apiNodes = await fetchSceneNodes(numericSceneId);
            const sceneHeaderNode = apiNodes.find((node) => node.type === 'SCENE_HEADER');
            const sceneHeaderId = sceneHeaderNode ? String(sceneHeaderNode.nodeId) : null;
            const nextNodes = apiNodes.map((node) =>
                createSceneNodeFromApi(node, sceneIdParam, sceneInfo, sceneHeaderId)
            );
            if (previousNodes.length) {
                const previousLookup = new Map(
                    previousNodes.map((node) => [String(node.id), node])
                );
                nextNodes.forEach((node) => {
                    if (!node.data || node.data.type !== NodeType.VIDEO) return;
                    const previous = previousLookup.get(String(node.id));
                    if (!previous?.data || previous.data.type !== NodeType.VIDEO) return;
                    const nextData = node.data as VideoNodeData;
                    const prevData = previous.data as VideoNodeData;
                    const nextHasMedia = Boolean(nextData.videoUrl || nextData.thumbnailUrl);
                    const prevHasMedia = Boolean(prevData.videoUrl || prevData.thumbnailUrl);
                    if (!nextHasMedia && prevHasMedia && prevData.jobStatus === JobStatus.SUCCEEDED) {
                        nextData.videoUrl = nextData.videoUrl ?? prevData.videoUrl;
                        nextData.thumbnailUrl = nextData.thumbnailUrl ?? prevData.thumbnailUrl;
                        if (!nextData.jobStatus) {
                            nextData.jobStatus = prevData.jobStatus;
                        }
                    }
                });
            }
            nodes.value = nextNodes;
            syncVideoThumbnailsFromShots();

            if (!nodes.value.find((n) => n.data?.type === NodeType.SCENE_HEADER)) {
                ensureSceneHeaderNode(sceneInfo);
            }

            await ensureActiveMasterNode();
            reindexNodeVersions();
            edges.value = deriveEdges(nodes.value);
            ensureTimelineOrder();

            if (storedState?.nodes?.length) {
                restoreNodeLayoutFromStorage(nodes.value, storedState.nodes);
            }

            const projectId = sceneStore.currentProjectId;
            if (projectId) {
                if (unsubscribeProjectEvents) {
                    unsubscribeProjectEvents();
                }
                unsubscribeProjectEvents = subscribeProjectEvents(projectId, handleProjectEvent);
            }

            if (preserveSelection && previousSelectedId) {
                const stillExists = nodes.value.some((node) => node.id === previousSelectedId);
                if (stillExists) {
                    selectNode(previousSelectedId);
                }
            }

            // Keep first render fast: hydrate media/details in background.
            const loadedSceneId = sceneIdParam;
            void (async () => {
                try {
                    await hydrateMissingShotThumbnails();
                    if (sceneId.value !== loadedSceneId) return;

                    syncVideoThumbnailsFromShots();
                    if (sceneId.value !== loadedSceneId) return;

                    await hydrateNodeMedia(nodes.value);
                    if (sceneId.value !== loadedSceneId) return;

                    await hydrateVideoDurations();
                    if (sceneId.value !== loadedSceneId) return;

                    await hydrateVideoDetailsForEdges();
                    if (sceneId.value !== loadedSceneId) return;

                    edges.value = deriveEdges(nodes.value);
                } catch (error) {
                    console.error('Background node hydration failed:', error);
                }
            })();
        } catch (error) {
            console.error('Failed to load scene nodes:', error);
        } finally {
            isLoading.value = false;
            if (sceneId.value && nodes.value.length) {
                persistNodePositions();
            }
        }
    }

    async function hydrateVideoDetailsForEdges(): Promise<void> {
        const targets = nodes.value.filter((node) => node.data?.type === NodeType.VIDEO);
        if (!targets.length) return;
        await Promise.all(targets.map((node) => hydrateNodeDetail(node.id)));
    }

    async function hydrateMissingShotThumbnails(): Promise<void> {
        const targets = nodes.value.filter((node) => {
            if (!node.data || node.data.type !== NodeType.SHOT) return false;
            const shot = node.data as ShotNodeData;
            const hasThumb = Boolean(shot.thumbnailUrl || shot.imageUrl);
            return !hasThumb && shot.jobStatus === JobStatus.SUCCEEDED;
        });
        if (!targets.length) return;
        await Promise.all(targets.map((node) => hydrateNodeDetail(node.id)));
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
                position: { x: 0, y: -200 },
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

    async function hydrateNodeMedia(nodesToHydrate: SceneNode[]): Promise<void> {
        const targets = nodesToHydrate.filter(
            (node) =>
                node.data?.type !== NodeType.SCENE_HEADER &&
                !!node.data &&
                !!node.data.thumbnailUrl
        );
        await processWithConcurrency(targets, NODE_MEDIA_HYDRATION_CONCURRENCY, async (node) => {
            const current = node.data?.thumbnailUrl || node.data?.imageUrl || node.data?.videoUrl;
            if (!current || current.startsWith('blob:')) return;
            if (!isApiResourceUrl(current)) return;
            const lease = await acquireMediaLease(current).catch(() => null);
            if (!lease || !node.data) {
                if (node.data && isApiResourceUrl(current)) {
                    if (node.data.type === NodeType.VIDEO) {
                        const video = node.data as VideoNodeData;
                        setNodeMediaLease(node.id, 'videoUrl', null);
                        video.videoUrl = null;
                        video.thumbnailUrl = null;
                    } else if (node.data.type === NodeType.MASTER_IMAGE) {
                        const master = node.data as MasterImageNodeData;
                        setNodeMediaLease(node.id, 'imageUrl', null);
                        master.imageUrl = null;
                        master.thumbnailUrl = null;
                    } else if (node.data.type === NodeType.STORYBOARD_GRID) {
                        const grid = node.data as StoryboardGridNodeData;
                        setNodeMediaLease(node.id, 'imageUrl', null);
                        grid.imageUrl = null;
                        grid.thumbnailUrl = null;
                    } else if (node.data.type === NodeType.SHOT) {
                        const shot = node.data as ShotNodeData;
                        setNodeMediaLease(node.id, 'imageUrl', null);
                        shot.imageUrl = null;
                        shot.thumbnailUrl = null;
                    }
                }
                return;
            }
            if (node.data.type === NodeType.VIDEO) {
                setNodeMediaLease(node.id, 'videoUrl', lease.releasable ? lease : null);
                (node.data as VideoNodeData).videoUrl = lease.url;
            } else if (node.data.type === NodeType.MASTER_IMAGE) {
                const master = node.data as MasterImageNodeData;
                setNodeMediaLease(node.id, 'imageUrl', lease.releasable ? lease : null);
                master.imageUrl = lease.url;
                master.thumbnailUrl = lease.url;
            } else if (node.data.type === NodeType.STORYBOARD_GRID) {
                const grid = node.data as StoryboardGridNodeData;
                setNodeMediaLease(node.id, 'imageUrl', lease.releasable ? lease : null);
                grid.imageUrl = lease.url;
                grid.thumbnailUrl = lease.url;
            } else if (node.data.type === NodeType.SHOT) {
                const shot = node.data as ShotNodeData;
                setNodeMediaLease(node.id, 'imageUrl', lease.releasable ? lease : null);
                shot.imageUrl = lease.url;
                shot.thumbnailUrl = lease.url;
            }
        });
    }

    async function processWithConcurrency<T>(
        items: T[],
        concurrency: number,
        worker: (item: T) => Promise<void>
    ): Promise<void> {
        if (!items.length) return;

        const limit = Math.max(1, Math.min(concurrency, items.length));
        let cursor = 0;

        const run = async () => {
            while (cursor < items.length) {
                const index = cursor++;
                await worker(items[index] as T);
            }
        };

        await Promise.all(Array.from({ length: limit }, () => run()));
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
        const nextVersion = getNextVersion(NodeType.MASTER_IMAGE, parentNodeId);
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
            additionalDetail: '',
            filmLook: DEFAULT_MASTER_FILM_LOOK,
            style: DEFAULT_MASTER_STYLE,
            timeOfDay: DEFAULT_MASTER_TIME_OF_DAY,
            mood: DEFAULT_MASTER_MOOD,
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
        reindexNodeVersions();
        ensureSceneInProgress();
        return newNode;
    }

    async function addStoryboardGridNode(parentNodeId: string): Promise<SceneNode | null> {
        if (!canConnect(nodes.value, parentNodeId, NodeType.STORYBOARD_GRID)) return null;

        const nextVersion = getNextVersion(NodeType.STORYBOARD_GRID, parentNodeId);

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
            additionalDetail: '',
            layout: DEFAULT_GRID_LAYOUT,
            shotTypes: [...DEFAULT_GRID_SHOT_TYPES],
            compositionHint: '',
            gridMode: 'SHOT_VARIATIONS',
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
        reindexNodeVersions();
        ensureSceneInProgress();
        return newNode;
    }

    async function addShotNode(parentNodeId: string, gridCellIndex?: number): Promise<SceneNode | null> {
        if (!canConnect(nodes.value, parentNodeId, NodeType.SHOT)) return null;

        const existingShots = nodes.value.filter(
            (n) =>
                n.data?.type === NodeType.SHOT &&
                n.data.parentNodeId === parentNodeId
        );
        const nextGridIndex = gridCellIndex ?? existingShots.length;
        const nextVersion = getNextVersion(NodeType.SHOT, parentNodeId);

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
        reindexNodeVersions();
        ensureSceneInProgress();
        return newNode;
    }

    async function addVideoNode(parentNodeId: string): Promise<SceneNode | null> {
        if (!canConnect(nodes.value, parentNodeId, NodeType.VIDEO)) return null;

        const nextVersion = getNextVersion(NodeType.VIDEO, parentNodeId);

        if (!sceneId.value) return null;
        const sceneNumericId = toFiniteNumber(sceneId.value);
        if (sceneNumericId === null) return null;

        let apiNodeId: number;
        try {
            apiNodeId = await apiCreateNode(sceneNumericId, {
                nodeType: UI_TO_API_NODE_TYPE[NodeType.VIDEO],
                parentNodeId: Number(parentNodeId),
                settings: {
                    startShotNodeId: Number(parentNodeId),
                    endShotNodeId: null,
                },
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
            duration: DEFAULT_VIDEO_DURATION,
            aspectRatio: DEFAULT_VIDEO_ASPECT_RATIO,
            isConfirmed: false,
            prompt: '',
            cameraMotion: DEFAULT_VIDEO_CAMERA_MOTION,
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
        reindexNodeVersions();
        ensureSceneInProgress();
        return newNode;
    }

    // ==========================================================================
    // Actions - Update / Delete
    // ==========================================================================

    function applyNodeUpdates(nodeId: string, updates: Partial<AnyNodeData>): AnyNodeData | null {
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
            return nextData;
        }
        return null;
    }

    function updateNodeLocal(nodeId: string, updates: Partial<AnyNodeData>): void {
        applyNodeUpdates(nodeId, updates);
    }

    async function updateNode(nodeId: string, updates: Partial<AnyNodeData>): Promise<void> {
        const nextData = applyNodeUpdates(nodeId, updates);
        if (!nextData) return;

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

    async function deleteNode(nodeId: string): Promise<void> {
        const targetNode = nodes.value.find((n) => n.id === nodeId);
        if (!targetNode || targetNode.data?.type === NodeType.SCENE_HEADER) return;

        // 하위 노드 재귀 삭제
        const descendants = getDescendantIds(nodeId);
        const toDelete = [nodeId, ...descendants];
        toDelete.forEach((id) => releaseNodeLeases(id));

        nodes.value = nodes.value.filter((n) => !toDelete.includes(n.id));
        edges.value = edges.value.filter(
            (e) => !toDelete.includes(e.source) && !toDelete.includes(e.target)
        );
        reindexNodeVersions();

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

    function getDescendantIds(nodeId: string, visited = new Set<string>()): string[] {
        if (visited.has(nodeId)) return [];
        visited.add(nodeId);
        const directChildren = edges.value
            .filter((e) => e.source === nodeId)
            .map((e) => e.target);

        return directChildren.flatMap((childId) => [
            childId,
            ...getDescendantIds(childId, visited),
        ]);
    }

    function hasDescendants(nodeId: string): boolean {
        return getDescendantIds(nodeId).length > 0;
    }

    function canDeleteNode(nodeId: string): boolean {
        const targetNode = nodes.value.find((n) => n.id === nodeId);
        if (!targetNode?.data) return false;
        return targetNode.data.type !== NodeType.SCENE_HEADER;
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
        persistNodePositions();
    }

    // ==========================================================================
    // Actions - Edges
    // ==========================================================================

    function addEdge(
        sourceId: string,
        targetId: string,
        options: { sourceHandle?: string; targetHandle?: string } = {}
    ): void {
        const edgeId = `edge-${sourceId}-${targetId}`;
        const nextEdge = buildEdge(nodes.value, sourceId, targetId, options);
        const existingIndex = edges.value.findIndex((e) => e.id === edgeId);
        if (existingIndex >= 0) {
            edges.value[existingIndex] = { ...edges.value[existingIndex], ...nextEdge };
            return;
        }
        edges.value.push(nextEdge);
    }

    // ==========================================================================
    // Actions - Persist Positions
    // ==========================================================================

    function applyRemoteNodeMove(nodeId: string, x: number, y: number): void {
        const target = nodes.value.find((node) => node.id === nodeId);
        if (!target) return;
        if (!Number.isFinite(x) || !Number.isFinite(y)) return;
        if (target.position?.x === x && target.position?.y === y) return;
        target.position = { x, y };
    }

    let persistPositionsTimeout: ReturnType<typeof setTimeout> | null = null;
    async function sendNodePositions(): Promise<void> {
        if (!sceneId.value) return;
        const numericSceneId = toFiniteNumber(sceneId.value);
        if (numericSceneId === null) return;

        const positions = nodes.value
            .map((node) => {
                const numericNodeId = toFiniteNumber(node.id);
                if (numericNodeId === null || numericNodeId <= 0) return null;
                return {
                    nodeId: numericNodeId,
                    x: node.position?.x ?? 0,
                    y: node.position?.y ?? 0,
                };
            })
            .filter(Boolean) as Array<{ nodeId: number; x: number; y: number }>;

        if (!positions.length) return;

        try {
            await apiUpdateNodePositions(numericSceneId, positions);
        } catch (error) {
            console.error('Failed to persist node positions:', error);
        }
    }

    function persistNodePositions(): void {
        if (persistPositionsTimeout) clearTimeout(persistPositionsTimeout);
        persistPositionsTimeout = setTimeout(() => {
            void sendNodePositions();
        }, 400);
    }

    function flushPersistNodePositions(): void {
        if (persistPositionsTimeout) {
            clearTimeout(persistPositionsTimeout);
            persistPositionsTimeout = null;
        }
        void sendNodePositions();
    }

    function deriveEdgesSnapshot(): Edge[] {
        return deriveEdges(nodes.value);
    }

    function canConnectNode(sourceId: string, targetType: NodeType): boolean {
        return canConnect(nodes.value, sourceId, targetType);
    }

    // ==========================================================================
    // Actions - Selection
    // ==========================================================================

    function selectNode(nodeId: string | null): void {
        selectedNodeId.value = nodeId;
        if (nodeId) {
            void hydrateNodeDetail(nodeId);
        }
    }

    function applyDetailSettings(targetNode: SceneNode, detail: Awaited<ReturnType<typeof fetchNodeDetail>>): void {
        if (!targetNode.data) return;
        if (targetNode.data.type === NodeType.SCENE_HEADER) return;
        if ('prompt' in targetNode.data && typeof detail.prompt === 'string') {
            const incoming = detail.prompt;
            const current = targetNode.data.prompt ?? '';
            const shouldOverwrite = incoming.trim().length > 0 || current.trim().length === 0;
            if (shouldOverwrite) {
                targetNode.data.prompt = incoming;
            }
        }

        const settings = detail.settings ?? undefined;
        if (!settings) return;

        if ('promptKo' in targetNode.data) {
            const promptKo = settings.promptKo as string | undefined;
            if (typeof promptKo === 'string') {
                const current = targetNode.data.promptKo ?? '';
                const shouldOverwrite = promptKo.trim().length > 0 || current.trim().length === 0;
                if (shouldOverwrite) {
                    targetNode.data.promptKo = promptKo;
                }
            }
            const promptEnFinal = settings.promptEnFinal as string | undefined;
            if (typeof promptEnFinal === 'string') {
                const current = targetNode.data.promptEnFinal ?? '';
                const shouldOverwrite =
                    promptEnFinal.trim().length > 0 || current.trim().length === 0;
                if (shouldOverwrite) {
                    targetNode.data.promptEnFinal = promptEnFinal;
                }
            }
            const promptEnFinalOverride = settings.promptEnFinalOverride as string | undefined;
            if (typeof promptEnFinalOverride === 'string') {
                // Empty string is a valid explicit clear signal for override.
                targetNode.data.promptEnFinalOverride = promptEnFinalOverride;
            }
        }

        if (targetNode.data.type === NodeType.MASTER_IMAGE) {
            const masterData = targetNode.data as MasterImageNodeData;
            const filmLookValue = (settings.filmLookKey ?? settings.filmLook) as string | undefined;
            const styleValue = (settings.styleKey ?? settings.style) as string | undefined;
            const timeValue = (settings.timeOfDayKey ?? settings.timeOfDay) as string | undefined;
            const moodValue = (settings.moodKey ?? settings.mood) as string | undefined;
            const objectIdsRaw = (settings.objectIds ?? settings.objects) as unknown;
            const detailKo = settings.detailKo as string | undefined;

            masterData.filmLook = resolveFilmLookLabel(filmLookValue) || masterData.filmLook || DEFAULT_MASTER_FILM_LOOK;
            masterData.style = resolveStyleLabel(styleValue) || masterData.style;
            masterData.timeOfDay = resolveTimeOfDayLabel(timeValue) || masterData.timeOfDay;
            masterData.mood = resolveMoodLabel(moodValue) || masterData.mood;
            if (Array.isArray(objectIdsRaw)) {
                masterData.objectIds = objectIdsRaw
                    .map((item) => toFiniteNumber(item as string | number))
                    .filter((item): item is number => item !== null);
            }
            if (detailKo !== undefined) {
                masterData.additionalDetail = detailKo ?? '';
            }
            return;
        }

        if (targetNode.data.type === NodeType.STORYBOARD_GRID) {
            const gridData = targetNode.data as StoryboardGridNodeData;
            const layoutValue = settings.layout as string | undefined;
            const shotTypesRaw = settings.shotTypes as string[] | undefined;
            const compositionHint =
                (settings.compositionHintKo ?? settings.compositionHint) as string | undefined;
            const detailKo = settings.detailKo as string | undefined;

            if (layoutValue) gridData.layout = layoutValue as StoryboardGridNodeData['layout'];
            if (shotTypesRaw?.length) {
                gridData.shotTypes = mapShotTypeKeysToLabels(shotTypesRaw);
            }
            if (compositionHint !== undefined) {
                gridData.compositionHint = compositionHint ?? '';
            }
            if (detailKo !== undefined) {
                gridData.additionalDetail = detailKo ?? '';
            }
            return;
        }

        if (targetNode.data.type === NodeType.SHOT) {
            const shotData = targetNode.data as ShotNodeData;
            const gridCellIndex = settings.gridCellIndex as number | undefined;
            const shotTypeValue = settings.shotType as string | undefined;
            const expressionValue = settings.expressionKey as string | undefined;
            const detailKo = settings.detailKo as string | undefined;

            if (typeof gridCellIndex === 'number') {
                shotData.gridCellIndex = gridCellIndex;
            }
            if (shotTypeValue) {
                const resolved = resolveShotTypeLabel(shotTypeValue);
                shotData.shotType = resolved || shotData.shotType;
                if (resolved) {
                    shotData.shotTypes = [resolved];
                }
            }
            if (expressionValue) {
                shotData.expression = resolveExpressionLabel(expressionValue) || shotData.expression;
            }
            if (detailKo !== undefined) {
                shotData.additionalDetail = detailKo ?? '';
            }
            return;
        }

        if (targetNode.data.type === NodeType.VIDEO) {
            const videoData = targetNode.data as VideoNodeData;
            const durationValue = settings.duration as number | undefined;
            const cameraMotionValue = settings.cameraMotionKey as string | undefined;
            const motionDescription =
                (settings.motionDescriptionKo ?? settings.motionDescription) as string | undefined;
            const aspectRatioValue =
                (settings.aspectRatio ?? settings.ratio) as string | undefined;
            const startShotNodeId = settings.startShotNodeId as number | undefined;
            const endShotNodeId = settings.endShotNodeId as number | null | undefined;

            if (typeof durationValue === 'number') {
                videoData.duration = durationValue;
            }
            if (aspectRatioValue) {
                videoData.aspectRatio = normalizeAspectRatio(aspectRatioValue);
            }
            if (cameraMotionValue) {
                videoData.cameraMotion = normalizeCameraMotionValue(cameraMotionValue);
            }
            if (motionDescription !== undefined) {
                videoData.motionDescription = motionDescription ?? '';
            }
            if (typeof startShotNodeId === 'number') {
                videoData.startShotId = String(startShotNodeId);
            }
            if (endShotNodeId !== undefined) {
                videoData.endShotId = endShotNodeId === null ? null : String(endShotNodeId);
            }
        }
    }

    async function hydrateNodeDetail(nodeId: string): Promise<void> {
        const targetNode = nodes.value.find((n) => n.id === nodeId);
        if (!targetNode?.data || targetNode.data.type === NodeType.SCENE_HEADER) return;
        if (hydratedNodeIds.value.has(nodeId)) return;

        const existingRequest = hydrationRequests.get(nodeId);
        if (existingRequest) return existingRequest;

        const request = (async () => {
            const numericId = toFiniteNumber(nodeId);
            if (numericId === null) return;
            try {
                const detail = await fetchNodeDetail(numericId);
                if (!targetNode.data || targetNode.data.type === NodeType.SCENE_HEADER) return;
                applyDetailSettings(targetNode, detail);

                const detailStatus = toJobStatus(detail.status ?? null);
                if (!targetNode.data.jobStatus && detailStatus) {
                    targetNode.data.jobStatus = detailStatus;
                }

                let resolvedDetailUrl: string | null = null;
                const hasApiDetailUrl = isApiResourceUrl(detail.contentUrl);
                let detailLease: MediaUrlLease | null = null;
                if (detail.contentUrl) {
                    const lease = await acquireMediaLease(detail.contentUrl).catch(() => null);
                    if (lease) {
                        resolvedDetailUrl = lease.url;
                        detailLease = lease.releasable ? lease : null;
                    } else {
                        resolvedDetailUrl = resolveApiUrl(detail.contentUrl);
                    }
                }
                if (resolvedDetailUrl && targetNode.data) {
                    if (targetNode.data.type === NodeType.VIDEO) {
                        const videoData = targetNode.data as VideoNodeData;
                        setNodeMediaLease(targetNode.id, 'videoUrl', detailLease);
                        videoData.videoUrl = resolvedDetailUrl;
                    } else if (targetNode.data.type === NodeType.MASTER_IMAGE) {
                        const masterData = targetNode.data as MasterImageNodeData;
                        setNodeMediaLease(targetNode.id, 'imageUrl', detailLease);
                        masterData.imageUrl = resolvedDetailUrl;
                        masterData.thumbnailUrl = resolvedDetailUrl;
                    } else if (targetNode.data.type === NodeType.STORYBOARD_GRID) {
                        const gridData = targetNode.data as StoryboardGridNodeData;
                        setNodeMediaLease(targetNode.id, 'imageUrl', detailLease);
                        gridData.imageUrl = resolvedDetailUrl;
                        gridData.thumbnailUrl = resolvedDetailUrl;
                    } else if (targetNode.data.type === NodeType.SHOT) {
                        const shotData = targetNode.data as ShotNodeData;
                        setNodeMediaLease(targetNode.id, 'imageUrl', detailLease);
                        shotData.imageUrl = resolvedDetailUrl;
                        shotData.thumbnailUrl = resolvedDetailUrl;
                    }
                } else if (hasApiDetailUrl && targetNode.data) {
                    if (targetNode.data.type === NodeType.VIDEO) {
                        const videoData = targetNode.data as VideoNodeData;
                        if (videoData.videoUrl && isApiResourceUrl(videoData.videoUrl)) {
                            setNodeMediaLease(targetNode.id, 'videoUrl', null);
                            videoData.videoUrl = null;
                        }
                        if (videoData.thumbnailUrl && isApiResourceUrl(videoData.thumbnailUrl)) {
                            setNodeMediaLease(targetNode.id, 'thumbnailUrl', null);
                            videoData.thumbnailUrl = null;
                        }
                    } else if (targetNode.data.type === NodeType.MASTER_IMAGE) {
                        const masterData = targetNode.data as MasterImageNodeData;
                        if (masterData.imageUrl && isApiResourceUrl(masterData.imageUrl)) {
                            setNodeMediaLease(targetNode.id, 'imageUrl', null);
                            masterData.imageUrl = null;
                        }
                        if (masterData.thumbnailUrl && isApiResourceUrl(masterData.thumbnailUrl)) {
                            masterData.thumbnailUrl = null;
                        }
                    } else if (targetNode.data.type === NodeType.STORYBOARD_GRID) {
                        const gridData = targetNode.data as StoryboardGridNodeData;
                        if (gridData.imageUrl && isApiResourceUrl(gridData.imageUrl)) {
                            setNodeMediaLease(targetNode.id, 'imageUrl', null);
                            gridData.imageUrl = null;
                        }
                        if (gridData.thumbnailUrl && isApiResourceUrl(gridData.thumbnailUrl)) {
                            gridData.thumbnailUrl = null;
                        }
                    } else if (targetNode.data.type === NodeType.SHOT) {
                        const shotData = targetNode.data as ShotNodeData;
                        if (shotData.imageUrl && isApiResourceUrl(shotData.imageUrl)) {
                            setNodeMediaLease(targetNode.id, 'imageUrl', null);
                            shotData.imageUrl = null;
                        }
                        if (shotData.thumbnailUrl && isApiResourceUrl(shotData.thumbnailUrl)) {
                            shotData.thumbnailUrl = null;
                        }
                    }
                }

                if (targetNode.data?.type === NodeType.SHOT) {
                    syncVideoThumbnailsFromShots(new Set([targetNode.id]));
                } else if (targetNode.data?.type === NodeType.VIDEO) {
                    const videoData = targetNode.data as VideoNodeData;
                    const shotId = videoData.startShotId || videoData.parentNodeId;
                    if (shotId) {
                        syncVideoThumbnailsFromShots(new Set([String(shotId)]));
                    }
                }

                hydratedNodeIds.value.add(nodeId);
            } catch (error) {
                console.error('Failed to hydrate node detail:', error);
            } finally {
                hydrationRequests.delete(nodeId);
            }
        })();

        hydrationRequests.set(nodeId, request);
        return request;
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
        let changed = false;
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
                        changed = true;
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
                        changed = true;
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
        edges.value = syncEdgeMeta(nodes.value, edges.value);
        if (changed) {
            try {
                await refreshCurrentSceneTimeline();
            } catch (error) {
                console.error('Failed to refresh scene timeline after confirm toggle:', error);
            }
        }
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

    function hasCollapsedAncestor(nodeId: string, visited = new Set<string>()): boolean {
        if (visited.has(nodeId)) return false;
        visited.add(nodeId);
        const node = nodes.value.find((n) => n.id === nodeId);
        const parentId = node?.data?.parentNodeId;
        if (!parentId) return false;
        const parent = nodes.value.find((n) => n.id === parentId);
        if (!parent?.data) return false;
        return parent.data.isCollapsed || hasCollapsedAncestor(parentId, visited);
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
            // 새 end shot 설정 (로컬 반영 + 서버 저장)
            void updateNode(endShotTargetVideoId.value, { endShotId: shotId });
            addEdge(shotId, endShotTargetVideoId.value, { targetHandle: 'end-shot' });
        }

        selectionMode.value = 'none';
        endShotTargetVideoId.value = null;
        edges.value = syncEdgeMeta(nodes.value, edges.value);
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
        void updateNode(videoId, { endShotId: null });
        edges.value = syncEdgeMeta(nodes.value, edges.value);
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
            reindexNodeVersions();
            positionHistory.value = [];
            resetInteractionState();
            resetHydrationState();
        } finally {
            isLoading.value = false;
        }
    }

    // ==========================================================================
    // Actions - Clear
    // ==========================================================================

    function clearNodes(): void {
        flushSave();
        flushPersistNodePositions();
        videoDurationCache.clear();
        videoDurationInflight.clear();
        nodes.value = [];
        edges.value = [];
        positionHistory.value = [];
        resetInteractionState();
        sceneId.value = null;
        sceneInfoRef.value = null;
        resetHydrationState();
        if (unsubscribeProjectEvents) {
            unsubscribeProjectEvents();
            unsubscribeProjectEvents = null;
        }
        if (nodeSyncTimeout) {
            clearTimeout(nodeSyncTimeout);
            nodeSyncTimeout = null;
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
        updateNodeLocal,
        updateNode,
        deleteNode,
        hasDescendants,
        canDeleteNode,
        pushPositionSnapshot,
        undoLastMove,

        // Actions - Edges
        addEdge,
        deriveEdges: deriveEdgesSnapshot,
        canConnect: canConnectNode,

        // Actions - Persist
        applyRemoteNodeMove,
        persistNodePositions,
        flushPersistNodePositions,

        // Actions - Selection
        selectNode,

        // Actions - Master
        setActiveMaster,

        // Actions - Video
        toggleVideoConfirm,
        updateTimelineOrder,
        resolveNodeMediaUrl,

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

        // Actions - Persistence
        flushSave,
    };
});
