# 03. 팀원별 To-Do (처음 보는 팀원도 바로 착수용)

> 이 문서는 “내가 지금 뭘 해야 하지?”를 바로 알 수 있도록, **주차별/핵심 산출물 중심**으로 정리했습니다.

---

## 공통 전제 (모든 팀원이 알아야 하는 것)

- **MVP 목표(3주차 말):** E2E 1회 완주
- **Job 처리 방식:** Spring API Server(=Dispatcher 역할) → Redis Streams → Worker → 결과 저장 → WS 이벤트로 FE 반영
- **Dispatcher 정의:** “Spring API 서버 안에서 Job을 생성/발행하고 상태를 관리하는 역할(모듈)”
    - 별도 프로세스로 분리하지 않음(이번 MVP 범위)
- **Worker 정의:** “Streams를 소비하는 별도 실행 프로세스(별도 Spring 앱 or 경량 런타임)"
    - **docker-compose로 별도 컨테이너로 띄우는 걸 기본으로**
- **실시간(WS):** 3주차에는 job/timeline 상태 알림 위주, 5주차에 chat/presence/rtc 확장

---

## 강보승 (Lead / Full-stack)

### W3 핵심 산출물
- [ ] 공통 스켈레톤(프로젝트 구조, 컨벤션, 공통 예외/응답)
- [ ] 인증(JWT) + 프로젝트 핵심 CRUD(프로젝트/노드 기본)
- [ ] D5 E2E 통합 테스트 + 우선순위 triage

### W3 일자별(권장)
- **D1(01/19):** 레포/모듈 구조, 공통 응답 규격, 인증 UI/BE 골격
- **D2(01/20):** JWT 필터/리프레시(최소), 프로젝트 CRUD 완성
- **D3(01/21):** 노드 CRUD 스켈레톤(scene_nodes) + FE mock 계약 확정
- **D4(01/22):** 병합 요청 API 스켈레톤(`/merge`)가 D5로 안 밀리도록 “형태” 고정
- **D5(01/23):** 통합(시나리오→노드→job→확정→병합→다운로드) 1회 성공

### W5(협업)에서의 최소 개입
- [ ] Follow(팀원 클릭→해당 씬/노드로 이동) FE와 함께 UX 정리

---

## 이용호 (BE / 인프라 + 이미지 Worker + 이벤트)

### W3 핵심 산출물
- [ ] Docker Compose 로컬 표준(MySQL/Redis/S3(LocalStack))
- [ ] Jenkins 파이프라인 안정화(빌드/테스트/PR 체크)
- [ ] Redis Streams Job Queue 스키마/규칙 문서화
- [ ] 이미지 Worker(Gemini) 1차 완성 + WS 이벤트(job.done/job.failed)

### W4 확장
- [ ] Worker 관측성(로그/지표/알람 초안)
- [ ] API 안정성(레이트리밋/timeout/retry)

### W5 협업 파트
- [ ] Presence/Cursor/Selection 이벤트 설계(서버/스키마)

---

## 김은서 (BE / 시나리오 + 영상 Worker + WebRTC)

### W3 핵심 산출물
- [ ] 시나리오 API v1(프롬프트→줄거리) + 씬 자동 생성(4단계 완주)
- [ ] 영상 Worker(Veo) **Mock 모드 포함** 1차 완성(최소 1건 성공)

### W5 핵심 산출물
- [ ] WebRTC 시그널링(offer/answer/candidate) WS 구현
- [ ] 프로젝트 룸 채팅(WebSocket broadcast)
- [ ] 오디오 통화 연결(재연결/에러 케이스)

---

## 장현준 (BE / 도메인 제약 + FFmpeg + 오브젝트 시트)

### W3 핵심 산출물
- [ ] 씬 CRUD + 순서 변경 API(drag reorder 대비)
- [ ] 노드 상태/제약(Active master 유일성, confirm 유일성)
- [ ] 영상 확정 API(confirm/unconfirm)
- [ ] 타임라인 조회 API + 병합 Worker(concat) 최소 성공

### W4 핵심 산출물
- [ ] 오브젝트 시트 BE(CRUD + 생성 Job)
- [ ] 멤버 초대/권한(Owner/Editor/Viewer)
- [ ] 파일 업로드 Presign(S3)

### W5 협업 파트
- [ ] TURN(coturn) 세팅/테스트

---

## 박재서 (FE / 씬 편집 + 협업 바)

### W3 핵심 산출물
- [ ] Vue Flow 기본 캔버스(드래그/줌) + 노드 컴포넌트 v1(마스터/그리드/샷/영상)
- [ ] 노드 사이드바(입력/승인/재시도/상태 배지)
- [ ] WS 구독 후 노드 상태 반영(토스트/배지)

### W4 확장
- [ ] 씬 편집 UX 개선(폴딩, Active master, 버전)

### W5 협업 파트
- [ ] 플로팅 협업 바 UI(오버레이/접기) + 오디오 디바이스/권한 UX

---

## 이진원 (FE / 대시보드 + 타임라인 + 협업 UI)

### W3 핵심 산출물
- [ ] 라우팅/공통 레이아웃(대시보드/상세/씬편집)
- [ ] 프로젝트 상세(시나리오 생성 UI/씬 목록) + 씬 편집 진입
- [ ] 타임라인 UI(확정 클립/드래그 순서) + 병합 진행/다운로드 UI

### W4 확장
- [ ] 오브젝트 시트 UI(목록/생성/참조선택)
- [ ] 멤버 관리 UI(초대/권한 변경)

### W5 협업 파트
- [ ] 채팅 UI 패널 + Presence/Cursor/Selection 표시

---

## 3주차 끝날 때까지 “각자 성공 기준” (체크용)

- 강보승: **E2E 1회 완주 + 기본 CRUD/인증 정상**
- 이용호: **Streams 스키마 고정 + 이미지 Worker 성공 + WS 이벤트 전송**
- 김은서: **시나리오 4단계 완주 + 영상 Worker 1건 성공(또는 Mock)**
- 장현준: **확정/타임라인/병합(concat) 최소 성공**
- 박재서: **노드 UI + 상태/재시도 UX + WS 반영**
- 이진원: **타임라인/병합 UI + 대시보드 흐름 연결**
