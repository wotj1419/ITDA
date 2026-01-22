# NODE-4 노드 상태-제약(Active master-confirm 유일성)

## 작업 개요
- D3 범위의 "제약 로직 설계" 중 **스켈레톤(요청 DTO + Mapper/SQL 틀)**만 반영
- 실제 Service 로직/Controller 연결은 아직 미구현

## 이번에 반영한 내용
### 1) 요청 DTO 추가
- Active Master 지정용 DTO
- Confirm Video 지정용 DTO

### 2) Mapper/SQL 스켈레톤 추가
- Active Master: 동일 씬의 기존 active 해제 + 대상 active 설정
- Confirm: 동일 SHOT의 기존 confirmed 해제 + 대상 confirmed 설정
- Confirm 취소용 update 쿼리 틀 추가

## 수정/추가 파일
- `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/SetActiveMasterRequest.java`
- `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/ConfirmVideoRequest.java`
- `itda-backend/src/main/java/com/itda/backend/node/repository/NodeMapper.java`
- `itda-backend/src/main/resources/mapper/NodeMapper.xml`

## 추가된 Mapper/SQL 메서드 목록
- `clearActiveMasterBySceneId(sceneId)`
- `setActiveMaster(nodeId)`
- `clearConfirmedByShotId(shotNodeId)`
- `setConfirmedVideo(nodeId)`
- `clearConfirmedVideo(nodeId)`

## 현재 상태
- 제약 처리용 **DB 레벨 update 쿼리 틀**만 준비됨
- Service 트랜잭션 로직/Controller API 연결은 다음 단계에서 구현 필요

## 다음 단계
- `Feat : Enforce active master uniqueness` (Service 트랜잭션 로직)
- `Feat : Enforce confirm uniqueness` (Service 트랜잭션 로직)
- 문서/Swagger 반영
