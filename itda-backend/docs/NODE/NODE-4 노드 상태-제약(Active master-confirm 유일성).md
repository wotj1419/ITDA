# NODE-4 노드 상태-제약(Active master-confirm 유일성)

## 작업 개요
- D3 범위의 "노드 상태/제약" 중 **Mapper/SQL 틀 + Service/Controller 로직**까지 반영
- Active Master: 씬 단위 1개
- Confirm: SHOT 단위 1개 (기존 확정 자동 해제)
- 활성화/확정 API는 path param 방식으로 유지 (요청 DTO 미사용)

## 이번에 반영한 내용
### 1) Mapper/SQL 스켈레톤 추가
- Active Master: 동일 씬의 기존 active 해제 + 대상 active 설정
- Confirm: 동일 SHOT의 기존 confirmed 해제 + 대상 confirmed 설정
- Confirm 취소용 update 쿼리 추가

### 2) Service 로직 추가 (Active Master)
- `setActiveMaster(userId, nodeId)` 추가
- 씬 단위 락(`findByIdForUpdate`) + 권한 확인
- MASTER 타입 검증 후 기존 active 해제 -> 대상 활성 트랜잭션

### 3) Service 로직 추가 (Confirm)
- `confirmVideo(userId, nodeId)` / `unconfirmVideo(userId, nodeId)` 추가
- VIDEO 타입 검증 + SHOT 관계 검증
- 기존 confirmed 해제 -> 대상 확정 / 확정 해제

### 4) Controller 연결
- `POST /api/nodes/{id}/activate`
- `POST /api/nodes/{id}/confirm`
- `DELETE /api/nodes/{id}/confirm`

## 수정/추가 파일
- `itda-backend/src/main/java/com/itda/backend/node/repository/NodeMapper.java`
- `itda-backend/src/main/resources/mapper/NodeMapper.xml`
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`
- `itda-backend/src/main/java/com/itda/backend/node/controller/NodeController.java`

## 추가된 Mapper/SQL 메서드 목록
- `clearActiveMasterBySceneId(sceneId)`
- `setActiveMaster(nodeId)`
- `clearConfirmedByShotId(shotNodeId)`
- `setConfirmedVideo(nodeId)`
- `clearConfirmedVideo(nodeId)`

## 테스트 결과 (Swagger 수동)
- 환경: local / itda_local
- 시나리오
  - Active master 전환: MASTER 2개에서 1개만 isActive=true 유지
  - Confirm 유일성: SHOT 아래 VIDEO 2개 중 1개만 isConfirmed=true 유지
  - Unconfirm: 대상 VIDEO isConfirmed=false로 해제
- 결과: 정상 동작 확인

## 현재 상태
- Active Master: Service 트랜잭션 + Controller 연결 완료
- Confirm: Service 트랜잭션 + Controller 연결 완료
- 수동 테스트 기록 완료

## 다음 단계
- 커밋/푸시 및 MR 반영 (미완료 시)
