# W5 협업(콜라보) 통합 구현 계획서 (Backend 2인 / STOMP + WebRTC Audio Mesh)

> 작성일: 2026-01-28  
> 목표: **채팅(히스토리 포함) + 오디오(WebRTC Mesh) + 커서/작업상태(Presence)** 를 프로젝트 단위로 제공하고, **2~6명 E2E 1회 완주**

---

## 0) 범위(Scope) / 비범위(Non-scope)

### 완료 정의(Definition of Done) ✅
- 프로젝트 멤버만 `/ws`에 CONNECT/SUBSCRIBE/SEND 가능(비멤버는 거절)
- Chat
  - WS로 메시지 전송/수신 정상
  - `chat_messages`에 저장됨
  - REST 히스토리 조회가 커서 페이징으로 동작
- Presence/작업상태
  - LOCATION/STATUS 브로드캐스트 정상
  - Disconnect 시 오프라인 반영(정책 고정)
- Cursor
  - 동일 `projectId` + 동일 `sceneId`에서 커서 업데이트 수신 정상
- RTC Signaling(오디오 Mesh)
  - JOIN/LEAVE 및 OFFER/ANSWER/CANDIDATE 1:1 전달 정상
  - 7번째 JOIN은 거절(정원 6)

### 포함 ✅
- WebSocket(STOMP) 기반 실시간 기능
  - Chat: 실시간 전송 + 프로젝트별 히스토리 저장/조회
  - Presence: 접속/작업상태/위치 공유
  - Cursor: 마우스 커서 위치 공유(씬 단위)
  - WebRTC Audio Signaling: Mesh 오디오 통화를 위한 시그널링(최대 6명)
- 인증/인가: JWT 기반, 프로젝트 멤버만 참여 가능(권장: VIEWER 포함)
- 배포: Reverse Proxy 뒤에서 `wss://{domain}/ws` 동작

### 미포함 ❌ (추후)
- 영상/화면공유
- TURN 운영/동적 ICE credential(필요 시 별도 인프라 작업)
- CRDT(Yjs) 기반 동기화
- 고급 Presence(원격 스크롤 동기화 등)

---

## 1) 아키텍처/라우팅(권장)

### 1.1 WebSocket Endpoint
- SockJS + STOMP Endpoint: `/ws`
- App Prefix: `/pub`
- Topic Prefix: `/topic`
- User Destination Prefix: `/user`

### 1.2 Topic/Publish 설계

**Chat**
- Subscribe: `/topic/chat/{projectId}`
- Publish: `/pub/chat/{projectId}`

**Presence(위치/작업상태/커서 통합)**
- Subscribe: `/topic/presence/{projectId}`
- Publish: `/pub/presence/{projectId}`

**WebRTC Audio Signaling**
- Publish: `/pub/rtc/{projectId}`
- 개인 수신: `/user/queue/rtc`

---

## 2) 공통 보안/인가(필수)

### 2.1 STOMP 인증
- 클라이언트는 STOMP CONNECT 헤더에 `Authorization: Bearer <JWT>` 포함
- 서버는 ChannelInterceptor 또는 Handshake 단계에서 토큰 검증 후 Principal 세팅

### 2.2 프로젝트 멤버 체크
- SUBSCRIBE/SEND 시 `{projectId}`를 추출하여 “프로젝트 멤버 여부” 확인
- 권장 정책:
  - Chat/Presence/Audio 참여는 **프로젝트 멤버 전원(VIEWER 포함)** 허용
  - 멤버/권한 모델은 `docs/contracts/Member_API_Docs.md` 참고

### 2.3 분업을 위한 선행 작업(병렬 개발 필수)
아래 항목을 먼저 고정/구현하면, 백엔드 2명이 충돌 없이 병렬로 진행할 수 있다.

- **Contract Freeze(우선 결정)**: `/ws`, `/pub`/`/topic`/`/user/queue` 경로, 메시지 타입/DTO 필드, 에러 응답 방식
- **STOMP 보안 골격(우선 구현)**: CONNECT JWT 인증 + SUBSCRIBE/SEND에서 `{projectId}` 추출 → 프로젝트 멤버 체크까지
- **공용 멤버 체크 함수**: `isMember(projectId, userId)` / role 정책(예: VIEWER 허용) 합의
- **채팅 히스토리 스키마/마이그레이션**: `chat_messages` 테이블 생성 방식(Flyway/Liquibase/수기) 및 인덱스 고정
- **DTO 패키지/네이밍 규칙**: Chat/Persistence/RTC DTO 위치/이름 규칙을 먼저 정해 변경 비용 최소화
- **레이트/제한 정책(최소치)**: chat 길이(예: 2000자), cursor throttle 권장값(예: 50~100ms), rtc 정원(6명)

### 2.4 병렬 개발을 위한 머지 순서(권장)
문서만 보고도 충돌 없이 진행하려면, PR을 아래 순서로 분리한다.

1) **PR#1 (Dev A)**: WS 공통 인프라(설정 + 인증/인가 인터셉터 + 공용 멤버 체크 hook)
2) **PR#2 (Dev B)**: Presence + RTC Signaling (base: PR#1)
3) **PR#3 (Dev A)**: Chat + DB/REST (base: PR#1)

> 브랜치 예시: `feat/ws-core-authz`, `feat/ws-presence-rtc`, `feat/ws-chat-history`

---

## 3) 메시지 스키마(권장)

### 3.1 PresenceMessage (Client → Server)

공통:
- `type`: `LOCATION | STATUS | CURSOR | NODE_SELECT`
- `sceneId?`: Long
- `nodeId?`: Long

타입별:
- `type=LOCATION`
  - `location`: `PROJECT_LIST | SCENE_LIST | SCENE_EDIT | TIMELINE | DASHBOARD | OTHER`
  - `sceneId?`, `nodeId?`
- `type=STATUS`
  - `status`: `IDLE | EDITING | REVIEWING | RENDERING | DISCUSSING`
  - `summary?`: string(권장 최대 120자)
- `type=CURSOR`
  - `sceneId`: Long(권장 필수)
  - `x`, `y`: number (canvas 기준)
- `type=NODE_SELECT`(옵션)
  - `sceneId`, `nodeId`, `action=SELECT|DESELECT|EDITING`

### 3.2 Presence Broadcast (Server → Client)
- 서버가 아래 필드를 추가하여 `/topic/presence/{projectId}`로 브로드캐스트:
  - `userId`, `name`, `profileImageUrl?`, `updatedAt`
  - (옵션) `color` (커서 표시용 사용자 고유 색상)

---

## 4) Chat (실시간 + 히스토리 저장)

### 4.1 WebSocket
- Publish: `/pub/chat/{projectId}`
- Subscribe: `/topic/chat/{projectId}`

Request(클라→서버):
- `content` (최대 2000자 권장)
- `type` (기본 `TEXT`)

Response(서버→클라):
- `messageId`, `projectId`, `sender{userId,name,profileImageUrl}`, `content`, `type`, `createdAt`

### 4.2 DB 스키마(권장)
테이블: `chat_messages`
- `id` BIGINT PK AUTO_INCREMENT
- `project_id` BIGINT NOT NULL (index)
- `sender_id` BIGINT NOT NULL (FK users)
- `content` TEXT NOT NULL
- `type` VARCHAR(20) NOT NULL DEFAULT 'TEXT'
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 4.3 REST API(히스토리)
- `GET /api/projects/{projectId}/chat/messages?size=50&before={messageId}`
  - size: 기본 50, 최대 100 권장
  - before: 커서 기반(선택)
  - 권한: 프로젝트 멤버

---

## 5) WebRTC Audio (6명 Mesh) - Signaling

### 5.1 메시지 타입
- `JOIN`, `LEAVE`, `OFFER`, `ANSWER`, `CANDIDATE`, `MUTE`

### 5.2 라우팅 정책(권장)
- `OFFER/ANSWER/CANDIDATE`는 `targetId` 기반으로 **1:1 전송** (`/user/queue/rtc`)
- `JOIN/LEAVE/MUTE`는 참여자 관리 목적이므로 프로젝트 참여자에게 브로드캐스트(또는 presence로 통합)

### 5.3 6명 정원 제한
- 프로젝트별 RTC 참여자 수를 in-memory로 관리
- JOIN 시 6명 초과면 거절(에러 프레임/에러 메시지)

### 5.4 Glare 방지 규칙(필수)
- “새로 들어온 참가자”가 기존 참가자들에게 offer를 보내는 규칙을 고정
  - JOIN 응답에 현재 참가자 목록 제공 → newcomer가 offer 생성/전송

---

## 6) Presence/Cursor 성능 가이드

- 커서(CURSOR)는 클라이언트에서 throttle 권장(50~100ms)
- 서버는 기본적으로 중계만 수행(필요 시 rate limit로 도배 방지)
- Presence(LOCATION/STATUS)는 “변경 시에만” 전송

---

## 7) 2인 분업(권장)

### 개발자 A (공통 인프라 + Chat/DB)
- PR#1(선행): STOMP 설정(`/ws`, `/pub`, `/topic`, `/user/queue`)
- PR#1(선행): 인증/인가 인터셉터(JWT + project 멤버 체크)
- PR#3: Chat WS 핸들러 + DB 저장 + 히스토리 REST
- PR#3: 채팅 Validation 및 에러 처리

#### Dev A 산출물 체크리스트
- [ ] WebSocket 설정: `/ws`, `/pub`, `/topic`, `/user`, `/user/queue`
- [ ] CONNECT 인증: `Authorization: Bearer <JWT>` 검증 및 Principal 세팅
- [ ] SUBSCRIBE/SEND 인가: `{projectId}` 추출 → 프로젝트 멤버 체크
- [ ] 공용 API: `boolean isProjectMember(long projectId, long userId)` 제공(Dev B가 재사용)
- [ ] Chat WS 구현 + DB 저장 + 브로드캐스트(`/topic/chat/{projectId}`)
- [ ] Chat REST 구현(히스토리 조회) + 페이징
- [ ] `chat_messages` 마이그레이션/인덱스 확정 및 적용

---

## 8) 현행사항 (2026-01-29)
### 완료
- WS 경로 고정: `/ws`, App Prefix `/pub`, Topic `/topic`, User `/user`, Queue `/user/queue`
- CONNECT JWT 인증 + Principal 세팅 (STOMP 인바운드 인터셉터)
- SUBSCRIBE/SEND 시 destination에서 `projectId` 추출 → 멤버 체크
- STOMP ERROR 프레임 핸들러 추가(에러 코드/메시지 반환)
- `ProjectAccessService.isProjectMember` 공용 메서드 추가

### 테스트
- Unit: `StompAuthChannelInterceptor`, `CollabStompErrorHandler`
- Integration: STOMP CONNECT 무인증 에러 프레임 수신, SUBSCRIBE 시 멤버 체크 호출

#### Dev A 권장 파일/클래스(예시)
- WebSocket 설정/보안
  - `com.itda.backend.ws.config.WebSocketConfig`
  - `com.itda.backend.ws.security.StompAuthChannelInterceptor`
  - `com.itda.backend.ws.security.ProjectMembershipAuthorizer`
- Chat
  - `com.itda.backend.chat.controller.ChatController` (STOMP)
  - `com.itda.backend.chat.controller.dto.ChatSendRequest`, `ChatMessageResponse`
  - `com.itda.backend.chat.service.ChatService`
  - `com.itda.backend.chat.repository.ChatMessageMapper` + `resources/mapper/ChatMessageMapper.xml`
  - (선택) `com.itda.backend.chat.controller.ChatHistoryController` (REST)

### 개발자 B (Presence + RTC Signaling)
- PR#2: Presence(LOCATION/STATUS/CURSOR/NODE_SELECT) 처리 + 브로드캐스트
- PR#2: Disconnect 처리(offline/leave) 및 in-memory 상태 정리
- PR#2: RTC Signaling(1:1 라우팅) + 참여자 관리(정원 6)
- PR#2: 커서/RTC Validation 및 에러 처리

#### Dev B 산출물 체크리스트
- [ ] Presence WS: `/pub/presence/{projectId}` 수신 → `/topic/presence/{projectId}` 브로드캐스트
- [ ] 타입별 Validation(필수 필드 누락 시 에러 프레임/에러 메시지)
- [ ] Disconnect 처리 정책 고정(leave/offline을 어떤 타입으로 방송할지) + 구현
- [ ] Cursor throttle 전제(서버는 중계) + (선택) 서버 rate limit
- [ ] RTC WS: `/pub/rtc/{projectId}` 수신
  - JOIN/LEAVE/MUTE: 브로드캐스트(또는 presence로 통합)
  - OFFER/ANSWER/CANDIDATE: targetId 기반 `/user/queue/rtc` 1:1 전송
- [ ] RTC 정원 6명 제한 + 에러 처리

#### Dev B 권장 파일/클래스(예시)
- Presence
  - `com.itda.backend.presence.controller.PresenceController` (STOMP)
  - `com.itda.backend.presence.controller.dto.PresenceMessage` (request/broadcast 공용 또는 분리)
  - `com.itda.backend.presence.service.PresenceService` (최신 상태 캐시/브로드캐스트 정책)
- RTC Signaling
  - `com.itda.backend.rtc.controller.RtcSignalingController` (STOMP)
  - `com.itda.backend.rtc.service.RtcRoomManager` (projectId별 참여자/정원 관리)
  - `com.itda.backend.rtc.controller.dto.RtcSignalMessage`

---

## 8) Contract Freeze (문서만 보고 구현 가능한 고정안)

> 이 섹션은 구현 시작 전 **변경 금지**로 합의한다. 변경 시 Dev A/Dev B 동시 수정이 필요하다.

### 8.1 STOMP 공통
- Endpoint: `/ws` (SockJS)
- CONNECT Header: `Authorization: Bearer <JWT>`
- Prefix
  - Publish: `/pub`
  - Topic: `/topic`
  - User queue: `/user/queue/*`
- 에러 전달(권장)
  - destination: `/user/queue/errors`
  - payload:
    ```json
    { "code": "FORBIDDEN", "message": "프로젝트 멤버만 접근 가능합니다." }
    ```

### 8.2 Chat
- Publish: `/pub/chat/{projectId}`
- Subscribe: `/topic/chat/{projectId}`

클라→서버:
```json
{ "content": "안녕하세요", "type": "TEXT" }
```

서버→클라:
```json
{
  "messageId": 12345,
  "projectId": 101,
  "sender": { "userId": 5, "name": "홍길동", "profileImageUrl": null },
  "content": "안녕하세요",
  "type": "TEXT",
  "createdAt": "2026-01-28T15:00:00Z"
}
```

### 8.3 Presence(작업상태/위치/커서 통합)
- Publish: `/pub/presence/{projectId}`
- Subscribe: `/topic/presence/{projectId}`

클라→서버(예시):
```json
{ "type": "LOCATION", "location": "SCENE_EDIT", "sceneId": 5, "nodeId": null }
```
```json
{ "type": "STATUS", "status": "EDITING", "summary": "Scene 3 컷 편집 중" }
```
```json
{ "type": "CURSOR", "sceneId": 5, "x": 450, "y": 320 }
```

서버→클라(예시; 서버가 사용자 정보/updatedAt을 채움):
```json
{
  "userId": 5,
  "name": "홍길동",
  "profileImageUrl": null,
  "color": "#FF5733",
  "type": "CURSOR",
  "sceneId": 5,
  "x": 450,
  "y": 320,
  "updatedAt": "2026-01-28T15:00:00.123Z"
}
```

### 8.4 RTC Signaling(오디오 Mesh)
- Publish: `/pub/rtc/{projectId}`
- 개인 수신 Subscribe: `/user/queue/rtc`

클라→서버(예시):
```json
{ "type": "JOIN", "projectId": 101 }
```
```json
{ "type": "OFFER", "projectId": 101, "targetId": 3, "sdp": { "type": "offer", "sdp": "..." } }
```
```json
{ "type": "CANDIDATE", "projectId": 101, "targetId": 3, "candidate": { "candidate": "...", "sdpMid": "0", "sdpMLineIndex": 0 } }
```

서버→클라(1:1, `/user/queue/rtc` 예시):
```json
{ "type": "OFFER", "projectId": 101, "senderId": 5, "sdp": { "type": "offer", "sdp": "..." } }
```

### 8.5 공통 코드 계약(인터페이스)
- 멤버 체크: `boolean isProjectMember(long projectId, long userId)`
- projectId 추출 규칙: destination의 마지막 path segment를 projectId로 파싱(실패 시 거절)

---

## 9) 일정 예시(5일)

- D1: STOMP + JWT 인터셉터 + project 멤버 체크 골격
- D2: Chat WS + DB 저장 + 히스토리 REST
- D3: Presence(LOCATION/STATUS) + disconnect 처리
- D4: Cursor(CURSOR) + RTC Signaling(JOIN/LEAVE/OFFER/ANSWER/CANDIDATE/MUTE)
- D5: 2~6명 E2E, 경계/장애 케이스 정리, 문서/샘플 payload 고정

---

## 10) 최소 검증 시나리오(E2E)

1. 인증 없이 CONNECT → 거절
2. 프로젝트 멤버 아님 → SUBSCRIBE/SEND 거절
3. Chat: A가 send → A/B 모두 수신 + DB 저장됨
4. Chat REST: 히스토리 50개 조회 + 커서 페이징 동작
5. Presence: A location/status 전송 → B 수신, A disconnect → B에서 오프라인 반영
6. Cursor: A 커서 이동 → B에서 커서 갱신(씬 필터링 확인)
7. RTC: A JOIN, B JOIN → 참가자 목록 동기화, offer/answer/candidate가 target에게만 전달
8. 정원: 7번째 JOIN 시 거절
