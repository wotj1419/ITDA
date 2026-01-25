# UI/UX 디자인 개선 리포트 (W3 범위)

> 기준 문서: `docs/PRD_AI_Movie_Studio_v2.5.md`, `docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`
>
> 탐색 일자: 2026-01-25
> 탐색 방식: Playwright MCP로 실제 구현 화면(데스크톱) 직접 탐색 + 스크린샷 캡처

## 목차

- 00) 개요/방향: `00-overview.md` (1440x900 재촬영본 기준으로 레이아웃 피드백 보강)
- 01) 랜딩(`/`): `01-landing.md`
- 02) 로그인/회원가입(`/auth`): `02-auth.md`
- 03) 대시보드(`/dashboard`) + 새 프로젝트 모달: `03-dashboard.md`
- 04) 프로젝트 상세(`/projects/:id`) (Story/Scenes/Objects): `04-project-detail.md`
- 05) 씬 편집(`/projects/:projectId/scenes/:sceneId`): `05-scene-editor.md`
- 06) 타임라인(`/projects/:id/timeline`): `06-timeline.md`
- 07) 기타 화면(프로필/즐겨찾기/공유/휴지통/권한없음): `07-secondary-pages.md`
- 08) 전환/모션 시스템 제안(공통): `08-motion-system.md`
- 09) 전반적 디자인 개선(비주얼 시스템): `09-visual-system.md`
- 10) Video Studio 방향 + Studio Mode(다크 토글): `10-studio-mode.md`
- 11) 컨셉/모드별 피드백 가이드(선택 가능): `11-concepts.md`

## Default vs Alternatives

핵심 제작 화면(`04-project-detail.md`, `05-scene-editor.md`, `06-timeline.md`)은
“옵션 제안”만 나열하지 않고 **Default(권장 레이아웃)** 를 먼저 못 박고,
그 다음에 **Alternatives(토글/선택 옵션)** 을 제안하는 구조로 작성했습니다.

## 스크린샷

스크린샷(기준 viewport: `1440x900`)은 아래에 있습니다.

- `docs/ui-audit-w3-2026-01-25/screenshots-1440x900/`

참고: 초기(좁은 viewport) 캡처본은 제거했습니다. 필요 시 Playwright MCP로 재촬영을 권장합니다.
