# 음성채팅(WebRTC Audio Mesh) 구현 정리

작성일: 2026-01-31  
참고 문서: `docs/contracts/implementation-plan-collab-w5.md`

## 1) 범위 요약
- **음성 통화만** 지원 (영상/화면공유는 비범위).
- WebRTC Mesh 방식 (P2P, **최대 6명**).
- 신호(Signaling)는 STOMP `/pub/rtc/{projectId}` → `/user/queue/rtc`.
- 6명 초과 시 `/user/queue/errors`로 에러 전달.

## 2) 시그널링 계약(Contract)
- Endpoint: `/ws` (SockJS + STOMP)
- Publish: `/pub/rtc/{projectId}`
- Subscribe: `/user/queue/rtc`
- Error: `/user/queue/errors`

### 메시지 타입
- Client → Server: `JOIN`, `LEAVE`, `OFFER`, `ANSWER`, `CANDIDATE`, `MUTE`
- Server → Client: `JOIN_ACK`, `JOIN`, `LEAVE`, `OFFER`, `ANSWER`, `CANDIDATE`, `MUTE`

### 최소 스키마(요약)
```json
// JOIN
{ "type": "JOIN", "projectId": 101 }

// JOIN_ACK (server)
{ "type": "JOIN_ACK", "projectId": 101, "participants": [{ "userId": 5, "name": "홍길동", "profileImageUrl": null, "muted": false }] }

// OFFER / ANSWER
{ "type": "OFFER", "projectId": 101, "targetId": 3, "sdp": { "type": "offer", "sdp": "..." } }
{ "type": "ANSWER", "projectId": 101, "targetId": 3, "sdp": { "type": "answer", "sdp": "..." } }

// CANDIDATE
{ "type": "CANDIDATE", "projectId": 101, "targetId": 3, "candidate": { "candidate": "...", "sdpMid": "0", "sdpMLineIndex": 0 } }

// MUTE
{ "type": "MUTE", "projectId": 101, "muted": true }
```

## 3) 백엔드 변경점
### 3.1 신규 클래스/파일
- `itda-backend/src/main/java/com/itda/backend/collab/controller/RtcSignalingController.java`
- `itda-backend/src/main/java/com/itda/backend/collab/service/RtcRoomManager.java`
- `itda-backend/src/main/java/com/itda/backend/collab/service/RtcDisconnectListener.java`
- `itda-backend/src/main/java/com/itda/backend/collab/messaging/RtcSignalRequest.java`
- `itda-backend/src/main/java/com/itda/backend/collab/messaging/RtcSignalResponse.java`
- `itda-backend/src/main/java/com/itda/backend/collab/messaging/RtcParticipant.java`
- `itda-backend/src/main/java/com/itda/backend/collab/messaging/RtcErrorMessage.java`

### 3.2 주요 로직
- **JOIN**: 참여자 등록 + `JOIN_ACK`(기존 참가자 목록) 전송.
- **OFFER/ANSWER/CANDIDATE**: `targetId` 기반 **1:1 라우팅**(`/user/queue/rtc`).
- **MUTE/LEAVE**: 다른 참여자에게 브로드캐스트.
- **Disconnect**: `RtcDisconnectListener`가 세션 정리 + `LEAVE` 브로드캐스트.
- **6명 제한**: `RtcRoomManager.MAX_PARTICIPANTS = 6` 초과 시 `RTC_ROOM_FULL`.
- **Redis Pub/Sub**: `CollabRedisPublisher` 사용, 실패 시 `SimpMessagingTemplate` fallback.

### 3.3 에러 코드 추가
- `itda-backend/src/main/java/com/itda/backend/global/response/ErrorCode.java`
  - `RTC_ROOM_FULL(409, "음성 통화는 최대 6명까지 참여할 수 있습니다.")`

### 3.4 권한/인증
기존 `StompAuthChannelInterceptor` 규칙 유지.

## 4) 프론트 변경점 (백엔드 개발자 작업분)
### 4.1 파일 변경
- `itda-frontend/src/services/ws/socket.ts`
  - RTC 및 에러 구독(`/user/queue/rtc`, `/user/queue/errors`) 추가
- `itda-frontend/src/stores/collab.ts`
  - RTC 플로우(Join Ack 기반 offer) 정렬
  - 마이크 선택/전환 로직 추가
  - 원격 볼륨 조절 로직 추가
- `itda-frontend/src/services/webrtc/peerConnection.ts`
  - `audioDeviceId` 지원 + `switchMicrophone(deviceId)` 트랙 교체
- `itda-frontend/src/components/collab/CollabPanel.vue`
  - 마이크 선택 UI, 원격 볼륨 슬라이더 UI

### 4.2 동작 변경
- **RTC 경로 정렬**: `/pub/rtc/{projectId}` + `/user/queue/rtc` 사용.
- **글레어 방지**: `JOIN_ACK` 수신 후 **신규 참가자만 offer 생성**.
- **ICE 전송 분리**: 로컬 candidate는 `sendRTC(CANDIDATE)`로 전송.

### 4.3 마이크 선택
- `enumerateDevices()`로 입력 목록 로딩.
- 선택값은 `localStorage` 키 `collab:micId`에 저장.
- 변경 시 `switchMicrophone()`으로 **replaceTrack**.
- 스피킹 모니터는 새 스트림 기준으로 재시작.

### 4.4 원격 볼륨 조절
- 참가자별 볼륨 슬라이더 추가 (로컬 본인 제외).
- 오디오 엘리먼트(`audio-{peerId}`)의 `volume`으로 반영.
- 메모리 맵(`remoteVolumeMap`)에 저장, **persist 없음**.

### 4.5 UI 표시 개선
- 마이크 이름의 내부 식별자(영문+숫자) 제거:
  - 예: `USB Microphone (1234:ABCD)` → `USB Microphone`
- 드롭다운 텍스트가 길어도 **가로 스크롤 없음**(ellipsis 처리).

## 5) 배포/운영 체크리스트
- **프론트/백엔드 동시 배포 필수** (RTC 계약 변경).
- `VITE_WS_URL`을 **https://도메인/ws**로 설정.
- Nginx/ALB에서 `/ws` **Upgrade/Connection 헤더 통과** 확인.
- Redis Pub/Sub 정상 연결 확인.
- TURN 미설정 시 사내/학교 NAT에서 음성 통화 실패 가능.

## 6) 제한사항
- TURN 미구성 → 일부 환경에서 연결 실패 가능.
- 원격 볼륨 설정은 세션 종료 시 초기화.
- 마이크 label은 권한 허용 전에는 비어 있을 수 있음.

## 7) 빠른 검증 시나리오
1. 2명 로그인 → 음성 시작
2. `JOIN_ACK` 수신 후 `OFFER/ANSWER/CANDIDATE` 교환
3. 서로 음성 송수신 확인
4. 7번째 참여 시 `RTC_ROOM_FULL` 수신 확인
