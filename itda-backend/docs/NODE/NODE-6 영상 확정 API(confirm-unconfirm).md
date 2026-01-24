## NODE-6 작업 가이드

### 1) 목표/범위
- 영상 확정(Confirm/Unconfirm) API 구현
- 동일 SHOT 내 **확정 VIDEO는 1개만 유지**
- 확정/해제 시 기존 확정 자동 해제 로직 보장

### 2) 기준 문서
- 일정: `S14P11C205/docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
- 인원 계획: `S14P11C205/docs/ai-movie-studio-md-pack-v3/03-team-plan-by-person.md`
- PRD: `S14P11C205/docs/PRD_AI_Movie_Studio_v2.5.md`
- APIdocs: `S14P11C205/docs/APIdocs.md`

### 3) 관련 API (APIdocs 기준)
- `POST /api/nodes/{id}/confirm`  
  설명: VIDEO 노드를 타임라인 확정으로 설정. 동일 SHOT에서 확정 VIDEO는 1개만 유지. 새 확정 시 기존 확정 자동 해제.
- `DELETE /api/nodes/{id}/confirm`  
  설명: 확정된 VIDEO 노드의 확정 상태 해제.

### 4) 핵심 제약/검증
- 노드 타입: **VIDEO**만 confirm 가능
- 관계 검증: VIDEO → SHOT 부모 관계 확인
- 권한: 씬/프로젝트 멤버십 검증
- 유일성 보장: 동일 SHOT 내 `isConfirmed=true`는 1개만 유지

### 5) 구현 체크리스트
- Service
  - confirm: 기존 확정 해제 → 대상 확정(트랜잭션)
  - unconfirm: 대상 확정 해제
- Mapper/SQL
  - shot 기준 기존 확정 해제 쿼리
  - 대상 VIDEO 확정/해제 쿼리
- 에러 코드
  - NOT_FOUND / FORBIDDEN / INVALID_NODE_RELATION 등 기존 규칙 유지

### 6) 테스트 시나리오 (Swagger 기준)
- 준비: 씬에 SHOT + VIDEO 2개 생성
- confirm A: `POST /api/nodes/{videoA}/confirm` → A만 confirmed
- confirm B: `POST /api/nodes/{videoB}/confirm` → A 해제, B만 confirmed
- unconfirm B: `DELETE /api/nodes/{videoB}/confirm` → 모두 unconfirmed
- 잘못된 타입/관계/권한 → 400/403/404 확인

### 7) 커밋 단위 제안
1. `Feat : Add video confirm mapper/service skeleton`
2. `Feat : Enforce confirm uniqueness per shot`
3. `Docs : Add confirm/unconfirm test notes`

### 종료 메모
- NODE-6(Confirm/Unconfirm) 구현/테스트는 NODE-4에서 완료됨
- 본 브랜치에서는 추가 코드 변경 없음
- 참고: NODE-4 테스트 결과 문서
