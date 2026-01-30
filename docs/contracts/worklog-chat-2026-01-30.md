# 작업 기록: 프로젝트 채팅 기능 (2026-01-30)

## 목적
- 프로젝트 단위 실시간 채팅 + 히스토리 저장/조회 구현

## 범위
- 백엔드: WS 채팅 처리, DB 저장, REST 히스토리
- 프론트: 기존 채팅 UI/스토어 활용, 숫자 projectId 적용

## 주요 변경
### 백엔드
- STOMP: `/pub/chat/{projectId}` 수신 → `/topic/chat/{projectId}` 브로드캐스트 (Redis Pub/Sub)
- 저장: `chat_messages` 테이블에 메시지 저장
- REST: `GET /api/projects/{projectId}/chat/messages?size=50&before={messageId}`
- 검증: content 공백/2000자 초과/타입(TEXT 외) 거절

### 프론트
- 채팅/프레즌스 구독 및 전송 시 `roomId` 대신 숫자 `projectId` 사용

## 데이터 계약
- 요청: `{ "content": "...", "type": "TEXT" }`
- 응답: `{ "messageId", "projectId", "sender{userId,name,profileImageUrl}", "content", "type", "createdAt" }`
- 히스토리: `{ "items": [...], "hasMore": true|false }`

## 메모
- 히스토리는 최신순으로 반환(프론트에서 역순 처리)
- 운영 마이그레이션 필요 시 별도 SQL 추가 예정
