# AI Movie Studio — 통합 문서 v3

이 폴더는 팀 내 공유를 위한 **단일 진실 소스(Single Source of Truth)** 입니다.

- 일정 기준: 3~6주차(2026-01-19 ~ 2026-02-13, 평일)
- MVP 목표: 3주차 말(2026-01-23 금) **E2E 1회 완주**

---

## 처음 합류한 팀원용 30분 읽기 코스

1. `00-overview.md`
2. `01-decisions-and-scope.md`
3. `02-schedule-w3-w6.md`
4. `03-team-plan-by-person.md`
5. `04-architecture-core.md`

---

## 문서 인덱스

- `00-overview.md` — 목표/DoD/큰 그림
- `01-decisions-and-scope.md` — 협업(WebRTC/Presence/CRDT) 스코프 결정 + 이유
- `02-schedule-w3-w6.md` — 3~6주차 일정(주차/일자 단위)
- `03-team-plan-by-person.md` — 팀원별 To-Do (바로 착수용)
- `04-architecture-core.md` — 전체 아키텍처(Dispatcher/Worker/WS)
- `05-dataflow-mvp.md` — E2E 데이터 플로우(요청→큐→처리→상태반영)
- `06-api-and-events.md` — HTTP API + WS 이벤트 계약(병렬 개발용)
- `07-dispatcher-queue-worker-guide.md` — Redis Streams 기반 워커 패턴/예시 코드
- `08-ffmpeg-merge-worker.md` — FFmpeg 병합 워커(MVP concat)
- `09-webrtc-collab-week5.md` — 5주차 WebRTC(음성 통화) 설계
- `10-presence-cursor-softlock.md` — Presence/Cursor/Follow/Soft Lock
- `11-yjs-crdt-decision.md` — CRDT(Yjs) 적용 여부/범위 결정
- `12-local-dev-runbook.md` — 로컬 실행 표준(Docker Compose)
- `13-runbook-devops.md` — 운영/배포 체크(서버/로그/보안)
- `14-release-demo-checklist.md` — 최종 통합/데모 체크리스트
- `15-tasks-by-role.md` — 역할별 책임/산출물 정리
- `16-glossary.md` — 용어집
- `17-checklists.md` — 각종 체크리스트 모음
