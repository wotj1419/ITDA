# W3 MVP Implementation Plan (API alignment + E2E)

> 작성일: 2026-01-24
> 목표: **3주차 MVP E2E 1회 완주** (협업 기능은 W4~W5 범위)
> 원칙: **Backend API를 기준(SoT)으로 문서/프론트 정합성 맞춤**

---

## 0) Scope & Non-scope

### W3 MVP 포함
- Auth (signup/login/refresh/me)
- Project/Scene CRUD + scene reorder
- Scenario (prompt/plot/scenes)
- Scene Nodes (CRUD, confirm/unconfirm)
- AI Job 생성/조회 (image/video)
- Timeline 조회 (confirmed video만)
- Merge(프로젝트 기준) → 다운로드
- Job 완료/실패 WS 이벤트

### W3 MVP 제외 (W4~W5 목표)
- 협업(WebRTC signaling, chat, presence/cursor)
- 멤버/권한 관리
- 오브젝트 시트
- 파일 업로드 presign
- CRDT/Yjs

---

## 1) Source of Truth 결정

- **API 규격의 기준: Backend 구현**
- 문서(APIdocs/PRD/contracts)와 FE 코드는 **Backend 기준으로 정리/수정**
- 협업(WebRTC/Presence/Chat)은 **W4~W5에 Raw WS로 별도 구현**
- W3 MVP는 **STOMP 기반 Job 이벤트 WS만 사용**

### 1.1 W3 계약(Contract Freeze) - 병렬 개발을 위한 고정안
아래 항목은 **W3 동안 변경 금지** (변경 시 FE/BE 동시 수정 필요).

- Base URL: `/api`
- Auth: `Authorization: Bearer <JWT>`
- 공통 응답: `{ code, message?, data?, details? }`
- Job status: `PENDING | RUNNING | SUCCEEDED | FAILED`
- W3 WS: **STOMP `/ws` + `/topic/projects/{projectId}`**
- W4~W5 WS: Raw WS `/ws/room/{roomId}` 및 `/ws/projects/{projectId}`로 별도 구현

---

## 2) W3 API Contract (Backend 기준)
> 이 섹션은 **이 문서만 보고 FE/BE가 병렬 개발**할 수 있도록 최소/필수 스펙을 고정한다.

### 2.1 Auth
**POST /api/auth/signup**
```json
{ "email": "a@b.com", "password": "string", "name": "string" }
```
응답 `data` (UserResponse)
```json
{ "id": 1, "email": "a@b.com", "name": "string", "profileImageUrl": null, "role": "USER" }
```

**POST /api/auth/login**
```json
{ "email": "a@b.com", "password": "string" }
```
응답 `data` (LoginResponse)
```json
{ "accessToken": "jwt", "refreshToken": "jwt", "expiresIn": 3600 }
```

**GET /api/auth/me**
응답 `data`: UserResponse (위와 동일)

### 2.2 Project
**POST /api/projects**
```json
{ "title": "string", "description": "string?", "genre": "string?" }
```
응답 `data` (ProjectCreateResponse)
```json
{ "projectId": 101, "title": "string", "role": "OWNER" }
```

**GET /api/projects?page=0&size=20**
응답 `data` (ProjectListResponse)
```json
{ "items": [ { "projectId": 101, "title": "string", "role": "OWNER" } ], "page": 0, "size": 20, "total": 1 }
```

**GET /api/projects/{projectId}**
응답 `data` (ProjectDetailResponse)
```json
{ "projectId": 101, "title": "string", "description": "string?", "genre": "string?", "myRole": "OWNER" }
```

**PUT /api/projects/{projectId}**
```json
{ "title": "string?", "description": "string?", "genre": "string?" }
```

**DELETE /api/projects/{projectId}**
응답 `code=SUCCESS`

### 2.3 Scene
**POST /api/projects/{projectId}/scenes**
```json
{ "title": "string", "description": "string?", "sceneOrder": 1? }
```
응답 `data`
```json
{ "sceneId": 201 }
```

**GET /api/projects/{projectId}/scenes**
응답 `data` (SceneSummaryResponse[])
```json
[{ "sceneId": 201, "title": "string", "order": 1, "status": null }]
```

**GET /api/scenes/{sceneId}**
응답 `data` (SceneDetailResponse)
```json
{ "sceneId": 201, "projectId": 101, "title": "string", "description": "string?", "order": 1 }
```

**PUT /api/projects/{projectId}/scenes/order**
```json
{ "orderedSceneIds": [201, 202, 203] }
```

### 2.4 Scenario (Backend 기준)
**GET /api/projects/{projectId}/scenario**

**POST /api/projects/{projectId}/scenario/prompt/generate**
```json
{
  "genre": "SF",
  "mood": "HOPEFUL",
  "sceneCount": 5,
  "keywords": ["a", "b"],
  "characterHints": "string?",
  "backgroundHints": "string?",
  "referenceStyle": "string?"
}
```

**POST /api/projects/{projectId}/scenario/plot/generate** (body 없음)

**POST /api/projects/{projectId}/scenario/scenes/generate** (body 없음)

### 2.5 Nodes
**POST /api/scenes/{sceneId}/nodes**
```json
{ "nodeType": "MASTER|GRID|SHOT|VIDEO", "parentNodeId": 123?, "prompt": "string?", "settings": {} }
```
응답 `data`
```json
{ "nodeId": 301 }
```

**GET /api/scenes/{sceneId}/nodes**
응답 `data`
```json
{ "nodes": [ { "nodeId": 301, "type": "MASTER", "parentNodeId": null, "status": "SUCCEEDED", "contentUrl": "http://..." } ] }
```

**PUT /api/nodes/{nodeId}**
```json
{ "prompt": "string?", "settings": {} }
```

**POST /api/nodes/{nodeId}/confirm**
**DELETE /api/nodes/{nodeId}/confirm**

### 2.6 AI Prompt API (W3 유지)
> 목적: 노드 UI에서 **프롬프트 생성/개선** 버튼이 끊기지 않게 하는 최소 API.

**POST /api/ai/prompts/generate**
```json
{ "nodeType": "MASTER|GRID|SHOT|VIDEO", "sceneOneLine": "string", "style": "string", "timeOfDay": "string", "mood": "string", "objects": ["string"] }
```
응답 `data`
```json
{ "prompt": "string" }
```

**POST /api/ai/prompts/improve**
```json
{ "nodeType": "MASTER|GRID|SHOT|VIDEO", "prompt": "string", "instruction": "string?" }
```
응답 `data`
```json
{ "prompt": "string" }
```

> W3에서는 **stub 허용**: 요청을 그대로 가공하거나 고정 문자열 반환.

### 2.7 Job 생성/조회
**POST /api/nodes/{nodeId}/generate**
```json
{ "prompt": "string", "settings": {} }
```
응답 `code=ACCEPTED`
```json
{ "jobId": 123, "status": "PENDING" }
```

**GET /api/ai/jobs/{jobId}**
응답 `data`
```json
{
  "jobId": 123,
  "type": "IMAGE_GENERATION|VIDEO_GENERATION|PROJECT_MERGE",
  "status": "PENDING|RUNNING|SUCCEEDED|FAILED",
  "resultUrl": "http://...",
  "error": { "code": "ERROR_CODE", "message": "string" }
}
```

### 2.8 Timeline / Merge / Export
**GET /api/projects/{projectId}/timeline**
```json
{ "items": [ { "videoNodeId": 301, "sceneId": 201, "order": 1, "url": "http://..." } ] }
```

**POST /api/projects/{projectId}/merge**
응답 `code=ACCEPTED`
```json
{ "jobId": 2001, "status": "PENDING" }
```

**GET /api/projects/{projectId}/export**
```json
{ "exportUrl": "http://localhost:8080/files/exports/{projectId}/final.mp4" }
```

### 2.9 W3 WebSocket (STOMP)
- Endpoint: `/ws` (SockJS 사용)
- Subscribe: `/topic/projects/{projectId}`
- Message Envelope
```json
{ "type": "job.done|job.failed", "data": {} }
```

---

## 3) 문서/코드 정합성 매트릭스 (핵심 불일치)

| 항목 | 문서 | 현재 코드 | 조치(계획) |
|---|---|---|---|
| Base URL | `/api` | FE: `/api/v1` | FE baseURL 수정, 문서 확인 |
| Auth 로그인 응답 | userId+token | BE: accessToken/refreshToken/expiresIn | 문서+FE 수정 (login 후 `/auth/me`) |
| Project 목록 응답 | items/page/size/total | FE는 배열만 기대 | FE 응답 타입 수정 |
| Project 수정 method | 문서: PUT | FE: PATCH | FE 수정 |
| Node 생성 필드 | 문서: `type` | BE: `nodeType` | 문서+FE 수정 |
| Node generate | 문서 존재 | BE 미구현 | BE 구현 |
| AI prompt API | 문서 존재 | BE 미구현 | W3 최소 구현(stub 허용) |
| Scenario 요청 body | 문서: genre/mood/sceneCount | FE: topic/script | FE 수정 |
| Timeline API | 문서 존재 | BE 미구현 | BE 구현 |
| Merge API | 문서 존재 | BE 미구현 | BE 구현 |
| WS Endpoint | 문서: Raw WS `/ws/projects/...` | BE: STOMP `/ws` + `/topic/projects/{id}` | W3: STOMP 유지 + 문서 주석, W5: Raw WS 추가 |

---

## 4) Backend 구현 계획 (W3 목표)

### 4.1 필수 엔드포인트 추가
- `POST /api/nodes/{id}/generate`
  - 노드 타입에 따라 IMAGE/VIDEO Job 생성
  - `202 ACCEPTED` + `jobId`
- `POST /api/ai/prompts/generate`
  - 노드용 프롬프트 생성 (W3는 stub 허용)
- `POST /api/ai/prompts/improve`
  - 프롬프트 개선 (W3는 stub 허용)
- `GET /api/projects/{projectId}/timeline`
  - confirmed VIDEO만 반환
- `POST /api/projects/{projectId}/merge`
  - merge Job 생성 + `jobId`
- `GET /api/projects/{projectId}/export`
  - export URL 반환(로컬 파일)
- (선택) `GET /api/scenes/{sceneId}/timeline`
  - UI에서 scene 단위 타임라인 필요 시

### 4.2 Job/Worker 연결
- Dispatcher: Job 생성 → enqueue
- Executor: Job 상태 전이(PENDING→RUNNING→SUCCEEDED/FAILED)
- Worker 타입 분기 연결
  - IMAGE_GENERATION
  - VIDEO_GENERATION (mock 지원)
  - PROJECT_MERGE (FFmpeg concat, 로컬 저장)

### 4.3 Worker 구현 상태/계획
- 현재 코드: **Worker 실구현 없음**, `JobExecutor`에 TODO만 존재
- W3 목표: **로컬/Mock Worker로라도 성공 플로우 확보**
  - 이미지/영상/병합 각각 최소 1건 성공
  - 실제 외부 AI 호출 실패 시에도 demo가 가능하도록 stub 지원

### 4.3 결과 저장/URL
- W3 목표: **로컬 파일 다운로드 우선**
- 저장 경로 예: `./uploads/ai/...`
- Job 결과에 `resultUrl` (local static path 또는 presigned placeholder)

### 4.4 WS 이벤트 (W3 범위)
- STOMP `/ws` + `/topic/projects/{projectId}` 유지
- 이벤트 type: `job.done`, `job.failed`
- FE는 토픽 구독만 구현

---

## 5) Frontend 정합성 수정 계획 (W3 목표)

### 5.1 환경/서비스 레이어
- `VITE_USE_MOCK=false`
- baseURL: `/api` 로 수정

### 5.2 Auth 흐름
- 로그인 응답: accessToken/refreshToken
- 로그인 직후 `/api/auth/me` 호출하여 user 정보 확보

### 5.3 Scenario 연동
- FE 요청 바디를 backend `GeneratePromptRequest`에 맞춤
- plot/scene generate API 요청 방식 수정

### 5.4 Node 연동
- create: `nodeType` 필드 사용
- generate: `/nodes/{id}/generate`
- confirm/unconfirm: 기존 유지

### 5.5 Timeline/Merge
- timeline 응답 타입 정의
- merge 요청 후 jobId 기반 폴링/WS 반영

### 5.6 WS 연동
- STOMP client 추가
- `/topic/projects/{projectId}` subscribe
- job.done/job.failed 처리

---

## 6) 문서 정합성 수정 계획 (W3 목표)

### 6.1 API 문서 정리
- `docs/APIdocs.md`
  - backend 기준으로 응답/요청 정리
  - login 응답 수정
  - timeline/merge/export 추가 반영
  - prompt API 요청/응답 형식 고정

### 6.2 PRD 반영
- W3 범위에 맞게 **WS는 job 이벤트만 명시**
- WebRTC signaling은 W5로 명확히 분리

### 6.3 contracts 업데이트
- `docs/contracts/api-min.md`
  - nodeType 필드 반영
  - login 응답 수정
  - timeline 응답에 url 추가
- `docs/contracts/ws-events.md`
  - W3는 STOMP 기반 사용 명시 (주석)

---

## 7) 병렬 작업 가이드 (이 문서만으로 작업 가능하게)

### 7.1 Backend 트랙
1. API 스펙 반영 (Section 2 기준)
2. /nodes/{id}/generate + /projects/{id}/timeline + /projects/{id}/merge + /projects/{id}/export 구현
3. Job 생성/상태 전이/WS 이벤트 발행 확인
4. 로컬 저장 경로 및 다운로드 URL 제공

### 7.2 Frontend 트랙
1. `VITE_USE_MOCK=false`, baseURL `/api`
2. Auth: login 후 `/auth/me`로 user 획득
3. Scenario request body 수정
4. Node 생성/생성요청/confirm API 연동
5. Timeline/merge UI 연동 + WS 구독

### 7.3 Docs 트랙
1. APIdocs/PRD/contracts를 Section 2 계약으로 정리
2. W3는 STOMP, W5는 Raw WS 로 명시

### 7.4 통합 트랙
1. Job 생성 → WS 이벤트 수신 → UI 배지 반영
2. Confirm → timeline → merge → export URL 확인

---

## 8) E2E 리허설 단계 (W3 완주)

1. 회원가입/로그인
2. 프로젝트 생성
3. 시나리오 생성 → 씬 자동 생성
4. 씬 편집 진입, 노드 생성
5. 이미지/영상 생성 job → WS 반영
6. 영상 confirm → 타임라인 반영
7. merge 요청 → 완료 시 download URL

---

## 9) 리스크 & 완화

- **Job 결과 URL 불명확** → W3는 로컬 경로로 먼저 동작, W4에 S3 전환
- **WS 프로토콜 불일치** → W3는 STOMP 유지, W5에 Raw WS 추가
- **AI 실제 호출 실패** → mock 모드로 성공 플로우 확보
- **FE mock 의존** → `VITE_USE_MOCK=false` 전환 우선

---

## 10) 체크리스트 (완료 기준)

- [ ] backend: `/nodes/{id}/generate` 구현
- [ ] backend: `/projects/{id}/timeline` 구현
- [ ] backend: `/projects/{id}/merge` 구현
- [ ] backend: `/projects/{id}/export` 구현
- [ ] backend: job resultUrl 반환
- [ ] backend: job.done/job.failed WS 전송 확인
- [ ] frontend: baseURL `/api`, mock 제거
- [ ] frontend: login→/me 흐름 반영
- [ ] frontend: scenario 요청 형식 수정
- [ ] frontend: timeline/merge 화면 연결
- [ ] E2E 1회 완주

---

## 11) W4~W5 협업 기능 분리 계획 (요약)

- W4: Presence 이벤트 추가 (WS 타입 확장)
- W5: Raw WS 기반 WebRTC signaling 서버 구현
- W5: 채팅/Presence/Cursor/Selection 이벤트 추가
- W5: PRD 8.9/8.10 계약으로 WS 재정렬
