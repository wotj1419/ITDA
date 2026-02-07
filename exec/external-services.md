# 외부서비스 정보 정리

## 1. 목적
본 문서는 프로젝트에서 사용하는 외부서비스와 필요한 설정 정보를 한 곳에 정리하기 위한 문서다.

## 2. 외부서비스 목록

### 2.1 Google Cloud Vertex AI
- 용도: 시나리오/프롬프트 텍스트 생성, 이미지 생성, 영상 생성 연동
- 사용 위치: 백엔드 API, Worker
- 필수 설정 키:
  - `GCP_PROJECT_ID`
  - `GCP_LOCATION`
  - `VERTEX_TEXT_MODEL`
  - `VERTEX_TIMEOUT_MS`
  - `GOOGLE_APPLICATION_CREDENTIALS`
  - `GEMINI_IMAGE_MODEL`
  - `GEMINI_TIMEOUT_MS`
  - `GEMINI_STUB`
- 비고:
  - 서비스 계정 키 파일은 Git에 커밋하지 않는다.
  - 운영/개발 계정을 분리한다.

### 2.2 AWS S3 (또는 S3 호환 스토리지)
- 용도: 생성 결과(이미지/영상), 업로드 파일 저장
- 사용 위치: 백엔드 API, Worker
- 필수 설정 키:
  - `STORAGE_PROVIDER`
  - `S3_REGION`
  - `S3_BUCKET`
  - `S3_ACCESS_KEY`
  - `S3_SECRET_KEY`
  - `S3_ENDPOINT`
  - `S3_PATH_STYLE`
  - `S3_PRESIGN_EXPIRE_SECONDS`
- 비고:
  - 로컬 개발은 LocalStack 사용 가능
  - 운영 키는 주기적으로 Rotation 권장

### 2.3 LocalStack (개발 환경)
- 용도: 로컬 S3 대체
- 실행 명령:
```bash
docker compose -f docker-compose.s3.yml up -d redis localstack localstack-init
docker compose -f docker-compose.s3.yml exec localstack awslocal s3 mb s3://itda-local
```
- 비고:
  - 개발용이며 운영 환경에서는 실제 S3 사용 권장

### 2.4 DuckDNS (도메인)
- 용도: 서비스 도메인 연결
- 예시 도메인:
  - `itdas.duckdns.org` (메인)
  - `itdasj.duckdns.org` (Jenkins)
- 비고:
  - 도메인 변경 시 Nginx `server_name`, 인증서 경로 동시 변경 필요

### 2.5 Let's Encrypt (TLS 인증서)
- 용도: HTTPS(443) 인증서
- 사용 위치: `/opt/itda/infra/nginx/conf.d/*.conf`
- 비고:
  - 인증서 경로 예시:
    - `/etc/letsencrypt/live/<domain>/fullchain.pem`
    - `/etc/letsencrypt/live/<domain>/privkey.pem`
  - 갱신 자동화(cron/certbot) 여부를 운영 문서에 명시

### 2.6 Jenkins
- 용도: CI/CD(백엔드/프론트 빌드 및 배포)
- 연결 대상:
  - GitLab 저장소
  - Docker Registry
  - 배포 서버(SSH)
- 비고:
  - Webhook 토큰, 계정 정보는 Jenkins Credentials로 관리
  - 프록시 도메인 예시: `itdasj.duckdns.org`

### 2.7 Mattermost Webhook
- 용도: Jenkins 빌드/배포 알림
- 비고:
  - Webhook URL은 Jenkins Credentials에 저장
  - 실패 로그 포함 알림 정책 사용 가능

### 2.8 Grafana
- 용도: 운영 모니터링 대시보드 시각화
- 연동 대상:
  - Prometheus(메트릭 수집원)
  - Nginx Reverse Proxy(`/grafana/` 경로)
- 관련 설정 키:
  - `GRAFANA_ADMIN_PASSWORD`
- 비고:
  - `deploy/docker-compose.yml`에서 `observability` profile로 실행
  - 외부 접근 시 Nginx 경로(`/grafana/`)와 `GF_SERVER_ROOT_URL` 설정 일치 필요

## 3. 외부서비스별 점검 체크리스트
- [ ] Vertex AI 서비스 계정 권한/리전 확인
- [ ] S3 버킷 권한(CORS/Presign/퍼블릭 정책) 확인
- [ ] LocalStack 버킷 생성 자동화 확인(개발 환경)
- [ ] DuckDNS 레코드 및 서버 공인 IP 매핑 확인
- [ ] Let's Encrypt 인증서 유효기간/자동 갱신 확인
- [ ] Jenkins Credentials 누락 여부 확인
- [ ] Mattermost 알림 수신 테스트 확인
- [ ] Grafana 로그인 및 대시보드 로딩 확인

## 4. 보안 주의사항
- Access Key, Secret Key, JWT Secret, 서비스 계정 키 파일은 문서에 실제 값 기재 금지
- `.env`, `*.json.key`, `*-key.json` 파일은 Git 추적 금지
- 키 노출 시 즉시 폐기(Rotate) 후 재발급
