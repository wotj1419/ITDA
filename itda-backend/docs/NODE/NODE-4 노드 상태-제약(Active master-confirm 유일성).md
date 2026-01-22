# NODE-4 노드 상태-제약(Active master-confirm 유일성)

## 작업 개요
- D3 범위의 "제약 로직 설계" 중 **스켈레톤(요청 DTO + Mapper/SQL 틀)**과
  **Active Master 적용 Service 로직**까지 반영
- Confirm Service 로직/Controller 연결은 아직 미구현

## 이번에 반영한 내용
### 1) 요청 DTO 추가
- Active Master 지정용 DTO
- Confirm Video 지정용 DTO

### 2) Mapper/SQL 스켈레톤 추가
- Active Master: 동일 씬의 기존 active 해제 + 대상 active 설정
- Confirm: 동일 SHOT의 기존 confirmed 해제 + 대상 confirmed 설정
- Confirm 취소용 update 쿼리 틀 추가

### 3) Service 로직 추가 (Active Master)
- `setActiveMaster(userId, nodeId)` 추가
- 씬 단위 락(`findByIdForUpdate`) + 권한 확인
- MASTER 타입 검증 후 기존 active 해제 -> 대상 활성 트랜잭션

## 수정/추가 파일
- `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/SetActiveMasterRequest.java`
- `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/ConfirmVideoRequest.java`
- `itda-backend/src/main/java/com/itda/backend/node/repository/NodeMapper.java`
- `itda-backend/src/main/resources/mapper/NodeMapper.xml`
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`

## 추가된 Mapper/SQL 메서드 목록
- `clearActiveMasterBySceneId(sceneId)`
- `setActiveMaster(nodeId)`
- `clearConfirmedByShotId(shotNodeId)`
- `setConfirmedVideo(nodeId)`
- `clearConfirmedVideo(nodeId)`

## 현재 상태
- Active Master: Service 트랜잭션 로직까지 반영 완료
- Confirm: Mapper/SQL 스켈레톤만 준비됨 (Service/Controller 미구현)

## 다음 단계
- `Feat : Enforce confirm uniqueness` (Service 트랜잭션 로직)
- 문서/Swagger 반영
