# 포팅 매뉴얼

## I. 개요

### 2.1 프로젝트 개요
AI Movie Studio는 시나리오 작성, 씬/노드 편집, AI 이미지/영상 생성, 타임라인 병합을 지원하는 웹 기반 영화 제작 서비스입니다.

### 2.2 프로젝트 사용 도구
- Frontend: Vue 3, TypeScript, Vite, Pinia, Vue Router, Vue Flow
- Backend: Java 17, Spring Boot 3.2.x, MyBatis, Spring Security, JWT, Redis
- AI: Google Vertex AI (Gemini/Veo)
- Infra: Docker, Docker Compose, Nginx, Jenkins
- Data/Storage: MySQL 8.0, Redis 7.2, S3(또는 LocalStack)
- Media: FFmpeg

### 2.3 개발환경
- OS: Windows/Ubuntu (Docker 실행 가능 환경)
- JDK: 17
- Node.js: 22.x
- npm: 10+
- Docker Engine + Docker Compose v2

### 2.4 외부 서비스
- Google Cloud Vertex AI
- AWS S3 (또는 S3 호환 스토리지)
- Jenkins (CI/CD)
- Mattermost Webhook (알림)
- Grafana (모니터링 대시보드)

상세 설정/운영 정보는 `exec/external-services.md` 문서를 참고합니다.

### 2.5 .gitignore 처리한 핵심 키들
- `.env`, `.env.local`, `.env.*.local`
- `*-key.json`, `*.json.key`
- `uploads/`, `dist/`, `build/`, `logs/`

## III. 빌드

### 3.1 환경변수 형태
아래는 값이 비어 있는 예시 `.env`입니다.

```env
# Database Configuration
DB_USERNAME=
DB_PASSWORD=

# Redis Configuration
REDIS_HOST=
REDIS_PORT=

# JWT Configuration
JWT_SECRET=

# storage.provider: LOCAL 또는 S3 (이미지/영상 등 생성 결과 저장 위치 선택)
# - LOCAL
#   - 로컬 파일로 저장 (upload-dir 기준, 기본 ./uploads)
#   - 별도 S3 설정 불필요
# - S3
#   - S3 또는 LocalStack에 업로드
#   - 아래 S3_* 설정 필요
STORAGE_PROVIDER=
#STORAGE_PROVIDER=
S3_REGION=
S3_BUCKET=
S3_ACCESS_KEY=
S3_SECRET_KEY=
S3_ENDPOINT=
S3_PATH_STYLE=
S3_PRESIGN_EXPIRE_SECONDS=

# Vertex AI Configuration
GCP_PROJECT_ID=
GCP_LOCATION=
VERTEX_TEXT_MODEL=
VERTEX_TIMEOUT_MS=
GOOGLE_APPLICATION_CREDENTIALS=

GEMINI_IMAGE_MODEL=
GEMINI_SAMPLE_COUNT=
GEMINI_ASPECT_RATIO=
GEMINI_TIMEOUT_MS=
# stub mode on/off (true -> 실제 gemini 호출 x / false -> 실제 API 호출)
GEMINI_STUB=
```

### 3.2 빌드하기
1. 개발 환경(LocalStack + Redis) 실행
```bash
docker compose -f docker-compose.s3.yml up -d redis localstack localstack-init
docker compose -f docker-compose.s3.yml exec localstack awslocal s3 mb s3://itda-local
```

2. 로컬 인프라 전체 실행(선택)
```bash
docker compose -f docker-compose.s3.yml up -d
```

3. 백엔드 빌드
```bash
cd itda-backend
./gradlew clean assemble -x test
```
Windows:
```powershell
cd itda-backend
.\gradlew.bat clean assemble -x test
```

4. 프론트엔드 빌드
```bash
cd itda-frontend
npm ci
npm run build
```

### 3.3 배포하기
1. 배포 파일 준비
- `deploy/docker-compose.yml`
- `deploy/nginx/conf.d/app.conf`
- `itda-frontend/dist`
- `deploy/.env`

2. Nginx 구성 확인(운영 기준)
- 외부 리버스 프록시: `/opt/itda/infra/nginx/conf.d/itdas.conf` (443 SSL 종료)
- 서비스 Nginx: `deploy/nginx/conf.d/app.conf` (80, SPA + `/api` + `/ws` 라우팅)
- 현재 구조가 `443 Nginx -> itda-nginx:80 -> api:18080` 이므로 `app.conf`는 필수입니다.
- `app.conf`를 제거하려면 외부 443 Nginx가 정적 파일, `/api`, `/ws`를 모두 직접 처리하도록 구조를 변경해야 합니다.

예시) `/opt/itda/infra/nginx/conf.d/itdas.conf`
```nginx
server {
    listen 443 ssl default_server;
    server_name itdas.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/itdas.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/itdas.duckdns.org/privkey.pem;

    resolver 127.0.0.11 valid=10s ipv6=off;
    set $app_upstream itda-nginx;

    location / {
      proxy_pass http://itda-nginx:80;
      proxy_set_header Host $host;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ws/ {
      proxy_pass http://itda-nginx:80;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
    }

    location /grafana/ {
      proxy_pass http://itda-grafana:3000;
      proxy_set_header Host $host;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

예시) `/opt/itda/infra/nginx/conf.d/proxy.conf` (Jenkins)
```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

server {
    listen 443 ssl;
    server_name itdasj.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/itdasj.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/itdasj.duckdns.org/privkey.pem;

    location / {
      proxy_pass http://itda-jenkins:9000;
      proxy_set_header Host $host;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Forwarded-Host $host;
      proxy_set_header X-Forwarded-Port 443;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection $connection_upgrade;
      proxy_request_buffering off;
    }
}
```

3. 기본 서비스 기동
```bash
docker compose -f deploy/docker-compose.yml up -d nginx api mysql redis
```

4. 선택 서비스(Worker/모니터링) 기동
```bash
docker compose -f deploy/docker-compose.yml --profile worker up -d
docker compose -f deploy/docker-compose.yml --profile observability up -d
```

### 3.4 서비스 이용 방법
1. 브라우저 접속 후 회원가입/로그인
2. 프로젝트 생성
3. 시나리오/씬 생성
4. 씬 에디터에서 노드 생성 후 AI 생성 실행
5. 타임라인에서 씬 병합/프로젝트 병합
6. 최종 결과 영상 다운로드

주요 경로:
- API: `/api/*`
- Swagger: `/swagger-ui.html`
- WebSocket: `/ws`
