# W5 Collab WebSocket Protocol (v0)

목표: **최대 6명 Mesh WebRTC(오디오)** 를 위한 시그널링 + **채팅/접속(presence)/커서(cursor)** 를 단일 WebSocket으로 처리한다.

## 1) Endpoint

- Raw WebSocket: `GET /ws/room/{roomId}`
  - 예: `/ws/room/project-123`
- 인증: v0(로컬 PoC)에서는 **무인증 허용**(Spring Security에서 `/ws/**` permitAll)
- Origin: 개발 환경에서 `*` 허용(추후 환경별 allowlist 권장)

## 2) Message Envelope

모든 메시지는 JSON 1건 = WS frame 1건.

```json
{
  "v": 0,
  "type": "string",
  "targetId": "optional-string",
  "sender": { "clientId": "string", "name": "string" },
  "data": {},
  "ts": 0
}
```

- `v`: 프로토콜 버전(현재 `0`)
- `type`: 메시지 타입
- `targetId`:
  - 1:1 relay(offer/answer/ice) 시 필수
  - broadcast 성격(chat/cursor/presence)에는 생략
- `sender`: 서버가 세팅/검증 (클라가 보내도 서버 기준으로 덮어씀)
- `ts`: 서버 수신/발행 타임스탬프(epoch millis)

## 3) Client Identity

- `clientId`: **탭/브라우저 단위 UUID** 권장
- `name`: 표시 이름(로컬 PoC는 임의 입력/랜덤)

## 4) Types

### 4.1 Handshake

#### C→S: `hello`

```json
{
  "v": 0,
  "type": "hello",
  "data": { "clientId": "optional", "name": "optional" }
}
```

#### S→C: `welcome`

```json
{
  "v": 0,
  "type": "welcome",
  "data": {
    "roomId": "project-123",
    "self": { "clientId": "c1", "name": "A" },
    "peers": [
      { "clientId": "c2", "name": "B", "state": { "isMuted": false } }
    ]
  }
}
```

### 4.2 Presence

- S→Room: `presence.join`
- S→Room: `presence.leave`
- C→S: `presence.update` (내 상태 변경: mute 등)
- S→Room: `presence.update` (브로드캐스트)

`presence.update` payload 예시:
```json
{
  "isMuted": true,
  "isVideoOff": true,
  "isScreenSharing": false,
  "currentLocation": "Scene 1 편집 중"
}
```

### 4.3 Chat

#### C→S: `chat.send`
```json
{
  "v": 0,
  "type": "chat.send",
  "data": { "messageId": "msg-...", "content": "hello" }
}
```

#### S→Room: `chat.message`
```json
{
  "v": 0,
  "type": "chat.message",
  "data": { "messageId": "msg-...", "content": "hello" }
}
```

### 4.4 Cursor

#### C→S: `cursor.move`
```json
{
  "v": 0,
  "type": "cursor.move",
  "data": { "x": 100, "y": 200 }
}
```

#### S→Room: `cursor.update`
```json
{
  "v": 0,
  "type": "cursor.update",
  "data": { "x": 100, "y": 200 }
}
```

- 권장: FE에서 throttle(예: 30~60ms) 후 전송

### 4.5 WebRTC Signaling (Mesh / 오디오)

- C→S: `webrtc.offer` (targetId 필수)
- C→S: `webrtc.answer` (targetId 필수)
- C→S: `webrtc.ice` (targetId 필수)
- S→Target: 동일 타입으로 relay

payload:
```json
{
  "sdp": { "type": "offer|answer", "sdp": "..." }
}
```
```json
{
  "candidate": { "candidate": "...", "sdpMid": "0", "sdpMLineIndex": 0 }
}
```

## 5) Call Flow (Glare 회피 규칙)

1. 클라가 WS 연결 후 `hello` 전송
2. 서버가 `welcome(peers[])` 응답
3. **신규 참가자(=welcome를 받은 클라)가 기존 peers 전원에게 offer 생성/전송**
4. 기존 참가자는 offer를 받으면 answer 생성/전송
5. ICE candidate는 연결 생성 직후부터 target에게 1:1 relay

> 서버가 `welcome` 시점의 `peers[]`를 제공하므로, “먼저 들어온 참가자”는 offer를 만들지 않아도 됨 → glare(offer 충돌) 확률을 크게 낮춤.

## 6) Error

- S→C: `error`
  - 예: hello 없이 offer 전송, target 미존재 등

