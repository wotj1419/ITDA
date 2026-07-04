# ITDA 프로젝트 이력서/면접용 코드 분석

아래 내용은 코드 기준 분석입니다. **확실**은 실제 코드 근거가 있는 내용이고, **추정**은 프로젝트 성격상 이력서에 자연스럽게 연결 가능하지만 작성자 본인 담당 여부는 확인이 필요한 항목입니다.

## 프로젝트 개요

ITDA는 AI 기반 영상/영화 제작 협업 플랫폼입니다. 사용자가 프로젝트를 만들고, 씬 단위로 스토리/오브젝트/노드 워크플로우를 구성한 뒤, AI 이미지/영상 생성 결과를 타임라인에 확정하고 병합 영상으로 내보내는 구조입니다.

주요 사용자는 영상 기획자, 크리에이터, 팀 협업 사용자로 보입니다. 핵심 흐름은 다음과 같습니다.

1. 로그인
2. 대시보드
3. 프로젝트 상세
4. 씬 편집(Vue Flow 노드 캔버스)
5. 타임라인 정렬/병합
6. 다운로드/미리보기

프론트엔드에서 중요한 화면은 다음입니다.

- `itda-frontend/src/pages/AuthPage.vue`
- `itda-frontend/src/pages/DashboardPage.vue`
- `itda-frontend/src/pages/ProjectDetailPage.vue`
- `itda-frontend/src/pages/SceneEditPage.vue`
- `itda-frontend/src/pages/TimelinePage.vue`

## 사용 기술 스택

### 확실

- **Vue 3 + TypeScript + Vite**
  - SPA 프론트엔드, 컴포넌트 기반 UI 구현.
- **Pinia**
  - 인증, 프로젝트, 씬, 노드, 타임라인, 협업 상태 관리.
- **Vue Router**
  - 인증 가드 포함 라우팅.
  - 관련 파일: `itda-frontend/src/router/index.ts`
- **Axios**
  - REST API 통신, JWT Authorization 헤더, 401/403 인터셉터 처리.
  - 관련 파일: `itda-frontend/src/services/api/client.ts`
- **Vue Flow**
  - 씬 편집 노드 캔버스 구현.
  - 관련 파일: `itda-frontend/src/components/scene-editor/NodeCanvas.vue`
- **STOMP + SockJS**
  - 실시간 프로젝트 이벤트, 채팅, presence, RTC signaling.
  - 관련 파일: `itda-frontend/src/services/ws/socket.ts`
- **WebRTC**
  - 음성 통화, 마이크 전환, 화면 공유, peer connection 관리.
  - 관련 파일: `itda-frontend/src/services/webrtc/peerConnection.ts`
- **dagre**
  - 노드 자동 레이아웃.
- **Playwright**
  - 프론트엔드 회귀 테스트.
- **Spring Boot 3, MyBatis, MySQL, Redis, JWT, S3, Vertex AI/Google GenAI**
  - 백엔드 API, AI, 스토리지, 작업 큐 기반.
- **Docker Compose, Nginx, Jenkins, Prometheus/Grafana**
  - 배포, 프록시, CI/CD, 모니터링 구성.

### 추정

- 프론트엔드 담당자로 정리한다면 “AI 영상 생성 워크플로우와 실시간 협업 UI를 담당”했다고 쓰는 것이 가장 자연스럽습니다.
- 백엔드까지 일부 기여했다면 노드/타임라인 API 계약 연동, WebSocket 이벤트 스펙 조율, S3 presigned URL 처리까지 확장 가능합니다.

## 내가 맡은 역할로 정리할 수 있는 부분

확실한 코드 근거 기준으로 가장 강한 역할은 다음입니다.

- AI 영상 제작 워크플로우 프론트엔드 구현
- Vue Flow 기반 노드 에디터 구현
- 노드 생성/수정/삭제/확정/활성화 API 연동
- 타임라인 클립 정렬, 병합 요청, 미리보기/다운로드 구현
- WebSocket 기반 실시간 작업 상태 반영
- 협업 presence, 커서, 노드 잠금, 노드 이동 동기화 구현
- WebRTC 음성 협업 기능 구현
- JWT 인증, 라우팅 가드, 프로필/회원가입/로그인 UX 구현
- 오브젝트 시트 파일 업로드/다운로드 및 프로젝트 상세 화면 구현
- mock/API 전환 가능한 서비스 계층 설계
- Playwright 기반 주요 유틸/화면 회귀 테스트 작성

## 주요 구현 기능

### 기능명: Vue Flow 기반 씬 노드 에디터

**관련 파일/폴더**

- `itda-frontend/src/pages/SceneEditPage.vue`
- `itda-frontend/src/components/scene-editor/NodeCanvas.vue`
- `itda-frontend/src/stores/sceneNode/index.ts`
- `itda-frontend/src/types/ui/sceneNodes.ts`

**구현 내용**

Scene Header, Master Image, Storyboard Grid, Shot, Video 노드 타입을 정의하고 부모-자식 연결 규칙, 노드 선택, 드래그, 자동 레이아웃, 삭제 확인, collapse, end-shot 선택 등을 처리했습니다.

**사용 기술**

- Vue 3
- Pinia
- Vue Flow
- dagre
- TypeScript

**이력서에 쓸 수 있는 표현**

> Vue Flow 기반 노드형 AI 영상 제작 에디터를 구현하고, 노드 타입/연결 규칙/자동 레이아웃/상태 동기화를 설계했습니다.

**면접에서 설명할 수 있는 포인트**

단순 캔버스가 아니라 AI 생성 워크플로우의 도메인 모델을 노드 그래프로 표현했고, 위치 저장/undo/원격 이동 반영까지 처리했다는 점을 설명할 수 있습니다.

### 기능명: AI 프롬프트 및 이미지/영상 생성 API 연동

**관련 파일/폴더**

- `itda-frontend/src/services/api/ai.ts`
- `itda-frontend/src/composables/useNodeGeneration.ts`
- `itda-frontend/src/stores/sceneNode/index.ts`

**구현 내용**

프롬프트 생성/개선/번역/리라이트, 노드 생성 job 요청, prompt preview, job 상태 처리, timeout/502/503/504 재시도 및 사용자 친화적 오류 메시지 처리를 구현했습니다.

**사용 기술**

- Axios
- TypeScript
- REST API
- 비동기 retry

**이력서에 쓸 수 있는 표현**

> AI 생성 요청의 비동기 job 흐름을 프론트엔드에서 추상화하고, 재시도/실패 상태/결과 URL 반영 로직을 구현했습니다.

**면접에서 설명할 수 있는 포인트**

생성형 AI API는 응답 시간이 길고 실패 가능성이 높기 때문에 timeout, gateway error, job 상태 이벤트를 별도로 관리한 점을 설명할 수 있습니다.

### 기능명: 타임라인 편집 및 영상 병합/내보내기

**관련 파일/폴더**

- `itda-frontend/src/pages/TimelinePage.vue`
- `itda-frontend/src/stores/timeline.ts`
- `itda-frontend/src/services/api/timeline.ts`
- `itda-frontend/src/components/timeline`

**구현 내용**

확정된 video node를 클립으로 표시하고, drag reorder, remove/unconfirm, scene/project 단위 병합 요청, WebSocket job 완료 이벤트 수신, export 목록/활성화/삭제/다운로드 처리를 구현했습니다.

**사용 기술**

- Pinia
- Axios
- WebSocket
- HTML video metadata
- Blob/download

**이력서에 쓸 수 있는 표현**

> AI 생성 영상 클립을 타임라인으로 구성하고, 순서 저장/병합 요청/결과 다운로드까지 이어지는 영상 제작 플로우를 구현했습니다.

**면접에서 설명할 수 있는 포인트**

duration hydration, presigned/media lease 처리, 병합 job의 cached/succeeded/failed 상태 대응을 설명할 수 있습니다.

### 기능명: 실시간 협업 기능

**관련 파일/폴더**

- `itda-frontend/src/stores/collab.ts`
- `itda-frontend/src/services/ws/socket.ts`
- `itda-frontend/src/services/ws/projectEvents.ts`

**구현 내용**

프로젝트 룸 join/leave, 채팅, presence, 커서 공유, 노드 선택 잠금, 노드 이동 throttle 동기화, 읽음 상태, 재접속 시 subscription 복구를 구현했습니다.

**사용 기술**

- STOMP
- SockJS
- Pinia
- throttle
- localStorage

**이력서에 쓸 수 있는 표현**

> STOMP/SockJS 기반 실시간 협업 상태를 구현해 사용자 presence, 커서, 노드 이동, 노드 잠금을 동기화했습니다.

**면접에서 설명할 수 있는 포인트**

local user 이벤트 무시, remote stale event 방지, scene 변경 시 lock/cursor 초기화 같은 충돌 방지 로직을 설명할 수 있습니다.

### 기능명: WebRTC 음성 협업

**관련 파일/폴더**

- `itda-frontend/src/services/webrtc/peerConnection.ts`
- `itda-frontend/src/stores/collab.ts`

**구현 내용**

마이크 스트림 획득, mesh peer connection 생성, offer/answer/ICE candidate 처리, mute, 마이크 전환, speaking detection, 화면 공유 일부 지원을 구현했습니다.

**사용 기술**

- WebRTC
- MediaDevices API
- AudioContext
- STOMP signaling

**이력서에 쓸 수 있는 표현**

> WebRTC 기반 음성 협업 기능을 구현하고 STOMP signaling과 연동해 실시간 회의 상태를 관리했습니다.

**면접에서 설명할 수 있는 포인트**

브라우저 media permission, peer cleanup, audio device switching, room max participant 제한을 설명할 수 있습니다.

### 기능명: 인증/인가 및 사용자 프로필

**관련 파일/폴더**

- `itda-frontend/src/stores/auth.ts`
- `itda-frontend/src/pages/AuthPage.vue`
- `itda-frontend/src/services/api/client.ts`
- `itda-frontend/src/router/index.ts`

**구현 내용**

로그인/회원가입, JWT localStorage 저장, `/auth/me` 조회, 프로필 수정/이미지 업로드, 라우터 인증 가드, 401 시 토큰 제거 및 로그인 이동, 403 접근 거부 이동을 구현했습니다.

**사용 기술**

- Pinia
- Vue Router
- Axios interceptor
- FormData

**이력서에 쓸 수 있는 표현**

> JWT 기반 인증 상태 관리와 라우팅 가드, Axios 인터셉터를 통한 인증 오류 처리 흐름을 구현했습니다.

**면접에서 설명할 수 있는 포인트**

토큰 형식 검증, protected route 처리, FormData Content-Type 제거 처리를 설명할 수 있습니다.

### 기능명: 프로젝트/씬/오브젝트 관리

**관련 파일/폴더**

- `itda-frontend/src/pages/DashboardPage.vue`
- `itda-frontend/src/pages/ProjectDetailPage.vue`
- `itda-frontend/src/pages/project/composables/useProjectDetail.ts`
- `itda-frontend/src/stores/project.ts`
- `itda-frontend/src/stores/object.ts`

**구현 내용**

프로젝트 생성/목록/최근 접근 정렬/즐겨찾기/휴지통, 씬 추가/삭제/순서 변경, 오브젝트 생성/수정/삭제/이미지 다운로드, 초대 알림 polling을 구현했습니다.

**사용 기술**

- Pinia
- REST API
- File upload/download
- localStorage

**이력서에 쓸 수 있는 표현**

> 프로젝트 대시보드와 상세 화면의 CRUD, 초대 알림, 오브젝트 파일 업로드/다운로드 플로우를 구현했습니다.

**면접에서 설명할 수 있는 포인트**

owner는 삭제, non-owner는 leave 처리하는 역할 기반 분기를 설명할 수 있습니다.

## 트러블슈팅 후보

### 문제 상황: AI 생성 요청이 timeout 또는 502/503/504로 실패

**원인**

생성형 AI/영상 생성 작업은 처리 시간이 길고 외부 API 응답이 불안정할 수 있습니다.

**해결 방법**

`generateNode`에 retry, backoff, 사용자 친화적 에러 메시지를 적용했습니다.

**사용한 기술/방식**

- Axios error status/code 판별
- retry loop
- timeout 처리

**개선 결과**

일시적 서버 오류에 대한 사용자 경험을 개선했습니다.

**이력서 표현**

> AI 생성 API의 timeout/5xx 오류에 대한 재시도 및 오류 메시지 개선으로 생성 플로우 안정성을 높였습니다.

**면접 답변용 설명**

생성 작업은 즉시 완료되지 않기 때문에 REST 응답과 WebSocket job 이벤트를 분리하고, retry 가능한 오류만 제한적으로 재시도했습니다.

### 문제 상황: 타임라인 클립 duration이 부정확하거나 0으로 표시

**원인**

백엔드 duration 단위가 ms/sec 혼재하거나 video metadata 로딩이 늦을 수 있습니다.

**해결 방법**

`normalizeDurationSeconds`, HTML video metadata hydration, duration cache를 적용했습니다.

**사용한 기술/방식**

- video `loadedmetadata`
- cache/inflight map
- Playwright regression test

**개선 결과**

총 길이 계산과 타임라인 UI 정확도를 개선했습니다.

**이력서 표현**

> 영상 메타데이터 기반 duration 보정 및 회귀 테스트로 타임라인 길이 계산 정확도를 개선했습니다.

**면접 답변용 설명**

API 값만 믿지 않고 실제 비디오 메타데이터를 비동기로 읽어 보정했습니다.

### 문제 상황: 실시간 협업 중 노드 위치가 튀거나 충돌

**원인**

로컬 드래그 이벤트와 원격 `NODE_MOVE` 이벤트가 동시에 들어오고, 오래된 이벤트가 뒤늦게 도착할 수 있습니다.

**해결 방법**

throttle, `localDraggingNodes`, `lastRemoteNodeMoveAt`, `actorId` 필터링을 적용했습니다.

**사용한 기술/방식**

- STOMP presence event
- timestamp 비교
- local/remote 이벤트 분리

**개선 결과**

원격 이동 반영 안정화 및 충돌을 줄였습니다.

**이력서 표현**

> 실시간 노드 이동 동기화에서 이벤트 중복/역전 문제를 timestamp와 throttle로 완화했습니다.

**면접 답변용 설명**

내가 드래그 중인 노드에는 원격 이벤트를 적용하지 않고, timestamp가 오래된 이벤트는 버렸습니다.

### 문제 상황: API 미디어 URL이 직접 접근 불가하거나 presigned URL 만료

**원인**

S3/보호 리소스는 매번 접근 가능한 URL이 아닐 수 있습니다.

**해결 방법**

`acquireMediaLease`, `releaseMediaLease`, `resolveApiUrl`로 미디어 URL 생명주기를 관리했습니다.

**사용한 기술/방식**

- lease map
- cleanup on unmount/clear
- fallback URL 처리

**개선 결과**

이미지/영상 미리보기 안정화 및 메모리/URL 누수를 줄였습니다.

**이력서 표현**

> 보호된 미디어 리소스의 URL lease 관리와 cleanup 로직을 구현해 미리보기 안정성을 개선했습니다.

**면접 답변용 설명**

컴포넌트가 사라질 때 blob/presigned 리소스를 정리하고, API resource URL은 lease를 얻어 재생했습니다.

### 문제 상황: 소스 내 한글 UI 문구가 깨져 보임

**원인**

일부 파일이 인코딩 문제로 mojibake 상태입니다.

**해결 방법**

추정입니다. UTF-8 재저장, i18n 리소스 분리, CI에서 인코딩 체크 추가가 필요합니다.

**사용한 기술/방식**

- UTF-8 normalization
- lint/check script

**개선 결과**

유지보수성과 QA 효율 개선 가능.

**이력서 표현**

이 항목은 실제 해결 코드 근거가 부족하므로 이력서에는 “트러블슈팅 후보”로만 사용하는 것을 권장합니다.

**면접 답변용 설명**

코드상 한글 문자열 깨짐이 보여 운영 전 인코딩 정리와 i18n 분리를 제안할 수 있습니다.

## 정리용 이력서 문장

- Vue 3/TypeScript 기반 AI 영상 제작 SPA에서 프로젝트, 씬, 노드 에디터, 타임라인, 협업 화면을 구현했습니다.
- Vue Flow를 활용해 Master Image, Storyboard Grid, Shot, Video로 이어지는 노드형 AI 생성 워크플로우를 설계하고 자동 레이아웃/위치 저장/노드 확정 로직을 구현했습니다.
- STOMP/SockJS와 WebRTC를 연동해 실시간 presence, 커서, 노드 잠금, 노드 이동 동기화, 음성 협업 기능을 구현했습니다.
- Axios 인터셉터와 Pinia store 기반으로 JWT 인증, API 오류 처리, mock/API 전환 가능한 서비스 계층을 구성했습니다.
- AI 생성 job, 타임라인 병합, 미디어 URL lease, 다운로드/미리보기 흐름을 구현해 생성 결과가 최종 영상 산출물로 이어지도록 연결했습니다.
