# 10. Presence / Cursor / Soft Lock 설계(5주차 협업 핵심)

## 1) 왜 "동시 편집(CRDT)"보다 "분업 협업"이 먼저인가
- 지금 워크플로우는 한 장면(Scene)을 여러 노드로 나눠서 작업함
- 팀 협업에서 체감이 큰 것은
  - 누가 어떤 씬/노드에 들어가 있는지(Presence)
  - 누가 무엇을 선택/조정하고 있는지(Cursor/Selection)
  - 같은 대상을 동시에 건드리지 않게 유도(Soft Lock)
- "같은 노드 필드를 두 명이 동시에 타이핑"은 충돌이 잦고, 효용 대비 구현 난이도가 큼

---

## 2) CRDT 없이 가능한 것(우리가 5주차에 하는 것)

### Presence
- 서버: 프로젝트 room에 접속한 사용자 목록 유지
- 이벤트: join/leave, currentRoute, currentSceneId

### Cursor
- FE: 마우스 이동 좌표를 일정 주기로 서버에 송신(예: 20~30fps 이하)
- 서버: 같은 room의 다른 참가자에게 broadcast
- FE: 상대 커서 + 이름 라벨 렌더
- 결론: **Cursor는 CRDT 필요 없음** (단순한 이벤트 스트림)

### Selection
- FE: 노드 클릭/선택 시 `selectedNodeId`를 서버에 송신
- 서버: room broadcast
- FE: 상대방 선택 노드를 하이라이트
- 결론: **Selection도 CRDT 필요 없음**

---

## 3) Soft Lock(중요)
Soft Lock은 "법적/강제"가 아니라 **협업 충돌을 줄이는 UX 규칙**입니다.

### 3.1 노드 단위 Soft Lock
- 누군가 노드를 편집 모드로 열면
  - 서버에 `lock(nodeId)` 이벤트 송신
  - 서버는 room에 broadcast
- 다른 사용자는
  - 해당 노드를 "편집 중" 표시
  - 기본은 편집 불가(또는 "편집하기" 클릭 시 경고 후 override 허용)

### 3.2 필드 단위 Soft Lock(너희가 선택한 방향)
- 노드 내부에서도 특정 필드는 충돌이 더 잦음
  - 예: prompt 텍스트, negative prompt, seed 등
- 규칙
  - 사용자가 입력 포커스를 잡으면 `lock(nodeId, fieldKey)` 송신
  - 다른 사용자는 해당 필드를 read-only로 보여주거나, "누가 편집 중"만 표시

### 3.3 TTL(자동 해제)
- 잠금이 영구히 남지 않게
  - `lastSeenAt` 기반으로 10~20초 heartbeat 없으면 자동 해제

---

## 4) 이벤트 스키마(예시)
```json
{
  "type": "presence.cursor",
  "projectId": 123,
  "userId": 45,
  "userName": "보승",
  "sceneId": 9,
  "x": 0.42,
  "y": 0.73,
  "ts": 1730000000000
}
```

```json
{
  "type": "presence.lock",
  "projectId": 123,
  "userId": 99,
  "sceneId": 9,
  "nodeId": "node-abc",
  "field": "prompt",
  "locked": true,
  "ts": 1730000000000
}
```

---

## 5) 저장과의 관계
- Presence/Cursor/Lock 이벤트는 **영구 저장 데이터가 아니라 '실시간 상태'**
- 따라서 DB에 저장하지 않고, 메모리/Redis TTL로 관리해도 됨
