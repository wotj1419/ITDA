# 13. 로컬/운영 Runbook(초안)

## 1) 로컬 Docker Compose 목표
- 팀원이 누구든 `docker compose up`으로
  - MySQL, Redis, S3(LocalStack)
  - API Server
  - worker-image / worker-video / worker-merge
  를 한 번에 띄울 수 있게 한다.

## 2) 서비스 분리 원칙
- API Server는 가볍게 유지(요청 처리 + job dispatch + websocket)
- 무거운 작업은 Worker로 분리(AI/FFmpeg)

## 3) 환경 변수(예시)
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`
- `GEMINI_API_KEY`, `VEO_API_KEY`
- `WS_BASE_URL`
- `TURN_URL`, `TURN_USER`, `TURN_PASS` (5주차 필요 시)

## 4) Jenkins 파이프라인 최소 스테이지(권장)
1. Checkout
2. Build
3. Test
4. Lint/Format(선택)
5. Docker build
6. Deploy(개발/운영 분리)

## 5) Redis Streams 운영 체크 포인트
- consumer group lag(처리 속도)
- pending 수(ACK 안 된 메시지)
- 평균 처리 시간(job duration)
- 실패율(job failed)

## 6) 장애/디버깅 빠른 체크
- Job이 pending에서 멈춤
  - worker가 실행 중인가?
  - stream / group 이름이 일치하는가?
  - pending이 쌓였는가?
- Job이 running에서 멈춤
  - 외부 API 타임아웃/쿼터?
  - 재시도 로직이 무한 루프?
  - worker 로그에 예외가 있는가?
## 5) Redis Streams 운영 체크(핵심)
- consumer group 별로
  - pending이 계속 늘지 않는지
  - 처리 지연(lag)이 계속 커지지 않는지
- 장애 시
  - worker 재시작
  - pending reclaim(재할당)

## 6) 로그/관측성(초안)
- Job 상태 변경 시 로그 1줄(상태, jobId, nodeId, elapsed)
- Worker별 처리 시간/실패율
- (가능하면) 대시보드로 보이게 만들기
