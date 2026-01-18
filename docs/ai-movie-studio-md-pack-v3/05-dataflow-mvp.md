# 05. 3주차 MVP 데이터 플로우(엔드 투 엔드)

이 문서는 "버튼을 눌렀을 때 데이터가 어디로 흘러가고, 누가 무엇을 업데이트 하는지"를 단계별로 정리합니다.

---

## Flow A) 시나리오 생성 → 씬 자동 생성
1. FE: `POST /api/projects/{projectId}/scenario/prompt/generate` (시나리오 프롬프트 생성)
2. FE: `POST /api/projects/{projectId}/scenario/scenes/generate` (프롬프트 기반 씬 리스트 생성)
3. API Server
   - LLM 호출(Gemini 등)
   - 결과(줄거리/씬 리스트) 파싱
   - DB: `scenes` 생성 (+ 선택적으로 기본 `scene_nodes` 생성)
4. FE
   - 응답으로 씬 목록을 받고 프로젝트 상세에 표시

---

## Flow B) 이미지 생성 Job
1. FE: "마스터 이미지 생성" 클릭
2. FE: `POST /api/nodes/{nodeId}/generate` (노드 타입에 따라 이미지/영상 생성)
3. API Server(Dispatcher)
   - 권한/입력 검증
   - DB: `jobs` 생성(`pending`)
   - Redis Streams: `ai:image:request`에 메시지 발행
   - FE에 `{jobId}` 응답
4. Image Worker
   - Streams에서 메시지 읽기(`XREADGROUP`)
   - DB: job 상태를 `running`으로 업데이트
   - 외부 API 호출 → 결과 파일 저장(S3)
   - DB: 결과 asset 레코드 저장 + job `succeeded` 또는 `failed`
   - Streams ACK
5. API Server
   - job 상태 변경 감지 후 WebSocket으로 `job.done/job.failed` 이벤트 브로드캐스트
6. FE
   - WS 이벤트를 받아 노드 배지/토스트 업데이트
   - 필요 시 `GET /api/ai/jobs/{jobId}` 폴링 fallback

---

## Flow C) 영상 생성 Job
- Flow B와 동일한 패턴
- Stream: `ai:video:request`
- Video Worker는 **Mock 모드**를 지원(데모용 샘플 파일로 성공 처리 가능)

---

## Flow D) 영상 확정 → 타임라인 반영
1. FE: "이 영상 확정" 클릭
2. FE: `POST /api/nodes/{nodeId}/confirm`
3. API Server
   - DB 제약(shot당 1개 confirm, active master 유일성 등)
   - DB: confirmedVideoId 업데이트
4. FE
   - `GET /api/projects/{projectId}/timeline`로 확정된 클립만 타임라인 표시

---

## Flow E) 병합(concat) Job → 다운로드
1. FE: "내보내기/병합" 클릭
2. FE: `POST /api/scenes/{sceneId}/merge` (씬 단위) 또는 `POST /api/projects/{projectId}/merge` (프로젝트 단위)
3. API Server(Dispatcher)
   - DB: merge job 생성
   - Streams: `ai:merge:request` 발행
4. Merge Worker(FFmpeg)
   - 타임라인 클립 수집(다운로드 or presigned URL)
   - FFmpeg concat 실행
   - 결과 mp4 업로드(S3)
   - DB: export 레코드 저장 + job 성공/실패 업데이트
5. FE
   - job 완료 이벤트 수신 → `resultUrl`로 다운로드

---

## 최소 이벤트 스키마(권장)

### WebSocket: `job.done`
```json
{
  "type": "job.done",
  "data": {
    "jobId": "...",
    "target": { "type": "NODE", "id": 301 },
    "resultUrl": "https://..."
  }
}
```

### WebSocket: `job.failed`
```json
{
  "type": "job.failed",
  "data": {
    "jobId": "...",
    "target": { "type": "NODE", "id": 301 },
    "error": {
      "code": "...",
      "message": "..."
    }
  }
}
```
