# 04) 프로젝트 상세 (`/projects/:id`) (Story/Scenes/Objects)

스크린샷(1440x900):

- Story(씬 없음): `screenshots-1440x900/04-project-detail-story-empty.png`
- Story(씬 1개): `screenshots-1440x900/05-project-detail-story-with-scene.png`
- Scenes 탭: `screenshots-1440x900/06-project-detail-scenes-tab.png`

## 현재 관찰

- “Story / Scenes / Objects” 탭 구조는 PRD와 맞고, 장기적으로 확장 가능.
- 다만:
  - 좌측 네비(Story/Scenes/Objects/Timeline/Settings)와 상단 탭(Story/Scenes/Objects)이 **중복**되어 정보 구조가 헷갈릴 수 있음.
  - Progress(0/1)가 상단에 있지만 “다음 액션”과 연결이 약함.
  - 씬 카드가 “초안/클립 0개” 상태일 때 화면이 허전하고, 데모에서 임팩트가 적음.

## 뷰포트(데스크톱)로 보니 더 뚜렷한 점 (1440x900 기준)

- 메인 콘텐츠가 중앙 “얇은 줄”처럼 놓여 있고, 좌/우로 큰 여백이 생김 → “관리 페이지”처럼 보이고 “제작 툴” 느낌이 약해짐.
- Scenes 탭 역시 리스트가 단일 카드 중심이라, 넓은 화면에서 **미리보기(16:9) + 큐/메타**를 같이 보여줄 자리가 남는데 활용이 안 됨.

## 디자인 개선 제안

### 1) 한 화면에 CTA는 1개만 “강하게”

Story 탭에서 사용자가 해야 할 일은 거의 항상 2개 중 하나:

1) “AI로 씬 생성(스토리 구성)” 또는
2) “씬 직접 추가”

추천:

- 상단에 “다음 해야 할 일” 고정 바(얇은 Stepper)
  - Step 1: 프로젝트 정보 → Step 2: 씬 구성 → Step 3: 스토리보드(노드) → Step 4: 타임라인
- 현재 화면의 Primary CTA는 1개만 강조(예: 씬이 0개면 `+ 씬 추가`가 Primary, 씬이 있으면 `Scene Editor로 이동`이 Primary)

### 2) 씬 카드의 정보 밀도/위계 강화

지금은 텍스트 중심이라 “영화 제작” 감각이 덜함.

- 씬 카드 왼쪽에 “씬 번호”만이 아니라 **씬 썸네일(Active Master)** 을 크게
  - 아직 없으면 필름 프레임 플레이스홀더
- 상태(초안/진행/완료/실패)는 배지 색만이 아니라:
  - 아이콘 + 짧은 문구(예: `준비됨`, `생성중`, `실패 - 재시도`)로 즉시 이해
- 우측 CTA는 2개(미리보기/편집)보다
  - “편집”을 Primary, “미리보기”는 아이콘 버튼으로 축소(데모 플로우 안정)

### 3) Scenes 탭의 “프로젝트 미리보기”를 더 영화처럼

- 현재 “Scene Preview” 리스트는 기능적이지만 감성/임팩트가 약함.
- 추천:
  - 상단에 “프로젝트 리일(짧은 합성 미리보기)” 카드(실제 영상 없으면 더미 리일)
  - 아래에 씬별 스트립(thumbnail strip) + “확정 클립 개수/길이”를 시각화

### 모드/컨셉 분리(사용자 선택형)

- Bright Studio(라이트, 기본)
  - 프로젝트 상세는 “관리/구성” 화면이므로 라이트 유지가 맞음
  - 대신 영상 제작툴 인상은 `Preview(16:9)` 카드, 타임코드, “Render Queue 요약” 같은 **툴 언어**로 만든다
- Studio Mode(다크, 선택)
  - 기본은 비적용(과하게 어두우면 관리 화면 가독성 하락)
  - 다만 사용자가 Studio Mode를 켠 상태로 들어올 수 있으니, 상단 Preview 카드만 “모니터”처럼 다크 패널로 바뀌는 하이브리드 옵션은 고려 가치가 있음

### 4) 협업(플로팅 바)의 존재감 조절

- 플로팅 바는 중요하지만, 현재는 모든 화면에서 동일한 크기/표현이라 주의를 분산시킴.
- 추천:
  - “프로젝트 상세”에서는 작게(상태만) 접혀 있고,
  - “씬 편집”에서는 도구처럼(명확한 컨트롤) 확장되도록 “컨텍스트 기반 크기” 적용

## 전환/모션 제안

### Story ↔ Scenes ↔ Objects 탭 전환

- 단순히 내용만 바뀌는 것이 아니라, “같은 페이지 안에서 패널이 바뀌는” 느낌:
  - 인디케이터가 슬라이드
  - 콘텐츠는 좌/우 12px 이동 + 페이드(160~200ms)

### 프로젝트 상세 → 씬 편집

- 씬 카드 썸네일/제목을 공유 요소로:
  - 클릭한 씬 카드가 “확대되며” 캔버스로 전환
  - (Chrome 데모용) `View Transition API` 활용 시 체감 완성도가 크게 올라감

## 데스크톱 레이아웃 제안(>=1280px)

프로젝트 상세는 “제작 진행의 허브”라서, 넓은 화면을 적극적으로 써야 스튜디오 느낌이 살아남.

### Default (권장안): 2-Panel Hub

- Left (Main): Story/Scenes/Objects content
- Right (Sticky Sidebar): **항상 노출**
  - `Preview Monitor (16:9)` (더미/플레이스홀더라도 “모니터” 프레임 유지)
  - `Render Queue / Job Status` (대기/진행/완료/실패)
  - `Project Meta` (16:9, 목표 길이, 씬 수, 총 확정 클립 수)

특히 Scenes 탭은 “리스트”만 두지 말고:

- 좌: 씬 리스트(스트립/카드)
- 우: 선택한 씬의 16:9 프리뷰 + 확정 클립 스트립 + “씬 편집” CTA

로 구성하면 “AI 영상 제작 스튜디오”로 바로 읽힘.

### Default (Scenes 탭 구체안): 2-Column Studio Layout

- Left: 씬 리스트 (세로 리스트 + 썸네일은 16:9 프레임)
  - 각 아이템은 `타임코드 합(총 길이)` + `확정 클립 수` + `상태 배지`를 한 줄로 보여주기
- Right: “씬 스튜디오” 패널
  - Top: `Preview Monitor (16:9)` + 타임코드 + 해상도 배지
  - Middle: `Confirmed Clips Strip` (가로 스트립, 없으면 Empty 가이드 + CTA)
  - Bottom: `Render Queue` (씬 단위/프로젝트 단위 job 상태)
  - Primary CTA: `씬 편집` (항상 1개만 강하게)

### Alternatives (옵션/토글)

- `Sidebar Compact` 토글: 오른쪽 패널을 접고 “Preview만 PiP”로 축소 (좁은 데스크톱/발표 화면 대응)
- `Studio Mode` 토글: 제작 화면(Scenes 탭 포함)을 어둡게 전환할지 선택
- `Atmosphere` 토글: Filmic 효과(grain/vignette/scanline)를 켜고 끄기
