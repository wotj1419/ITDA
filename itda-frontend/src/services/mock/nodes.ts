/**
 * Mock API for Node data
 * @module services/mock/nodes
 */
import type { Node, NodeStatus, NodeSettings } from '../../types/api/nodes';

// =============================================================================
// Mock Data
// =============================================================================

/**
 * Mock 노드 데이터 - projectId-sceneId 키 형식
 */
const mockNodesData: Record<string, Node[]> = {
    '1-1': [
        {
            nodeId: 1,
            type: 'MASTER',
            parentNodeId: undefined,
            prompt: 'Wide shot of desolate red desert with ancient metallic structure...',
            status: 'SUCCEEDED',
            contentUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=600&auto=format',
            thumbnailUrl: 'https://images.unsplash.com/photo-1614728853975-69c960f723ad?w=200&auto=format',
            position: { x: 0, y: 0 },
            isActive: true,
            title: 'Master Image',
        },
        {
            nodeId: 2,
            type: 'GRID',
            parentNodeId: 1,
            prompt: '2x3 storyboard grid based on Mars base scene...',
            status: 'SUCCEEDED',
            contentUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=600&auto=format',
            thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
            position: { x: 0, y: 200 },
            title: 'Grid (2x3)',
            settings: {
                gridLayout: '2x3',
                gridCells: [
                    { index: 0, imageUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format', selected: true },
                    { index: 1, imageUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&auto=format', selected: false },
                    { index: 2, imageUrl: 'https://images.unsplash.com/photo-1516663713099-37eb6d60c825?w=200&auto=format', selected: false },
                    { index: 3, imageUrl: 'https://images.unsplash.com/photo-1442544213729-6a890436906a?w=200&auto=format', selected: false },
                    { index: 4, imageUrl: 'https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=200&auto=format', selected: false },
                    { index: 5, imageUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&auto=format', selected: false },
                ],
            },
        },
        {
            nodeId: 3,
            type: 'SHOT',
            parentNodeId: 2,
            prompt: 'Cinematic shot of a vast red desert on Mars...',
            status: 'SUCCEEDED',
            contentUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=600&auto=format',
            thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
            position: { x: 0, y: 400 },
            title: 'Shot #1',
        },
        {
            nodeId: 4,
            type: 'VIDEO',
            parentNodeId: 3,
            prompt: 'Slow zoom in on astronaut looking out window...',
            status: 'SUCCEEDED',
            contentUrl: 'https://example.com/video.mp4',
            thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
            position: { x: 0, y: 600 },
            title: 'Video #1-A',
            isConfirmed: true,
            settings: {
                cameraMotion: 'ZOOM_IN',
                duration: 4,
                provider: 'Veo 3.1',
            },
        },
    ],
    '1-2': [
        {
            nodeId: 21,
            type: 'VIDEO',
            parentNodeId: undefined,
            prompt: 'Astronaut walking on Mars surface...',
            status: 'SUCCEEDED',
            contentUrl: 'https://example.com/video2.mp4',
            thumbnailUrl: 'https://images.unsplash.com/photo-1541873676-a18131494184?w=200&auto=format',
            position: { x: 0, y: 0 },
            title: 'Walking Sequence',
            isConfirmed: true,
            settings: { duration: 5 },
        },
    ],
    '1-3': [
        {
            nodeId: 31,
            type: 'VIDEO',
            parentNodeId: undefined,
            prompt: 'Discovering the ancient monolith...',
            status: 'SUCCEEDED',
            contentUrl: 'https://example.com/video3.mp4',
            thumbnailUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&auto=format',
            position: { x: 0, y: 0 },
            title: 'Discovery',
            isConfirmed: true,
            settings: { duration: 8 },
        },
    ],
    '1-4': [
        {
            nodeId: 41,
            type: 'VIDEO',
            parentNodeId: undefined,
            prompt: 'Base countdown sequence...',
            status: 'SUCCEEDED',
            contentUrl: 'https://example.com/video4.mp4',
            thumbnailUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&auto=format',
            position: { x: 0, y: 0 },
            title: 'Countdown',
            isConfirmed: true,
            settings: { duration: 6 },
        },
    ],
};

// =============================================================================
// Helper Functions
// =============================================================================

/**
 * API 응답 지연 시뮬레이션
 */
const delay = (ms: number): Promise<void> =>
    new Promise((resolve) => setTimeout(resolve, ms));

/**
 * projectId-sceneId 키 생성
 */
const createKey = (projectId: number, sceneId: number): string =>
    `${projectId}-${sceneId}`;

// =============================================================================
// API Functions
// =============================================================================

/**
 * 특정 씬의 노드 목록 조회
 */
export async function fetchNodesBySceneId(
    projectId: number,
    sceneId: number
): Promise<Node[]> {
    await delay(300);
    const key = createKey(projectId, sceneId);
    return mockNodesData[key] || [];
}

/**
 * 노드 상태 업데이트
 */
export async function updateNodeStatus(
    projectId: number,
    sceneId: number,
    nodeId: number,
    status: NodeStatus
): Promise<Node | null> {
    await delay(200);
    const key = createKey(projectId, sceneId);
    const nodes = mockNodesData[key];
    if (!nodes) return null;

    const node = nodes.find((n) => n.nodeId === nodeId);
    if (node) {
        node.status = status;
        return node;
    }
    return null;
}

/**
 * 노드 설정 업데이트
 */
export async function updateNodeSettings(
    projectId: number,
    sceneId: number,
    nodeId: number,
    settings: Partial<NodeSettings>
): Promise<Node | null> {
    await delay(200);
    const key = createKey(projectId, sceneId);
    const nodes = mockNodesData[key];
    if (!nodes) return null;

    const node = nodes.find((n) => n.nodeId === nodeId);
    if (node) {
        node.settings = { ...node.settings, ...settings };
        return node;
    }
    return null;
}

/**
 * 비디오 노드를 타임라인에 확정
 */
export async function confirmVideoToTimeline(
    projectId: number,
    sceneId: number,
    nodeId: number
): Promise<boolean> {
    await delay(200);
    const key = createKey(projectId, sceneId);
    const nodes = mockNodesData[key];
    if (!nodes) return false;

    const node = nodes.find((n) => n.nodeId === nodeId && n.type === 'VIDEO');
    if (node) {
        node.isConfirmed = true;
        return true;
    }
    return false;
}

/**
 * 타임라인에서 비디오 노드 확정 취소
 */
export async function unconfirmVideoFromTimeline(
    projectId: number,
    sceneId: number,
    nodeId: number
): Promise<boolean> {
    await delay(200);
    const key = createKey(projectId, sceneId);
    const nodes = mockNodesData[key];
    if (!nodes) return false;

    const node = nodes.find((n) => n.nodeId === nodeId && n.type === 'VIDEO');
    if (node) {
        node.isConfirmed = false;
        return true;
    }
    return false;
}
