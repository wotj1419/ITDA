export const nodeCursorAnchors = {
  master: { top: '21%', left: '33%' },
  grid: { top: '21%', left: '69%' },
  shot: { top: '60%', left: '69%' },
  video: { top: '60%', left: '33%' },
} as const

export type NodeCursorAnchors = typeof nodeCursorAnchors
export type NodeCursorAnchorKey = keyof NodeCursorAnchors

export interface NodeCollabCursor {
  id: string
  name: string
  color: string
  start: NodeCursorAnchorKey
}

const nodeCursorAnchorKeys = Object.keys(nodeCursorAnchors) as NodeCursorAnchorKey[]

export const nodeCollabCursors: NodeCollabCursor[] = [
  { id: 'cursor-jin', name: '지민', color: '#ff6b9b', start: 'master' },
  { id: 'cursor-min', name: '민수', color: '#60a5fa', start: 'grid' },
  { id: 'cursor-yeon', name: '서연', color: '#34d399', start: 'shot' },
]

export const getNodeCursorStyle = (cursor: NodeCollabCursor) => {
  const startAnchor = nodeCursorAnchors[cursor.start]
  return {
    top: startAnchor.top,
    left: startAnchor.left,
    '--cursor-color': cursor.color,
  } as Record<string, string>
}

export const pickNextNodeCursorAnchorKey = (currentKey: NodeCursorAnchorKey): NodeCursorAnchorKey => {
  const currentAnchor = nodeCursorAnchors[currentKey]

  const diagonalCandidates = nodeCursorAnchorKeys.filter((key) => {
    if (key === currentKey) return false
    const anchor = nodeCursorAnchors[key]
    return anchor.top !== currentAnchor.top && anchor.left !== currentAnchor.left
  })

  const straightCandidates = nodeCursorAnchorKeys.filter((key) => {
    if (key === currentKey) return false
    const anchor = nodeCursorAnchors[key]
    return anchor.top === currentAnchor.top || anchor.left === currentAnchor.left
  })

  const useDiagonal = diagonalCandidates.length > 0 && Math.random() < 0.56
  const candidatePool = useDiagonal ? diagonalCandidates : [...straightCandidates, ...diagonalCandidates]
  if (!candidatePool.length) return currentKey

  const nextIndex = Math.floor(Math.random() * candidatePool.length)
  return candidatePool[nextIndex] ?? currentKey
}
