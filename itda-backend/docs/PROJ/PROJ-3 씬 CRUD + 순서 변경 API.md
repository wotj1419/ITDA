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
- update/reorder 응답에 InvalidRequest(400) 추가
- Scene 목록 예시에서 status 값을 null로 표기 (현 구현 기준)

## 6) 현재 상태
- update/delete 서비스 로직까지 반영 완료
- CRUD Swagger 응답 예시 정리 완료
- reorder API는 미구현 상태 (스텁 유지)

## 7) 다음 단계
- CRUD 플로우 테스트 (create/list/detail/update/delete)
- reorder API 구현 (validation + batch update)

## 8) 참고 문서
- APIdocs: `S14P11C205/docs/APIdocs.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
