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

    function parseNumericSize(value: unknown): number | null {
        if (typeof value === 'number' && Number.isFinite(value)) return value;
        if (typeof value === 'string') {
            const parsed = Number.parseFloat(value);
            if (!Number.isNaN(parsed)) return parsed;
        }
        return null;
    }

    function resolveNodeDimension<T>(
        node: Node<T>,
        dimension: 'width' | 'height'
    ): number {
        const measured = (node as { dimensions?: { width?: number; height?: number } })
            .dimensions?.[dimension];
        if (typeof measured === 'number' && measured > 0) return measured;

        const direct = parseNumericSize(
            (node as { width?: unknown; height?: unknown })[dimension]
        );
        if (typeof direct === 'number' && direct > 0) return direct;

        const styled = parseNumericSize(
            (node as { style?: Record<string, unknown> }).style?.[dimension]
        );
        if (typeof styled === 'number' && styled > 0) return styled;

        const nodeType = (node.data as { type?: string })?.type || node.type || '';
        return dimension === 'width' ? getNodeWidth(nodeType) : getNodeHeight(nodeType);
    }

    function resolveNodeWidth<T>(node: Node<T>): number {
        return resolveNodeDimension(node, 'width');
    }

    function resolveNodeHeight<T>(node: Node<T>): number {
        return resolveNodeDimension(node, 'height');
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
                width: resolveNodeWidth(node),
                height: resolveNodeHeight(node),
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
            const nodeWidth = resolveNodeWidth(node);
            const nodeHeight = resolveNodeHeight(node);

            return {
                ...node,
                position: {
                    x: nodeWithPosition.x - nodeWidth / 2,
                    y: nodeWithPosition.y - nodeHeight / 2,
                },
            };
        });

        // [커스텀 정렬 로직] 부모 기준 중앙 정렬 + 동일 간격 분배
        // - Dagre 결과의 y는 유지하고, 같은 부모를 가진 자식들의 x만 재분배한다.
        // - 이렇게 하면 연결 라인의 기준점(부모 하단/자식 상단)이 중앙에 맞춰진다.
        const nodeMap = new Map(layoutedNodes.map((node) => [node.id, node]));
        const childrenByParent = new Map<string, Node<T>[]>();

        layoutedNodes.forEach((node) => {
            const parentId = (node.data as { parentNodeId?: string | null })?.parentNodeId;
            if (!parentId) return;
            const group = childrenByParent.get(parentId);
            if (group) {
                group.push(node);
            } else {
                childrenByParent.set(parentId, [node]);
            }
        });

        const siblingGap = opts.nodeSep;

        childrenByParent.forEach((children, parentId) => {
            const parentNode = nodeMap.get(parentId);
            if (!parentNode) return;

            const parentWidth = resolveNodeWidth(parentNode);
            const parentCenterX = parentNode.position.x + parentWidth / 2;

            const sortedChildren = [...children].sort(
                (a, b) => a.position.x - b.position.x
            );

            const totalWidth =
                sortedChildren.reduce((sum, child) => sum + resolveNodeWidth(child), 0) +
                siblingGap * Math.max(sortedChildren.length - 1, 0);

            let cursorX = parentCenterX - totalWidth / 2;

            sortedChildren.forEach((child) => {
                const childWidth = resolveNodeWidth(child);
                child.position = {
                    ...child.position,
                    x: cursorX,
                };
                cursorX += childWidth + siblingGap;
            });
        });

        return { nodes: layoutedNodes, edges };
    }

    return {
        getLayoutedElements,
        getNodeWidth,
        getNodeHeight,
    };
}
