/**
 * useAutoLayout - Dagre 기반 자동 레이아웃 composable
 * @module composables/useAutoLayout
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 8.1
 */
import dagre from 'dagre';
import type { Node, Edge } from '@vue-flow/core';
import { NODE_WIDTHS, NODE_HEIGHTS, NodeType } from '../types/ui/sceneNodes';

export interface LayoutOptions {
    direction: 'TB' | 'LR';
    nodeSep: number;
    rankSep: number;
    marginX: number;
    marginY: number;
}

const DEFAULT_OPTIONS: LayoutOptions = {
    direction: 'TB',
    nodeSep: 50,
    rankSep: 80,
    marginX: 20,
    marginY: 20,
};

/**
 * Dagre 기반 자동 레이아웃 composable
 */
export function useAutoLayout() {
    /**
     * 노드의 실제 너비 반환 (동적 크기 지원)
     */
    function getNodeWidth(nodeOrType: Node | string): number {
        if (typeof nodeOrType === 'string') {
            return NODE_WIDTHS[nodeOrType as NodeType] || 200;
        }

        const node = nodeOrType as any;

        // 1. dimensions 속성 확인 (Vue Flow 내부 상태)
        if (node.dimensions && node.dimensions.width > 0) {
            return node.dimensions.width;
        }

        // 2. style 속성 확인
        if (node.style && typeof node.style === 'object' && node.style.width) {
            const styleWidth = parseInt(node.style.width as string);
            if (!isNaN(styleWidth)) return styleWidth;
        }

        // 3. width 속성 확인
        if (node.width && typeof node.width === 'number') {
            return node.width;
        }

        // 4. 타입 기반 기본값
        const type = (node.data as { type?: string })?.type || node.type || '';
        return NODE_WIDTHS[type as NodeType] || 200;
    }

    /**
     * 노드의 실제 높이 반환 (동적 크기 지원)
     */
    function getNodeHeight(nodeOrType: Node | string): number {
        if (typeof nodeOrType === 'string') {
            return NODE_HEIGHTS[nodeOrType as NodeType] || 150;
        }

        const node = nodeOrType as any;

        // 1. dimensions 속성 확인 (Vue Flow 내부 상태)
        if (node.dimensions && node.dimensions.height > 0) {
            return node.dimensions.height;
        }

        // 2. style 속성 확인
        if (node.style && typeof node.style === 'object' && node.style.height) {
            const styleHeight = parseInt(node.style.height as string);
            if (!isNaN(styleHeight)) return styleHeight;
        }

        // 3. height 속성 확인
        if (node.height && typeof node.height === 'number') {
            return node.height;
        }

        // 4. 타입 기반 기본값
        const type = (node.data as { type?: string })?.type || node.type || '';
        return NODE_HEIGHTS[type as NodeType] || 150;
    }

    /**
     * Dagre를 사용하여 노드와 엣지에 레이아웃 적용
     */
    function getLayoutedElements<T>(
        nodes: Node<T>[],
        edges: Edge[],
        options: Partial<LayoutOptions> = {}
    ): { nodes: Node<T>[]; edges: Edge[] } {
        const opts = { ...DEFAULT_OPTIONS, ...options };

        const dagreGraph = new dagre.graphlib.Graph();
        dagreGraph.setDefaultEdgeLabel(() => ({}));
        dagreGraph.setGraph({
            rankdir: opts.direction,
            nodesep: opts.nodeSep,
            ranksep: opts.rankSep,
            marginx: opts.marginX,
            marginy: opts.marginY,
        });

        // 노드 추가
        nodes.forEach((node) => {
            dagreGraph.setNode(node.id, {
                width: getNodeWidth(node),
                height: getNodeHeight(node),
            });
        });

        // 엣지 추가
        edges.forEach((edge) => {
            dagreGraph.setEdge(edge.source, edge.target);
        });

        // 레이아웃 계산
        dagre.layout(dagreGraph);

        // 계산된 위치 적용
        const layoutedNodes = nodes.map((node) => {
            const nodeWithPosition = dagreGraph.node(node.id);

            // 중앙 정렬을 위해 실제 너비/높이 사용
            const width = getNodeWidth(node);
            const height = getNodeHeight(node);

            return {
                ...node,
                position: {
                    x: nodeWithPosition.x - width / 2,
                    y: nodeWithPosition.y - height / 2,
                },
            };
        });

        return { nodes: layoutedNodes, edges };
    }

    return {
        getLayoutedElements,
        getNodeWidth,
        getNodeHeight,
    };
}
