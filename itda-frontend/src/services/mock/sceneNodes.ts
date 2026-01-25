/**
 * Mock Scene Nodes Data
 * 
 * 워크플로우 테스트를 위한 Mock 데이터.
 * 다양한 상태(succeeded, running, failed)와 구조(분기, 트랜지션)를 포함.
 * 
 * 구조:
 * 씬헤더 → 마스터1(Active) → 그리드1 → 샷A → 영상A1(확정✓)
 *                          │        └ 샷B → 영상B1, B2(running)
 *                          └ 그리드2(running)
 *       → 마스터2 → 그리드3 → 샷C → 영상C1(failed), 트랜지션(C→A)
 * 
 * @see docs/vue-flow-node-workflow-design.md
 */
import type { Node, Edge } from '@vue-flow/core';
import {
    NodeType,
    JobStatus,
    PromptStatus,
    NODE_HEIGHTS,
    NODE_WIDTHS,
    type SceneHeaderNodeData,
    type MasterImageNodeData,
    type StoryboardGridNodeData,
    type ShotNodeData,
    type VideoNodeData,
    type AnyNodeData,
} from '../../types/ui/sceneNodes';

// =============================================================================
// Helper Functions
// =============================================================================

let nodeIdCounter = 0;

function createNodeId(prefix: string): string {
    return `mock-${prefix}-${++nodeIdCounter}`;
}

function createBaseData(
    id: string,
    type: NodeType,
    parentNodeId: string | null = null,
    jobStatus: JobStatus | null = null,
    version = 1
): Omit<AnyNodeData, 'type'> & { type: NodeType } {
    return {
        id,
        type,
        jobStatus,
        promptStatus: PromptStatus.APPROVED,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
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

// =============================================================================
// Node Data Factory Functions
// =============================================================================

function createSceneHeader(
    id: string,
    sceneId: string,
    title: string,
    description: string,
    sceneOrder = 1
): Node<SceneHeaderNodeData> {
    return {
        id,
        type: 'sceneHeader',
        position: { x: 0, y: 0 },
        ...getDefaultNodeDimensions(NodeType.SCENE_HEADER),
        data: {
            ...createBaseData(id, NodeType.SCENE_HEADER),
            type: NodeType.SCENE_HEADER,
            sceneId,
            title,
            description,
            sceneOrder,
        },
    };
}

function createMasterImage(
    id: string,
    parentId: string,
    sceneId: string,
    isActive: boolean,
    jobStatus: JobStatus | null = JobStatus.SUCCEEDED,
    version = 1
): Node<MasterImageNodeData> {
    return {
        id,
        type: 'masterImage',
        position: { x: 0, y: 0 },
        ...getDefaultNodeDimensions(NodeType.MASTER_IMAGE),
        data: {
            ...createBaseData(id, NodeType.MASTER_IMAGE, parentId, jobStatus, version),
            type: NodeType.MASTER_IMAGE,
            sceneId,
            isActive,
            imageUrl: 'https://picsum.photos/seed/master' + id + '/400/225',
            thumbnailUrl: 'https://picsum.photos/seed/master' + id + '/200/112',
            prompt: '화성 기지 전경, 붉은 사막 위의 돔 구조물',
            style: '실사',
            timeOfDay: '아침',
            mood: '고독',
            objectIds: [],
        },
    };
}

function createStoryboardGrid(
    id: string,
    parentId: string,
    layout: '2x2' | '2x3' | '3x3' = '2x3',
    jobStatus: JobStatus | null = JobStatus.SUCCEEDED,
    version = 1
): Node<StoryboardGridNodeData> {
    return {
        id,
        type: 'storyboardGrid',
        position: { x: 0, y: 0 },
        ...getDefaultNodeDimensions(NodeType.STORYBOARD_GRID),
        data: {
            ...createBaseData(id, NodeType.STORYBOARD_GRID, parentId, jobStatus, version),
            type: NodeType.STORYBOARD_GRID,
            imageUrl: 'https://picsum.photos/seed/grid' + id + '/400/300',
            thumbnailUrl: 'https://picsum.photos/seed/grid' + id + '/200/150',
            prompt: '다양한 앵글의 샷 구성',
            layout,
            shotTypes: ['와이드샷', '클로즈업', '투샷'],
            compositionHint: '',
        },
    };
}

function createShot(
    id: string,
    parentId: string,
    gridCellIndex: number,
    shotType: string,
    jobStatus: JobStatus | null = JobStatus.SUCCEEDED,
    version = 1
): Node<ShotNodeData> {
    return {
        id,
        type: 'shot',
        position: { x: 0, y: 0 },
        ...getDefaultNodeDimensions(NodeType.SHOT),
        data: {
            ...createBaseData(id, NodeType.SHOT, parentId, jobStatus, version),
            type: NodeType.SHOT,
            imageUrl: 'https://picsum.photos/seed/shot' + id + '/300/300',
            thumbnailUrl: 'https://picsum.photos/seed/shot' + id + '/150/150',
            prompt: `${shotType} - 캐릭터 감정 표현`,
            gridCellIndex,
            shotType,
            expression: '고독한 표정',
            additionalDetail: '',
        },
    };
}

function createVideo(
    id: string,
    parentId: string,
    cameraMotion: VideoNodeData['cameraMotion'],
    duration: number,
    jobStatus: JobStatus | null = JobStatus.SUCCEEDED,
    isConfirmed = false,
    endShotId: string | null = null,
    version = 1
): Node<VideoNodeData> {
    return {
        id,
        type: 'video',
        position: { x: 0, y: 0 },
        ...getDefaultNodeDimensions(NodeType.VIDEO),
        data: {
            ...createBaseData(id, NodeType.VIDEO, parentId, jobStatus, version),
            type: NodeType.VIDEO,
            startShotId: parentId,
            endShotId,
            videoUrl: 'https://sample-videos.com/video.mp4',
            thumbnailUrl: 'https://picsum.photos/seed/video' + id + '/200/112',
            duration,
            isConfirmed,
            prompt: `${cameraMotion} 모션으로 ${duration}초 영상 생성`,
            cameraMotion,
            motionDescription: '',
        },
    };
}

// =============================================================================
// Edge Factory
// =============================================================================

function createEdge(
    sourceId: string,
    targetId: string,
    isConfirmed = false,
    isTransition = false,
    targetHandle?: string
): Edge {
    const edge: Edge = {
        id: `edge-${sourceId}-${targetId}`,
        source: sourceId,
        target: targetId,
        type: 'smoothstep',
        data: { isTransition, isConfirmed },
    };

    if (targetHandle) {
        edge.targetHandle = targetHandle;
    }

    if (isConfirmed) {
        edge.class = 'confirmed';
    } else if (isTransition) {
        edge.class = 'transition';
    }

    return edge;
}

// =============================================================================
// Mock Data Generator
// =============================================================================

export interface MockSceneNodesResult {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    nodes: Node<any>[];
    edges: Edge[];
}

/**
 * 다양한 상태와 구조를 포함한 Mock 노드/엣지 데이터 생성
 */
export function generateMockSceneNodes(sceneId: string): MockSceneNodesResult {
    // Reset counter for consistent IDs
    nodeIdCounter = 0;

    // Node IDs
    const headerId = createNodeId('header');
    const master1Id = createNodeId('master');
    const master2Id = createNodeId('master');
    const grid1Id = createNodeId('grid');
    const grid2Id = createNodeId('grid');
    const grid3Id = createNodeId('grid');
    const shotAId = createNodeId('shot');
    const shotBId = createNodeId('shot');
    const shotCId = createNodeId('shot');
    const videoA1Id = createNodeId('video');
    const videoB1Id = createNodeId('video');
    const videoB2Id = createNodeId('video');
    const videoC1Id = createNodeId('video');
    const transitionId = createNodeId('video');

    // Create nodes (type assertion needed due to Vue Flow Node generic constraints)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const nodes: Node<any>[] = [
        // Scene Header
        createSceneHeader(
            headerId,
            sceneId,
            '고립된 아침',
            '화성 기지의 새벽. 주인공 은수가 창밖을 바라보며 지구를 그리워하는 씬. 붉은 사막 위로 태양이 떠오르고, 기지 안은 고요하다.'
        ),

        // Master 1 (Active, Succeeded)
        createMasterImage(master1Id, headerId, sceneId, true, JobStatus.SUCCEEDED, 1),

        // Master 2 (Non-Active, Succeeded)
        createMasterImage(master2Id, headerId, sceneId, false, JobStatus.SUCCEEDED, 2),

        // Grid 1 under Master 1 (Succeeded)
        createStoryboardGrid(grid1Id, master1Id, '2x3', JobStatus.SUCCEEDED, 1),

        // Grid 2 under Master 1 (Running)
        createStoryboardGrid(grid2Id, master1Id, '2x2', JobStatus.RUNNING, 2),

        // Grid 3 under Master 2 (Succeeded)
        createStoryboardGrid(grid3Id, master2Id, '2x3', JobStatus.SUCCEEDED, 1),

        // Shot A under Grid 1 (Succeeded)
        createShot(shotAId, grid1Id, 0, '와이드샷', JobStatus.SUCCEEDED, 1),

        // Shot B under Grid 1 (Succeeded)
        createShot(shotBId, grid1Id, 1, '클로즈업', JobStatus.SUCCEEDED, 1),

        // Shot C under Grid 3 (Succeeded)
        createShot(shotCId, grid3Id, 0, '투샷', JobStatus.SUCCEEDED, 1),

        // Video A1 under Shot A (Confirmed ✓)
        createVideo(videoA1Id, shotAId, 'lowZoomIn', 5, JobStatus.SUCCEEDED, true),

        // Video B1 under Shot B (Succeeded, not confirmed)
        createVideo(videoB1Id, shotBId, 'panLeftToRight', 4, JobStatus.SUCCEEDED, false),

        // Video B2 under Shot B (Running)
        createVideo(videoB2Id, shotBId, 'staticCamera', 3, JobStatus.RUNNING, false, null, 2),

        // Video C1 under Shot C (Failed)
        createVideo(videoC1Id, shotCId, 'tiltUp', 5, JobStatus.FAILED, false),

        // Transition Video (C → A)
        createVideo(transitionId, shotCId, 'panLeftToRight', 3, JobStatus.SUCCEEDED, false, shotAId, 1),
    ];

    // Create edges
    const edges: Edge[] = [
        // Header → Masters
        createEdge(headerId, master1Id),
        createEdge(headerId, master2Id),

        // Master 1 → Grids
        createEdge(master1Id, grid1Id),
        createEdge(master1Id, grid2Id),

        // Master 2 → Grid 3
        createEdge(master2Id, grid3Id),

        // Grid 1 → Shots
        createEdge(grid1Id, shotAId),
        createEdge(grid1Id, shotBId),

        // Grid 3 → Shot C
        createEdge(grid3Id, shotCId),

        // Shot A → Video A1 (confirmed edge)
        createEdge(shotAId, videoA1Id, true),

        // Shot B → Videos
        createEdge(shotBId, videoB1Id),
        createEdge(shotBId, videoB2Id),

        // Shot C → Videos
        createEdge(shotCId, videoC1Id),
        createEdge(shotCId, transitionId),

        // Transition edge (Shot A → Transition Video)
        createEdge(shotAId, transitionId, false, true, 'end-shot'),
    ];

    return { nodes, edges };
}

/**
 * 간단한 워크플로우 Mock 데이터 (기본 흐름만)
 */
export function generateSimpleMockNodes(sceneId: string): MockSceneNodesResult {
    nodeIdCounter = 0;

    const headerId = createNodeId('header');
    const masterId = createNodeId('master');
    const gridId = createNodeId('grid');
    const shotId = createNodeId('shot');
    const videoId = createNodeId('video');

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const nodes: Node<any>[] = [
        createSceneHeader(headerId, sceneId, '테스트 씬', '간단한 테스트용 씬입니다.'),
        createMasterImage(masterId, headerId, sceneId, true, JobStatus.SUCCEEDED),
        createStoryboardGrid(gridId, masterId, '2x2', JobStatus.SUCCEEDED),
        createShot(shotId, gridId, 0, '와이드샷', JobStatus.SUCCEEDED),
        createVideo(videoId, shotId, 'staticCamera', 5, JobStatus.SUCCEEDED, true),
    ];

    const edges: Edge[] = [
        createEdge(headerId, masterId),
        createEdge(masterId, gridId),
        createEdge(gridId, shotId),
        createEdge(shotId, videoId, true),
    ];

    return { nodes, edges };
}
