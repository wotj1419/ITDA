# 04. 코어 아키텍처(3주차 MVP 기준)

## 1) 한 줄 요약
- **API Server(Spring)**: 요청 수신 → 검증 → **Job 생성/Queue 발행(Dispatch)** → 상태조회/이벤트 푸시
- **Redis Streams**: Job Queue(작업 대기열)
- **Workers(별도 컨테이너)**: Queue에서 Job을 꺼내 **실제 작업(AI 호출/FFmpeg)**을 수행

---

## 2) 컴포넌트 설명(용어 포함)

### 2.1 API Server (Spring Boot)
- 역할
  - Auth, Project/Scene/Node/Timeline CRUD
  - AI 작업 요청(이미지/영상/병합)을 받으면 **Job 레코드를 만들고 Queue에 메시지를 발행한 뒤** `jobId`를 즉시 응답한다.
  - FE가 작업 진행을 알 수 있도록 **Job 상태 조회 API**와 **WebSocket 이벤트**를 제공한다.

- 3주차 기준으로 API 서버가 제공해야 하는 최소 엔드포인트 예시(APIdocs 기준)
  - `POST /api/nodes/{id}/generate` → `{jobId}` (노드 타입에 따라 이미지/영상 생성)
  - `POST /api/scenes/{id}/merge` → `{jobId}`
  - `POST /api/projects/{id}/merge` → `{jobId}`
  - `GET  /api/ai/jobs/{jobId}` → `PENDING|RUNNING|SUCCEEDED|FAILED` + output

---

### 2.2 Dispatcher (API 서버 내부 모듈)
- "Dispatcher"는 별도의 서버가 아니라, **API 서버 안의 서비스/유스케이스 레이어**를 말한다.
- 핵심 책임
  1. 입력/권한 검증 (예: 프로젝트 소유/권한)
  2. Job 생성(DB) — 초기 상태 `PENDING`
  3. Redis Streams에 메시지 발행(=dispatch)
  4. 호출자에게 `jobId` 반환

> 즉, "API 서버를 분리"하자는 얘기가 아니라 **역할을 명확히 하자**는 의미다.

---

### 2.3 Redis Streams (Job Queue)
- 목적: 무거운 작업을 API 서버에서 분리하고, Worker와의 결합을 낮추기 위함
- 핵심 개념
  - **Stream**: 토픽/큐 이름 (예: `ai:image:request`)
  - **Consumer Group**: 여러 Worker가 같은 Stream을 나눠 먹게 하는 그룹
  - **ACK**: 작업 완료 후 "이 메시지 처리 끝" 표시
  - **Pending**: 어떤 Worker가 가져갔는데 ACK 안 된 메시지 목록(장애/타임아웃 때 중요)

- 왜 Streams인가?
  - Worker를 늘리면(복제하면) 처리량을 올릴 수 있음
  - 작업이 오래 걸려도 API 서버가 블로킹되지 않음
  - 장애 시 재처리(pending reclaim) 설계가 쉬움

---

### 2.4 Workers (별도 컨테이너/프로세스)
- 정의: Redis Streams에서 메시지를 읽어 **실제 작업을 수행하는 실행기**
- 형태
  - Spring Boot Worker 앱(권장) 또는 Python Worker도 가능
  - docker compose에서 `worker-image`, `worker-video`, `worker-merge`처럼 분리

- 공통 책임
  1. Streams에서 메시지 읽기 (`XREADGROUP`)
  2. Job 상태를 `RUNNING`으로 변경(시작 시간 기록)
  3. 실제 처리 수행 (AI 호출/FFmpeg)
  4. 결과 파일 Storage 저장 + DB 기록
  5. Job 상태를 `SUCCEEDED` 또는 `FAILED`로 업데이트
  6. 메시지 `ACK`

- 담당 분리(현재 일정표 기준)
  - 이미지 Worker: 이용호
  - 영상 Worker: 김은서
  - 병합(FFmpeg) Worker: 장현준

---

### 2.5 WebSocket (실시간 이벤트)
WebSocket은 "작업 결과를 실시간으로 FE에 알려주기" 위한 채널이다.
- 주요 용도(3~4주차)
  - `job.done`, `job.failed` 이벤트를 FE에 푸시
  - FE는 폴링 없이도 노드 배지/토스트를 즉시 갱신
- 주요 용도(5주차)
  - 채팅
  - Presence/Cursor/Selection 이벤트
  - WebRTC 시그널링(offer/answer/candidate)

> WebSocket은 큰 파일을 전송하는 용도가 아니라, **작은 이벤트 메시지**를 주고 받는 용도다.

---

### 2.6 Storage(S3) + DB(MySQL)
- 결과 파일(이미지/영상/병합 결과)은 Storage에 저장
- DB에는 메타데이터만 저장
  - 파일 키, 길이, 해상도, 생성 모델, 소요시간
  - Job 상태, 에러 메시지
- 다운로드는 presigned URL로 제공

---

## 3) 텍스트 아키텍처 다이어그램

```text
[Browser/FE]
  |  HTTP (CRUD / Job 요청)
  v
[API Server: Spring]
  |  (Dispatcher: Job 생성 + Redis Streams 발행)
  |-------------------------------.
  |                               |
  |  WebSocket (job.done/failed)  |
  '---------------> [Browser/FE]  |
                                  |
                         Redis Streams
                                  |
              .-------------------+-------------------.
              |                   |                   |
        [Worker: Image]     [Worker: Video]     [Worker: Merge]
              |                   |                   |
              v                   v                   v
             S3                 S3                 S3
              |                   |                   |
              '-------------------+-------------------'
                                  |
                                  v
                                MySQL
```

---

## 4) @Async(단일 프로세스 비동기) vs Streams+Worker(분산 비동기)

### 4.1 @Async를 쓰는 경우
- "API 서버 안에서" 짧은 비동기 작업을 실행
- 장점: 구현이 빠름
- 단점
  - 서버 내려가면 작업이 같이 끊김
  - 긴 작업/외부 API 지연에 취약
  - 팀 병렬화(작업 분담) 관점에서 경계가 흐려짐

### 4.2 Streams+Worker를 쓰는 경우(우리가 가는 방향)
- 무거운 작업을 "Worker"로 분리하고 Queue로 연결
- 장점
  - 실패 재처리/모니터링 설계가 쉬움
  - Worker만 확장(복제)해서 성능을 올릴 수 있음
  - 팀원이 Worker를 독립 개발 가능(병렬화 최적)

### 4.3 "혼자 빠르게 MVP" 만들 때는 어떻게?
- API 서버 + Redis + Worker를 **docker compose로 같이 띄우면** 혼자서도 E2E 검증 가능
- 즉, "혼자 빠르게"도 Streams 구조로 가능하며, 팀원이 합류하면 Worker를 나눠 맡기면 된다.

---

## 5) docker compose 권장 구성
- `api`: Spring API Server
- `mysql`: 메인 DB
- `redis`: Streams
- `localstack`: S3(로컬)
- `worker-image`: 이미지 생성 Worker
- `worker-video`: 영상 생성 Worker
- `worker-merge`: FFmpeg 병합 Worker

Worker는 로컬에서도 컨테이너로 띄우고, 운영에서도 동일하게 배포하면 된다.
