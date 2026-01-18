# 📡 WebSocket 이벤트 스키마 (Job 상태 알림)

> W3 MVP에서 WebSocket은 **“실시간 UI 업데이트”** 용도입니다.
> - 폴링(`/api/ai/jobs/{id}`)은 fallback
> - 기본은 서버 PUSH(WebSocket)로 “노드 배지/토스트/타임라인/다운로드 버튼”을 즉시 반영

---

## 1) WebSocket이 하는 일(정의)

### ✅ 서버 → 클라이언트 PUSH
- Worker가 Job을 끝내면(성공/실패), API 서버는 해당 프로젝트/씬에 연결된 클라이언트에게 **이벤트를 푸시**합니다.
- FE는 이벤트 수신 시 다음을 갱신합니다.
  - 노드 상태 뱃지(queued/running/succeeded/failed)
  - “재시도” 버튼 노출
  - 타임라인(확정 클립 목록)
  - export(병합) 완료 시 다운로드 링크

### ❌ WebSocket이 안 하는 일(MVP)
- WebRTC 시그널링(5주차)
- 텍스트 채팅(5주차)
- Presence(현재 어디 보고 있는지)(5주차)

---

## 2) 연결 방식(권장)

- Endpoint 예시: `ws://{host}/ws/projects/{projectId}`
- 인증: 연결 시 `Authorization: Bearer <JWT>` 또는 `?token=...` (팀 표준 1개로)
- 구독 단위: **project** (씬 편집/타임라인/다운로드가 프로젝트 단위로 묶임)

---

## 3) 메시지 envelope(공통)

모든 이벤트는 아래 envelope로 감쌉니다.

```json
{
  "type": "job.updated",
  "projectId": 123,
  "occurredAt": "2026-01-19T10:01:22+09:00",
  "data": { }
}
```

- `type`: 이벤트 이름
- `projectId`: FE 라우트가 바뀌어도 “어떤 프로젝트에 대한 이벤트인지” 필터링 가능
- `occurredAt`: 디버깅/재현에 도움
- `data`: 이벤트별 payload

---

## 4) 이벤트 타입 목록 (W3 MVP)

### 4.1 `job.updated`
Job 상태가 바뀔 때마다(queued→running, running→succeeded/failed) 전송.

```json
{
  "type": "job.updated",
  "projectId": 123,
  "occurredAt": "2026-01-21T15:01:22+09:00",
  "data": {
    "jobId": "J_01H...",
    "jobType": "image.generate",
    "status": "RUNNING",
    "progress": 0.3,
    "sceneId": 45,
    "nodeId": 888,
    "message": "calling Gemini..."
  }
}
```

- `progress`는 선택. 없으면 FE는 스피너만 표시.

### 4.2 `job.succeeded`
성공 완료. 결과 리소스(이미지/영상/export)의 **URL/리소스 ID**를 포함.

```json
{
  "type": "job.succeeded",
  "projectId": 123,
  "occurredAt": "2026-01-21T15:02:10+09:00",
  "data": {
    "jobId": "J_01H...",
    "jobType": "video.generate",
    "status": "SUCCEEDED",
    "sceneId": 45,
    "nodeId": 889,
    "result": {
      "assetId": 777,
      "assetType": "VIDEO",
      "url": "https://.../video.mp4",
      "thumbnailUrl": "https://.../thumb.jpg",
      "durationMs": 4200
    }
  }
}
```

### 4.3 `job.failed`
실패 완료. UI에서 재시도 버튼/에러 메시지 표시.

```json
{
  "type": "job.failed",
  "projectId": 123,
  "occurredAt": "2026-01-21T15:02:10+09:00",
  "data": {
    "jobId": "J_01H...",
    "jobType": "merge.export",
    "status": "FAILED",
    "sceneId": null,
    "nodeId": null,
    "error": {
      "code": "FFMPEG_EXIT_1",
      "message": "concat failed",
      "retryable": true
    }
  }
}
```

---

## 5) FE 처리 규칙(간단)

- `job.updated` 수신
  - 해당 `jobId`를 가진 노드/화면 컴포넌트 상태 갱신
- `job.succeeded` 수신
  - 결과 URL 저장 + 노드 상태 `SUCCEEDED`로
  - 타임라인/다운로드 UI 갱신 트리거
- `job.failed` 수신
  - `retryable=true`면 “재시도” 버튼 노출
  - 로그/알림용으로 에러 code를 함께 표시(개발 모드에서)

---

## 6) 폴링 fallback (연결이 끊겼을 때)

- FE는 WebSocket이 끊기면
  - **현재 화면에 보이는 jobId들만** `GET /api/ai/jobs/{id}`로 주기적 폴링(예: 2~3초)
  - 복구되면 폴링 중단

---

## 7) 구현 위치(백엔드)

- Worker는 WebSocket을 직접 쏘지 않습니다.
- Worker는 **DB 업데이트 + Streams ack**만 책임.
- API 서버가 DB 변화(또는 Worker 콜백/도메인 이벤트)를 감지하여 WebSocket으로 브로드캐스트합니다.

> 구현 방식은 2가지 중 택1
> 1) Worker가 API 서버의 `POST /internal/jobs/{id}/events` 같은 내부 엔드포인트 호출
> 2) Worker가 DB 업데이트 후, API 서버가 `JobService`에서 상태 변경 시점에 이벤트 publish

MVP에서는 **1번(내부 엔드포인트)**가 단순합니다.

