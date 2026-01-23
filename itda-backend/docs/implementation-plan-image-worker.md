# Image Worker 구현 계획서 (Gemini)

## 목적

IMAGE_GENERATION Job 처리 흐름과 구현 상세를 문서화하고,
팀원이 동일한 기준으로 확장/연동하도록 기준을 제공한다.

## 범위

- IMAGE_GENERATION Job 처리
- Gemini 이미지 생성 클라이언트 (실연동)
- 로컬 파일 저장 및 Asset 등록
- JobExecutor 연결

## 아키텍처 정합 (04 참고)

- API Server는 Job 생성/검증 후 Dispatcher로 Queue에 넣고 `jobId`를 즉시 응답
- Worker는 Redis Streams에서 Job을 가져와 실제 작업을 수행
- 처리 완료 후 DB 상태 업데이트(`SUCCEEDED/FAILED`) 및 Streams ACK
- 작업 완료 이벤트는 API Server가 `job.done`/`job.failed`로 WebSocket 발행
- 결과 파일은 Storage에 저장, DB에는 메타데이터만 저장 (향후 S3 전환)

## Redis Streams 계약 (07 참고)

- Stream key: `ai:image`
- Consumer group: `image-workers`
- 최소 payload 필드: `jobId`, `projectId`, `type`, `createdAt`
- 메시지는 key-value map 형태 (JSON 아님)
- 처리 원칙:
  - Job이 이미 `SUCCEEDED`이면 처리하지 않고 ACK
  - `RUNNING`이 오래된 경우 pending reclaim 후 재처리
  - 실패 시 `retry_count` 증가 및 재시도 한도 적용

## Worker 공통 처리 순서 (07 참고)

1) Streams에서 메시지 수신 (`XREADGROUP`)
2) DB에서 Job 조회 → 실행 가능 여부 확인
3) `RUNNING` 전환 (조건부 업데이트)
4) 실제 작업 수행 (AI 호출/FFmpeg)
5) 결과 저장 + DB 업데이트 (`SUCCEEDED`/`FAILED`)
6) Streams ACK (`XACK`)  
   - 성공/실패가 DB에 반영된 이후에만 ACK  
   - 실패는 ACK하지 않고 pending에 남겨 재처리 가능

## Worker 컨테이너/서비스 규칙 (04/07 참고)

- 서비스명 예시: `worker-image`, `worker-video`, `worker-merge`
- 소비자 이름 규칙: `{workerType}-{hostname}-{pid}` (예: `image-worker-app01-1234`)
- 각 Worker는 자신 Stream만 구독 (image→`ai:image`, video→`ai:video`, merge→`media:merge`)

## 로컬/Streams 이원화 운영 방침 (04 참고)

- 로컬 개발: `LocalAsyncJobDispatcher`로 `@Async` 처리 (Redis Streams 없이 즉시 실행)
- 운영/스테이징: `RedisStreamsJobDispatcher`로 Streams 기반 처리
- 전환 원칙:
  - Dispatcher 인터페이스 유지로 코드 변경 최소화
  - Worker의 처리 순서(ACK 타이밍 포함)는 동일 규칙 준수
  - 로컬은 단일 프로세스, 운영은 worker 컨테이너 분리

## 프로파일/설정 최소 기준 (공통)

- 로컬(Async): `spring.profiles.active=local` + `LocalAsyncJobDispatcher` 사용
- 운영(Streams): `spring.profiles.active=redis-streams` + Streams 기반 Dispatcher 사용
- 공통 환경 변수:
  - `REDIS_HOST`, `REDIS_PORT`
  - `GCP_PROJECT_ID`, `GCP_LOCATION` (AI Worker 공통)
  - `GEMINI_STUB`, `GEMINI_IMAGE_MODEL` (이미지 Worker)

## 구현 계획 (진행 현황 포함)

### 처리 흐름 (계획)

- [x] JobExecutor가 IMAGE_GENERATION Job을 ImageGenerationWorker에 위임
- [x] request_json에서 prompt 추출 (JSON 파싱 실패 시 raw 텍스트 사용)
- [x] GeminiImageClient가 이미지 생성
  - [x] `ai.gemini.stub=true`이면 스텁 PNG 반환
  - [x] Vertex AI REST `predict` 호출 (API Key 또는 ADC)
  - [x] `predictions`에서 base64 이미지 추출
- [x] LocalImageStorage가 이미지 파일 저장
- [x] AssetMapper로 assets 테이블에 레코드 저장
- [x] JobExecutor가 result_asset_id로 Job 성공 처리

### 실연동 구현 계획 (Vertex AI REST)

- [x] 호출 엔드포인트
  - `https://{location}-aiplatform.googleapis.com/v1/projects/{projectId}/locations/{location}/publishers/google/models/{model}:predict`
  - 또는 full resource name 사용 시 `projects/.../locations/.../publishers/.../models/...:predict`
- [x] 인증
  - `ai.gemini.api-key`가 있으면 Query `?key=...`
  - 없으면 ADC(`GOOGLE_APPLICATION_CREDENTIALS`)로 Bearer 토큰 발급
- [x] 요청 포맷
  - `instances=[{prompt}]`
  - `parameters.sampleCount`, `parameters.aspectRatio` (설정 값 있을 때만 포함)
- [x] 응답 파싱
  - `predictions[0]`에서 base64 bytes + mimeType 추출
  - 미존재 시 오류 처리
- [x] 오류/타임아웃 처리
  - 401/403 → 인증 실패
  - 429 → rate limited
  - 408/504 → timeout
  - 5xx → provider error

### 입력/출력 정의 (계획)

입력:
- generation_jobs.request_json
```json
{
  "prompt": "a scenic view of a mountain at sunset"
}
```

출력:
- assets 테이블에 IMAGE 에셋 생성
- generation_jobs.result_asset_id 업데이트

### 저장 규칙 (계획)

- 저장 경로: `${file.upload-dir}/ai/image/{projectId}/job-{jobId}.png`
- storage_provider: `LOCAL`
- storage_key: 상대 경로 문자열 (예: `ai/image/12/job-101.png`)
- content_type: `image/png`

### 예외/로그 처리 (계획)

- request_json 파싱 실패 시 경고 로그 후 raw 텍스트를 prompt로 사용
- 파일 저장 실패 시 예외 발생 → JobExecutor가 FAILED 처리
- Gemini 클라이언트는 스텁 모드 시 경고 로그 출력
- Streams 모드에서는 성공/실패 DB 반영 후 ACK, 실패 시 pending에 남아 재시도 대상

## 구현 상세 (계획)

### 파일/구성 (계획)

- Gemini 설정/클라이언트
  - `src/main/java/com/itda/backend/ai/gemini/GeminiProperties.java`
  - `src/main/java/com/itda/backend/ai/gemini/GeminiImageClient.java`
  - `src/main/java/com/itda/backend/ai/gemini/GeminiImageResult.java`
- Image Worker
  - `src/main/java/com/itda/backend/worker/image/ImageGenerationWorker.java`
  - `src/main/java/com/itda/backend/worker/image/LocalImageStorage.java`
- Asset 저장
  - `src/main/java/com/itda/backend/asset/domain/Asset.java`
  - `src/main/java/com/itda/backend/asset/domain/AssetType.java`
  - `src/main/java/com/itda/backend/asset/repository/AssetMapper.java`
  - `src/main/resources/mapper/AssetMapper.xml`
- Job 연결
  - `src/main/java/com/itda/backend/job/service/JobExecutor.java`

### 주요 로직 요약 (계획)

- ImageGenerationWorker.execute()
  - prompt 추출 → GeminiImageClient 호출 → LocalImageStorage 저장
  - AssetMapper.insert()로 asset 저장
  - asset.id 반환

- GeminiImageClient.generateImage()
  - stub 모드면 1x1 PNG 반환
  - Vertex AI REST `predict` 호출 (ADC 또는 API Key)
  - request payload: `instances=[{prompt}]`, `parameters`(sampleCount/aspectRatio)
  - response: `predictions[0]`에서 base64 bytes/mimeType 추출

## 설정 (실제 연동)

### 설정 정의

```yaml
ai:
  gemini:
    stub: ${GEMINI_STUB:true}
    api-key: ${GEMINI_API_KEY:}
    image-model: ${GEMINI_IMAGE_MODEL:imagen-3.0-generate-001}
    sample-count: ${GEMINI_SAMPLE_COUNT:1}
    aspect-ratio: ${GEMINI_ASPECT_RATIO:}
    timeout-ms: ${GEMINI_TIMEOUT_MS:60000}
```

```yaml
spring.ai.vertex.ai.gemini:
  project-id: ${GCP_PROJECT_ID}
  location: ${GCP_LOCATION:us-central1}
```

### 호출 방식

- `ai.gemini.api-key` 설정 시 Query `?key=...` 로 인증
- 미설정 시 ADC (`GOOGLE_APPLICATION_CREDENTIALS`)
- 로컬 파일 저장 경로: `${file.upload-dir}/ai/image/{projectId}/job-{jobId}.png`

## 다음 단계 계획 (구체화)

### 1) Gemini 실연동 고도화

- 모델 파라미터 확장 (negative prompt, seed 등)
- 응답 스키마 변경 대응/회귀 테스트
- 재시도/백오프 정책 고도화

### 2) 스토리지 확장 (S3)

- S3 업로드 로직 추가 (LocalStack 지원)
- storage_provider 값을 `S3`로 전환
- 업로드 실패 시 재시도 및 오류 메시지 표준화

### 3) Asset/URL 연동

- AssetService 구현 (presigned URL 생성)
- `JobResultResolver`에 presigned URL 연동
- API 응답에서 resultUrl 반환 확인

### 4) request_json 확장

- prompt 외 옵션 정의 (style, size, aspectRatio 등)
- JSON 스키마 문서화 및 유효성 검증 추가

### 5) 테스트/관측

- ImageGenerationWorker 단위 테스트
- JobExecutor 통합 테스트
- 처리 시간, 실패율 로그/메트릭 추가

## 체크리스트

- [x] IMAGE_GENERATION Job 실행 연결
- [x] 로컬 저장 및 Asset 등록
- [x] Gemini 스텁 이미지 반환
- [x] Gemini 실연동
- [ ] S3 업로드
- [ ] presigned URL 연동
- [ ] consumer group 생성 (`image-workers`)
- [ ] pending 모니터링/재처리 정책
- [ ] ACK 정책 (성공/실패 시점)
- [ ] retry 한도 및 backoff
- [ ] job timeout/kill 정책

