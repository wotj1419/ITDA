# 오브젝트 시트 AI 생성 구현 계획서 (상세)

> 작성일: 2026-01-26
> 담당: 김은서
> 기준 일정: 2026-01-27(화) OBJ-1

## 1) 목적

오브젝트 시트(캐릭터/소품/기타) 생성 기능을 **AI 기반 이미지 생성 워크플로우**로 구현하기 위한
DB/도메인/CRUD/Job/Worker/FE 연동 흐름을 상세히 문서화한다.

## 2) 범위

### ✅ 1/27(화) — OBJ-1 (현재 우선 범위)
- 오브젝트 시트 **DB 스키마 / 엔티티 / CRUD API(v1)** 구현
- API 응답 포맷, 기본 검증, 권한 체크 최소 적용

### 🚧 다음 단계 (OBJ-2~)
- 생성 요청 시 Job 발행 + Streams publish
- 이미지 Worker(Gemini) 연동 + 결과 저장/상태 업데이트
- WS 이벤트 및 FE 진행 상태 표시

## 3) 참고 문서

- `docs/PRD_AI_Movie_Studio_v2.5.md`
  - 4.2.3 오브젝트 시트 생성, 8.3 오브젝트 시트 API
- `docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
  - W4 D2: OBJ-1/OBJ-2 업무 범위 정의
- `docs/APIdocs.md`
  - 2.3 오브젝트 시트 API 요청/응답 형식
  - 공통 응답 포맷, ACCEPTED 규칙
- `itda-backend/src/main/resources/sql/schema-local.sql`
  - 기존 테이블 구조 (assets, generation_jobs 등)

## 4) 현 상태

- FE는 `services/mock/characters.ts` 기반으로 캐릭터(Mock) 생성 중
- BE에 object 도메인 패키지 placeholder만 존재
- `schema-local.sql`에 **objects 테이블이 없음**
- 비동기 Job 패턴은 `generation_jobs` 테이블로 이미 정의됨

## 4.1) 오브젝트의 역할/사용처 (씬/노드 연동 관점)

오브젝트 시트는 단순 목록이 아니라 **씬/노드 생성의 핵심 입력값**이다.

### 역할 요약
- **씬 편집에서 “등장 오브젝트” 선택**: 씬 컨텍스트를 정의하고 장면 내 등장 요소를 고정
- **MASTER 노드 프롬프트 생성 입력**: `objectIds` 기반으로 프롬프트에 오브젝트 이름/특징 포함
- **이미지 생성 시 레퍼런스 제공**: 오브젝트 시트 이미지(정면/측면/후면 등)를 AI에 참조로 전달 → **일관성 유지**

### 현재 코드 기준 연동 지점
- FE: `components/scene-editor/panels/MasterImagePanel.vue`
  - 등장 오브젝트 선택 UI (objectIds)
- FE: `services/api/ai.ts`
  - `objectIds`/`objects`를 프롬프트 생성 요청 payload로 전달 (현재 objectIds가 문자열 리스트로 들어갈 위험)
- BE: `AiPromptService`
  - `objects` 리스트를 프롬프트 문장에 포함
- BE: `NodeService`
  - MASTER 노드 settings 내 `objectIds` 정규화 로직 존재
- BE: `UpdateSceneRequest`는 `objectIds` 필드를 받지만, **저장 로직/응답 반영은 없음**

### 설계 보완 포인트 (확정안 반영)
- `objectIds` DB 저장 방식 **확정: 옵션 A (조인 테이블)** 로 진행
  - 이유: 정규화/조회 일관성/확장성 측면에서 가장 안전
  - 씬과 오브젝트는 N:M 관계 → 중간 테이블이 표준 해법
  - 씬 상세/수정/프롬프트 생성 흐름을 일관되게 유지 가능

#### 확정안 구현 상세 (옵션 A)

##### 1) DB 스키마
```sql
CREATE TABLE scene_objects (
    scene_id BIGINT NOT NULL,
    object_id BIGINT NOT NULL,
    PRIMARY KEY (scene_id, object_id),
    KEY idx_scene_objects_object (object_id),
    CONSTRAINT fk_scene_objects_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_objects_object FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
- 추가 위치: `schema-local.sql`에 scenes 다음 섹션으로 배치
- migration 도 동일 구조로 추가

##### 2) BE 도메인/Repository
- 신규 Mapper/Repository
  - `SceneObjectMapper` (MyBatis)
    - `insertBatch(sceneId, objectIds)`
    - `deleteBySceneId(sceneId)`
    - `findObjectIdsBySceneId(sceneId)`
- Object 조회
  - `ObjectRepository`에서 `findByIds(List<Long>)` 추가

##### 3) Service 로직
- `SceneService.updateScene(...)`
  - request.objectIds가 **null이면 기존 유지**
  - request.objectIds가 **빈 배열이면 모두 제거**
  - request.objectIds가 **값이 있으면 교체**
    - `scene_objects` 기존 삭제 → 신규 bulk insert
- `SceneService.getSceneDetail(...)`
  - `scene_objects`에서 objectIds 조회 → 응답에 포함
- `ObjectService`와 연결하여 **objectIds 유효성 검증** (존재하지 않는 ID 차단)

##### 4) DTO/응답
- `SceneDetailResponse`에 `objectIds` 필드 추가
- (필요 시) `SceneSummaryResponse`에는 미포함 (리스트에서는 노이즈)
- `UpdateSceneRequest`의 objectIds는 이미 존재 → 실제 저장 로직만 추가

##### 5) API 응답 예시 (Scene Detail)
```json
{
  "code": "SUCCESS",
  "data": {
    "sceneId": 201,
    "projectId": 101,
    "title": "Scene 1: Mars Base",
    "description": "Morning at the base",
    "order": 1,
    "objectIds": [1, 3, 5]
  }
}
```

##### 6) 프롬프트 생성 연동
- **원칙:** 프롬프트 생성에 들어가는 `objects`는 **텍스트(이름/특징)** 여야 함
- 구현 위치:
  - BE에서 `objectIds` → `ObjectSheet.name/description` 조회 후 텍스트 변환
  - 변환 결과를 `AiPromptGenerateRequest.objects`로 전달
- 이유:
  - FE에서 id만 보내면 `AiPromptService`가 의미 없는 숫자를 문장에 넣게 됨

##### 7) FE 흐름
- 씬 상세 API에서 `objectIds` 받아오기
- MASTER 노드 생성/프롬프트 요청 시 `objectIds` 유지 전달
- 오브젝트 상세(이미지/이름)는 별도 캐시/목록 API로 매핑
- 프롬프트 생성 시 **objectIds → object names/description** 매핑 필요
  - 현재 FE는 objectIds를 그대로 `objects[]`로 보낼 수 있음 → 프롬프트 품질 저하/오류 가능
- PRD 기준 씬 상세에 등장 오브젝트 표시 필요 (현재 SceneDetailResponse에 없음)

## 5) 데이터 모델 설계

### 5.1 오브젝트 시트 테이블 (신규)

> 기존 DB 구조와 정렬을 위해 `projects` 다음에 배치 권장

```sql
CREATE TABLE objects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,         -- CHARACTER, PROP, ETC
    description TEXT NOT NULL,
    style VARCHAR(100),
    sheet_image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, SUCCEEDED, FAILED
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_objects_project (project_id),
    KEY idx_objects_status (status),
    CONSTRAINT fk_objects_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_objects_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.2 도메인 모델 초안

- ObjectType: `CHARACTER | PROP | ETC`
- ObjectStatus: `PENDING | RUNNING | SUCCEEDED | FAILED`
- ObjectSheet (응답 DTO): id, name, type, description, style, sheetImageUrl, status

## 6) API 설계 (APIdocs 준수)

### 6.1 생성 API (비동기 전제)
- `POST /api/projects/{id}/objects`
- 요청:
```json
{
  "name": "우주인 민준",
  "type": "CHARACTER",
  "description": "20대 후반 남성, 우주복 착용, 헬멧 벗음",
  "style": "SF 실사"
}
```
- 응답 (비동기 기준):
```json
{
  "code": "ACCEPTED",
  "data": {
    "jobId": 123,
    "status": "PENDING"
  }
}
```

> **OBJ-1 구현시 결정 필요:**
> - Job 발행 전까지는 임시로 `SUCCESS + ObjectSheet` 반환 or `ACCEPTED + jobId(placeholder)` 반환
> - FE와 계약 충돌 방지를 위해 **OBJ-2 완료 전까지는 FE에서 mock 유지**가 안전

### 6.2 목록/상세/수정/삭제
- `GET /api/projects/{id}/objects`
- `GET /api/objects/{id}`
- `PUT /api/objects/{id}`
- `DELETE /api/objects/{id}`

응답은 공통 포맷(`code`, `data`) 사용.

## 7) 비동기 생성 흐름 설계 (OBJ-2~)

### 7.1 처리 흐름
1. FE 생성 요청
2. BE에서 Object 생성 (status=PENDING) + GenerationJob 생성
3. Streams publish (OBJECT_SHEET_GENERATION)
4. Worker가 Gemini 이미지 생성
5. 이미지 저장(assets) + Object.sheet_image_url 업데이트 + status=SUCCEEDED
6. WS 이벤트(job.done/job.failed) 발행
7. FE가 상태 업데이트 및 이미지 표시

### 7.2 Job payload 예시
```json
{
  "objectId": 101,
  "projectId": 12,
  "name": "우주인 민준",
  "type": "CHARACTER",
  "description": "30대 남성, 우주복 착용, 헬멧, 밝은 표정",
  "style": "SF 실사"
}
```

### 7.3 Job 타입 정의
- 신규: `OBJECT_SHEET_GENERATION`
- `generation_jobs.job_type`에 추가
- 상태 전이: PENDING → RUNNING → SUCCEEDED/FAILED

## 8) Worker/Asset 연동 설계

### 8.1 이미지 생성
- Gemini 2.5 Flash Image 사용 (PRD 기준)
- Prompt 템플릿은 ObjectType별 분기

예시 규칙:
- CHARACTER: 정면/측면/후면 + 표정/포즈 다양화
- PROP: 다양한 각도 + 디테일 강조
- 공통: 단색/중립 배경, 형태/재질 명확

### 8.2 결과 저장
- assets 테이블에 IMAGE 에셋 생성
- storage_key 예시: `ai/object/{projectId}/object-{objectId}.png`
- Object.sheet_image_url = presigned URL (또는 storage_key + resolver)

## 9) FE 연동 계획

### 9.1 현재 구조
- `services/mock/characters.ts`
- `components/project/AddCharacterModal.vue`
- `pages/project/sections/ObjectsTab.vue`
- `stores/character.ts`

### 9.2 변경 방향
- Mock → API 전환 시점에 `services/api/objects.ts` 활성화
- ObjectType 선택 UI 추가 (캐릭터/소품/기타)
- 상태 표시(PENDING/RUNNING/SUCCEEDED/FAILED)
- 완료 시 이미지 카드 렌더링

## 10) 테스트 계획

### 10.1 OBJ-1
- Object CRUD API 통합 테스트 1건
- 유효성 검증 테스트 (name/type/description)

### 10.2 OBJ-2~
- GenerationJob 생성 검증
- Worker 성공/실패 케이스
- status 전이 및 WS 이벤트

## 11) 일정 (W4 D2 기준)

### 01/27(화) — OBJ-1
1. objects 테이블 추가 (schema-local.sql + migration)
2. Entity/Repository/Service/Controller 생성
3. CRUD API v1 동작 확인

### 이후 (OBJ-2~)
1. Object 생성 요청 → Job 발행
2. Worker 구현 및 상태 업데이트
3. FE 상태 표시/토스트 연동

## 12) 리스크/결정 필요

- API 응답 정책: `ACCEPTED + jobId` vs `SUCCESS + object`
- 이미지 저장 방식: `assets` 사용 여부 / sheet_image_url 직접 저장 여부
- ObjectType 확장 시 enum 변경 범위
- Scene ↔ Object 연결 테이블 필요 여부 (scene_objects 등)
- objectIds → object names/description 매핑 위치 (FE 변환 vs BE 조회)
- SceneDetailResponse에 등장 오브젝트 포함 여부 및 응답 계약

---

## 구현 순서 (OBJ-1 기준)

1) DB 스키마 반영  
   - `itda-backend/src/main/resources/sql/schema-local.sql`  
     - `objects` 테이블 추가  
     - `scene_objects` 조인 테이블 추가  
   - 마이그레이션 체계가 있으면 신규 migration 생성 (동일 스키마 반영)

2) 오브젝트 도메인 CRUD 구현  
   - 패키지: `itda-backend/src/main/java/com/itda/backend/object/`  
     - Entity: Object  
     - Enum: ObjectType, ObjectStatus  
     - DTO: Create/Update/Response  
     - Mapper/Repository: 기본 CRUD  
     - Service: 권한/유효성 검증 포함  
     - Controller:  
       - `POST /api/projects/{id}/objects`  
       - `GET /api/projects/{id}/objects`  
       - `GET /api/objects/{id}`  
       - `PUT /api/objects/{id}`  
       - `DELETE /api/objects/{id}`

3) 씬-오브젝트 연동 구현  
   - `scene_objects` Mapper 추가 (insert/delete/find)  
   - `SceneService.updateScene(...)`에 objectIds 저장 로직 추가  
   - `SceneDetailResponse`에 `objectIds` 필드 추가  

4) 응답/계약 정리  
   - `docs/APIdocs.md`  
     - 씬 상세 응답에 objectIds 포함 예시 추가  
     - (필요 시) 오브젝트 API 응답 형식 보강

5) 최소 테스트  
   - Object CRUD 1건 통합 테스트  
   - Scene update 시 objectIds 저장/조회 확인

---

## 변경 완료 목록 (OBJ-1 기준)

- DB 스키마: `itda-backend/src/main/resources/sql/schema-local.sql`  
  - `objects` 테이블 추가  
  - `scene_objects` 테이블 추가
- 오브젝트 도메인/CRUD: `itda-backend/src/main/java/com/itda/backend/object/`  
  - ObjectType/ObjectStatus/ObjectSheet  
  - Create/Update/Response DTO  
  - ObjectMapper + ObjectService + ObjectController(Swagger)
- 씬-오브젝트 연동:  
  - `SceneObjectMapper` + 매퍼 XML 추가  
  - `SceneService.updateScene/getSceneDetail`에 objectIds 저장/조회 로직 추가
- 응답 모델: `itda-backend/src/main/java/com/itda/backend/scene/controller/dto/response/SceneDetailResponse.java`  
  - objectIds 필드 추가
- 에러 코드: `itda-backend/src/main/java/com/itda/backend/global/response/ErrorCode.java`  
  - OBJECT_NOT_FOUND 추가
- 문서: `docs/APIdocs.md`  
  - 씬 상세 응답 예시에 objectIds 추가
