import type { Edge } from '@vue-flow/core';
import type { SceneNode, EdgeMeta } from './types';
import type { VideoNodeData } from '../../types/ui/sceneNodes';
import { NodeType, VALID_CONNECTIONS } from '../../types/ui/sceneNodes';

function getEdgeMeta(nodes: SceneNode[], sourceId: string, targetId: string): EdgeMeta {
  const targetNode = nodes.find((n) => n.id === targetId);
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

export function buildEdge(
  nodes: SceneNode[],
  sourceId: string,
  targetId: string,
  options: { sourceHandle?: string; targetHandle?: string } = {}
): Edge {
  const edgeId = `edge-${sourceId}-${targetId}`;
  const meta = getEdgeMeta(nodes, sourceId, targetId);
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

export function deriveEdges(nodes: SceneNode[]): Edge[] {
  const edgeMap = new Map<string, Edge>();

  nodes
    .filter((n) => n.data?.parentNodeId)
    .forEach((n) => {
      const edge = buildEdge(nodes, n.data!.parentNodeId as string, n.id);
      edgeMap.set(edge.id, edge);
    });

  nodes
    .filter((n) => n.data?.type === NodeType.VIDEO)
    .forEach((n) => {
      const videoData = n.data as VideoNodeData;
      if (!videoData.endShotId) return;
      const edge = buildEdge(nodes, videoData.endShotId, n.id, {
        targetHandle: 'end-shot',
      });
      edgeMap.set(edge.id, edge);
    });

  return Array.from(edgeMap.values());
}

export function canConnect(nodes: SceneNode[], sourceId: string, targetType: NodeType): boolean {
  const sourceNode = nodes.find((n) => n.id === sourceId);
  if (!sourceNode || !sourceNode.data) return false;
  return VALID_CONNECTIONS[sourceNode.data.type]?.includes(targetType) ?? false;
}

export function syncEdgeMeta(nodes: SceneNode[], edges: Edge[]): Edge[] {
  return edges.map((edge) => {
    const meta = getEdgeMeta(nodes, edge.source, edge.target);
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
