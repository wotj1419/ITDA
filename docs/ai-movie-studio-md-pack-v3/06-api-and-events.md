# 06. API & WebSocket 이벤트 계약 (FE/BE/Worker 공통)

> 목적: **FE/BE/Worker가 병렬 개발**할 수 있도록, “최소 합의”를 한 문서로 고정합니다.

---

## 0) 공통 규칙

- 모든 JSON REST 응답은 공통 포맷(`code`, `message`, `data`, `details`)
- 인증: JWT Bearer (`Authorization: Bearer ...`)
- 공통 응답(예시)

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {}
}
```

실패 응답(예시)

```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to edit this project.",
  "details": { "projectId": 123 }
}
```

---

## 1) HTTP API (MVP 최소)

> 아래는 **W3 MVP가 돌아가는 데 필요한 최소 계약**입니다.

### Auth
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Project / Scene
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/scenes`
- `GET /api/projects/{projectId}/scenes`
- `PUT /api/projects/{projectId}/scenes/order`

### Scenario
- `GET /api/projects/{projectId}/scenario`
- `POST /api/projects/{projectId}/scenario/prompt/generate`
- `POST /api/projects/{projectId}/scenario/scenes/generate`

### Scene Nodes (Vue Flow)
- `GET /api/scenes/{sceneId}/nodes`
- `POST /api/scenes/{sceneId}/nodes`
- `PATCH /api/nodes/{nodeId}`

### Generate Jobs (image/video)
- `POST /api/nodes/{nodeId}/generate` → 202 + jobId
- `POST /api/nodes/{nodeId}/confirm`
- `DELETE /api/nodes/{nodeId}/confirm`

### AI/Prompt/Job
- `POST /api/ai/prompts/generate`
- `POST /api/ai/prompts/improve`
- `GET /api/ai/jobs/{jobId}` (폴링 fallback)

### Timeline / Merge
- `GET /api/scenes/{sceneId}/timeline`
- `PUT /api/scenes/{sceneId}/timeline`
- `GET /api/projects/{projectId}/timeline`
- `POST /api/scenes/{sceneId}/merge` → 202 + jobId
- `POST /api/projects/{projectId}/merge` → 202 + jobId

### Files
- `POST /api/files/presign`
- `POST /api/files/complete`
- `GET /api/files/{fileId}`

> 상세 request/response 예시는 `07-minimum-api-contracts.md` 를 참고합니다.

---

## 2) WebSocket (W3: Job 상태 / W5: 협업 이벤트 확장)

### 2.1 WS 엔드포인트(권장)
- `WS /ws/projects/{projectId}?token=<JWT>`

> 하나의 WS로 시작하고(3주차), 5주차에 **메시지 타입**만 확장하는 방식을 권장합니다.

### 2.2 메시지 Envelope

```json
{
  "type": "job.done",
  "data": {}
}
```

### 2.3 W3 필수 이벤트 타입

- `job.done` — 작업 완료
- `job.failed` — 작업 실패

`job.done` 예시:

```json
{
  "type": "job.done",
  "data": {
    "jobId": 123,
    "target": { "type": "NODE", "id": 301 },
    "resultUrl": "https://..."
  }
}
```

`job.failed` 예시:

```json
{
  "type": "job.failed",
  "data": {
    "jobId": 123,
    "target": { "type": "NODE", "id": 301 },
    "error": { "code": "GENERATION_FAILED", "message": "Generation failed." }
  }
}
```

### 2.4 W5 협업 이벤트 타입(추가)

- `presence.join` / `presence.leave`
- `presence.update` (현재 화면/씬/노드 등)
- `cursor.update` (마우스/뷰포트 좌표)
- `selection.update` (선택한 nodeId 리스트)
- `follow.request` / `follow.accept` (선택 기능)
- `chat.message`

Presence 예시:

```json
{
  "type": "presence.update",
  "projectId": 10,
  "data": {
    "userId": 3,
    "displayName": "은서",
    "route": "/projects/10/scenes/1",
    "sceneId": 1,
    "activeNodeId": 100
  },
  "ts": "2026-02-04T14:00:00+09:00"
}
```

Cursor 예시:

```json
{
  "type": "cursor.update",
  "projectId": 10,
  "data": {
    "userId": 3,
    "sceneId": 1,
    "x": 0.42,
    "y": 0.77,
    "viewport": { "zoom": 0.9, "panX": -120, "panY": 40 }
  }
}
```

Chat 예시:

```json
{
  "type": "chat.message",
  "projectId": 10,
  "data": {
    "messageId": "m_...",
    "userId": 3,
    "text": "이 샷 프롬프트 조금만 더 강하게 가볼까?",
    "sentAt": "2026-02-04T14:01:00+09:00"
  }
}
```

---

## 3) WebRTC 시그널링 메시지 (W5)

> WebRTC는 **미디어(P2P)** + **시그널링(서버/WS)** 로 나뉩니다.
> 여기서는 “서버가 WS로 전달해줄 최소 메시지 타입”만 합의합니다.

- `rtc.join` / `rtc.leave`
- `rtc.offer`
- `rtc.answer`
- `rtc.ice`

Offer 예시:

```json
{
  "type": "rtc.offer",
  "projectId": 10,
  "data": {
    "fromUserId": 3,
    "toUserId": 5,
    "sdp": "v=0..."
  }
}
```

ICE Candidate 예시:

```json
{
  "type": "rtc.ice",
  "projectId": 10,
  "data": {
    "fromUserId": 3,
    "toUserId": 5,
    "candidate": { "candidate": "candidate:...", "sdpMid": "0", "sdpMLineIndex": 0 }
  }
}
```

---

## 4) 4주차 확장(권장): 멤버/권한/오브젝트시트

> W4는 안정화 주간이라, 아래는 “있으면 좋은 최소 계약”만 적습니다.

### Member / Permission
- `GET /api/projects/{projectId}/members`
- `POST /api/projects/{projectId}/members`
- `PATCH /api/projects/{projectId}/members/{memberId}` (role 변경)
- `DELETE /api/projects/{projectId}/members/{memberId}`
- `DELETE /api/projects/{projectId}/members/me`

### Object Sheet
- `POST /api/projects/{projectId}/objects`
- `GET /api/projects/{projectId}/objects`
- `GET /api/objects/{objectId}`
- `PUT /api/objects/{objectId}`

---

## 5) 구현 시 주의점 (실수 방지)

- WS 이벤트는 **FE가 쉽게 매핑 가능한 key**(jobId, sceneId, nodeId)를 반드시 포함합니다.
- 이벤트 타입은 W3에 최소로 고정하고, W5에 확장하되 **기존 타입 호환성을 깨지 않습니다.**
- `cursor.update`는 10~20Hz 정도로 **throttle** 하고, 서버는 broadcast만(저장 X)합니다.
