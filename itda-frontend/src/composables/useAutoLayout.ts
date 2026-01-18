/**
 * useAutoLayout - Dagre 기반 자동 레이아웃 composable
 * @module composables/useAutoLayout
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 8.1
 */
import dagre from 'dagre';
import type { Node, Edge } from '@vue-flow/core';
import { NODE_WIDTHS, NODE_HEIGHTS, NodeType } from '../types/node';

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
     * 노드 타입별 너비 반환
     */
    function getNodeWidth(type: string): number {
        return NODE_WIDTHS[type as NodeType] || 200;
    }

    /**
     * 노드 타입별 높이 반환
     */
    function getNodeHeight(type: string): number {
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
            const nodeType = (node.data as { type?: string })?.type || node.type || '';
            dagreGraph.setNode(node.id, {
                width: getNodeWidth(nodeType),
                height: getNodeHeight(nodeType),
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
            const nodeType = (node.data as { type?: string })?.type || node.type || '';

            return {
                ...node,
                position: {
                    x: nodeWithPosition.x - getNodeWidth(nodeType) / 2,
                    y: nodeWithPosition.y - getNodeHeight(nodeType) / 2,
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
