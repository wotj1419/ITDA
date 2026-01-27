# WebSocket 이벤트 점검 보고서

## 목적
- 프로젝트 이벤트(WebSocket) `job.done` / `job.failed` 발행 여부 확인
- 노드 상태 WS 연동(Backend -> Frontend) 흐름 점검

## 범위
- Backend: WebSocket 설정, Job 이벤트 발행 로직
- Frontend: 프로젝트 이벤트 구독 및 노드 상태 반영 로직

## 결론 요약
- **구현됨**: BE에서 `job.done` / `job.failed` 이벤트 발행
- **구현됨**: FE에서 `/topic/projects/{projectId}` 구독 후 노드 상태 반영
- **주의**: WS 인증/권한 검증은 별도 장치 없음 (추가 보완 필요 가능)

---

## 1. Backend 점검 결과

### 1) WebSocket 설정
- 엔드포인트: `/ws` (SockJS 사용)
- 브로커: `/topic` simple broker
- 설정 파일: `itda-backend/src/main/java/com/itda/backend/global/config/WebSocketConfig.java`

### 2) Job 이벤트 발행
- Job 완료/실패 시 `JobExecutor` -> `JobEventPublisher` 호출
- 구현체: `WebSocketJobEventPublisher`
- 실제 전송: `ProjectEventWebSocketPublisher`가 `/topic/projects/{projectId}`에 발행

관련 코드
- `itda-backend/src/main/java/com/itda/backend/job/service/WebSocketJobEventPublisher.java`
- `itda-backend/src/main/java/com/itda/backend/job/event/ProjectEventWebSocketPublisher.java`
- `itda-backend/src/main/java/com/itda/backend/job/event/JobEventMessage.java`

### 3) 메시지 포맷
```json
{
  "event": "job.done",
  "data": {
    "jobId": 123,
    "type": "IMAGE_GENERATION",
    "status": "SUCCEEDED",
    "target": { "type": "NODE", "id": 10 },
    "resultUrl": "https://..."
  }
}
```
* event: job.done 또는 job.failed
* status: SUCCEEDED 또는 FAILED

---

## 2. Frontend 점검 결과

### 1) WS 연결 및 구독
- SockJS 연결: `/ws`
- 구독 토픽: `/topic/projects/{projectId}`
- 파일: `itda-frontend/src/services/ws/projectEvents.ts`

### 2) 노드 상태 반영
- `job.done` / `job.failed` 수신 시 노드 `jobStatus` 업데이트
- 완료 시 `resultUrl`로 결과 이미지/영상 URL 반영
- 파일: `itda-frontend/src/stores/sceneNode/index.ts`

---

## 3. 이슈/주의사항

1) **WS 인증/권한 검증 부재**
   - `/ws`는 SecurityConfig에서 `permitAll`
   - STOMP 연결 시 프로젝트 멤버 권한 검증 로직 없음
   - 보안 요구가 있으면 구독 시점에 권한 체크 필요

2) **WebSocketJobEventPublisher 주석**
   - "로그만 출력"으로 작성돼 있으나 실제로는 publish 수행
   - 주석 정합성 업데이트 권장

---

## 4. 점검 방법 요약

1) 이미지/영상 Job 실행
2) Backend 로그에서 `job.done` / `job.failed` 발행 로그 확인
3) Frontend에서 해당 프로젝트 페이지 접속 후 노드 상태 변경 확인

---

## 5. 보완 제안 (선택)

- STOMP 연결 시 JWT 전달 및 구독 권한 검증 추가
- 프로젝트 멤버가 아닌 경우 `/topic/projects/{id}` 구독 차단

---

## 6. 추가 점검 (다운로드 UI)
- Backend에는 다운로드 API가 있지만, Frontend에는 다운로드 버튼/링크가 없음
- WS 이벤트 수신 후 결과를 확인할 수 있는 UI 접근 경로 추가 필요
- 관련 API:
  - GET /api/nodes/{id}/content
  - GET /api/projects/{projectId}/export/file
  - GET /api/scenes/{sceneId}/export/file

## 7. 추가 점검 (WS payload의 S3 presigned URL)
- job.done payload에는 resultUrl이 포함됨 (S3 사용 시 presigned URL)
- FE는 fetchProtectedBlobUrl에서 apiClient를 사용 (Authorization 헤더 자동 추가)
- S3/LocalStack로의 cross-origin 요청 + Authorization 헤더 조합은 CORS/서명 오류를 유발할 수 있음
- 대응 방안:
  - payload에 presigned URL 대신 /api/nodes/{id}/content 사용
  - 또는 절대 URL은 apiClient 없이 직접 fetch
