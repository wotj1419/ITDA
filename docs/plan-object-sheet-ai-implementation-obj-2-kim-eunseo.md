# 오브젝트 시트 업로드/레퍼런스 연동 구현 계획서 (OBJ-2)

> 작성일: 2026-01-26
> 담당: 김은서
> 기준 일정: W4 D2 이후
> 전제: OBJ-1 CRUD + 씬-오브젝트 연동 완료 (Swagger 테스트 완료)

## 1) 목적

AI 생성 없이, 자주 등장하는 인물/객체 이미지를 **오브젝트 시트에 업로드**하고
노드 이미지 생성 시 **레퍼런스 이미지로 활용**되도록 한다.

## 2) 범위

### ✅ OBJ-2 (이번 범위)
- 오브젝트 시트 **이미지 업로드(복수/멀티뷰) 기능** 추가
- 업로드 이미지 **Asset 등록 + Object 연동**
- ObjectSheet 상태 전이: 이미지 미보유(PENDING) → 이미지 보유(SUCCEEDED)
- ObjectSheet 기본 이미지(sheet_image_url) 설정
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
2) FE가 오브젝트 이미지 **복수 업로드**
3) BE가 파일 저장 + Asset 등록 + Object 이미지 연동
4) ObjectSheet.sheet_image_url(대표 이미지) 설정 및 status=SUCCEEDED
5) FE가 노드 생성/편집 시 오브젝트 선택
6) 노드 AI 생성 요청에 레퍼런스 오브젝트 IDs 포함
7) BE가 레퍼런스 이미지로 변환하여 AI 호출

## 6) API 계약 변경 (OBJ-2)

### 6.1 오브젝트 이미지 업로드 (신규)
- `POST /api/projects/{id}/objects/{objectId}/images`
- `multipart/form-data` (files[])
- 응답: 업로드된 이미지 메타데이터 목록

### 6.2 오브젝트 이미지 조회/삭제 (신규)
- `GET /api/projects/{id}/objects/{objectId}/images`
- `DELETE /api/projects/{id}/objects/{objectId}/images/{imageId}`

### 6.3 오브젝트 대표 이미지 변경 (선택)
- `PATCH /api/projects/{id}/objects/{objectId}/images/{imageId}/primary`

### 6.4 노드 AI 생성 요청 확장
- `GenerateNodeRequest`에 `referenceObjectIds: List<Long>` 추가
- Job request_json에는 **referenceObjectIds만 유지**하고, 실행 시점에 이미지로 변환

## 7) 데이터 모델 변경

### 7.1 신규 테이블: object_images
- id, object_id, asset_id, view_type(optional), sort_order, is_primary, created_by, created_at
- ObjectSheet는 `sheet_image_url`에 대표 이미지 URL 저장

### 7.2 ObjectStatus 처리
- 이미지 0개: PENDING
- 이미지 ≥1개: SUCCEEDED
- 상태 전이 책임: 업로드/삭제 서비스에서 즉시 갱신

## 8) 저장/에셋 정책 (확정안)

- **저장 경로 통일**: `ai/object-images/{projectId}/{objectId}/{uuid}.{ext}`
- Local/S3 모두 동일 prefix 사용
- 업로드 시 Asset 생성 후 `object_images.asset_id`로 연동
- 대표 이미지는 `is_primary=true` (없으면 첫 업로드를 기본으로 설정)
- 대표 이미지 삭제 시: 다음 우선순위 이미지로 자동 승격 (정렬 기준 참조)

## 9) 노드 AI 레퍼런스 연동 방식 (확정안)

- GenerateNodeRequest의 `referenceObjectIds`를 기반으로
  - Object → ObjectImages → Asset → storageKey/URL 조회
- 레퍼런스 이미지는 **대표 이미지 + 상위 2장**만 사용 (총 3장 고정)
- 정렬 기준: `is_primary` 우선 → `sort_order` → `created_at` 오름차순
- AI 호출 방식
  - 1순위: 이미지 bytes를 **inline data**로 전달 (Gemini 지원 범위 확인 후 적용)
  - 2순위: 임시 URL 리스트를 prompt에 포함 (fallback)

## 10) 코드 변경 포인트

- `object` 도메인
  - ObjectImage 도메인/Mapper/Service 추가
  - ObjectSheet 상태 업데이트 로직 추가
- `asset` 도메인
  - 오브젝트 업로드용 Asset 등록 경로 추가 (ownerId = userId 권장)
- `node` 도메인
  - `GenerateNodeRequest`에 `referenceObjectIds` 추가
  - NodeService에서 레퍼런스 이미지 조회 및 Job request_json 확장
- `ai/gemini`
  - 레퍼런스 이미지 inline 전달 지원 (필요 시 클라이언트 확장)
- `docs/APIdocs.md`
  - 오브젝트 이미지 업로드/조회/삭제/대표 설정 API 계약 추가

## 11) 테스트 계획

### 11.1 Swagger 수동 테스트
- 오브젝트 생성 → 이미지 다중 업로드 → 대표 이미지 확인
- 오브젝트 이미지 삭제 후 status 전이 확인
- 노드 AI 생성 요청에 referenceObjectIds 포함 시 정상 동작 확인

### 11.2 통합 테스트(가능하면)
- 오브젝트 이미지 업로드 → Asset 연동 → 조회 결과 일관성
- 레퍼런스 이미지 제한(N개) 적용 확인

## 12) OBJ-1 충돌 점검 체크리스트

- ObjectSheet.status 의미 변경에 따른 FE 필터/표시 로직 확인 (PENDING → SUCCEEDED 전이 기준)
- sheet_image_url null → 값 변경 타이밍에 따른 FE 표시/캐시 영향 확인
- 오브젝트 삭제 시 object_images/asset 정리 정책 적용 여부 확인
- 기존 오브젝트 조회/리스트 응답 스키마 호환 여부 확인

## 13) 구현 순서 (OBJ-2)

1) object_images 테이블 및 도메인/Mapper/Service 추가
2) 오브젝트 이미지 업로드/조회/삭제/대표 설정 API 추가
3) ObjectSheet status/sheet_image_url 갱신 로직 추가
4) 저장 경로 및 Asset 등록 정책 적용
5) GenerateNodeRequest에 referenceObjectIds 추가 및 Job request_json 확장
6) NodeService에서 레퍼런스 이미지 조회/정렬/제한 적용
7) Gemini 클라이언트 레퍼런스 이미지 전달 확장(필요 시)
8) 문서(APIdocs) 갱신 + Swagger 테스트
