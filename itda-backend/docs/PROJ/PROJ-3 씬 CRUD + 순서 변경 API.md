# PROJ-3 씬 CRUD + 순서 변경 API (D2)

## 1) 목표/범위
- 목표: 씬 CRUD 완료 후 순서 변경 API 적용 (drag reorder 대비)
- 기준: APIdocs 스펙 우선, PRD 흐름 참고

## 2) 1단계 진행 내용 (DTO + Controller skeleton)
- DTO 추가
  - `UpdateSceneRequest`
  - `ReorderScenesRequest`
- Controller 엔드포인트 스켈레톤 추가
  - `PUT /api/scenes/{id}`
  - `DELETE /api/scenes/{id}`
  - `PUT /api/projects/{id}/scenes/order`
- Service 스텝 메서드 추가 (미구현 표시)
  - update/delete/reorder는 다음 단계에서 구현

## 3) 2단계 진행 내용 (Mapper/SQL)
- Mapper 메서드 추가
  - `SceneMapper.updateScene`
  - `SceneMapper.deleteScene`
- SQL 추가
  - dynamic update (null 필드 무시)
  - hard delete (scene row)

## 4) 현재 상태
- 요청/응답 구조와 라우팅 정의 완료
- update/delete SQL 반영 완료
- 비즈니스 로직(Service)과 reorder는 아직 미구현

## 5) 다음 단계
- 3단계: CRUD Service 로직
- 4단계: CRUD Swagger 예시/검증
- 5단계: reorder API 착수

## 6) 참고 문서
- APIdocs: `S14P11C205/docs/APIdocs.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
