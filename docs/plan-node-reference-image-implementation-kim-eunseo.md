# 노드 AI 생성 시 레퍼런스 이미지 연결 구현 계획서

> 작성일: 2026-01-28
> 담당: 김은서
> 기준 문서: `docs/PRD_AI_Movie_Studio_v2.5.md`, `docs/APIdocs.md`
> 전제: 오브젝트 시트 CRUD + 이미지 업로드(서버 업로드) 완료

## 0) 결정 사항 (오브젝트 생성 Job/Streams 제외 사유)

- PRD는 **오브젝트 시트 이미지를 AI 생성 Job**으로 가정하지만, 현재 구현은 **사용자 직접 업로드(서버 업로드)** 방식이다.
- 업로드가 동기 처리이므로 **Job/Streams 발행이 필요 없다**.
- 상태 전이는 업로드 완료 시 즉시 `SUCCEEDED`로 처리할 수 있으며, 비동기 생성 상태 관리가 불필요하다.
- 추후 오브젝트 이미지 자동 생성/후처리 파이프라인을 도입할 경우에만 Job/Streams 재도입을 검토한다.

## 1) 목적

노드 이미지 생성 요청에 포함된 **referenceObjectIds**를 실제 이미지 레퍼런스로 변환하여
Gemini 이미지 생성 호출에 전달함으로써 **오브젝트 일관성 유지**를 달성한다.

## 2) PRD 요구사항 요약

- 오브젝트 시트는 캐릭터/소품 등의 **레퍼런스 이미지**를 보관한다.
- 씬/노드 생성 시 **등장 오브젝트를 선택**하면, Gemini 호출에 레퍼런스 이미지를 함께 전송한다.
- AI는 레퍼런스를 참고하여 **일관된 오브젝트**를 생성해야 한다.

## 3) 현 상태 요약 (코드 기준)

- **NodeService**: `GenerateNodeRequest.referenceObjectIds`를 검증 후 request_json에 포함 (완료)
- **JobRequestParser**: prompt/settings만 파싱, referenceObjectIds는 무시
- **ImageGenerationWorker**: prompt/settings만 사용하여 Gemini 호출
- **GeminiImageClient**: 텍스트 기반 이미지 생성만 지원 (레퍼런스 이미지 미지원)
- **ObjectSheet**: `sheet_image_asset_id` 기반으로 이미지 접근 가능

즉, **레퍼런스 ID는 저장되지만 실제 이미지 전달은 미구현** 상태이다.

## 4) 목표 동작 흐름 (End-to-End)

1) FE가 노드 결과 생성 요청(`POST /api/nodes/{id}/generate`)에 `referenceObjectIds` 전달
   - 노드 편집 화면에서 **선택한 오브젝트가 있을 때만 포함**, 미선택 시 필드 생략
2) NodeService가 referenceObjectIds를 검증 후 request_json에 포함
3) Worker에서 request_json 파싱 시 referenceObjectIds 추출
4) referenceObjectIds를 통해 오브젝트 이미지 bytes를 로드
5) Gemini 이미지 생성 요청에 prompt + 레퍼런스 이미지를 함께 전달
6) 생성 결과 저장 및 Job 완료 처리

## 5) 범위

### ✅ 포함
- ImageGenerationWorker에서 레퍼런스 이미지 전달
- JobRequestParser/ParsedJobRequest 확장
- 오브젝트/에셋 조회 및 이미지 bytes 로드
- Gemini 이미지 요청에 레퍼런스 이미지 inline 전달

### 🚫 제외 (차후)
- VideoGenerationWorker 레퍼런스 이미지 전달 (영상은 별도 전략 필요)
- 오브젝트 이미지 자동 생성 Job/Streams
- 레퍼런스 이미지 다중 뷰/버전 관리

## 6) API/Request 포맷 변경

### 6.1 노드 생성 요청
- 기존: `prompt`, `settings`
- 확장: `referenceObjectIds: List<Long>`
- 이미 request_json에는 포함되므로 **APIdocs 업데이트만 필요**

### 6.2 Job request_json 포맷
```json
{
  "prompt": "...",
  "settings": { ... },
  "referenceObjectIds": [1, 2, 3]
}
```

## 7) 설계 상세

### 7.1 JobRequestParser 확장
- request_json에서 `referenceObjectIds`를 파싱하여 ParsedJobRequest에 포함
- 파싱 실패 시 빈 리스트 처리 (기존 호환)

### 7.2 ParsedJobRequest 확장
- 기존: `prompt`, `settings`
- 추가: `referenceObjectIds` (List<Long>)

### 7.3 Reference 이미지 로더 설계

#### 책임
- referenceObjectIds를 받아 **오브젝트 이미지 bytes** 리스트로 변환

#### 조회 경로
1) ObjectSheet 조회 (projectId + ids)
2) `sheet_image_asset_id`로 Asset 조회
3) Asset.storageProvider에 따라 bytes 로드

#### StorageProvider 처리
- **S3**: `S3Client.getObject`로 bytes 로드
- **LOCAL**: `LocalFileStorage`에 read 메서드 추가 후 bytes 로드

#### 제한 및 정책
- 최대 참조 개수: **3개로 제한** (비용/지연/효과 체감 감소 고려)
- 1장당 용량 제한: **2MB 상한** (sizeBytes 기준, 초과 시 실패 처리)
- 중복 ID 제거
- referenceObjectIds가 전달된 경우 **하나라도 로드 실패 시 전체 실패** 처리
- referenceObjectIds가 없는 경우에만 **prompt 단독 호출** 허용

### 7.4 GeminiImageClient 확장

- 기존: `generateImage(prompt, settings)`
- 확장: `generateImage(prompt, settings, referenceImages)`
  - `referenceImages`: List<ReferenceImage(bytes, mimeType)>
- 요청 포맷은 **텍스트 + inline 이미지 파트**로 구성
- referenceImages가 비어 있으면 기존 로직 유지

## 8) 코드 변경 포인트

### 8.1 Node/Job
- `JobRequestParser`: referenceObjectIds 파싱
- `ParsedJobRequest`: referenceObjectIds 필드 추가

### 8.2 Worker
- `ImageGenerationWorker`: referenceObjectIds → 이미지 로더 호출 → Gemini 전달
- 신규 컴포넌트(예시)
  - `ObjectReferenceImageLoader`
  - `ReferenceImage` DTO

### 8.3 Asset/Object 조회
- `ObjectMapper`: **projectId + ids 조회로 스코프 보장** (권장 신규 메서드)
- `AssetMapper`: 필요 시 batch 조회 추가 (성능 최적화용, 선택)

### 8.4 Storage
- `LocalFileStorage`: `readBytes(String relativePath)` 추가 (uploadRoot 경로 검증 포함)
- S3 로드 유틸: `S3Client.getObject` 사용

### 8.5 AI
- `GeminiImageClient`: 텍스트+이미지 요청 구성 지원

## 9) 오류 처리 정책

- NodeService 단계: referenceObjectIds 유효성 검증 실패 시 `INVALID_REQUEST`
- Worker 단계:
  - referenceObjectIds가 **비어있지 않은데 로드 실패 발생** → Job 실패 처리
  - referenceObjectIds가 비어있는 경우에만 prompt 단독 호출 허용
  - S3/파일 오류는 실패 사유로 기록
  - 에러코드 세분화:
    - 잘못된 입력/용량 초과/비지원 MIME → `INVALID_REQUEST`
    - 권한/프로젝트 불일치 → `FORBIDDEN`
    - 파일 없음/빈 파일 → `CONTENT_NOT_FOUND`
    - 스토리지/IO 오류 → `INTERNAL_ERROR`

## 10) 테스트 계획

### 10.1 단위 테스트
- JobRequestParser: referenceObjectIds 파싱 여부
- ObjectReferenceImageLoader: 정상/누락/권한 없는 케이스
- GeminiImageClient: referenceImages 포함 시 요청 생성

### 10.2 통합 테스트
1) 오브젝트 생성 + 이미지 업로드
2) 노드 생성 요청에 referenceObjectIds 포함
3) ImageGenerationWorker에서 레퍼런스 이미지가 포함되어 호출되는지 확인
4) 일부 레퍼런스 누락/로드 실패 시 Job 실패 처리 확인

## 11) 문서 업데이트

- `docs/APIdocs.md`
  - `/api/nodes/{id}/generate` 요청에 `referenceObjectIds` 필드 추가
  - 응답 변화 없음

## 12) 구현 순서

1) JobRequestParser/ParsedJobRequest 확장
2) ObjectReferenceImageLoader 구현 (Object/Asset 조회 + bytes 로드)
3) LocalFileStorage read 기능 추가
4) GeminiImageClient referenceImages 지원
5) ImageGenerationWorker 연결
6) APIdocs 업데이트
7) 테스트 추가

## 13) 리스크/비용 영향

- 레퍼런스 이미지가 추가되면 **입력 토큰 비용이 증가**한다.
  - Vertex AI 기준으로 이미지 입력이 토큰으로 환산되어 과금됨 (예: 1024×1024 이미지 ≈ 1290 tokens)
  - Gemini Developer API 기준으로 이미지 입력이 고정 토큰(예: 560 tokens/이미지)으로 환산됨
- 레퍼런스 이미지 개수/해상도가 늘수록 비용이 선형 증가하므로, **참조 개수 3개 제한** 및 **해상도 축소** 정책이 필요하다.

## 14) 성능/비용 가드 권장 규칙

- 초기 구현은 **핵심 가드만 우선 적용**하고, 나머지는 단계적으로 확장한다.
- 핵심 가드: **참조 개수 3개 제한 + sizeBytes 기반 용량 제한(2MB)**

- 최대 3장까지만 참조
- 1장당 최대 용량 제한(예: 2MB) 및 초과 시 Job 실패 처리
- 0바이트(빈 파일) 요청은 실패 처리
- MIME 타입은 **이미지 타입만 허용** (예: image/png, image/jpeg, image/webp)
- 긴 변 기준 해상도 제한(예: 1024px) 적용
- 로드 실패 시 **전체 실패 처리** (referenceObjectIds 미전달 시에만 prompt 단독 허용)
- 레퍼런스 로드/AI 호출 시간 로그를 남겨 지연 원인 추적

### 14.1 백엔드 적용 방식 (구체화)
- **용량 제한(sizeBytes)**: Asset의 `sizeBytes` 기반으로 1차 차단 (다운로드 전에 실패 처리)
- **S3 최적화**: `HeadObject`로 Content-Length 확인 후 제한 초과면 실패 처리
- **LOCAL 최적화**: 파일 크기 확인 후 제한 초과면 실패 처리
- **해상도 제한**: bytes 로드 후 `ImageIO`로 width/height 확인 → 초과 시 스킵 또는 리사이즈
- **메타 확장(선택)**: 업로드 시 width/height를 Asset에 저장해 **로드 없이 판단**하도록 개선 가능
- **MIME 검증**: Asset contentType을 기준으로 허용된 이미지 포맷만 통과
- **빈 파일 방지**: sizeBytes/실제 bytes 기준 0바이트면 실패 처리

## 15) Swagger 테스트 방법

1) 프로젝트 생성 (project API)
2) 씬 생성 (scene API)
3) 오브젝트 시트 생성 + 이미지 업로드 (object API)
4) 노드 생성 (scene 노드 생성 API)
5) `POST /api/nodes/{id}/generate`에 `referenceObjectIds` 포함하여 호출
6) Job 완료 후 결과 이미지 확인
7) 서버 로그에서 레퍼런스 이미지 로드/전달 여부 확인

- Swagger에서는 요청/응답 검증이 가능하며, **레퍼런스 반영 여부는 로그/결과 이미지 비교로 확인**한다.
- referenceObjectIds를 전달했는데 이미지 로드가 실패하면 **Job 실패**로 확인되어야 한다.

## 16) 변경 완료 (목록/의도)

### 16.1 문서
- `docs/plan-node-reference-image-implementation.md`: 전체 구현 계획 정리 및 정책(3개 제한/2MB/실패 시 전체 실패) 반영
- `docs/APIdocs.md`: `/api/nodes/{id}/generate` 요청 바디에 `referenceObjectIds` 필드 추가

### 16.2 요청 파싱/검증
- `itda-backend/src/main/java/com/itda/backend/worker/ParsedJobRequest.java`: request_json에 referenceObjectIds 전달을 위한 DTO 확장
- `itda-backend/src/main/java/com/itda/backend/worker/JobRequestParser.java`: request_json에서 referenceObjectIds 파싱/검증 추가
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`: referenceObjectIds 최대 3개/프로젝트 스코프 검증 후 request_json에 포함

### 16.3 레퍼런스 이미지 로드/AI 연동
- `itda-backend/src/main/java/com/itda/backend/worker/image/ObjectReferenceImageLoader.java`: 오브젝트→에셋→이미지 bytes 로드 (LOCAL/S3), 2MB 제한 및 실패 시 전체 실패
- `itda-backend/src/main/java/com/itda/backend/worker/LocalFileStorage.java`: 로컬 파일 size/read 지원 추가
- `itda-backend/src/main/java/com/itda/backend/ai/gemini/ReferenceImage.java`: Gemini 전달용 레퍼런스 이미지 DTO
- `itda-backend/src/main/java/com/itda/backend/ai/gemini/GeminiImageClient.java`: 텍스트+inline 이미지 파트로 생성 호출 지원
- `itda-backend/src/main/java/com/itda/backend/worker/image/ImageGenerationWorker.java`: referenceObjectIds 존재 시 로더 → Gemini 호출로 분기
- 보완 가드: assetType/projectId 검증, MIME 허용 목록 검증, 0바이트/스토리지 오류 분기 처리

### 16.4 테스트
- `itda-backend/src/test/java/com/itda/backend/worker/JobRequestParserTest.java`: referenceObjectIds 파싱 케이스 추가
- `itda-backend/src/test/java/com/itda/backend/worker/image/ImageGenerationWorkerTest.java`: 레퍼런스 이미지 사용 분기 테스트 추가
- `itda-backend/src/test/java/com/itda/backend/worker/video/VideoGenerationWorkerTest.java`: ParsedJobRequest 확장에 따른 생성자 변경 반영
- `itda-backend/src/test/java/com/itda/backend/node/controller/NodeControllerGenerateTest.java`: Swagger 요청 바디(referenceObjectIds) 수용 테스트 추가
