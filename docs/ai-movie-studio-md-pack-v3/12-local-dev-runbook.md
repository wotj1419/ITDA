# 12. Local Dev Runbook (Docker Compose + 실행 순서)

> 목표: 팀원이 로컬에서 **API + Redis + Worker + WS**를 10분 안에 띄우고, 샘플 Job을 1회 성공시킨다.

---

## 1) 사전 요구사항
- Docker / Docker Compose
- JDK 17+
- Node 18+ (FE)

---

## 2) 로컬 표준 서비스 (Docker Compose)

### 2.1 compose에 들어갈 것
- MySQL
- Redis (Streams 사용)
- S3 (로컬은 LocalStack) — 개발용 자산 저장

> Jenkins/배포는 W3에서 표준화 단계로 분리

### 2.2 예시 `docker-compose.yml` (최소)

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: aimovie
    ports:
      - "3306:3306"

  redis:
    image: redis:7.2
    command: ["redis-server", "--appendonly", "yes"]
    ports:
      - "6379:6379"

  localstack:
    image: localstack/localstack:latest
    environment:
      SERVICES: s3
      AWS_DEFAULT_REGION: ap-northeast-2
    ports:
      - "4566:4566"
    volumes:
      - localstack_data:/var/lib/localstack

volumes:
  localstack_data:
```

### 2.3 실행

```bash
docker compose up -d
```

---

## 3) 애플리케이션 구성(권장)

### 3.1 Spring profiles
- `local`: DB/Redis/S3(LocalStack) 로컬 접속
- `dev`: 공유 개발서버 접속

### 3.2 구성 모듈(예시)
- `api-server` : 스프링 API + WS
- `worker-image` : 이미지 Worker
- `worker-video` : 영상 Worker
- `worker-merge` : FFmpeg Worker

> 한 repo에서 멀티모듈로 나눠도 되고, 단일모듈에서 `spring.profiles.active=worker-image` 같이 구분해도 됩니다.

---

## 4) 실행 순서(로컬)

1) 인프라 올리기
```bash
docker compose up -d
```

2) API 서버 실행
```bash
./gradlew :api-server:bootRun
```

3) Worker 실행(본인 담당만)
```bash
./gradlew :worker-image:bootRun
# or :worker-video:bootRun / :worker-merge:bootRun
```

4) FE 실행
```bash
pnpm i
pnpm dev
```

---

## 5) 빠른 검증(샘플 Job 1회)

### 5.1 시나리오 생성 → 씬 생성
- FE에서 버튼으로 호출하거나, 아래처럼 curl로 호출

```bash
# 로그인 후 access token 확보했다고 가정
curl -X POST "http://localhost:8080/api/projects" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"demo"}'
```

### 5.2 이미지 생성 Job enqueue
```bash
curl -X POST "http://localhost:8080/api/nodes/<NODE_ID>/generate" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"a cinematic master shot"}'
```

### 5.3 확인 포인트
- DB `jobs.status` : pending → running → succeeded
- S3에 결과 파일 업로드
- FE에서 WebSocket 이벤트로 노드 배지 변경

---

## 6) 흔한 이슈 체크리스트

### 6.1 Worker가 메시지를 안 먹는다
- consumer group 생성 여부
- `XREADGROUP` block 설정
- `ACK` 누락 시 pending 쌓임(`XPENDING`)

### 6.2 WS가 안 들어온다
- FE가 연결한 WS URL 확인
- 인증 토큰 전달 방식(쿼리/헤더) 확인
- 서버에서 publish 호출이 실제로 되는지 로그 확인

### 6.3 S3 업로드가 실패한다
- endpoint/port(4566) 확인(LocalStack)
- bucket 생성 여부
- presigned URL 생성 시 region/credentials 확인

---

## 7) 팀 공통 규칙(로컬 개발)
- **Job enqueue는 무조건 Dispatcher를 통해서만** 한다.
- Worker는 Job 상태 전이를 반드시 지킨다(03 문서 참고).
- WS 이벤트 스키마는 04 문서 고정.
