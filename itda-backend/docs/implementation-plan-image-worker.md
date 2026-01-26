# Image Worker 구현 계획서 (Gemini)

## 목적

IMAGE_GENERATION Job 처리 흐름과 구현 상세를 문서화하고,
팀원이 동일한 기준으로 확장/연동하도록 기준을 제공한다.

## 범위

- IMAGE_GENERATION Job 처리
- Gemini 이미지 생성 클라이언트 (Vertex AI REST)
- 로컬 파일 저장 및 Asset 등록
- JobExecutor 연결

## 구현 계획 (진행 현황 포함)

### 처리 흐름 (계획)

- [x] JobExecutor가 IMAGE_GENERATION Job을 ImageGenerationWorker에 위임
- [x] request_json에서 prompt 추출 (JSON 파싱 실패 시 raw 텍스트 사용)
- [x] GeminiImageClient가 이미지 생성 (Vertex AI REST + ADC)
- [x] LocalImageStorage가 이미지 파일 저장
- [x] AssetMapper로 assets 테이블에 레코드 저장
- [x] JobExecutor가 result_asset_id로 Job 성공 처리

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
- Gemini 클라이언트는 provider 오류 요약을 반환
- JobExecutor는 provider 요약을 `error_message`에 기록 (길이 제한 적용)

## 구현 상세 (계획)

### 파일/구성 (구현 반영)

- Gemini 설정/클라이언트
  - `src/main/java/com/itda/backend/ai/gemini/GeminiImageProperties.java`
  - `src/main/java/com/itda/backend/ai/gemini/GeminiImageClient.java`
  - `src/main/java/com/itda/backend/ai/gemini/GeminiImageResult.java`
  - `src/main/java/com/itda/backend/ai/VertexAiAuthProvider.java`
  - `src/main/java/com/itda/backend/ai/AiProviderException.java`
- Image Worker
  - `src/main/java/com/itda/backend/worker/image/ImageGenerationWorker.java`
  - `src/main/java/com/itda/backend/worker/image/LocalImageStorage.java`
  - `src/main/java/com/itda/backend/worker/JobRequestParser.java`
  - `src/main/java/com/itda/backend/worker/ParsedJobRequest.java`
  - `src/main/java/com/itda/backend/worker/StoredAsset.java`
  - `src/main/java/com/itda/backend/worker/ExecutionResult.java`
- Asset 저장
  - `src/main/java/com/itda/backend/asset/domain/Asset.java`
  - `src/main/java/com/itda/backend/asset/domain/AssetType.java`
  - `src/main/java/com/itda/backend/asset/repository/AssetMapper.java`
  - `src/main/resources/mapper/AssetMapper.xml`
- Job 연결
  - `src/main/java/com/itda/backend/job/service/JobExecutor.java`
  - `src/main/resources/application-local.yml`
  - `src/main/resources/application-dev.yml`
  - `src/main/resources/application-prod.yml`
  - `env`

### 주요 로직 요약 (계획)

- ImageGenerationWorker.execute()
  - prompt 추출 → GeminiImageClient 호출 → LocalImageStorage 저장
  - AssetMapper.insert()로 asset 저장
  - asset.id 반환

- GeminiImageClient.generateImage()
  - Vertex AI REST 호출 (`:generateContent`)
  - `responseModalities: ["TEXT","IMAGE"]`로 inlineData(base64) 수신

## 설정 (현재)

- `file.upload-dir` (기본값 `./uploads`)
- `spring.ai.vertex.ai.gemini.project-id`
- `spring.ai.vertex.ai.gemini.location`
- `ai.gemini.image-model` (기본 `gemini-2.5-flash-image`)
- `ai.gemini.timeout-ms`
- 인증: `GOOGLE_APPLICATION_CREDENTIALS` (ADC)

## 다음 단계 계획 (구체화)

### 1) Gemini 실연동

- 완료 (REST + ADC 기반 실제 호출)

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
- [x] Gemini 실연동 (REST + ADC)
- [ ] S3 업로드
- [ ] presigned URL 연동
