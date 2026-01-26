# WebSocket Events (MVP)

> 목적: FE가 **Job 완료/실패**를 실시간 수신할 수 있도록 최소 이벤트 스키마를 고정합니다.  
> 기준 문서: `docs/APIdocs.md` (2.10), `docs/ai-movie-studio-md-pack-v3/ai-movie-studio-md-pack/06-api-and-events.md`

---

## 1) WS Endpoint / Channel 규칙 (W3)

- **STOMP + SockJS**: `/ws`
- Subscribe: `/topic/projects/{projectId}`
- **채널 단위 = projectId**  
  프로젝트 멤버만 구독 가능

> W5 확장: Raw WS `/ws/room/{roomId}`(협업/채팅) 및 `/ws/projects/{projectId}`(프로젝트 이벤트) 별도 구현

---

## 2) 메시지 Envelope

```json
{
  "type": "job.done | job.failed",
  "data": {}
}
```

---

## 3) Event: job.done

```json
{
  "type": "job.done",
  "data": {
    "jobId": 123,
    "target": { "type": "NODE | PROJECT", "id": 301 },
    "resultUrl": "https://..."
  }
}
```

- W3 범위에서는 `NODE` 또는 `PROJECT` 기준으로 전달됩니다.

---

## 4) Event: job.failed

```json
{
  "type": "job.failed",
  "data": {
    "jobId": 123,
    "target": { "type": "NODE | PROJECT", "id": 301 },
    "error": { "code": "ERROR_CODE", "message": "String" }
  }
}
```

---

## 5) HTTP Fallback

- WS 연결이 없으면 `GET /api/ai/jobs/{jobId}` 폴링으로 상태 확인

