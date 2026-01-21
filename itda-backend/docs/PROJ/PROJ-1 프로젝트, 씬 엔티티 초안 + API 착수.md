# Day1 PROJ-1 작업 정리 (프로젝트/씬 최소 기능)

## TL;DR
- 프로젝트/씬 최소 API 구현 완료 (생성/목록/상세)
- 스키마 로컬 초기화 가능 (`schema-local.sql`)
- 문서 기준: APIdocs 스펙 우선 (구현 차이는 이슈로 분리)
- 썸네일/상태/멤버 요약은 스펙 포함, 구현 연동은 추후 확정

## 1) 목표와 범위
- 목표: 프로젝트/씬 최소 API를 구현해서 D1 범위의 기능 흐름이 동작하도록 기반을 마련
- 범위: 프로젝트 생성/목록/상세, 씬 생성/목록/상세
- 인증: 기존 JWT 인증 흐름 그대로 사용 (CustomUserDetails에서 userId 사용)

## 2) 구현 결과 요약 (한눈 요약)
- 프로젝트 API 3개와 씬 API 3개를 구현함
- MyBatis Mapper/SQL 및 DTO/서비스/컨트롤러 계층을 모두 추가함
- DB 스키마는 `schema-local.sql` 기준으로 정리하여 로컬 초기화 가능
- D1 한정으로 일부 응답 필드 연동은 추후 확정

## 3) API 구현 상세
### 프로젝트 API
- `POST /api/projects`
  - 기능: 새로운 프로젝트(영화 제작 공간)를 생성하고, 요청자를 그 프로젝트의 소유자(OWNER)로 등록합니다.
  - 요청: title(필수), description/genre(선택)
  - 처리: projects 테이블 insert 후 project_members에 OWNER로 연결
  - 응답: projectId, title, role(OWNER), createdAt
- `GET /api/projects`
  - 기능: 내가 참여 중인(소유하거나 초대받은) 모든 프로젝트의 목록을 조회합니다. 대시보드 카드 UI에 사용됩니다.
  - 처리: 참여 중인 프로젝트 목록 조회 후 페이징 적용 (방식 미정, 스펙은 page/size 기준)
  - 응답: items, page, size, total
  - 특이사항: thumbnailUrl은 스펙에 포함되며, 연동은 추후 확정
- `GET /api/projects/{id}`
  - 기능: 특정 프로젝트의 상세 정보를 조회합니다. 프로젝트 내부로 진입할 때 호출됩니다.
  - 처리: 프로젝트 존재 확인 + 멤버십 확인
  - 응답: projectId, title, description, genre, myRole, ownerId, createdAt
  - 특이사항: members는 스펙에 포함되며, 연동은 추후 확정

### 씬 API
- `POST /api/projects/{id}/scenes`
  - 기능: 프로젝트 내 스토리보드에 새로운 씬(장면)을 하나 추가합니다. 순서는 자동으로 마지막 번호로 지정됩니다.
  - 처리: 프로젝트 멤버십 확인 후 scene 생성
  - order_index는 해당 프로젝트 내 max + 1로 자동 증가
  - 응답: sceneId, title, order
- `GET /api/projects/{id}/scenes`
  - 기능: 해당 프로젝트의 모든 씬 목록을 순서대로 조회합니다. 스토리보드 화면을 그릴 때 사용됩니다.
  - 처리: 멤버십 확인 후 해당 프로젝트의 씬 목록 반환 (order_index 기준)
  - 응답: sceneId, title, order, status
  - 특이사항: status는 스펙에 포함되며, 연동은 추후 확정
- `GET /api/scenes/{id}`
  - 기능: 특정 씬의 상세 정보를 조회합니다. (현재 UI에서는 잘 안 쓰이지만 로직 검증용으로 구현됨)
  - 처리: scene 조회 후 멤버십 확인
  - 응답: sceneId, projectId, title, description, order

## 4) 서비스 로직 핵심 흐름
### 프로젝트 생성 흐름
1. 요청 DTO 검증
2. `projects` insert (status=ACTIVE, created/updated 기본값)
3. `project_members`에 OWNER로 연결
4. 응답 DTO 생성

### 씬 생성 흐름
1. `project_members`로 멤버십 확인
2. `scenes`에서 max(order_index) 조회
3. `scenes` insert (order_index = max + 1)
4. 응답 DTO 생성

## 5) DB 스키마 관련 정리
- 기준 파일: `S14P11C205/itda-backend/src/main/resources/sql/schema-local.sql`
- 주요 테이블 (D1 핵심):
  - projects, project_members, scenes, nodes
- 반영 포인트:
  - `CREATE DATABASE IF NOT EXISTS itda_local; USE itda_local;` 포함
  - DROP 구간에 `SET FOREIGN_KEY_CHECKS = 0/1` 추가
  - 줄바꿈/주석 정리로 컬럼 파싱 문제 수정
  - `scenes.active_master_node_id` FK는 nodes 생성 후 ALTER로 추가

## 6) 실제 추가/변경된 코드 위치
### 프로젝트
- Controller: `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/controller/ProjectController.java`
- Service: `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/service/ProjectService.java`
- Domain: `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/domain/Project.java`
- Mapper: `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/repository/ProjectMapper.java`
- Mapper: `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/repository/ProjectMemberMapper.java`
- Mapper XML: `S14P11C205/itda-backend/src/main/resources/mapper/ProjectMapper.xml`
- Mapper XML: `S14P11C205/itda-backend/src/main/resources/mapper/ProjectMemberMapper.xml`
- DTOs:
  - `.../project/controller/dto/request/CreateProjectRequest.java`
  - `.../project/controller/dto/response/ProjectCreateResponse.java`
  - `.../project/controller/dto/response/ProjectSummaryResponse.java`
  - `.../project/controller/dto/response/ProjectListResponse.java`
  - `.../project/controller/dto/response/ProjectDetailResponse.java`
  - `.../project/repository/dto/ProjectSummary.java`

### 씬
- Controller: `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/controller/SceneController.java`
- Service: `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/service/SceneService.java`
- Domain: `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/domain/Scene.java`
- Mapper: `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/repository/SceneMapper.java`
- Mapper XML: `S14P11C205/itda-backend/src/main/resources/mapper/SceneMapper.xml`
- DTOs:
  - `.../scene/controller/dto/request/CreateSceneRequest.java`
  - `.../scene/controller/dto/response/SceneCreateResponse.java`
  - `.../scene/controller/dto/response/SceneSummaryResponse.java`
  - `.../scene/controller/dto/response/SceneDetailResponse.java`
  - `.../scene/repository/dto/SceneSummary.java`

## 7) 검증 체크리스트 (D1 마감 기준)
- DB 초기화:
  - `itda_local`에 `schema-local.sql`이 정상 적용되었는지 확인
- 최소 플로우:
  - 호출 순서 예시: 로그인 → 프로젝트 생성 → 프로젝트 목록/상세 → 씬 생성 → 씬 목록/상세
  1) 로그인
  2) 프로젝트 생성
  3) 프로젝트 목록/상세 조회
  4) 씬 생성
  5) 씬 목록/상세 조회

## 8) D1 기준 남은 이슈/주의사항
- 프로젝트 상세 응답의 `members` 필드 미포함 (추후 확장 필요)
- thumbnailUrl/status는 스펙 포함, 구현 연동/표시 방식 추후 확정
- `page`/`size` 음수 입력 시 예외 가능 (검증 보완 여지)
- 프로젝트 미존재 시 씬 API가 403으로 떨어질 수 있음 (404 정책 확인 필요)

## 9) 다음 작업 연결 포인트
- 프로젝트 상세에 members 요약 추가
- 씬 status 계산/저장 방식 확정
- 프로젝트 썸네일(assets 연동) 설계

## 10) 사용자 시나리오 기반 코드 분석 (AI Movie Studio 실제 사용 흐름)
Day 1 범위의 실제 동작 흐름을 “사용자 행동 → API 호출 → 서버 처리” 순서로 정리합니다.

### 시나리오 1. 새 프로젝트 생성
- 사용자 행동: 대시보드에서 **[+ 새 프로젝트]** 클릭 → 제목/장르 입력
- 호출 API: `POST /api/projects`
- 서버 처리:
  1) 요청 DTO 검증
  2) `projects`에 프로젝트 생성 (status=ACTIVE)
  3) `project_members`에 요청자를 OWNER로 연결
- 응답: projectId, title, role(OWNER), createdAt

### 시나리오 2. 대시보드 프로젝트 목록 조회
- 사용자 행동: 대시보드 진입 시 프로젝트 카드 목록 표시
- 호출 API: `GET /api/projects?page=0&size=20`
- 서버 처리:
  1) `project_members` 기준으로 참여 프로젝트 조회
  2) 멤버 수/씬 수는 서브쿼리로 계산
  3) 응답 페이징 방식은 미정 (스펙은 page/size 기준)
- 응답: items, page, size, total
- 참고: thumbnailUrl은 스펙 포함, 연동은 추후 확정

### 시나리오 3. 씬 생성
- 사용자 행동: 프로젝트 진입 후 **[+ Scene 추가]** 클릭
- 호출 API: `POST /api/projects/{id}/scenes`
- 서버 처리:
  1) `project_members`로 멤버십 확인
  2) `scenes`에서 max(order_index) 조회
  3) 새 씬 생성 (order_index = max + 1)
- 응답: sceneId, title, order

### 시나리오 4. 씬 목록/상세 조회
- 사용자 행동: 스토리보드 화면 진입 및 씬 선택
- 호출 API:
  - `GET /api/projects/{id}/scenes`
  - `GET /api/scenes/{id}`
- 서버 처리:
  - 목록은 order_index 기준 정렬 조회
  - 상세는 scene 조회 후 멤버십 확인
- 응답:
  - 목록: sceneId, title, order, status (스펙 포함, 연동은 추후 확정)
  - 상세: sceneId, projectId, title, description, order

---
위 흐름이 Day 1에 구현된 전체 동작 범위이며, Controller → Service → Mapper 순서로 처리됩니다.

## 11) Swagger 문서화 개선 작업 정리
### 목표
- 성공/실패 응답 예시를 ApiResponse 구조로 통일
- 컨트롤러 가독성 유지 (ref 기반)
- 공통 응답을 한 곳에서 관리

### 적용 내용
- 전역 components.responses 사용 (성공/실패 모두 ref 방식)
- 예시 JSON을 전용 클래스로 분리해 SwaggerConfig 간소화
- APIdocs/PRD 기준 예시 필드 보정
  - 프로젝트 목록: thumbnailUrl 포함
  - 프로젝트 상세: members 요약 추가
  - 씬 목록: status 예시 반영 (COMPLETED/미기재)

### 변경 파일
- 공통 예시 등록
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/global/config/SwaggerExamples.java`
- Swagger 설정 간소화
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/global/config/SwaggerConfig.java`
- 컨트롤러 응답 ref 적용
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/project/controller/ProjectController.java`
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/controller/SceneController.java`

### 기대 효과
- Swagger UI에서 성공/실패 예시가 정확히 표시됨
- 신규 API 추가 시 공통 응답 ref만 붙이면 문서 품질 유지

## 12) Scene 도메인 클린코드 개선 (가독성/유지보수)
### 목표
- 도메인 생성 책임 분리
- 서비스 로직 단순화 (orderIndex 계산 분리)
- 변경 영향 범위를 최소화하면서 가독성 개선

### 적용 내용
- Scene 생성 로직을 도메인 팩토리로 이동
  - `Scene.create(projectId, title, description, orderIndex)`
- orderIndex 계산을 Mapper 쿼리로 이동
  - `findNextOrderIndex`로 변경 (DB에서 `MAX + 1`)
- 서비스에서는 팩토리 호출만 수행

### 변경 파일
- 도메인 팩토리 추가
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/domain/Scene.java`
- Mapper 메서드/쿼리 변경
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/repository/SceneMapper.java`
  - `S14P11C205/itda-backend/src/main/resources/mapper/SceneMapper.xml`
- 서비스 로직 단순화
  - `S14P11C205/itda-backend/src/main/java/com/itda/backend/scene/service/SceneService.java`

### 기대 효과
- Scene 생성 흐름이 한 곳에서 관리되어 유지보수 용이
- 서비스 계층 가독성 향상 및 null/계산 실수 방지

## 13) SceneService 핵심 로직 요약
### 역할 요약
- 프로젝트 멤버 권한 체크 (접근 제어)
- 씬 생성 시 orderIndex 계산 및 저장
- 씬 목록/상세 조회 응답 변환

### 핵심 흐름
- 생성: 멤버십 확인 -> 다음 orderIndex 조회 -> Scene 생성 -> DB 저장 -> 응답 DTO 변환
- 목록: 멤버십 확인 -> 목록 조회 -> 요약 DTO 변환
- 상세: scene 조회 -> 멤버십 확인 -> 상세 DTO 변환

### 예외/정책
- 멤버 아닐 경우 `FORBIDDEN`
- scene 미존재 시 `SCENE_NOT_FOUND`
- 페이징 방식은 스펙 기준으로 확정 예정
