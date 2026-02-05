/**
 * Node Types for Vue Flow based Scene Editor
 * @module types/ui/sceneNodes
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3
 */

// =============================================================================
// Constants (using const instead of enum for erasableSyntaxOnly compatibility)
// =============================================================================

/** 노드 타입 */
export const NodeType = {
    SCENE_HEADER: 'sceneHeader',
    MASTER_IMAGE: 'masterImage',
    STORYBOARD_GRID: 'storyboardGrid',
    SHOT: 'shot',
    VIDEO: 'video',
} as const;

export type NodeType = (typeof NodeType)[keyof typeof NodeType];

/** PRD 기준 상태값 (Job Status) */
export const JobStatus = {
    PENDING: 'pending',
    RUNNING: 'running',
    SUCCEEDED: 'succeeded',
    FAILED: 'failed',
} as const;

export type JobStatus = (typeof JobStatus)[keyof typeof JobStatus];

/** UI 전용 생성 상태 (버튼 클릭 시점/실패 표시) */
export const GenerationState = {
    REQUESTED: 'requested',
    FAILED: 'failed',
} as const;

export type GenerationState = (typeof GenerationState)[keyof typeof GenerationState];

/** 프롬프트 승인 상태 (UI/업무 로직용) */
export const PromptStatus = {
    DRAFT: 'draft',
    GENERATED: 'generated',
    APPROVED: 'approved',
} as const;

export type PromptStatus = (typeof PromptStatus)[keyof typeof PromptStatus];

/** 카메라 모션 타입 */
export type CameraMotion =
    | 'lowZoomIn'
    | 'zoomOut'
    | 'panLeftToRight'
    | 'tiltUp'
    | 'staticCamera';

/** 영상 비율 타입 */
export type AspectRatio = '16:9' | '9:16';

/** 그리드 레이아웃 타입 */
export type GridLayout = '2x2' | '2x3' | '3x3';

/** 그리드 모드 타입 */
export type GridMode = 'SHOT_VARIATIONS' | 'STORY_BEATS';

// =============================================================================
// Interfaces
// =============================================================================

/** 노드 공통 인터페이스 */
export interface BaseNodeData {
    id: string;
    type: NodeType;
    jobStatus: JobStatus | null;
    generationState: GenerationState | null;
    promptStatus: PromptStatus;
    isPromptGenerating?: boolean;
    isFinalPromptGenerating?: boolean;
    createdAt: string;
    updatedAt: string;

    // Optional media URLs (not all node types have media)
    imageUrl?: string | null;
    thumbnailUrl?: string | null;
    videoUrl?: string | null;

    // 버전 관리
    versionGroupId: string;
    version: number;

    // 트리 구조 (P0 기준 단일 진실)
    parentNodeId: string | null;

    // UI 상태
    isCollapsed: boolean;
    childCount: number;
}

/** 씬 헤더 노드 */
export interface SceneHeaderNodeData extends BaseNodeData {
    type: typeof NodeType.SCENE_HEADER;
    sceneId: string;
    title: string;
    description: string;
    sceneOrder: number;
}

/** 마스터 이미지 노드 */
export interface MasterImageNodeData extends BaseNodeData {
    type: typeof NodeType.MASTER_IMAGE;
    sceneId: string;

    // 마스터 고유
    isActive: boolean;
    imageUrl: string | null;
    thumbnailUrl: string | null;

    // 입력 파라미터
    prompt: string;
    promptKo?: string;
    promptEnFinal?: string;
    promptEnFinalOverride?: string;
    additionalDetail: string;
    filmLook?: string;
    style: string;
    timeOfDay: string;
    mood: string;
    objectIds: number[];  // 등장 오브젝트 IDs (캐릭터 포함)
}

/** 스토리보드 그리드 노드 */
export interface StoryboardGridNodeData extends BaseNodeData {
    type: typeof NodeType.STORYBOARD_GRID;

    imageUrl: string | null;
    thumbnailUrl: string | null;

    // 입력 파라미터
    prompt: string;
    promptKo?: string;
    promptEnFinal?: string;
    promptEnFinalOverride?: string;
    additionalDetail: string;
    layout: GridLayout;
    shotTypes: string[];
    compositionHint: string;
    gridMode?: GridMode;
    beats?: string[];
    continuityRules?: string;
}

/** 샷 노드 */
export interface ShotNodeData extends BaseNodeData {
    type: typeof NodeType.SHOT;

    imageUrl: string | null;
    thumbnailUrl: string | null;

    // 입력 파라미터
    prompt: string;
    promptKo?: string;
    promptEnFinal?: string;
    promptEnFinalOverride?: string;
    gridCellIndex: number;
    shotTypes?: string[];
    shotType: string;
    expression: string;
    additionalDetail: string;
}

/** 영상 노드 */
export interface VideoNodeData extends BaseNodeData {
    type: typeof NodeType.VIDEO;
    startShotId: string;
    endShotId: string | null;

    videoUrl: string | null;
    thumbnailUrl: string | null;
    duration: number;
    aspectRatio: AspectRatio;

    // 확정 상태
    isConfirmed: boolean;

    // 입력 파라미터
    prompt: string;
    promptKo?: string;
    promptEnFinal?: string;
    promptEnFinalOverride?: string;
    cameraMotion: CameraMotion;
    motionDescription: string;
    timelineOrder?: number;
}

/** 모든 노드 데이터 타입 유니온 */
export type AnyNodeData =
    | SceneHeaderNodeData
    | MasterImageNodeData
    | StoryboardGridNodeData
    | ShotNodeData
    | VideoNodeData;

// =============================================================================
// Edge Types
// =============================================================================

/** 커스텀 엣지 데이터 */
export interface CustomEdgeData {
    isTransition: boolean;
    isConfirmed: boolean;
}

// =============================================================================
// Connection Rules
// =============================================================================

/** 연결 가능 관계 */
export const VALID_CONNECTIONS: Record<NodeType, NodeType[]> = {
    [NodeType.SCENE_HEADER]: [NodeType.MASTER_IMAGE],
    [NodeType.MASTER_IMAGE]: [NodeType.STORYBOARD_GRID],
    [NodeType.STORYBOARD_GRID]: [NodeType.SHOT],
    [NodeType.SHOT]: [NodeType.VIDEO],
    [NodeType.VIDEO]: [],
};

// =============================================================================
// UI Constants
// =============================================================================

/** 노드 타입별 너비 */
export const NODE_WIDTHS: Record<NodeType, number> = {
    [NodeType.SCENE_HEADER]: 420,
    [NodeType.MASTER_IMAGE]: 420,
    [NodeType.STORYBOARD_GRID]: 520,
    [NodeType.SHOT]: 420,
    [NodeType.VIDEO]: 360,
};

/** 노드 타입별 높이 */
export const NODE_HEIGHTS: Record<NodeType, number> = {
    [NodeType.SCENE_HEADER]: 160,
    [NodeType.MASTER_IMAGE]: 360,
    [NodeType.STORYBOARD_GRID]: 440,
    [NodeType.SHOT]: 360,
    [NodeType.VIDEO]: 260,
};

/** 노드 타입별 아이콘 */
export const NODE_ICONS: Record<NodeType, string> = {
    [NodeType.SCENE_HEADER]: '📖',
    [NodeType.MASTER_IMAGE]: '🎬',
    [NodeType.STORYBOARD_GRID]: '📐',
    [NodeType.SHOT]: '📷',
    [NodeType.VIDEO]: '🎥',
};
