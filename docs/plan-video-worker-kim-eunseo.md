# Video Worker 구현 계획서 (Veo)

> 작성일: 2026-01-23  
> 대상 범위: W3 D3(2026-01-21) 영상 Worker 착수 ~ W3 D4(2026-01-22) 영상 Worker 1차 완성

## 목적

VIDEO_GENERATION Job 처리 흐름과 구현 상세를 문서화하고,
팀원이 동일한 기준으로 확장/연동하도록 기준을 제공한다.

## 범위

- VIDEO_GENERATION Job 처리
- Veo 영상 생성 클라이언트 (Vertex AI REST)
- 로컬 파일 저장 및 결과 연결
- JobExecutor 연결
- WS 이벤트 발행 확인

## 선행/의존 사항 (현 상태)

- Generate API 입구는 기존대로 사용
- `com.itda.backend.worker` 패키지 신규 추가됨
- `JobExecutor`는 `resultAssetId` 필수
- VIDEO 노드 `status`/`content_url` 갱신 로직은 `JobExecutor`에서 처리
- Veo Client/설정 추가됨

## 구현 계획 (진행 현황 포함)

### 처리 흐름 (구현 반영)

- [x] JobExecutor가 VIDEO_GENERATION Job을 VideoGenerationWorker에 위임
- [x] request_json에서 prompt/settings 추출 (파싱 실패 시 raw 텍스트 사용)
- [x] VeoClient가 영상 생성 (Vertex AI REST + ADC, LongRunning 폴링)
- [x] LocalVideoStorage가 영상 파일 저장
- [x] Asset 저장 + result_asset_id 업데이트
- [x] JobExecutor가 성공 처리 + WS 이벤트 발행

### 입력/출력 정의 (계획)

입력:
- generation_jobs.request_json (VIDEO)
```json
{
  "nodeId": 301,
  "projectId": 12,
  "prompt": "황혼의 사막을 걷는 탐험가",
  "settings": {
    "startShotNodeId": 501,
    "endShotNodeId": null,
    "cameraMotion": "PAN",
    "duration": 5,
    "motionDescription": "천천히 오른쪽으로 이동",
    "provider": "VEO_3_1"
  }
}
```

출력:
- (선택 A) assets 테이블에 VIDEO 에셋 생성 + `result_asset_id` 저장
- (선택 B) VIDEO 노드 `content_url`, `status` 갱신 + Job 성공 처리

### 저장 규칙 (계획)

- 저장 경로: `${file.upload-dir}/ai/video/{projectId}/job-{jobId}.mp4`
- storage_provider: `LOCAL` (초기)
- storage_key: 상대 경로 문자열 (예: `ai/video/12/job-101.mp4`)
- content_type: `video/mp4`

### 예외/로그 처리 (계획)

- request_json 파싱 실패 시 경고 로그 후 raw 텍스트 사용
- Veo 호출 실패 시 FAILED 처리 + error_message 기록
- 파일 저장 실패 시 예외 발생 → JobExecutor가 FAILED 처리
- provider 응답 요약을 `error_message`에 기록 (길이 제한 적용)

### settings 매핑 (구현 반영)

- `duration` → `durationSeconds`
- `cameraMotion`, `motionDescription` → 프롬프트 문장으로 합성
- `provider` → 모델 ID 매핑 (`VEO_3_1`, `VEO_3_1_FAST`, `VEO_3_0`, `VEO_2_0`)
- `aspectRatio` 없으면 기본 `16:9`

## 구현 상세 (계획)

### 파일/구성 (구현 반영)

- Veo 설정/클라이언트
  - `itda-backend/src/main/java/com/itda/backend/ai/veo/VeoProperties.java`
  - `itda-backend/src/main/java/com/itda/backend/ai/veo/VeoClient.java`
  - `itda-backend/src/main/java/com/itda/backend/ai/veo/VeoResult.java`
  - `itda-backend/src/main/java/com/itda/backend/ai/veo/VeoRequest.java`
  - `itda-backend/src/main/java/com/itda/backend/ai/VertexAiAuthProvider.java`
  - `itda-backend/src/main/java/com/itda/backend/ai/AiProviderException.java`
- Video Worker
  - `itda-backend/src/main/java/com/itda/backend/worker/video/VideoGenerationWorker.java`
  - `itda-backend/src/main/java/com/itda/backend/worker/video/LocalVideoStorage.java`
  - `itda-backend/src/main/java/com/itda/backend/worker/JobRequestParser.java`
  - `itda-backend/src/main/java/com/itda/backend/worker/ParsedJobRequest.java`
  - `itda-backend/src/main/java/com/itda/backend/worker/StoredAsset.java`
  - `itda-backend/src/main/java/com/itda/backend/worker/ExecutionResult.java`
- Job 연결
  - `itda-backend/src/main/java/com/itda/backend/job/service/JobExecutor.java`
  - `itda-backend/src/main/resources/application-local.yml`
  - `itda-backend/src/main/resources/application-dev.yml`
  - `itda-backend/src/main/resources/application-prod.yml`
  - `itda-backend/env`

### 주요 로직 요약 (구현 반영)

- VideoGenerationWorker.execute()
  - request_json 파싱 → VeoClient 호출 → LocalVideoStorage 저장
  - Asset 저장 + resultAssetId 반환

- VeoClient.generateVideo()
  - Vertex AI REST 호출 (`:predictLongRunning`)
  - Operation 폴링 → base64 영상 디코딩

## 설정 (현재)

- `file.upload-dir` (기본값 `./uploads`)
- `job.execution.max-retry-count`
- `ai.veo.project-id`
- `ai.veo.location`
- `ai.veo.model`
- `ai.veo.timeout-ms`
- `ai.veo.poll-interval-ms`
- 인증: `GOOGLE_APPLICATION_CREDENTIALS` (ADC)

## 다음 단계 계획 (구체화)

### 1) Veo 실연동

- 완료 (REST + ADC, LongRunning 폴링)

### 2) 스토리지 확장 (S3)

- S3 업로드 로직 추가 (LocalStack 지원)
- storage_provider 값을 `S3`로 전환
- 업로드 실패 시 재시도 및 오류 메시지 표준화

### 3) 결과/URL 연동

- AssetService 구현 (presigned URL 생성)
- JobResultResolver에 presigned URL 연동
- API 응답에서 resultUrl 반환 확인

### 4) request_json 확장

- 영상 옵션 추가 (aspect ratio, fps 등)
- JSON 스키마 문서화 및 유효성 검증 추가

### 5) 테스트/관측

- VideoGenerationWorker 단위 테스트
- JobExecutor 통합 테스트
- 처리 시간, 실패율 로그/메트릭 추가

## 체크리스트

- [x] `POST /api/nodes/{id}/generate` 구현/연결 확인
- [x] VideoGenerationWorker 기본 구현
- [x] request_json 파싱 로직 확정
- [x] JobExecutor VIDEO 분기 연결
- [x] Job 상태/WS 이벤트 확인
- [x] 결과 파일/URL 확인
