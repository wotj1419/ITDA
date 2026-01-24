# Video Worker 구현 계획서 (Veo)

> 작성일: 2026-01-23  
> 대상 범위: W3 D3(2026-01-21) 영상 Worker 착수 ~ W3 D4(2026-01-22) 영상 Worker 1차 완성

## 목적

VIDEO_GENERATION Job 처리 흐름과 구현 상세를 문서화하고,
팀원이 동일한 기준으로 확장/연동하도록 기준을 제공한다.

## 범위

- VIDEO_GENERATION Job 처리
- Veo 영상 생성 클라이언트 (초기 Mock 포함)
- 로컬 파일 저장 및 결과 연결
- JobExecutor 연결
- WS 이벤트 발행 확인

## 선행/의존 사항 (필수 공백)

아래 항목이 비어 있으면 본 계획대로 구현이 불가능함.

1. **Generate API 입구 미구현**
   - `POST /api/nodes/{id}/generate` 및 `/regenerate`는 APIdocs에만 존재
2. **Worker 패키지 부재**
   - `com.itda.backend.worker` 패키지가 현재 없음
3. **JobExecutor는 resultAssetId 필수**
   - `resultAssetId == null`이면 예외 발생
4. **Node 상태/URL 업데이트 경로 없음**
   - VIDEO 노드 `status`/`content_url` 갱신 메서드가 없음
5. **Veo Client/설정 미존재**
   - `ai/veo` 패키지와 `ai.veo.*` 설정 없음

## 구현 계획 (진행 현황 포함)

### 처리 흐름 (계획)

- [ ] JobExecutor가 VIDEO_GENERATION Job을 VideoGenerationWorker에 위임
- [ ] request_json에서 prompt/settings 추출 (파싱 실패 시 raw 텍스트 사용)
- [ ] VeoClient가 영상 생성
  - [ ] `ai.veo.stub=true` 또는 키 미설정이면 스텁 mp4 반환
- [ ] LocalVideoStorage가 영상 파일 저장
- [ ] 결과 연결 (Asset 최소 구현 또는 JobExecutor 로직 수정)
- [ ] JobExecutor가 성공 처리 + WS 이벤트 발행

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
- Mock 모드일 때 경고 로그 출력

## 구현 상세 (계획)

### 파일/구성 (계획)

- Veo 설정/클라이언트
  - `src/main/java/com/itda/backend/ai/veo/VeoProperties.java` (추가)
  - `src/main/java/com/itda/backend/ai/veo/VeoClient.java`
  - `src/main/java/com/itda/backend/ai/veo/VeoResult.java`
- Video Worker
  - `src/main/java/com/itda/backend/worker/video/VideoGenerationWorker.java`
  - `src/main/java/com/itda/backend/worker/video/LocalVideoStorage.java`
- Job 연결
  - `src/main/java/com/itda/backend/job/service/JobExecutor.java`
- Node/Asset 연결 (선택)
  - (A) `asset` 도메인/mapper 추가
  - (B) Node 상태/URL 업데이트 로직 추가

### 주요 로직 요약 (계획)

- VideoGenerationWorker.execute()
  - request_json 파싱 → VeoClient 호출 → LocalVideoStorage 저장
  - 결과 연결(Asset 또는 Node 업데이트)
  - resultAssetId 반환 또는 성공 처리

- VeoClient.generateVideo()
  - stub 모드면 짧은 mp4 반환
  - 실제 호출은 TODO (다음 단계)

## 설정 (현재)

- `file.upload-dir` (기본값 `./uploads`)
- `job.execution.max-retry-count`

추가 필요:
- `ai.veo.project-id`
- `ai.veo.location`
- `ai.veo.api-key`
- `ai.veo.stub` (기본 true 권장)
- `ai.veo.timeout-ms`

## 다음 단계 계획 (구체화)

### 1) Veo 실연동

- API 스펙 확정 (모델, 입력 포맷, 응답 포맷)
- VeoClient 실제 HTTP/SDK 호출 구현
- 실패 시 재시도 정책 정의 (429/5xx)

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

- [ ] `POST /api/nodes/{id}/generate` 구현/연결 확인
- [ ] VideoGenerationWorker 기본 구현
- [ ] request_json 파싱 로직 확정
- [ ] Mock mp4 생성/저장 로직 구현
- [ ] JobExecutor VIDEO 분기 연결
- [ ] Job 상태/WS 이벤트 확인
- [ ] 결과 파일/URL 확인
