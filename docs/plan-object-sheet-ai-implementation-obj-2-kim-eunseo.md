# 오브젝트 시트 업로드/레퍼런스 연동 구현 계획서 (OBJ-2)

> 작성일: 2026-01-26
> 담당: 김은서
> 기준 일정: W4 D2 이후
> 전제: OBJ-1 CRUD + 씬-오브젝트 연동 완료 (Swagger 테스트 완료)

## 1) 목적

AI 생성 없이, **단일 이미지 업로드가 필수인 오브젝트 시트**를 제공하고
노드 이미지 생성 시 **레퍼런스 이미지로 활용**되도록 한다.

## 2) 범위

### ✅ OBJ-2 (이번 범위)
- 오브젝트 시트 **단일 이미지 업로드 기능** 추가
- 업로드 이미지 **Asset 등록 + Object 연동**
- ObjectSheet 상태 전이: 이미지 미보유(PENDING) → 이미지 보유(SUCCEEDED)
- ObjectSheet 기본 이미지(`sheet_image_url`) 설정
- 노드 이미지 생성 요청에 **레퍼런스 오브젝트 연동**

### 🚫 제외 (차후)
- 오브젝트 이미지 버전 관리/히스토리
- 고급 태깅(포즈/표정/의상 메타데이터)
- 자동 리사이징/품질 보정 파이프라인

## 3) 참고 문서

- `docs/PRD_AI_Movie_Studio_v2.5.md` (오브젝트/노드 관련 요구)
- `docs/APIdocs.md` (오브젝트/노드 API 계약)
- 기존 코드
  - `object/service/ObjectService`, `object/repository/ObjectMapper`
  - `node/service/NodeService`, `node/controller/dto/request/GenerateNodeRequest`
  - `worker/LocalFileStorage`, `worker/AssetRegistrar`
  - `media/MediaFileService`, `media/MediaUrlResolver`

## 4) 현 상태 요약

- 오브젝트 CRUD 및 scene_objects 연동 완료
- ObjectSheet는 `sheet_image_url`, `status` 필드만 존재 (업로드 로직 없음)
- 노드 AI 생성 요청은 prompt/settings만 전달 (레퍼런스 미지원)

## 5) 목표 동작 흐름

1) FE가 오브젝트 시트 생성(메타데이터)
2) FE가 오브젝트 이미지 **단일 업로드**
3) BE가 파일 저장 + Asset 등록 + Object 이미지 연동
4) `sheet_image_url` 설정 및 `status=SUCCEEDED`
5) FE가 노드 생성/편집 시 오브젝트 선택
6) 노드 AI 생성 요청에 레퍼런스 오브젝트 IDs 포함
7) BE가 레퍼런스 이미지로 변환하여 AI 호출

## 6) API 계약 변경 (OBJ-2)

### 6.1 오브젝트 이미지 업로드/교체 (신규)
- `POST /api/projects/{id}/objects/{objectId}/image`
- `multipart/form-data` (`file`) 단일 업로드
- 동작
  - 기존 이미지가 있으면 **교체**
  - 업로드 시 Asset 생성 후 Object와 연동
- 응답: 업로드된 이미지 메타데이터 또는 ObjectSheetResponse

### 6.2 오브젝트 이미지 조회/삭제 (신규)
- 조회: `GET /api/projects/{id}/objects/{objectId}/image`
- 삭제: `DELETE /api/projects/{id}/objects/{objectId}/image`
- 삭제 시 처리
  - `sheet_image_url = null`, `status = PENDING`
  - 기존 Asset/파일 정리 (best-effort)
- 노드 편집창 미리보기는 **ObjectSheetResponse.sheetImageUrl** 사용
  - 실제 호출 엔드포인트: `GET /api/objects/{objectId}/image`
  - 별도 노드 전용 이미지 API는 불필요

### 6.3 노드 AI 생성 요청 확장
- `GenerateNodeRequest`에 `referenceObjectIds: List<Long>` 추가
- Job request_json에는 **referenceObjectIds만 유지**하고, 실행 시점에 이미지로 변환

### 6.4 공통 에러 처리 (권장)
- 권한/접근 불가: `FORBIDDEN`
- 프로젝트/오브젝트 없음: `PROJECT_NOT_FOUND`, `OBJECT_NOT_FOUND`
- 잘못된 입력: `INVALID_INPUT_VALUE` 또는 `INVALID_REQUEST`
- 파일/스토리지 오류: `INTERNAL_ERROR`

## 7) 데이터 모델 변경

### 7.1 오브젝트 테이블 보완
- `objects`에 `sheet_image_asset_id`(또는 `sheet_asset_id`) 컬럼 추가 권장
- `sheet_image_url`은 그대로 유지 (응답/캐싱 용도)

### 7.2 ObjectStatus 처리
- 이미지 0개: `PENDING`
- 이미지 1개: `SUCCEEDED`
- 상태 전이 책임: 업로드/삭제 서비스에서 즉시 갱신

### 7.3 단일 이미지 규칙 (디테일)
- 생성 후 **이미지 1개가 반드시 존재**해야 정상 상태로 전이
- 이미지 교체 시 기존 파일/Asset 정리

## 8) 저장/에셋 정책 (확정안)

- **저장 경로 통일**: `ai/object-images/{projectId}/{objectId}/{uuid}.{ext}`
- Local/S3 모두 동일 prefix 사용
- 업로드 시 Asset 생성 후 `objects.sheet_image_asset_id`로 연동
- `sheet_image_url`은 Object에 `/api/objects/{objectId}/image`로 저장

### 8.1 구현 구조 (추천 방식)
- Object 도메인에서 파일 저장 처리 후 Asset 등록
- `MediaFileService` + `AssetRegistrar` 흐름 재사용
- Object 생성은 메타데이터만, 이미지는 별도 업로드로 처리

### 8.2 권한/접근 통제 (디테일)
- `projectId` 접근 권한 검사 (ProjectAccessService 활용)
- `createdBy`는 인증 사용자 ID로 기록
- 이미지 업로드/삭제는 `objectId` 기준 프로젝트 접근 가능 여부 검증

### 8.3 트랜잭션/실패 처리 (디테일)
- 업로드 성공 후 Asset 등록 및 Object 연동
- 연동 실패 시: **파일/Asset 정리** 시도 (best-effort)
- 정리 실패는 로그만 남기고 API는 실패 응답 유지

### 8.4 삭제/정리 정책 (디테일)
- 이미지 교체 시: 기존 Asset 삭제 → 실제 파일 삭제
- Asset이 다른 도메인에서 참조되는 경우를 대비해 **참조 여부 체크** 후 삭제
- 오브젝트 삭제 시: 연동 Asset/파일 정리 포함

### 8.5 URL 해석 정책
- 응답 URL은 `MediaUrlResolver` 또는 동일 정책으로 변환
- `sheet_image_url`은 업로드 이미지 URL로 유지

## 9) 노드 AI 레퍼런스 연동 방식 (확정안)

- GenerateNodeRequest의 `referenceObjectIds`를 기반으로
  - Object (`sheet_image_asset_id`) → Asset → storageKey/URL 조회
  - 필요 시 `sheet_image_url` 직접 사용 (fallback)
- 레퍼런스 이미지는 **단일 이미지 1장**만 사용
- AI 호출 방식
  - 1순위: 이미지 bytes를 **inline data**로 전달 (Gemini 지원 범위 확인 후 적용)
  - 2순위: 임시 URL을 prompt에 포함 (fallback)

## 10) 코드 변경 포인트

- `object` 도메인
  - 단일 이미지 업로드/삭제 API 추가
  - `sheet_image_asset_id` 저장 로직 추가
  - ObjectSheet 상태 업데이트 로직 추가
  - DB 마이그레이션: `objects.sheet_image_asset_id` 추가
- `asset` 도메인
  - 오브젝트 업로드용 Asset 등록 경로 추가 (ownerId = userId 권장)
- `node` 도메인
  - `GenerateNodeRequest`에 `referenceObjectIds` 추가
  - NodeService에서 레퍼런스 이미지 조회 및 Job request_json 확장
- `ai/gemini`
  - 레퍼런스 이미지 inline 전달 지원 (필요 시 클라이언트 확장)
- `docs/APIdocs.md`
  - 오브젝트 단일 이미지 업로드/삭제 API 계약 반영

## 11) 테스트 계획

### 11.1 Swagger 수동 테스트
- 오브젝트 생성 → 이미지 단일 업로드 → 대표 이미지 확인
- 이미지 교체 후 상태/URL 갱신 확인
- 이미지 삭제 후 status 전이 확인
- 노드 AI 생성 요청에 referenceObjectIds 포함 시 정상 동작 확인

### 11.2 통합 테스트(가능하면)
- 단일 이미지 업로드 → Asset 연동 → 조회 결과 일관성
- 레퍼런스 이미지 1장 적용 확인

## 12) OBJ-1 충돌 점검 체크리스트

- ObjectSheet.status 의미 변경에 따른 FE 필터/표시 로직 확인 (PENDING → SUCCEEDED 전이 기준)
- `sheet_image_url` null → 값 변경 타이밍에 따른 FE 표시/캐시 영향 확인
- 오브젝트 삭제 시 Asset/파일 정리 정책 적용 여부 확인
- 기존 오브젝트 조회/리스트 응답 스키마 호환 여부 확인

## 13) 구현 순서 (OBJ-2)

1) objects 테이블 `sheet_image_asset_id` 컬럼 추가
2) 오브젝트 이미지 업로드/삭제 API 추가
3) ObjectSheet status/sheet_image_url 갱신 로직 추가
4) 저장 경로 및 Asset 등록 정책 적용
5) GenerateNodeRequest에 referenceObjectIds 추가 및 Job request_json 확장
6) NodeService에서 레퍼런스 이미지 조회/적용
7) Gemini 클라이언트 레퍼런스 이미지 전달 확장(필요 시)
8) 문서(APIdocs) 갱신 + Swagger 테스트

## 14) 변경 완료

- `docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`: UP-2 비고를 "assetId 반환/연결"로 정리해 업로드 API 결과 활용을 명시
- `docs/plan-object-sheet-ai-implementation-obj-2-kim-eunseo.md`: 단일 이미지 필수 업로드 흐름 및 오브젝트-레퍼런스 연동 계획 정리
- `itda-backend/src/main/java/com/itda/backend/asset/repository/AssetMapper.java`: Asset 레코드 정리용 `deleteById` 추가
- `itda-backend/src/main/resources/mapper/AssetMapper.xml`: assets 삭제 쿼리 추가
- `itda-backend/src/main/java/com/itda/backend/media/MediaFileService.java`: 오브젝트 이미지 로드(`loadObjectImage`) 추가 및 S3/로컬 조회 분기
- `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/GenerateNodeRequest.java`: `referenceObjectIds` 요청 필드 추가
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`: referenceObjectIds 검증/중복 제거 및 Job `request_json` 포함 처리
- `itda-backend/src/main/java/com/itda/backend/object/controller/ObjectController.java`: 오브젝트 생성 multipart 전환, 이미지 교체(PATCH)와 이미지 조회(GET) API 추가
- `itda-backend/src/main/java/com/itda/backend/object/domain/ObjectSheet.java`: `sheetImageAssetId` 필드 추가
- `itda-backend/src/main/java/com/itda/backend/object/repository/ObjectMapper.java`: 이미지 업데이트용 `updateSheetImage` 추가
- `itda-backend/src/main/resources/mapper/ObjectMapper.xml`: `sheet_image_asset_id` 매핑/삽입/업데이트 SQL 반영
- `itda-backend/src/main/java/com/itda/backend/object/service/ObjectService.java`: 이미지 필수 검증, 저장/Asset 등록/상태 갱신, 이미지 교체 및 기존 Asset 정리 로직 추가
- `itda-backend/src/main/java/com/itda/backend/worker/LocalFileStorage.java`: 로컬 파일 삭제 유틸 추가
- `itda-backend/src/main/resources/sql/schema-local.sql`: `objects.sheet_image_asset_id` 컬럼 추가
- `itda-backend/src/main/java/com/itda/backend/object/service/ObjectAssetRegistrar.java`: 오브젝트 이미지용 Asset 등록 컴포넌트 추가
- `itda-backend/src/main/java/com/itda/backend/object/storage/ObjectImageStorage.java`: 오브젝트 이미지 저장 인터페이스 추가
- `itda-backend/src/main/java/com/itda/backend/object/storage/ObjectImageStorageResult.java`: 저장 결과(키/타입/크기/스토리지) 모델 추가
- `itda-backend/src/main/java/com/itda/backend/object/storage/LocalObjectImageStorage.java`: 로컬 저장 구현 및 경로/확장자 정책 추가
- `itda-backend/src/main/java/com/itda/backend/object/storage/S3ObjectImageStorage.java`: S3 저장 구현 및 버킷/컨텐트타입 처리 추가
- `itda-backend/src/main/java/com/itda/backend/object/storage/ObjectImageCleaner.java`: 오브젝트 이미지 삭제(LOCAL/S3) 클린업 추가
