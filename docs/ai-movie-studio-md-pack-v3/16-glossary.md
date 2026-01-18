# 16. 용어 사전(팀 공통)

> 처음 보는 팀원을 위해 **용어를 한 문단씩** 정리했습니다.

## A. 비동기/Queue/Worker 관련

### Job
- "지금 당장 끝나지 않는 작업"을 DB에 기록한 단위
- 예: 이미지 생성, 영상 생성, FFmpeg 병합
- 보통 상태: `pending` → `running` → `succeeded`/`failed`

### Dispatcher
- 별도 서버가 아니라 **API 서버 내부의 역할/모듈**
- 하는 일
  - 요청 검증/권한 확인
  - Job 레코드 생성
  - Redis Streams에 메시지 발행(=dispatch)

### Worker
- Queue에서 Job을 가져가 **실제 일을 수행**하는 프로세스/컨테이너
- API 서버와 분리하면 장점
  - 무거운 작업 때문에 API 서버가 느려지지 않음
  - 실패/재시도/관측이 쉬움
  - Worker만 확장(복제)해서 처리량을 올릴 수 있음

### Redis Streams
- Redis가 제공하는 로그 기반 메시지 스트림
- 기본 개념
  - Stream: 토픽(예: `ai:image:request`)
  - Consumer Group: 여러 Worker가 한 Stream을 나눠 처리하기 위한 그룹
  - ACK: 작업을 완료했음을 Redis에 확인
  - Pending: 가져갔지만 ACK 안 된 메시지(중단/오류 시 남음)

### Retry / Idempotency
- Retry: 실패한 Job을 다시 시도
- Idempotency: 같은 Job을 여러 번 처리해도 결과가 망가지지 않도록 설계
  - 예: jobId로 결과 경로를 고정, DB 업데이트를 조건부로 수행

---

## B. 실시간(WebSocket) 관련

### WebSocket
- 브라우저와 서버가 **연결을 유지**하며 메시지를 주고받는 방식
- 우리 사용처
  1) Job 완료/실패 이벤트(`job.done`, `job.failed`)
  2) 채팅
  3) Presence/Cursor(누가 어디서 작업 중인지)
  4) WebRTC 시그널링(offer/answer/candidate)

---

## C. WebRTC(협업 통화) 관련

### WebRTC
- 브라우저끼리 음성/영상 데이터를 실시간으로 주고받는 표준 기술

### Signaling(시그널링)
- WebRTC 연결을 시작하려면 "서로의 연결 정보"를 교환해야 한다.
- 이 교환을 담당하는 서버/채널을 Signaling이라고 한다.
- 우리는 WebSocket으로 `offer/answer/candidate`를 중계한다.

### SDP(offer/answer)
- 통화에 필요한 미디어 정보(코덱, 포트 후보 등)를 담은 텍스트
- offer(제안) → answer(응답)로 협상한다.

### ICE candidate
- P2P 연결을 위해 서로가 접근 가능한 네트워크 경로 후보(주소/포트)

### STUN
- "내가 외부에서 보일 때 IP/포트가 뭔지"를 알아내는 서버
- P2P가 잘 붙도록 돕는다.

### TURN
- P2P가 직접 안 붙을 때(기업망/엄격한 NAT 등) **중계 서버로 우회**한다.
- TURN을 쓰면 연결 성공률은 올라가지만, 서버 트래픽 비용/부하가 생긴다.

### P2P Mesh vs SFU
- P2P Mesh
  - 참가자들이 서로에게 직접 송수신
  - 6명 수준에서는 구현이 간단하고 빠르게 가능
  - 단점: 사람 수가 늘수록 각 클라이언트 업로드 부담 증가
- SFU(Selective Forwarding Unit)
  - 중앙 서버가 미디어를 받아서 참가자들에게 분배
  - 장점: 인원 확장에 유리
  - 단점: 서버 구축/운영/품질 이슈가 커지고, 구현 난도가 상승

> 이번 프로젝트(최대 6명)에서는 **P2P Mesh + TURN 옵션**이 현실적이다.

---

## D. CRDT/Yjs 관련

### CRDT
- 여러 사용자가 동시에 편집해도 **충돌 없이 합쳐지도록** 하는 데이터 구조/알고리즘

### Yjs
- 웹에서 CRDT를 구현하기 위한 대표 라이브러리
- 강점: 동시 편집(텍스트/배열/맵 등) 지원
- 난점: "그래프(Vue Flow)" 같은 복잡한 구조에 적용하면
  - 변경 이벤트가 많아지고
  - undo/redo, 선택/드래그 동기화, 권한/락 처리 등 고려사항이 급증한다.

- API 서버와 분리하면 좋은 점
  - API 응답이 느려지지 않음
  - 실패/재시도/재처리(pending)가 쉬움
  - 작업 종류별로 병렬 개발/배포가 쉬움

### Redis Streams
- Redis의 메시지 스트림(Queue) 기능
- 핵심 키워드
  - **Stream**: 토픽(메시지가 쌓이는 곳)
  - **Consumer Group**: 여러 Worker가 "나눠서" 처리하도록 하는 묶음
  - **ACK**: "이 메시지 처리 끝"이라고 확인
  - **Pending**: 누군가 가져갔지만 ACK 안 된 메시지(장애/타임아웃/버그 가능)

### Pending / Retry
- Pending이 계속 쌓이면 "작업이 멈췄다"는 신호가 될 수 있음
- 보통은
  - 실행 시간을 제한하고(timeout)
  - 특정 횟수까지 retry 하고
  - 그래도 실패하면 failed로 종료

---

## B. 실시간(WebSocket) 관련

### WebSocket(WS)
- HTTP와 달리 연결을 유지하면서 서버↔클라이언트가 **즉시 메시지를 주고받는** 통신
- 여기서 WS를 쓰는 곳
  1) Job 완료/실패 이벤트 푸시(`job.done`, `job.failed`)
  2) 협업 Presence/Cursor(누가 어디서 작업하는지)
  3) (5주차) WebRTC 시그널링(offer/answer/candidate), 채팅

### Event(이벤트)
- WS로 FE에게 보내는 JSON 메시지
- 예: "jobId=123이 성공", "userA가 씬편집 화면에 들어옴"

---

## C. WebRTC 관련

### WebRTC
- 브라우저끼리 오디오/비디오를 **P2P**로 연결할 수 있게 해주는 기술

### P2P Mesh
- 참여자들이 서로 **직접 연결**하는 방식
- 6명이라면 연결 수가 늘지만(각자 여러 연결) 개발이 단순함
- 이번 프로젝트(최대 6명, 음성 중심)에서는 **Mesh로 충분**하다는 판단

### SFU (Selective Forwarding Unit)
- 참여자들이 SFU 서버에 한 번만 업로드하고, SFU가 다른 참여자들에게 전달
- 장점: 대규모(수십~수백명), 다자 영상에서 대역폭/CPU 효율 좋음
- 단점: SFU 구축/운영 난이도 ↑
- 이번 스코프에서는 과함

### SDP
- 통화 세션 설정 정보(코덱, 미디어 정보 등)
- offer/answer로 교환

### ICE Candidate
- 통신 가능한 네트워크 경로 후보들(주소/포트)
- NAT/방화벽 환경에서 가능한 경로를 찾기 위해 교환

### STUN
- 내 외부 IP/포트를 알아내는 서버(주로 "내가 NAT 뒤에 있네" 같은 정보)

### TURN
- P2P가 막힐 때(대칭 NAT, 기업망 등) **중계 서버**로 트래픽을 우회
- 이번 프로젝트에서 TURN은 "있으면 성공률이 올라가는 보험" 같은 존재
- EC2 1대에서 coturn으로 띄울 수 있음

## B. 실시간(WebSocket) 관련

### WebSocket
- 브라우저와 서버가 **연결을 유지**하며 이벤트를 푸시하는 통신 방식
- 이 프로젝트에서 목적
  1) **Job 상태 이벤트(job.done/job.failed)**를 FE에 실시간 반영
  2) 협업 Presence/Cursor 이벤트(누가 어디서 작업 중인지)
  3) (5주차) WebRTC 시그널링(offer/answer/ICE) 및 채팅

### Presence
- "현재 누가 접속해 있고, 어떤 화면/씬을 보고 있는지"를 보여주는 기능

### Cursor / Selection
- Cursor: 다른 사용자의 마우스 커서 위치 + 이름 라벨
- Selection: 다른 사용자가 어떤 노드/필드를 선택했는지 하이라이트
- **CRDT 없이도 구현 가능**
  - 위치/선택은 "상태 브로드캐스트"로 충분

### Soft Lock
- 동시에 같은 노드를 편집하지 않게 "유도"하는 락
- 방식 예
  - 사용자가 노드 편집 시작 → `node.lock` 이벤트 발행
  - 다른 사용자는 해당 노드를 읽기 전용/경고 표시
  - 일정 시간 후 자동 해제(heartbeat/timeout)

---
