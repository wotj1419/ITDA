import type { SceneNode, NodePositionSnapshot } from './types';

export function buildPositionSnapshot(nodes: SceneNode[]): NodePositionSnapshot {
  return nodes.map((node) => {
    const style = typeof node.style === 'object' && node.style !== null ? node.style : {};
    return {
      id: node.id,
      position: { x: node.position.x, y: node.position.y },
      dimensions: {
        width: ('width' in style ? style.width : '') ?? '',
        height: ('height' in style ? style.height : '') ?? '',
      },
    };
  });
}

export function snapshotsEqual(a: NodePositionSnapshot, b: NodePositionSnapshot): boolean {
  if (a.length !== b.length) return false;
  const positions = new Map(a.map((item) => [item.id, item]));
  return b.every((item) => {
    const existing = positions.get(item.id);
    return (
      existing !== undefined &&
      existing.position.x === item.position.x &&
      existing.position.y === item.position.y &&
      existing.dimensions?.width === item.dimensions?.width &&
      existing.dimensions?.height === item.dimensions?.height
    );
  });
}
