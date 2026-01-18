# 🔌 Minimum API Contracts (W3 MVP)

> 목적: FE/BE/Worker가 동시에 개발할 수 있도록 “요청/응답/상태코드/필수 필드”를 최소로 합의합니다.

---

## 0) 공통 규칙

- 모든 응답은 JSON
- 에러 형태(예시)

```json
{
  "errorCode": "...",
  "message": "...",
  "traceId": "...",
  "details": {}
}
```

- 인증: JWT Bearer (`Authorization: Bearer ...`)

---

## 1) Auth

### 1.1 회원가입
- `POST /api/auth/signup`

Request
```json
{ "email": "user@example.com", "password": "...", "name": "..." }
```

Response (201)
```json
{ "userId": 1 }
```

### 1.2 로그인
- `POST /api/auth/login`

Request
```json
{ "email": "user@example.com", "password": "..." }
```

Response (200)
```json
{ "accessToken": "...", "refreshToken": "..." }
```

---

## 2) Project / Scene

### 2.1 프로젝트 생성
- `POST /api/projects`

Request
```json
{ "title": "My Movie" }
```

Response (201)
```json
{ "projectId": 10 }
```

### 2.2 프로젝트 목록
- `GET /api/projects`

Response (200)
```json
{ "items": [ { "projectId": 10, "title": "My Movie" } ] }
```

### 2.3 씬 목록
- `GET /api/projects/{projectId}/scenes`

Response (200)
```json
{ "items": [ { "sceneId": 1, "order": 1, "title": "Scene 1" } ] }
```

### 2.4 씬 순서 변경
- `POST /api/projects/{projectId}/scenes/reorder`

Request
```json
{ "sceneIds": [3,1,2] }
```

Response (200)
```json
{ "ok": true }
```

---

## 3) Scenario

### 3.1 시나리오 생성
- `POST /api/projects/{projectId}/scenario:generate`

Request
```json
{ "prompt": "..." }
```

Response (202)
```json
{ "jobId": "job_..." }
```

### 3.2 시나리오 결과 조회(폴링)
- `GET /api/ai/jobs/{jobId}`

Response (200)
```json
{
  "jobId": "job_...",
  "type": "scenario",
  "status": "SUCCEEDED",
  "result": { "summary": "..." }
}
```

---

## 4) Scene Nodes (Vue Flow)

> BE는 **노드 저장/상태/제약(유일성)**을 책임, FE는 렌더/편집을 책임.

### 4.1 노드 목록
- `GET /api/scenes/{sceneId}/nodes`

Response (200)
```json
{ "items": [ { "nodeId": 1, "type": "master", "data": {}, "status": "IDLE" } ] }
```

### 4.2 노드 생성
- `POST /api/scenes/{sceneId}/nodes`

Request
```json
{ "type": "shot", "data": { "prompt": "..." }, "position": { "x": 10, "y": 20 } }
```

Response (201)
```json
{ "nodeId": 100 }
```

### 4.3 노드 수정(위치/데이터)
- `PATCH /api/scenes/{sceneId}/nodes/{nodeId}`

Request
```json
{ "data": { "prompt": "..." }, "position": { "x": 50, "y": 60 } }
```

Response (200)
```json
{ "ok": true }
```

---

## 5) Generate Jobs (image/video)

### 5.1 이미지 생성 요청
- `POST /api/scenes/{sceneId}/nodes/{nodeId}/image:generate`

Response (202)
```json
{ "jobId": "job_..." }
```

### 5.2 영상 생성 요청
- `POST /api/scenes/{sceneId}/nodes/{nodeId}/video:generate`

Response (202)
```json
{ "jobId": "job_..." }
```

### 5.3 영상 확정/해제
- `POST /api/shots/{shotId}/confirm`
- `POST /api/shots/{shotId}/unconfirm`

Response (200)
```json
{ "ok": true }
```

---

## 6) Timeline / Merge

### 6.1 타임라인 조회
- `GET /api/projects/{projectId}/timeline`

Response (200)
```json
{ "clips": [ { "shotId": 1, "videoUrl": "s3://...", "order": 1 } ] }
```

### 6.2 타임라인 순서 변경
- `POST /api/projects/{projectId}/timeline/reorder`

Request
```json
{ "shotIds": [3,1,2] }
```

Response (200)
```json
{ "ok": true }
```

### 6.3 병합 요청
- `POST /api/projects/{projectId}/merge`

Response (202)
```json
{ "jobId": "job_..." }
```

### 6.4 병합 결과 다운로드 URL
- `GET /api/exports/{exportId}`

Response (200)
```json
{ "url": "https://...presigned...", "expiresAt": "2026-01-23T23:59:59+09:00" }
```

---

## 7) WebSocket 구독(요약)

- `WS /ws` 연결 후
- 프로젝트 기준 채널(예: `/topic/projects/{projectId}`) 구독
- 메시지 payload는 `04-WebSocket-Event-Schema.md` 참고

