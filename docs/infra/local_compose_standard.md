# Docker Compose 로컬 표준 (MySQL/Redis/LocalStack S3)

## 기준 파일
- `docker-compose.s3.yml`

## 파일명/실행 명령(확정)
- 표준 파일명: `docker-compose.s3.yml`
- 실행:
  - `docker compose -f docker-compose.s3.yml up -d`
  - 종료: `docker compose -f docker-compose.s3.yml down`

## 환경변수 기준(기본값 포함)
- MySQL
  - `MYSQL_ROOT_PASSWORD` (default: `ssafy`)
  - `MYSQL_DATABASE` (default: `itda_local`)
  - `MYSQL_PORT` (default: `3307`)
- Redis
  - `REDIS_PORT` (default: `6379`)
- LocalStack S3
  - `LOCALSTACK_TAG` (default: `latest`)
  - `LOCALSTACK_PORT` (default: `4566`)
  - `AWS_DEFAULT_REGION` (default: `ap-northeast-2`)
  - `AWS_ACCESS_KEY_ID` (default: `test`)
  - `AWS_SECRET_ACCESS_KEY` (default: `test`)
  - `S3_BUCKET` (default: `itda-local`)

## LocalStack 초기화
- `localstack-init` 서비스가 S3 버킷을 자동 생성
- 버킷 확인 명령:
  - `aws --endpoint-url=http://localhost:4566 s3 ls`

## 확인 포인트
- MySQL: `localhost:${MYSQL_PORT}` 접속 가능
- Redis: `localhost:${REDIS_PORT}` 접속 가능
- LocalStack: `http://localhost:${LOCALSTACK_PORT}/_localstack/health` 응답 확인
