import type { Node } from '@vue-flow/core';
import type { AnyNodeData } from '../../types/ui/sceneNodes';

export type SceneNode = Node<AnyNodeData>;

export type NodePositionSnapshot = Array<{
  id: string;
  position: { x: number; y: number };
  dimensions?: { width: number | string; height: number | string };
}>;

export type EdgeMeta = {
  isTransition: boolean;
  isConfirmed: boolean;
};
