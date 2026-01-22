# PROJ-3 씬 CRUD + 순서 변경 API (D2)

## 1) 목표/범위
- 목표: 씬 CRUD 완료 + 순서 변경 API 착수 (drag reorder 대비)
- 기준: APIdocs 스펙 우선, PRD 흐름 참고

## 2) 1단계 진행 내용 (DTO + Controller skeleton)
- DTO 추가
  - `UpdateSceneRequest`
  - `ReorderScenesRequest`
- Controller 엔드포인트 추가
  - `PUT /api/scenes/{id}`
  - `DELETE /api/scenes/{id}`
  - `PUT /api/projects/{id}/scenes/order`
- Service 메서드 시그니처 추가 (초기 스텁)

## 3) 2단계 진행 내용 (Mapper/SQL)
- Mapper 메서드 추가
  - `SceneMapper.updateScene`
  - `SceneMapper.deleteScene`
- SQL 추가
  - dynamic update (null 필드 무시)
  - hard delete (scene row)

## 4) 3단계 진행 내용 (Service 로직)
- updateScene
  - title/description 모두 null이면 INVALID_REQUEST
  - scene 존재 확인 후 멤버십 체크
  - update 실행 후 재조회하여 상세 반환
- deleteScene
  - scene 존재 확인 후 멤버십 체크
  - delete 실행 결과 확인

## 5) 4단계 진행 내용 (Swagger 문서/예시)
- 400 응답은 ValidationError 하나로 통합 (InvalidRequest 중복 제거)
- ValidationError 설명에 invalid request 포함 문구 추가
- Scene 목록 예시에서 status 값은 null로 표기 (현 구현 기준)

## 6) 5단계 진행 내용 (CRUD 플로우 테스트)
- 테스트 시나리오
  - 로그인: POST /api/auth/login → accessToken 확보
  - 씬 생성: POST /api/projects/{projectId}/scenes (201 + sceneId, order)
  - 씬 목록: GET /api/projects/{projectId}/scenes (생성 씬 포함, order 증가 확인)
  - 씬 상세: GET /api/scenes/{sceneId} (title/description 확인)
  - 씬 수정: PUT /api/scenes/{sceneId} (변경 반영 확인)
  - 씬 삭제: DELETE /api/scenes/{sceneId} (200)
  - 삭제 확인: GET /api/scenes/{sceneId} → 404
- 주의: reorder는 미구현이라 테스트에서 제외
- 수동 테스트 결과: Swagger에서 진행했고 이상 없음
- 테스트 환경(프로필/DB): local / itda_local
- 테스트 대상 버전: feat/proj-3-scene-crud-reorder (커밋/날짜 기록)

## 7) 6단계 진행 내용 (reorder Mapper/SQL + DTO 검증)
- DTO 검증 보강
  - orderedSceneIds 요소 null 방지 (@NotNull)
- Mapper/SQL 추가
  - reorderScenes(projectId, orderedSceneIds)
  - CASE WHEN 기반 order_index 일괄 업데이트

## 8) 현재 상태
- CRUD 서비스 로직/Swagger 예시/수동 테스트 완료
- reorder Mapper/SQL + DTO 검증 완료
- reorder API는 Service/검증 로직 미구현

## 9) 다음 단계
- reorder Service 로직 구현 (validation + 소속 체크)
- reorder 동작 테스트 및 문서 정리

## 10) 참고 문서
- APIdocs: `S14P11C205/docs/APIdocs.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
