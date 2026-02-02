# 협업 커서/노드 이동 진행 정리 (2026-02-01)

## 목적
- 커서 공유와 노드 이동을 실시간으로 동기화한다.
- STOMP Presence 채널(`/pub/presence/{projectId}`)을 재사용한다.
- 동일 `projectId` + 동일 `sceneId`에서만 표시/반영한다.

---

## 현재 동작 요약
### 커서 (CURSOR)
- 전송 좌표: VueFlow의 **flow 좌표** (줌/패닝 보정 적용)
- 수신 후 렌더: flow 좌표를 화면 좌표로 변환해 표시
- 스로틀: 80ms (FE에서만)
- 서버: 중계만 수행 (Rate limit 없음)

### 노드 이동 (NODE_MOVE)
- 전송 좌표: VueFlow node.position (flow 좌표)
- 드래그 중: 80ms 스로틀 전송
- 드래그 종료: 최종 위치 1회 전송
- 수신 적용: 동일 씬일 때만 반영
- 동시성 방지:
  - 로컬 드래그 중인 노드는 원격 적용 무시
  - 서버 `updatedAt` 기준으로 최신 이벤트만 적용

---

## 메시지 스키마 (Presence)
### CURSOR (Client → Server)
```json
{ "type": "CURSOR", "sceneId": 5, "x": 123.4, "y": 567.8 }
```

### CURSOR (Server → Client)
```json
{
  "type": "CURSOR",
  "userId": 5,
  "name": "홍길동",
  "profileImageUrl": null,
  "sceneId": 5,
  "x": 123.4,
  "y": 567.8,
  "updatedAt": "2026-02-01T03:00:00.000Z"
}
```

### NODE_MOVE (Client → Server)
```json
{ "type": "NODE_MOVE", "sceneId": 5, "nodeId": 101, "x": 120, "y": 340 }
```

### NODE_MOVE (Server → Client)
```json
{
  "type": "NODE_MOVE",
  "userId": 5,
  "name": "홍길동",
  "profileImageUrl": null,
  "sceneId": 5,
  "nodeId": 101,
  "x": 120,
  "y": 340,
  "updatedAt": "2026-02-01T03:00:00.000Z"
}
```

---

## 백엔드 변경
### Presence 스키마 확장
- `PresenceRequest`: status/summary/x/y/action 필드 추가
  - `itda-backend/src/main/java/com/itda/backend/collab/messaging/PresenceRequest.java`

### Presence 이벤트 확장
- `PresenceEvent`: status/summary/x/y/action/updatedAt 추가
- 타입 추가: `STATUS`, `CURSOR`, `NODE_SELECT`, `NODE_MOVE`
  - `itda-backend/src/main/java/com/itda/backend/collab/messaging/PresenceEvent.java`

### Presence 컨트롤러 처리
- `type`별 Validation
- LOCATION만 snapshot 캐시 (CURSOR/NODE_MOVE는 캐시하지 않음)
  - `itda-backend/src/main/java/com/itda/backend/collab/controller/CollabPresenceController.java`

---

## 프론트엔드 변경
### 커서 전송/렌더
- 전송: screen → flow 좌표 변환 후 전송
- 렌더: flow → screen 좌표 변환 후 표시
  - `itda-frontend/src/components/scene-editor/NodeCanvas.vue`
  - `itda-frontend/src/components/collab/CursorOverlay.vue`
  - `itda-frontend/src/stores/collab.ts`

### 노드 이동 동기화
- 드래그 이벤트에서 `NODE_MOVE` 전송
- 수신 시 노드 위치 반영
  - `itda-frontend/src/components/scene-editor/NodeCanvas.vue`
  - `itda-frontend/src/stores/collab.ts`
  - `itda-frontend/src/stores/sceneNode/index.ts`

### 컬러 정책
- 사용자 ID 해시 기반 고정 색상
  - `itda-frontend/src/stores/collab.ts`

---

## 동시성/충돌 처리
- 로컬 드래그 중인 노드는 원격 업데이트 무시
- 원격 이벤트는 `updatedAt` 최신만 적용
- 씬 전환 시 커서/원격 타임스탬프 캐시 초기화

---

## 확인 체크리스트
- [ ] 같은 씬에서 커서가 정확히 보이는지 (줌/패닝 다를 때 포함)
- [ ] 다른 씬에서는 커서/노드 이동이 보이지 않는지
- [ ] 노드 드래그 시 실시간 반영되는지
- [ ] 동시 드래그 충돌이 최소화되는지

---

## 남은 작업 (필요 시)
- STATUS/NODE_SELECT UI 반영 (현재 수신은 무시)
- 커서 오프셋 보정 (커서 아이콘 tip 기준 보정)
