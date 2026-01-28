import { NODE_HEIGHTS, NODE_WIDTHS, type NodeType } from '../types/ui/sceneNodes';

export const NODE_RESIZER_STYLE = {
  '--node-resizer-color': 'var(--rose-500, #FF85A1)',
} as const;

export function getNodeMinSize(type: NodeType): { minWidth: number; minHeight: number } {
  return {
    minWidth: NODE_WIDTHS[type] ?? 200,
    minHeight: NODE_HEIGHTS[type] ?? 140,
  };
}
