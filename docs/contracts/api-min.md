# API Minimum Contract (MVP)

> 목적: FE가 mock 없이 바로 붙을 수 있는 **최소 계약**만 고정합니다.  
> 기준 문서: `docs/APIdocs.md`, `docs/ai-movie-studio-md-pack-v3/ai-movie-studio-md-pack/06-api-and-events.md`

---

## 0) 공통 규칙

- Base path: `/api`
- Auth: `Authorization: Bearer <JWT>`
- Idempotency: `Idempotency-Key` 헤더를 지원(같은 키 재요청 시 동일 `jobId` 반환)
- 공통 응답 포맷

```json
{
  "code": "SUCCESS | ACCEPTED | <ERROR_CODE>",
  "message": "optional",
  "data": {},
  "details": {}
}
```

---

## 1) Auth

### 회원가입
`POST /api/auth/signup`

Request
```json
{ "email": "String", "password": "String", "name": "String" }
```

Response
```json
{
  "code": "SUCCESS",
  "data": { "userId": 1, "accessToken": "jwt", "refreshToken": "jwt" }
}
```

### 로그인
`POST /api/auth/login`

Request
```json
{ "email": "String", "password": "String" }
```

Response
```json
{
  "code": "SUCCESS",
  "data": { "userId": 1, "accessToken": "jwt", "refreshToken": "jwt" }
}
```

### 토큰 갱신
`POST /api/auth/refresh`

Request
```json
{ "refreshToken": "jwt" }
```

Response
```json
{
  "code": "SUCCESS",
  "data": { "accessToken": "jwt", "refreshToken": "jwt" }
}
```

---

## 2) Project

### 생성
`POST /api/projects`

Request
```json
{ "title": "String", "description": "String?", "genre": "String?" }
```

Response
```json
{
  "code": "SUCCESS",
  "data": { "projectId": 101, "title": "String", "role": "OWNER" }
}
```

### 목록
`GET /api/projects?page=0&size=20`

Response
```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      { "projectId": 101, "title": "String", "role": "OWNER" }
    ],
    "page": 0, "size": 20, "total": 1
  }
}
```

### 상세
`GET /api/projects/{projectId}`

Response (최소)
```json
{
  "code": "SUCCESS",
  "data": { "projectId": 101, "title": "String", "description": "String?" }
}
```

---

## 3) Scene

### 생성
`POST /api/projects/{projectId}/scenes`

Request
```json
{ "title": "String", "description": "String?" }
```

Response
```json
{ "code": "SUCCESS", "data": { "sceneId": 201 } }
```

### 목록
`GET /api/projects/{projectId}/scenes`

Response
```json
{
  "code": "SUCCESS",
  "data": [
    { "sceneId": 201, "title": "String", "order": 1, "status": "COMPLETED?" }
  ]
}
```

### 순서 변경
`PUT /api/projects/{projectId}/scenes/order`

Request
```json
{ "orderedSceneIds": [201, 202, 203] }
```

Response
```json
{ "code": "SUCCESS" }
```

---

## 4) Nodes

### 생성
`POST /api/scenes/{sceneId}/nodes`

Request (최소)
```json
{
  "type": "MASTER | GRID | SHOT | VIDEO",
  "parentNodeId": 123,
  "prompt": "String?",
  "settings": {}
}
```

Response
```json
{ "code": "SUCCESS", "data": { "nodeId": 301 } }
```

### 목록
`GET /api/scenes/{sceneId}/nodes`

Response (최소)
```json
{
  "code": "SUCCESS",
  "data": {
    "nodes": [
      { "nodeId": 301, "type": "MASTER", "status": "SUCCEEDED?", "contentUrl": "https://..." }
    ]
  }
}
```

### 수정
`PUT /api/nodes/{nodeId}`

Request
```json
{ "prompt": "String?", "settings": {} }
```

Response
```json
{ "code": "SUCCESS" }
```

### 생성 작업 요청 (AI Job 생성)
`POST /api/nodes/{nodeId}/generate`

Response (202)
```json
{ "code": "ACCEPTED", "data": { "jobId": 123, "status": "PENDING" } }
```

---

## 5) AI Jobs

### 상태 조회 (폴링 fallback)
`GET /api/ai/jobs/{jobId}`

Response (최소)
```json
{
  "code": "SUCCESS",
  "data": {
    "jobId": 123,
    "type": "IMAGE_GENERATION | VIDEO_GENERATION | SCENE_MERGE | PROJECT_MERGE",
    "status": "PENDING | RUNNING | SUCCEEDED | FAILED",
    "resultUrl": "https://...",
    "error": { "code": "ERROR_CODE", "message": "String" }
  }
}
```

> 작업 생성 API는 `/api/nodes/{nodeId}/generate`, `/api/scenes/{sceneId}/merge`,
> `/api/projects/{projectId}/merge` 에서 `202 + jobId` 형태로 반환합니다.

---

## 6) Merge

### 씬 병합 요청
`POST /api/scenes/{sceneId}/merge`

Response (202)
```json
{ "code": "ACCEPTED", "data": { "jobId": 1001, "status": "PENDING" } }
```

### 프로젝트 병합 요청
`POST /api/projects/{projectId}/merge`

Response (202)
```json
{ "code": "ACCEPTED", "data": { "jobId": 2001, "status": "PENDING" } }
```

### 최종 영상 URL
`GET /api/projects/{projectId}/export`

Response
```json
{ "code": "SUCCESS", "data": { "exportUrl": "https://..." } }
```

---

## 7) Files (S3 Presign)

### 업로드 URL 발급
`POST /api/files/presign`

Request (최소)
```json
{ "filename": "String", "contentType": "String" }
```

Response (예시)
```json
{
  "code": "SUCCESS",
  "data": {
    "uploadUrl": "https://...",
    "fileKey": "uploads/...",
    "expiresAt": "2026-01-15T17:00:00+09:00"
  }
}
```

> 업로드는 `uploadUrl`로 **HTTP PUT**을 권장하며, `Content-Type`은 `contentType`과 동일하게 설정합니다.

---

## 8) Local Upload Quick Test (MinIO / LocalStack)

1) Presign 요청
```bash
curl -X POST "http://localhost:8080/api/files/presign" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"filename":"sample.png","contentType":"image/png"}'
```

2) 업로드 (uploadUrl로 PUT)
```bash
curl -X PUT "<UPLOAD_URL_FROM_RESPONSE>" \
  -H "Content-Type: image/png" \
  --data-binary "@sample.png"
```

3) 업로드 완료 등록
```bash
curl -X POST "http://localhost:8080/api/files/complete" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fileKey":"uploads/...","fileSize":123456,"mimeType":"image/png"}'
```

### 업로드 완료 등록
`POST /api/files/complete`

Request
```json
{ "fileKey": "String", "fileSize": 123456, "mimeType": "String" }
```

Response
```json
{ "code": "SUCCESS", "data": { "fileId": 999 } }
```
