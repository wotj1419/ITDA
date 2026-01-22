# 전체 아키텍처 계획서 (PRD 7.3 기준)

## 목적

PRD 7.3 아키텍처를 기반으로 전체 시스템 구성, 데이터 흐름, 배포 토폴로지,
운영 관점까지 한 문서에 정리한다.

## 범위

- Client / API / Worker / Data / Infra 전체 구성
- 실시간 협업(WebRTC/WebSocket) 및 비동기 Job 처리 흐름
- 배포/모니터링/보안 구성

## 시스템 컨텍스트

```
[User Browser]
  - Web UI (Vue)
  - WebRTC P2P media
  - HTTPS API + WebSocket
        |
        v
[Edge]
  - Nginx (TLS, reverse proxy)
        |
        v
[Backend]
  - Spring Boot API
  - WebSocket (signaling, job events)
  - Job Dispatcher
        |
        v
[Data/Infra]
  - MySQL
  - Redis (Cache + Streams)
  - S3/MinIO (assets)
        |
        v
[Worker]
  - Image Worker (Gemini)
  - Video Worker (Veo)
  - Merge Worker (FFmpeg)
```

## 컴포넌트 구성

| 영역 | 구성 요소 | 역할 |
|---|---|---|
| Client | Web UI | 프로젝트/씬/노드 편집, 프롬프트 입력, 결과 조회 |
| Edge | Nginx | TLS 종료, API 라우팅, 정적 파일 전달 |
| API | Spring Boot | 인증, 프로젝트/씬/노드 API, Job 생성/조회 |
| Realtime | WebSocket | WebRTC signaling, job.done/job.failed 알림 |
| Queue | Redis Streams | 비동기 Job 큐 |
| Worker | Image/Video/Merge | AI 호출, FFmpeg 병합, 결과 저장 |
| Data | MySQL | 유저/프로젝트/노드/Job/Asset 저장 |
| Storage | S3/MinIO | 이미지/영상 파일 저장 |
| Observability | Prometheus/Grafana | 모니터링/알림 |

## 주요 데이터 흐름

### 1) AI 이미지 생성 흐름

```
Client -> API (create job)
API -> DB (generation_jobs INSERT)
API -> Redis Streams (ai:image)
Worker -> Redis Streams (consume)
Worker -> AI Provider (Gemini)
Worker -> Storage (S3/Local)
Worker -> DB (assets INSERT, job status SUCCEEDED)
API -> WebSocket (job.done)
Client -> API (job status poll/WS)
```

### 2) WebRTC 협업 흐름

```
Client A <-> WebSocket Signaling <-> Client B
Client A <-> P2P Media (WebRTC) <-> Client B
```

### 3) 영상 병합 흐름

```
Client -> API (merge request)
API -> DB (generation_jobs INSERT)
API -> Redis Streams (media:merge)
Worker -> FFmpeg -> Storage
Worker -> DB (job status SUCCEEDED)
API -> WebSocket (job.done)
```

## 배포 토폴로지 (초기)

```
EC2 (Docker)
  - Nginx
  - API Container
  - Worker Container(s)
  - MySQL
  - Redis
  - Prometheus/Grafana
```

확장 시:
- API/Worker 수평 확장
- Redis Streams 소비자 그룹 확장
- RDS/S3로 데이터 계층 분리

## 보안/권한

- JWT 인증 기반
- 프로젝트 멤버 권한 (Owner/Editor/Viewer)
- WebSocket 접속은 JWT 검증 + 프로젝트 권한 확인
- 민감 정보는 Secrets/Jenkins Credentials로 관리

## 장애/복구 전략

- Job 상태는 DB 기준
- Redis Streams at-least-once 대응 (idempotency, retry)
- Worker 실패 시 재시도 및 DLQ 적용
- 이전 이미지 태그로 롤백

## 운영/모니터링

- API/Worker 메트릭 수집 (응답 시간, 실패율)
- Job 처리 시간/대기 시간 모니터링
- DLQ 적재량 알림

## 미구현/예정 (현행 리포지토리 기준)

- Redis Streams 기반 Job Dispatcher (현재 LocalAsyncJobDispatcher 사용, `redis-streams` 프로파일 미구현)
- Image/Video/Merge Worker 실제 구현 및 JobExecutor 연결 (현재 TODO 상태)
- Gemini/Veo 실연동 및 생성 결과 저장 정책 확정 (스텁/미구현)
- Job 완료/실패 WebSocket 이벤트의 실제 전송 및 클라이언트 연동 검증
- TLS(443) 기반 Nginx 운영 구성 및 인증서 적용 (현재 compose는 80 포트)
- Observability 스택(Prometheus/Grafana) 운영 환경 적용 및 대시보드/알림 구성

## 결정 사항 / TODO

- AI Provider 호출 방식 (SDK/HTTP)
- S3 vs MinIO 운영 선택
- Worker 배포 수량 및 auto-scaling 기준
- 로그 수집 방식 (ELK/CloudWatch)
