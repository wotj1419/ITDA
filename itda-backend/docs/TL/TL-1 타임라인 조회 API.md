## TL-1 작업 문서

### 1) 목표/범위
- 타임라인 조회 API 구현 (`/timeline`)
- **확정(confirmed) VIDEO만 노출**
- D4 범위는 조회(Read) 중심, reorder/merge는 연계 참고

### 2) 기준 문서
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
- 인원 계획: `S14P11C205/docs/ai-movie-studio-md-pack-v3/03-team-plan-by-person.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- APIdocs: `S14P11C205/docs/APIdocs.md`

### 3) 관련 API (APIdocs 기준)
- `GET /api/scenes/{id}/timeline`
  - 씬 내 **확정된 VIDEO 노드** 목록을 순서대로 반환
- `GET /api/projects/{id}/timeline`
  - 프로젝트 타임라인(씬 병합 결과) 조회
  - `sceneVideoId` 흐름: confirm -> scene timeline reorder -> merge -> job.done -> project timeline

### 4) 데이터 기준(확정 VIDEO만)
- **씬 타임라인**
  - 소스: `nodes`
  - 조건: `node_type = 'VIDEO'` AND `is_confirmed = 1` AND `scene_id = {id}`
  - 정렬: `order_index`
  - 매핑
    - `videoNodeId` = `nodes.id`
    - `sceneId` = `nodes.scene_id`
    - `thumbnailUrl` = `content_url`(없으면 null)
    - `duration` = 저장 필드 확인 필요(없으면 null/0)
    - `order` = `order_index`
    - `totalDuration` = items duration 합산
- **프로젝트 타임라인**
  - APIdocs는 `sceneVideoId` 기반 전제
  - D4에서는 **데이터 소스 확정 필요**
    - `timeline_items`(project_id, order_index) 사용 가능 여부 확인
    - merge 결과 저장 구조가 아직 없으면 **빈 리스트 반환** 허용

### 스키마 의존 사항
- `scene_videos` 테이블 및 `timeline_items.scene_video_id`, `timeline_items.video_node_id` 컬럼 사용 전제
- 정렬 기준은 `timeline_items.order_index`

### 5) 구현 체크리스트
- Controller
  - 인증/권한: 프로젝트 멤버십 확인
  - `GET /api/scenes/{id}/timeline`
  - `GET /api/projects/{id}/timeline`
- Service
  - confirmed VIDEO만 필터
  - `totalDuration` 계산
- Mapper/SQL
  - sceneId 기준 confirmed VIDEO 조회 쿼리
  - projectId 기준 timeline 아이템 조회(있다면)
- DTO
  - `TimelineItemResponse`(videoNodeId, sceneId, thumbnailUrl, duration, order)
  - `TimelineResponse`(items, totalDuration)

### 6) 에러 처리
- `PROJECT_NOT_FOUND` / `SCENE_NOT_FOUND`
- `FORBIDDEN` (멤버십 없음)
- items 없으면 빈 리스트 + totalDuration 0

### 7) 테스트 시나리오 (Swagger 기준)
- 준비: 씬에 VIDEO 2개 생성 후 1개 confirm
- `GET /api/scenes/{id}/timeline`
  - confirm된 VIDEO만 1개 반환되는지 확인
- confirm 변경 후 재조회
  - 기존 confirm 해제되고 새로운 VIDEO만 포함되는지 확인
- unconfirm 후 조회
  - items 빈 리스트, totalDuration 0 확인
- 프로젝트 타임라인
  - merge 결과가 없으면 빈 리스트 반환 확인(동작 정의 필요)

### 8) 커밋 단위 제안
1. `Feat : Add timeline read DTO/mapper skeleton`
2. `Feat : Implement scene timeline read (confirmed VIDEO only)`
3. `Feat : Implement project timeline read (fallback empty if no merge data)`
4. `Docs : Add TL-1 test notes`

### 9) 1단계 진행 현황
- 완료: `Feat : Add timeline read DTO/mapper skeleton`
- DTO/Mapper 추가 완료 (scene/project timeline 응답 DTO + mapper DTO)
- SQL 기준 확정: `timeline_items.order_index` 기준 정렬
- project timeline은 `timeline_items.scene_video_id` -> `scene_videos` 조인 기준
