# 15. 담당자별 "이번 주에 뭘 하면 되는지" (실행용)

이 문서는 일정표를 "구현 관점"으로 재해석해서, 각자가 바로 착수할 수 있게 정리합니다.

---

## 강보승 (Lead / 풀스택)
### 3주차(핵심)
- API Server 스켈레톤
  - 프로젝트/씬/노드 CRUD의 기본 골격 확립
  - Dispatcher(=Job 생성 + Streams 발행) 공통 패턴 확정
- 인증(JWT) 최소 구현 + FE 라우팅 흐름 연결
- E2E 통합(우선순위 triage, 버그픽스)

### 4~6주차(보조)
- 공통 코드 리뷰 규칙/패키지 구조 정리
- 릴리즈 체크리스트/데모 플로우 점검

---

## 이용호 (BE / 인프라 / 이미지 Worker / WS 이벤트)
### 3주차(핵심)
- Docker Compose 로컬 표준(MySQL/Redis/S3(LocalStack))
- Jenkins 파이프라인 표준화/안정화
- Redis Streams 규칙 문서화
  - Stream 이름/메시지 스키마
  - consumer group 규칙
  - ACK/pending/retry 기준
- 이미지 Worker 구현(Streams 소비 → 이미지 생성 → Storage/DB 업데이트)
- Job 완료/실패 WebSocket 이벤트 발행(프로젝트 room)

### 4~6주차
- Worker 모니터링/로깅(지표 초안)
- Presence(프로젝트 접속/화면 위치) 서버 측 이벤트(5주차)

---

## 김은서 (BE / 시나리오 / 영상 Worker / WebRTC 시그널링·채팅)
### 3주차(핵심)
- Gemini 연동 PoC → 시나리오 API v1
- 시나리오→씬 자동 생성(4단계 완주)
- 영상 Worker 착수(Veo)
  - Mock 모드(고정 샘플 반환) 먼저
  - 실 API 전환은 최소 1건 성공 기준

### 5주차(핵심)
- WebRTC 시그널링 서버(WebSocket)
  - room 관리(프로젝트 단위)
  - offer/answer/candidate relay
- 채팅(WebSocket broadcast)

---

## 장현준 (BE / 노드·타임라인·권한 / FFmpeg Worker / 오브젝트시트)
### 3주차(핵심)
- 씬 CRUD + 순서 변경 API
- 노드 제약(Active master 유일성, confirm 유일성)
- 영상 확정(confirm/unconfirm) API
- 타임라인 조회/순서변경 API
- 병합 API 스켈레톤 + FFmpeg 병합 Worker(concat)

### 4주차(핵심)
- 오브젝트 시트 API/CRUD + 생성 Job
- 멤버 초대/권한(Owner/Editor/Viewer)
- 파일 업로드 presigned URL

### 5주차(보조)
- TURN/STUN 설정/테스트(필요 시)

---

## 박재서 (FE / 씬 편집 캔버스(Vue Flow) / WebRTC 미디어 UI)
### 3주차(핵심)
- Vue Flow 학습 + 기본 캔버스 렌더
- 노드 컴포넌트 v1(마스터/그리드/샷/영상) + 상태 배지
- 노드 사이드바(입력/승인/생성/재시도)
- WebSocket 구독 → 노드 상태 실시간 반영

### 5주차(핵심)
- 플로팅 협업 바 UI(접기/고정)
- 음성 통화 UI(마이크 on/off, 참여자 목록)
- 통화 유지(라우트 이동) 대응: 전역 상태/싱글턴 연결

---

## 이진원 (FE / 대시보드·타임라인 / WS·Presence·채팅 UI)
### 3주차(핵심)
- 라우팅 + 공통 레이아웃
- 로그인→대시보드→프로젝트 상세→씬 편집 연결
- 타임라인 UI(확정 클립 목록, reorder) + 병합 진행/다운로드 UI

### 5주차(핵심)
- 채팅 UI 패널
- Presence 표시(누가 어디서 작업 중인지)
- Cursor/Selection 표시(이벤트 기반)
