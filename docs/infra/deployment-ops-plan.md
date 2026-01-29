# CI/CD·배포·운영 통합 계획서 (PRD 7.3 기준)

## 목적

PRD 7.3 아키텍처를 기준으로 CI/CD 파이프라인과 배포·운영 구성(Compose)을 한 문서로 정리한다.

## 범위

- 대상: `itda-backend`, `itda-frontend`
- CI/CD 도구: Jenkins
- 컨테이너 레지스트리: Docker Hub 또는 AWS ECR
- 배포 인프라: AWS EC2 + Docker Compose
- 모니터링: Prometheus + Grafana

## 전제/의존

- Jenkins CI 파이프라인 정상 동작
- 도메인 및 TLS 인증서 확보
- 운영 계정/권한 및 시크릿 저장소 준비

## 현재 상태

- Jenkinsfile: backend 빌드 + frontend 빌드 후 단일 API 이미지 빌드/배포
- Deploy 스테이지: `docker compose up -d nginx api mysql redis`
- 배포 구성: `deploy/docker-compose.yml` 사용, 80 포트만 공개
- Worker/Observability는 compose `profiles`로 선택 실행
- 프론트 정적 파일은 `deploy/nginx/html`로 복사하여 Nginx에서 서빙
- TLS 인증서는 `deploy/nginx/certs/`에 배치 예정
- Redis Streams: 기본 프로필에 `redis-streams` 포함 → **API 서버가 dispatcher + consumer 역할을 함께 수행**
- 운영 컨테이너 기준으로 worker는 미기동(분리 미완)

## Redis Streams 처리 현황

- Redis Streams 활성 프로필이 API 서버에 포함되어 있어, 현재는 **API가 직접 소비**하는 구조
- 컨슈머 ACK가 **성공/실패 관계없이 호출**되어 실패 메시지 재처리가 어렵다
- Pending 메시지 회수(XPENDING/XCLAIM) 로직이 없어 소비자 장애 시 PEL에 쌓일 수 있다
- 컨슈머 그룹 생성 시 `ReadOffset.latest()` 사용 → 기존 메시지 스킵 가능성 있음

## 개선 방안(권장)

1) 역할 분리
   - API: enqueue 전용(Dispatcher만)
   - Worker: consume/execute 전용(Consumer + JobExecutor)
2) ACK 정책 개선
   - 성공 시 ACK, 실패 시 미ACK(재처리 가능)
3) Pending 회수 루프 추가
   - XPENDING/XCLAIM 기반으로 오래 묶인 메시지 재처리
4) 그룹 생성 오프셋 재검토
   - 최초 생성 시 `0-0`부터 소비하도록 변경 고려

## 목표 아키텍처 요약

```
Developer -> Git (PR/merge)
  -> Jenkins (CI)
    -> Docker Build
    -> Registry (Docker Hub/ECR)
  -> Jenkins (CD)
    -> EC2 (Docker/Compose)
      -> Nginx -> Spring API
      -> Worker (image/video/merge)
      -> MySQL / Redis / S3(또는 MinIO)
  -> Prometheus/Grafana (모니터링)
```

## 표준 파이프라인 흐름(권장)

1) Checkout
2) Backend Build/Test
3) Frontend Install/Build
4) Docker Build
5) Push to Registry
6) Deploy (브랜치 조건)

## CI 파이프라인 상세

### 트리거

- develop: PR merge 시 자동 빌드 + dev 이미지 푸시
- main: release/merge 시 빌드 + prod 이미지 푸시 + 배포

### 단계

1) Checkout
- git branch/commit 체크아웃

2) Build/Test
- `./gradlew clean test`
- `./gradlew bootJar`
- 프론트 `npm ci` + `npm run build`

3) Docker Build
- 이미지 생성 (API, Worker 분리 가능)

4) Push to Registry
- Docker Hub 또는 ECR 푸시

5) (옵션) 이미지 스캔
- Trivy 등 보안 스캔 도입 가능

## 배포(CD) 계획

### 배포 대상

- EC2 (Docker 기반)
- Nginx reverse proxy
- Spring API 컨테이너
- Worker 컨테이너 (Redis Streams 구독)
- MySQL / Redis / Observability

### 배포 방식

- Jenkins에서 EC2로 SSH 접속
- 최신 이미지 pull 후 컨테이너 재기동
- 배포 스크립트/compose로 서비스 관리

### 배포 단계 (예시)

1) EC2 접속
2) `docker login` (Registry)
3) `docker pull itda-backend-api:<tag>`
4) `docker pull itda-backend-worker:<tag>` (선택)
5) `docker compose up -d`
6) 헬스체크 확인

## 이미지/태깅 규칙

- 이미지명: `itda-backend-api`, `itda-backend-worker`
- 태그: `{branch}-{shortSha}` + `{env}-latest`
- 예: `develop-a1b2c3d`, `dev-latest`, `prod-latest`

## Docker Compose 배포 구성

### 디렉터리 구조 예시

- deploy/docker-compose.yml
- deploy/.env
- deploy/nginx/conf.d/app.conf
- deploy/nginx/certs/
- deploy/prometheus/prometheus.yml
- deploy/grafana/provisioning/
- data/mysql/
- data/redis/

### 포트/방화벽 전략

- 외부 공개: 22/tcp, 443/tcp (필요 시 80/tcp)
- 내부 전용: API, MySQL, Redis, Prometheus, Grafana

### 서비스 구성 요약

- Nginx: TLS 종료 + reverse proxy + 정적 파일 서빙
- API: Spring Boot 컨테이너, 내부 포트로 노출
- Worker: image/video/merge 컨테이너 (옵션)
- MySQL/Redis: 내부 전용, 데이터 볼륨 유지
- Prometheus/Grafana: 관측 스택 (옵션)

### Nginx 라우팅 예시

```nginx
server {
  listen 443 ssl;
  server_name i14xxxx.p.ssafy.io;

  ssl_certificate /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;

  location / {
    proxy_pass http://api:18080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }

  location /ws/ {
    proxy_pass http://api:18080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
  }
}
```

### .env 키 예시

```
API_IMAGE=
WORKER_IMAGE=
WORKER_VIDEO_IMAGE=
WORKER_MERGE_IMAGE=

MYSQL_DATABASE=
MYSQL_USER=
MYSQL_PASSWORD=
MYSQL_ROOT_PASSWORD=

DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=

GCP_PROJECT_ID=
GCP_LOCATION=

S3_ENDPOINT=
S3_REGION=
S3_ACCESS_KEY=
S3_SECRET_KEY=
S3_BUCKET=
S3_PATH_STYLE=

GRAFANA_ADMIN_PASSWORD=
```

## 환경 준비 체크리스트

- 서버
  - EC2 스펙/OS/타임존 확정
  - 보안그룹: 80/443/22, 내부 서비스 포트 제한
  - 디스크 용량/로그 보관 정책
- 네트워크
  - 도메인, DNS 레코드, TLS 인증서 적용
  - 내부 서비스 간 통신 포트 정의
- 계정/시크릿
  - DB/Redis/S3/AI Provider 자격증명
  - JWT Secret 및 WebSocket 인증키
  - Jenkins Credentials 등록

## 운영 검증 시나리오

- AI 이미지 생성 플로우: Job 생성 -> Worker 처리 -> 저장 -> job.done
- WebRTC 협업 플로우: Signaling -> P2P 연결 확인
- 영상 병합 플로우: Job 생성 -> FFmpeg 병합 -> 결과 저장

## 모니터링/로그

- 지표: API/Worker 응답시간, 실패율, 큐 적체량, 처리 시간
- 로그: API/Worker/Nginx 통합 로그 수집
- 알림: 장애/지연/에러율 임계치 알림

## 데이터 보호/복구

- MySQL 백업 정책 (일/주 단위, 보관 기간)
- Redis 지속성 설정 및 복구 절차
- S3/MinIO 버킷 정책 및 보존 규칙

## 장애 대응/롤백

- 배포 실패 시 이전 버전 롤백 절차
- Worker 재시작/재시도 정책 확인
- DLQ 적재 시 재처리 절차

## 운영 체크리스트

- [ ] Jenkins Job 생성 및 Credentials 설정
- [ ] Docker 이미지 빌드/푸시 성공 확인
- [ ] EC2 배포 스크립트/compose 정리
- [ ] dev/prod 환경 변수 분리
- [ ] 헬스체크/롤백 절차 확인
- [ ] 모니터링 대시보드 연결

## 결정 사항 (2026-01-21)

- Deployment: Docker (Compose-based)
- Object storage: S3 (cross-account allowed)
