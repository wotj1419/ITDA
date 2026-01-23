# PROJ-3 씬 CRUD + 순서 변경 API (D2)

## 개요
- 목적: 씬 CRUD 완료 + reorder API 구현
- 기준: APIdocs 우선, PRD 흐름 참고

## 씬 CRUD 작업
### DTO + Controller skeleton
- `UpdateSceneRequest`
- `ReorderScenesRequest`
- 엔드포인트
  - `PUT /api/scenes/{id}`
  - `DELETE /api/scenes/{id}`
  - `PUT /api/projects/{id}/scenes/order`

### Mapper/SQL
- `SceneMapper.updateScene`
- `SceneMapper.deleteScene`
- SQL: dynamic update / hard delete

### Service 로직
- updateScene
  - title/description 둘 다 null이면 INVALID_REQUEST
  - scene 존재 확인 + 멤버십 체크
  - update 실행 후 재조회 반환
- deleteScene
  - scene 존재 확인 + 멤버십 체크
  - delete 실행 결과 확인

### Swagger 문서/예시
- 400 응답은 ValidationError 하나로 통합
- ValidationError 설명에 invalid request 포함 문구 추가
- Scene 목록 예시 status는 null 표기 (현 구현 기준)

### 수동 테스트
- 시나리오: 로그인 → 생성 → 목록 → 상세 → 수정 → 삭제 → 404 확인
- reorder 제외
- 환경: local / itda_local
- 브랜치: feat/proj-3-scene-crud-reorder (커밋/날짜 기록)

## reorder API 작업
### DTO 검증
- orderedSceneIds: @NotEmpty + 요소 @NotNull

### Mapper/SQL
- `reorderScenes(projectId, orderedSceneIds)`
- CASE WHEN 기반 order_index 일괄 업데이트
- 검증용 count 쿼리 추가
  - `countByProjectId`
  - `countByProjectIdAndIds`

### Service 로직 + 검증
- orderedSceneIds null/empty/요소 null 체크
- 중복 체크
- 멤버십 체크
- 전체 씬 수 == orderedSceneIds 길이 검증
- projectId 소속 씬 여부 검증
- reorder 적용

### 동작 규칙
- reorder 요청은 **프로젝트 내 전체 씬 ID 리스트** 전송 방식
- 일부 ID만 전송하는 방식은 지원하지 않음 (INVALID_REQUEST)

## 현재 상태
- reorder API Swagger 수동 테스트 완료 (정상 동작 확인)
- 씬 CRUD 완료
- reorder Mapper/SQL + Service 검증 로직 완료
- SceneService 공통 검증/조회 헬퍼 정리 (requireProject/requireScene/validate*)
- projectId 존재 확인 선행으로 404/403 응답 일관성 보완
- SceneService 헬퍼 메서드 구분

## 다음 작업
- reorder 동작 테스트 및 결과 기록
- (필요 시) APIdocs에 “삭제 후 정렬은 reorder 호출” 명시

## 참고 문서
- APIdocs: `S14P11C205/docs/APIdocs.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`

