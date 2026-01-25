# 노드 생성

### 1. MASTER 생성
`POST /api/scenes/{sceneId}/nodes`
```json
{
  "nodeType": "MASTER",
  "parentNodeId": null,
  "prompt": "화성 기지의 아침 식사 풍경",
  "settings": {}
}
```

### 2. GRID 생성 (parent = MASTER 노드 id)
`POST /api/scenes/{sceneId}/nodes`
```json
{
  "nodeType": "GRID",
  "parentNodeId": MASTER NodeId,
  "prompt": "아침 식사 장면의 컷 구성",
  "settings": {}
}
```

### 3. SHOT 생성 (parent = GRID 노드 id)
`POST /api/scenes/{sceneId}/nodes`
```json
{
  "nodeType": "SHOT",
  "parentNodeId": GRID NodeId,
  "prompt": "주인공이 식탁에 앉는 장면",
  "settings": {}
}
```

### 4. VIDEO 생성 (parent = SHOT 노드 id)
`POST /api/scenes/{sceneId}/nodes`
```json
{
  "nodeType": "VIDEO",
  "parentNodeId": SHOT NodeId,
  "prompt": "짧은 영상 클립",
  "settings": {
    "startShotNodeId": Start SHOT NodeId,
    "endShotNodeId": End SHOT NodeId
  }
}
```