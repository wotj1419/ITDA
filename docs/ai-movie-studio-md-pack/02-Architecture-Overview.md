# 🧩 Architecture Overview (텍스트 다이어그램 + 용어 정의)

> 대상: Redis Streams / Worker / Dispatcher / WebSocket이 처음인 팀원

---

## 1) 전체 구조 한 장 (텍스트 다이어그램)

아래가 이번 프로젝트의 **MVP(3주차) 기준 최소 구조**입니다.

```
┌──────────────────────────────────────────────────────────────────────┐
│                                Frontend                               │
│      Vue (Dashboard / Project / Scene Editor(Vue Flow) / Timeline)     │
│                                                                      │
│  (A) REST API 호출                     (B) WebSocket 구독             │
│      - 로그인/프로젝트/노드/병합 요청        - job.done/job.failed 등     │
└───────────────▲───────────────────────────────▲───────────────────────┘
                │                               │
                │ HTTP                           │ WS
                │                               │
┌───────────────┴───────────────────────────────┴───────────────────────┐
│                         API Server (Spring Boot)                       │
│  - 인증/도메인 API(프로젝트/씬/노드/타임라인)                             │
│  - Job 생성/조회 API                                                    │
│  - Dispatcher(큐에 작업을 넣는 역할)                                      │
│  - WebSocket 서버(상태 변경 이벤트를 FE로 push)                            │
└───────────────▲────────────────────────────────────────────────────────┘
                │
                │ enqueue
                │
┌───────────────┴────────────────────────────────────────────────────────┐
│                      Redis Streams (Job Queue)                          │
│   Streams: ai:image / ai:video / media:merge                             │
│   Consumer Groups: image-workers / video-workers / merge-workers          │
└───────────────▲────────────────────────────────────────────────────────┘
                │ XREADGROUP
                │
┌───────────────┴────────────────────────────────────────────────────────┐
│                               Workers                                   │
│  Image Worker (Gemini)  |  Video Worker (Veo/Mock)  |  Merge Worker(FFmpeg)
│  - 작업 수행             |  - 작업 수행              |  - concat/export
│  - 결과를 Storage 저장    |  - 결과를 Storage 저장     |  - 결과를 Storage 저장
│  - DB에 Job 상태 업데이트 |  - DB에 Job 상태 업데이트  |  - DB에 Job 상태 업데이트
└────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────┐     ┌─────────────────────┐
│   MySQL (Domain + Jobs)    │     │  MinIO/S3 (Assets)   │
│ - projects/scenes/nodes     │     │ - images/videos/export│
│ - jobs(job_type/status/...) │     │ - presigned url       │
└───────────────────────────┘     └─────────────────────┘
```

---

## 2) 용어 정의 (팀 공통)

| 용어 | 정확한 의미 | 우리 프로젝트에서의 역할 |
|---|---|---|
| **API Server** | 스프링 서버(HTTP + WS) | 유저 요청 처리, DB/권한/도메인 로직, Job 생성/조회, WS 푸시 |
| **Job** | “오래 걸리거나 실패 가능성이 있는 작업”을 DB에 기록한 것 | 이미지/영상 생성, 병합을 **비동기**로 처리하기 위해 필요 |
| **Queue (Redis Streams)** | Job을 Worker에게 전달하는 메시지 통로 | Worker 프로세스 분리로 **병렬 개발/확장** 가능 |
| **Worker** | Queue에서 Job을 받아 실제 작업을 수행하는 프로세스 | Image/Video/Merge가 각각 담당 |
| **Dispatcher** | “Job을 Queue에 넣는” 코드(추상화) | API Server 안에 존재. `enqueue(job)`만 제공 |
| **Local Executor** | Queue 없이 “로컬 스레드/프로세스”로 Job을 실행하는 방식 | W3 초반 Lead가 혼자 E2E 검증용(옵션) |
| **WebSocket(WS)** | 서버→클라이언트 실시간 이벤트 푸시 채널 | Job 완료/실패를 즉시 UI에 반영(폴링 대체) |

> 핵심: **Dispatcher/Queue/Worker는 “기능 분리”가 아니라 “실행 분리(프로세스 분리)”를 위한 패턴**입니다.

---

## 3) WebSocket은 왜 필요한가?

- 이미지/영상 생성/병합은 **수 초~수 분**이 걸릴 수 있고, 실패도 잦습니다.
- FE가 매번 `GET /jobs/{id}`로 폴링하면:
  - 서버 부하↑
  - UI 반응성↓
  - 동시 사용자 늘면 비용↑

따라서 MVP에서는:
- **요청은 REST**(`POST /generate`, `POST /merge`)
- **상태 반영은 WS**(`job.done`, `job.failed`)
- WS가 끊길 경우에만 **폴링 fallback**을 둡니다.

---

## 4) MVP 플로우 3개 (API ↔ Queue ↔ Worker ↔ WS)

### 4.1 이미지 생성 플로우

1) FE: `POST /api/ai/images` (nodeId, prompt)
2) API 서버:
   - Job 레코드 생성(status=QUEUED)
   - `Dispatcher.enqueue(job)` → stream `ai:image`에 메시지 발행
   - FE에 `{jobId}` 즉시 응답
3) Image Worker:
   - `XREADGROUP`으로 메시지 수신
   - Gemini 호출 → 결과 파일을 MinIO/S3 업로드
   - DB에 outputUrl 저장 + Job status=SUCCEEDED(or FAILED)
   - `XACK`
4) API 서버:
   - Worker가 호출하는 내부 endpoint(완료/실패 콜백)를 받음
   - WS로 `job.done/job.failed` 발행
5) FE:
   - WS 수신 후 노드 상태 배지/토스트/썸네일 업데이트

### 4.2 영상 생성 플로우
- 위와 동일, stream만 `ai:video`, worker만 Video Worker
- W3에서는 데모가 막히지 않도록 **Mock mode**(가짜 결과 생성)를 반드시 지원

### 4.3 병합(concat) 플로우
1) FE: `POST /api/timeline/merge`
2) API 서버:
   - 타임라인 클립 목록 조회(확정된 video만)
   - merge Job 생성 → stream `media:merge`에 발행
3) Merge Worker:
   - FFmpeg concat(무오디오/H.264/720p 제한)
   - 결과 업로드 + exportUrl 저장 + ACK
4) API 서버:
   - WS로 `merge.done`(또는 `job.done`에 type=MERGE)
5) FE:
   - 다운로드 버튼 활성화

---

## 5) “Job 상태 변경 → WS 발행”은 누가 하나?

### ✅ MVP 권장: API 서버가 WS를 **단일 책임**으로 가진다

- Worker는 **DB 업데이트 + 완료 콜백 호출**까지만 한다.
- WS 발행은 API 서버가 전담한다.

이유:
- FE가 “WS 연결 대상”을 하나만 알면 됨
- 인증/보안 정책을 한 곳(API)에서 통제
- 이벤트 스키마를 중앙에서 고정 가능

> WS 이벤트 스키마는 `04-WebSocket-Event-Schema.md`에서 고정합니다.

---

## 6) 프로세스 분리 방식 (docker compose)

- 레포는 하나여도 OK, **실행 프로세스만 분리**하면 됩니다.

예시:
- `api-server` : 스프링(HTTP+WS)
- `worker-image` : 스프링/자바 프로세스(프로필로 실행)
- `worker-video`
- `worker-merge`
- `redis`, `mysql`, `minio`

---

## 7) Local Executor/@Async는 어디까지 쓰나?

- 팀 병렬 개발/운영 안정성의 최종 형태는 **Streams Worker**입니다.
- 다만 Lead가 “혼자 빠르게 E2E 검증”하려면 W3 초반에 **Local Executor**가 유용합니다.

권장 운영:
- **W3 D1~D2:** LocalAsyncDispatcher(모킹 가능)로 “E2E 1회”
- **W3 D3~D5:** RedisStreamsDispatcher로 전환 + 팀원 Worker를 연결

---

## 8) 다음 문서로 연결

- 구현 규칙 + 샘플 코드: `03-Dispatcher-Queue-Worker-Guide.md`
- WS 이벤트 스키마: `04-WebSocket-Event-Schema.md`
- 로컬 실행 가이드: `05-Local-Dev-Runbook.md`
- 최소 API 목록: `06-Minimum-API-Contract.md`
