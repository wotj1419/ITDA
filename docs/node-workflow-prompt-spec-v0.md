# AI Movie Studio — Node Workflow & Prompt Spec (v0)
> 목적: 이 문서만 보고 **프론트엔드/백엔드가 동일한 규칙**으로 “마스터 → 그리드 → 샷 → 영상” 노드 워크플로우를 구현/수정할 수 있도록, **입력값(UX)·저장 스키마·프롬프트 전략(한글 UI / 영어 생성)·API 계약**을 한 번에 정리한다.

---

## 0. 배경 / 문제정의

현재 `/projects/:projectId/scenes/:sceneId`(Vue Flow 씬 편집) 화면은 CRUD/비동기 Job 흐름이 연결되어 있으나, 다음 문제가 반복된다.

1) 노드 목록 조회가 “요약 정보” 중심이라, 새로고침/재진입 시 패널 폼(스타일/시간대/분위기/프롬프트 등)이 비어 보여 **‘결과(contentUrl)만 남는’ 체감**이 생김.
2) 프롬프트/설정이 UI에서 존재해도, 모델이 실질적으로 잘 먹는 형태(특히 Video)로 “영문/구조화”되지 않아 결과 품질/재현성이 흔들림.
3) `GRID`를 “샷 후보 그리드(추출용)” 외에도 “스토리 비트(8초 단위 등)”로 확장하려면, 입력/프롬프트 템플릿을 모드 단위로 분리해야 함.

이 문서는 위 문제를 해결하기 위한 **MVP 규칙**을 정의한다.

---

## 1. 목표(결정사항 요약)

### 1.1 한국어 UI, 영어 생성 프롬프트
- UI(프롬프트 텍스트 박스)는 **항상 한국어(promptKo)** 를 표시/수정한다.
- 실제 이미지/영상 생성(Job request)에 들어가는 프롬프트는 **영어(promptEn)** 를 사용한다.
- 특히 Veo 3.1은 prompt language가 English로 제한된다. (참고: Vertex AI Veo 3.1 문서의 “Prompt languages: English”, “Video length: 4, 6, or 8 seconds”)  
  - https://cloud.google.com/vertex-ai/generative-ai/docs/models/veo/3-1-generate

### 1.2 노드 상세 “하이드레이트”는 선택 시 1회
- 씬 진입 시에는 `GET /api/scenes/{sceneId}/nodes`로 **트리(요약)만** 로드한다.
- 사용자가 노드를 클릭하여 패널을 열면, 해당 노드에 한해 `GET /api/nodes/{id}`로 **상세(prompt/settings)** 를 가져와 Store에 병합한다.
- 동일 노드는 캐시하여 중복 호출을 방지한다.

### 1.3 기본값은 “필수 강제”가 아니라 “자동 주입”
- 사용자가 timeOfDay/mood 등을 반드시 고르도록 강제하지 않는다.
- 대신 **폼 초기값(디폴트)** 을 채워두고, “프롬프트 생성/생성 요청”에는 항상 값이 들어가도록 한다.
- 서버는 최종적으로 `promptEn`이 빈 문자열이면 400으로 막는다(안전장치).

### 1.4 GRID는 모드 2개로 분리
- `GRID_MODE=SHOT_VARIATIONS`: 샷 후보를 한 장의 그리드 이미지로 생성하고, 셀 인덱스로 `SHOT`를 추출한다.
- `GRID_MODE=STORY_BEATS`: 스토리 비트(예: 4/6/8초 단위 구성 등)를 칸으로 나눈 한 장의 “스토리보드”를 만든다. (추출은 가능하되, 템플릿/입력값이 다름)

### 1.5 병렬 개발을 위한 Contract Freeze(필수)
이 문서 기반으로 프론트/백/문서를 병렬로 작업하려면, 아래 항목을 먼저 고정해야 재작업이 최소화된다.

#### 1.5.1 이번 스프린트 범위(P0/P1) 선언(권장)
- P0(이번): `MASTER`, `GRID(SHOT_VARIATIONS)`, `SHOT`, `VIDEO` + promptKo/promptEn 분리 + 노드 선택 시 하이드레이트
- P1(다음): `GRID(STORY_BEATS)`(UI/검증/템플릿) + `endShotNodeId`의 first+last frame API 호출 확장

#### 1.5.2 반드시 합의할 3가지(결정 결과를 기록)
- [DECISION] GRID(SHOT_VARIATIONS)에서 `layout 패널 수`와 `shotTypes.length`가 다를 때:
  - A) 자동 보정 / B) 400으로 막기
- [DECISION] 번역/요약(ko→en) 수행 위치:
  - A) 서버 PromptRenderer에서만 수행(권장, 단일 소스) / B) 프론트 일부 수행
- [DECISION] VIDEO 대사/오디오 정책:
  - v0에서는 “소리는 신경 X”(품질 목표/정책 강제 없음). 다만 텍스트 렌더링 위험을 줄이는 표현 가이드는 준수.

---

## 2. 용어 / 엔티티

- **promptKo**: UI에서 사용자에게 보여주고 수정 가능한 한국어 프롬프트
- **promptEn**: 생성(Job)에 사용되는 영어 프롬프트(서버가 생성/저장)
- **settings**: 노드 타입별 구조화 입력값. DB에는 JSON(`data_json`)으로 저장.
- **하이드레이트(hydrate)**: 노드 요약 목록으로 만든 UI 노드에, `GET /api/nodes/{id}` 상세를 추가 병합하여 폼 상태를 복원하는 것.

---

## 3. 노드 타입 설명(이 문서만 보고 이해 가능하게)

> 노드는 “씬 편집 화면(Vue Flow)”에서 위→아래로 진행된다: **MASTER → GRID → SHOT → VIDEO**.  
> 각 노드는 **입력(사용자 폼 + 상속 값)** → **promptKo 생성/수정/승인** → **결과 생성(Job)** → **결과 저장(contentUrl)**의 공통 흐름을 가진다.

### 3.1 공통 규칙(모든 생성 노드)
- 상태 표시: `PENDING/RUNNING/SUCCEEDED/FAILED` (Job 실행 결과로 갱신)
- 프롬프트 단계: `DRAFT → GENERATED → APPROVED` (UI gating용)
  - `APPROVED`가 아니면 “결과 생성” 버튼 비활성화(권장 UX)
- 저장:
  - `node.prompt` = `promptKo` (UI용)
  - `node.data_json.settings` = 구조화 settings + `promptEn`(서버가 생성해 캐시)
- 생성(Job) 요청은 **항상 비동기**이며 `POST /api/nodes/{id}/generate`는 `202 Accepted + jobId`를 반환한다.

### 3.2 SCENE_HEADER 노드(가상)
- 목적: 씬 제목/설명(스토리)을 캔버스 상단에 고정 표시하는 메타 노드
- 생성/삭제: 서버가 가상으로 제공(테이블에 저장하지 않음)
- 입력/출력: AI 생성 대상 아님(읽기 전용)
- 활용: 다른 노드의 “씬 컨텍스트(sceneTitle/sceneDescription)” 소스

### 3.3 MASTER 노드(마스터 이미지)
- 목적: 해당 씬의 “룩/세계관” 기준이 되는 **와이드/설정샷 1장** 생성
- 생성 위치: SCENE_HEADER 아래(루트). 여러 개 가능하지만 제한 있음.
- 입력(사용자):
  - `styleKey`, `timeOfDayKey`, `moodKey`, `objectIds`(선택), `aspectRatio`
  - promptKo(한국어 텍스트)는 UI에서 생성/수정 가능
- 출력:
  - 이미지 1장(contentUrl). UI에 thumbnail로 표시
- 제약/정책:
  - 씬당 MASTER 최대 3개
  - `Active Master`는 씬당 1개(후속 GRID/SHOT/VIDEO 기본 상속/기준)
  - Active가 아닌 MASTER 브랜치는 접기(폴딩) 가능(캔버스 복잡도 관리)
- 생성 흐름(UX):
  1) (기본값 자동 주입된) 폼 확인
  2) “프롬프트 생성” → promptKo 생성(GENERATED)
  3) 사용자 수정 → “승인”(APPROVED)
  4) “이미지 생성” → Job 생성 → 완료 시 이미지 URL 저장

### 3.4 GRID 노드(스토리보드 그리드 이미지: 1장 안에 여러 칸)
- 목적: “한 장의 이미지 안에서 여러 컷(패널)”을 만들어 **샷 후보를 빠르게 탐색**하거나, **스토리 비트**를 시각화한다.
- 부모/자식 관계:
  - 부모: MASTER
  - 자식: SHOT(추출)
- 입력/출력 공통:
  - 출력은 “그리드 이미지 1장”이며, 이 이미지에서 셀 인덱스를 기준으로 SHOT을 만든다.
- 모드 2개(필수 분리):
  - `gridMode=SHOT_VARIATIONS`
    - 용도: 다양한 카메라 프레이밍/샷 타입을 한 장에 생성 → 특정 칸을 선택해 SHOT 추출
    - 입력: `layout`, `shotTypes[]`, `compositionHintKo`
    - 템플릿 핵심: “ONE storyboard grid image”, “each panel is different framing”, “no scene cuts(패널 분할일 뿐)”
  - `gridMode=STORY_BEATS`
    - 용도: 칸별로 “연속된 사건/비트”를 요약한 스토리보드 1장을 생성(예: 4/6/8초 단위 구성 보조)
    - 입력: `layout`, `beatsKo[]`(칸 수=layout), `continuityRulesKo`(선택)
    - 템플릿 핵심: “panels 1..N represent beats in order”, “keep continuity across panels”
- 주의(상속 방해 가능성):
  - GRID는 “한 장에 여러 칸”이므로, MASTER 상속(룩)은 유지하되 디테일/모션 같은 요소를 과하게 주입하면 노이즈가 생길 수 있다.
  - v0에서는 GRID가 “룩 상속 + 칸별 지시(shotTypes 또는 beats) 중심”이 되도록 템플릿을 단순하게 유지한다.

### 3.5 SHOT 노드(그리드에서 특정 칸 추출한 고품질 단일 이미지)
- 목적: GRID의 특정 셀(인덱스)을 기반으로, **단일 컷을 고해상도/고품질로 재생성**한다.
- 부모/자식 관계:
  - 부모: GRID
  - 자식: VIDEO(클립)
- 입력(사용자):
  - 필수: `gridCellIndex`
  - 추천: `shotType`, `expressionKey`, `detailKo`
  - 상속(자동): `styleKey/timeOfDayKey/moodKey/objectIds/aspectRatio`는 Active MASTER에서 가져온다.
- 출력:
  - 이미지 1장(contentUrl)
- UX 포인트:
  - 그리드 이미지에서 셀 클릭 → `gridCellIndex` 자동 선택(권장)

### 3.6 VIDEO 노드(Image-to-Video 클립)
- 목적: SHOT 이미지를 시작 프레임으로 사용해 **4/6/8초 영상 클립**을 생성한다.
- 부모/자식 관계:
  - 부모: SHOT
  - 자식: 없음 (완성 클립)
- 입력(사용자):
  - 필수: `duration(4|6|8)`, `cameraMotionKey`, `motionDescriptionKo`(선택), `provider(기본 VEO_3_1)`
  - 트랜지션(P1): `endShotNodeId`를 추가로 지정(끝 프레임 힌트)
- 출력:
  - 비디오 1개(contentUrl) + (선택) thumbnail
- “장면 전환” 정책(중요):
  - VIDEO 클립 내부는 **단일 연속 샷**이어야 한다(“no scene cuts / no time jumps”를 promptEn에 자동 포함).
  - 장면 전환은 “클립과 클립 사이(타임라인/병합)”에서 일어나도록 설계한다.
- “이어붙이기(편집)”를 위한 권장 규칙:
  - 클립 말미에 **짧은 정지(hold/pause)** 를 넣으면 다음 클립으로 컷이 부드럽다.
  - 구현은 promptEn에 아래 같은 한 줄을 기본 포함하는 방식이 가장 간단하다.
    - 예: `Hold the final pose for the last half-second.`
- 확정(Confirm) 개념:
  - VIDEO가 SUCCEEDED 된 후 “타임라인에 확정” 가능
  - 규칙: “SHOT 아래 VIDEO 확정은 최대 1개” (다른 VIDEO를 확정하면 기존 확정 자동 해제)

---

## 4. 모델 / 제약(반드시 반영)

### 4.1 Video: Veo 3.1
- prompt language: **English**
- duration: **4/6/8 seconds**
- aspect ratio: **16:9 또는 9:16**
- (선택) 첫/마지막 프레임 기반 생성 기능 지원(Preview 범위 포함)

참고:  
https://cloud.google.com/vertex-ai/generative-ai/docs/models/veo/3-1-generate

### 4.2 Image: Gemini 2.5 Flash Image
- 모델 ID: `gemini-2.5-flash-image`
- 지원 aspect ratios(예: 16:9 등)

참고:  
https://cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-flash-image

### 4.3 Veo Prompt Rewriter(재현성 주의)
- Veo 3/3.1은 프롬프트 리라이터(prompt rewriter)를 끌 수 없다.
  - 즉, 저장된 `promptEn`이 “모델이 실제로 내부에서 사용한 최종 프롬프트”와 1:1로 일치하지 않을 수 있다.
  - 참고: https://cloud.google.com/vertex-ai/generative-ai/docs/video/turn-the-prompt-rewriter-off
- 권장 대응:
  - (가능한 경우) Job 결과/로그에서 rewritten prompt가 제공되면 함께 저장한다. 예: `settings.promptEnRewritten`
  - VIDEO의 promptEn은 **장문 서술**보다 **핵심 지시 + 금지 규칙** 중심으로 짧게(리라이터가 확장할 여지)

---

## 5. 데이터 저장 규칙 (DB / 응답)

### 5.1 Node 테이블(현 구조 유지)
- `prompt` 컬럼: **promptKo** 저장(사용자 편집/검토용)
- `data_json`(settings JSON): 노드 타입별 설정 + **promptEn** 저장
  - 예: `data_json.promptEn`, `data_json.styleKey`, `data_json.timeOfDayKey` 등

> 이유: DB 마이그레이션 없이 promptKo/promptEn을 분리하고, `GET /api/nodes/{id}`로 복원 가능.

### 5.2 `GET /api/scenes/{sceneId}/nodes`는 “요약” 유지
- 목록 응답은 트리/캔버스 구성(타입/부모/상태/position/contentUrl 등)에 집중한다.
- prompt/settings는 **선택 시 하이드레이트**로 채운다.

### 5.3 `GET /api/nodes/{id}`는 “상세” 단일 소스
- `prompt`: promptKo
- `settings`: settings JSON (promptEn 포함)

---

## 6. API 계약(Back-end 기준으로 정리)

### 6.1 노드 생성
`POST /api/scenes/{sceneId}/nodes`
```json
{
  "nodeType": "MASTER|GRID|SHOT|VIDEO",
  "parentNodeId": 123,
  "prompt": "한국어 promptKo (선택)",
  "settings": { "..." : "..." }
}
```

### 6.2 노드 수정
`PUT /api/nodes/{id}`
```json
{
  "prompt": "한국어 promptKo (선택)",
  "settings": { "..." : "..." }
}
```

### 6.3 결과 생성(비동기 Job)
`POST /api/nodes/{id}/generate`
```json
{
  "prompt": "한국어 promptKo (필수: 비어있으면 400)",
  "nodeType": "MASTER|GRID|SHOT|VIDEO (선택)",
  "settings": { "..." : "..." },
  "idempotencyKey": "optional",
  "requeueIfExisting": false,
  "force": false
}
```

**서버 동작(핵심)**: 요청으로 들어온 `prompt(=promptKo)`와 settings를 이용해 `promptEn`을 만들고, Job request JSON의 `prompt`에는 promptEn을 넣는다.

### 6.4 Contract Freeze: 프론트가 의존하는 “상세 조회” 필드
노드 선택 시 하이드레이트를 위해 `GET /api/nodes/{id}`에서 아래 필드가 안정적으로 내려와야 한다.

- 공통: `prompt`(promptKo), `settings.promptEn`(있으면), `settings.aspectRatio`
- MASTER: `settings.styleKey`, `settings.timeOfDayKey`, `settings.moodKey`, `settings.objectIds`
- GRID: `settings.gridMode`, `settings.layout`, `settings.shotTypes[]` 또는 `settings.beatsKo[]`, `settings.compositionHintKo?`
- SHOT: `settings.gridCellIndex`, `settings.shotType`, `settings.expressionKey`, `settings.detailKo?`
- VIDEO: `settings.duration`, `settings.cameraMotionKey`, `settings.motionDescriptionKo?`, `settings.startShotNodeId`, `settings.endShotNodeId?`, `settings.provider`

---

## 7. settings 스키마(v0 제안)

> 원칙: “사용자가 이해하기 쉬운 입력(키/프리셋)”을 저장하고, 서버에서 “모델 친화적인 값(promptEn, 영어 토큰)”으로 변환한다.

### 7.1 공통
- `aspectRatio`: **MVP는 16:9 또는 9:16만 허용** (Veo 제약과 충돌 방지)
- `promptEn`: 서버가 생성한 영어 프롬프트(캐시)
- `promptEnRewritten`: (선택) Veo 등에서 리라이터가 적용된 “추정 최종 프롬프트”(제공되는 경우에만 저장)

### 7.2 MASTER(settings)
- `styleKey`: 예) `PHOTO_REAL`
- `timeOfDayKey`: 예) `DAY`
- `moodKey`: 예) `NEUTRAL`
- `objectIds`: 예) `["obj-1","obj-2"]` (향후 오브젝트 시트 연동)

### 7.3 GRID(settings)
- `gridMode`: `SHOT_VARIATIONS|STORY_BEATS`
- `layout`: `2x2|2x3|3x3|1x4|...` (모드별 허용 레이아웃 지정 가능)

`SHOT_VARIATIONS` 추가:
- `shotTypes`: 예) `["WIDE","MEDIUM","CLOSE_UP","OTS"]`
- `compositionHintKo`: 한국어 구도 힌트(자유 입력)

`STORY_BEATS` 추가:
- `beatsKo`: 예) `["0~4s: ...", "4~8s: ..."]` (칸 개수=layout)
- `continuityRulesKo`: 예) `"인물/의상/조명 유지"` (선택)

### 7.4 SHOT(settings)
- `gridCellIndex`: number
- `shotType`: 예) `"CLOSE_UP"` (또는 `shotTypes` 중 선택)
- `expressionKey`: 예) `NEUTRAL|SMILE|SAD|ANGRY`
- `detailKo`: 한국어 디테일(자유 입력)

### 7.5 VIDEO(settings)
- `startShotNodeId`: number (필수)
- `endShotNodeId`: number|null (선택, 트랜지션)
- `duration`: 4|6|8
- `cameraMotionKey`: 예) `STATIC|SLOW_ZOOM_IN|ZOOM_OUT|PAN_LR|TILT_UP`
- `motionDescriptionKo`: 한국어 모션 설명(자유 입력)
- `provider`: 기본 `"VEO_3_1"`

### 7.6 프리셋 키(Back-end 단일 소스) — 최소 enum 제안
> 프론트는 “라벨(한글)”을 표시하되, 저장/전송은 아래 key를 사용한다.

#### 7.6.1 MASTER 프리셋
- `styleKey`: `PHOTO_REAL|ANIME_2D|STYLIZED_3D|WATERCOLOR_ILLUSTRATION|OIL_PAINT_ILLUSTRATION`
- `timeOfDayKey`: `DAWN|DAY|DUSK|NIGHT`
- `moodKey`: `NEUTRAL|COZY|LONELY|TENSE|HOPEFUL|DARK`

##### 7.6.1-1 프리셋 키 → prompt fragment 매핑(Back-end 단일 소스)
> 실제 영문 fragment는 `PresetFragments` enum 기준으로 렌더링된다.

**Style fragments**
| styleKey | fragment |
| --- | --- |
| PHOTO_REAL | photo-realistic |
| ANIME_2D | 2D anime illustration, clean line art |
| STYLIZED_3D | stylized 3D animated feature film look |
| WATERCOLOR_ILLUSTRATION | watercolor illustration, soft washes, subtle paper texture |
| OIL_PAINT_ILLUSTRATION | oil paint illustration, textured brush strokes |

**Time of day fragments**
| timeOfDayKey | fragment |
| --- | --- |
| DAWN | dawn |
| DAY | daytime |
| DUSK | golden hour, sunset |
| NIGHT | night |

**Mood fragments**
| moodKey | fragment |
| --- | --- |
| NEUTRAL | natural color grade, balanced lighting, moderate contrast |
| COZY | warm color grade, soft diffused lighting, gentle contrast |
| LONELY | cooler tones, slightly desaturated, more negative space, calm atmosphere |
| TENSE | low-key lighting, higher contrast, cooler grade, subtle shadow emphasis |
| HOPEFUL | bright high-key lighting, vibrant but natural colors, soft highlights |
| DARK | desaturated cool palette, soft low contrast, overcast or dim ambience |

##### 7.6.1-2 레거시 호환(설정 키/라벨)
> 서버는 **레거시 키/라벨**을 `styleKey/timeOfDayKey/moodKey`로 매핑한다.  
> **지원 종료일: 2026-02-28** (이후 제거 예정).

**Style 레거시 → styleKey**
- `CINEMATIC_REAL` → `PHOTO_REAL`
- `ANIME` → `ANIME_2D`
- `PIXAR` → `STYLIZED_3D`
- `NOIR`, `DOCUMENTARY` → `PHOTO_REAL`
- 한글 라벨: `실사` → `PHOTO_REAL`, `애니메이션`/`애니` → `ANIME_2D`, `픽사` → `STYLIZED_3D`, `수채화` → `WATERCOLOR_ILLUSTRATION`, `유화` → `OIL_PAINT_ILLUSTRATION`

**Time 레거시 → timeOfDayKey**
- `MORNING` → `DAWN`, `EVENING` → `DUSK`
- 한글 라벨: `아침` → `DAWN`, `낮` → `DAY`, `저녁` → `DUSK`, `밤` → `NIGHT`

**Mood 레거시 → moodKey**
- 한글 라벨: `편안` → `COZY`, `고독` → `LONELY`, `긴장` → `TENSE`, `행복` → `HOPEFUL`, `우울` → `DARK`

#### 7.6.2 SHOT 프리셋
- `expressionKey`: `NEUTRAL|SMILE|SAD|SURPRISED|ANGRY|BLANK`
- `shotType`: `WIDE|MEDIUM|CLOSE_UP|EXTREME_CLOSE_UP|OTS|POV|HIGH_ANGLE|LOW_ANGLE`

#### 7.6.3 VIDEO 프리셋
- `cameraMotionKey`: `STATIC|SLOW_ZOOM_IN|ZOOM_OUT|PAN_LR|TILT_UP`

---

## 8. 기본값(자동 주입) — “필수 강제” 대신

### 8.1 글로벌 기본값(v0)
- `aspectRatio`: `"16:9"` (MVP 고정 권장. 필요하면 9:16을 “프로젝트/씬 전역 토글”로 제공)
- MASTER: `styleKey=PHOTO_REAL`, `timeOfDayKey=DAY`, `moodKey=NEUTRAL`
- GRID(SHOT_VARIATIONS): `layout=2x2`, `shotTypes=["WIDE","MEDIUM","CLOSE_UP","OTS"]`
- GRID(STORY_BEATS): `layout=1x4`, `beatsKo`는 4개 기본 템플릿 자동 채움(아래 “promptKo 기본값” 참고)
- SHOT: `gridCellIndex=0`, `shotType=WIDE`, `expressionKey=NEUTRAL`
- VIDEO: `duration=4`, `cameraMotionKey=STATIC`, `provider=VEO_3_1`

#### 8.1.1 GRID(SHOT_VARIATIONS) 패널 수 정합성 규칙(중요)
- `layout`의 패널 수(=rows×cols)와 `shotTypes.length`는 **가능하면 1:1로 맞춘다**.
  - 예: `2x2`(4패널) ↔ shotTypes 4개
  - `2x3`(6패널)을 쓰려면 shotTypes도 6개로 늘리는 것이 안정적
- 서버(또는 프론트)는 방어적으로 다음 중 하나를 수행한다(팀 합의 필요):
  - (권장) **자동 보정**: 부족하면 기본 shotType을 채우고, 많으면 앞에서부터 잘라 패널 수와 맞춘다
  - (엄격) **검증 실패(400)**: 길이가 다르면 생성 요청을 거부하고 UI에서 수정하도록 안내

### 8.2 상속 규칙(우선순위)
1) 해당 노드 settings에 값이 있으면 사용
2) 없으면 Active MASTER의 settings에서 상속(룩 관련: style/timeOfDay/mood/objectIds/aspectRatio)
3) 그래도 없으면 글로벌 기본값 사용

> GRID/STORY_BEATS의 “비트 텍스트(beatsKo)”처럼 모드 고유 입력은 상속 대상이 아니다.

---

## 9. 프롬프트 생성 전략(v0)

### 9.1 전체 흐름
1) 사용자는 UI에서 한국어 입력을 조정한다(프리셋 + 자유 텍스트).
2) UI는 노드 생성 시점에 `promptKo`를 **기본값으로 미리 채워둔다**. (빈 프롬프트로 인한 생성 실패 방지)
3) (선택) “프롬프트 생성” 버튼으로 **promptKo를 생성/보정**한다(LLM 사용 가능).
4) 사용자가 promptKo를 수정하고 “승인”한다.
5) “이미지/영상 생성”을 누르면 서버가 **promptKo + settings → promptEn** 으로 렌더링하고 Job을 큐에 넣는다.

### 9.2 promptEn 렌더링(서버 책임)
- `promptEn`은 “모델 입력”이므로, **한글 입력(자유 텍스트)도 포함되면 안 됨**(특히 Veo).
- 서버는 다음을 수행한다:
  - (A) 프리셋 키(`styleKey`, `moodKey`, `cameraMotionKey` 등)를 영문 문구로 매핑
  - (B) 자유 텍스트(`compositionHintKo`, `detailKo`, `motionDescriptionKo`, `beatsKo`)는 영문으로 번역/요약
  - (C) 노드 타입별 템플릿에 합성
  - (D) 결과가 빈 문자열이면 400

> 번역/요약은 Vertex Gemini 텍스트 모델을 사용해도 되고(권장), v0에서는 단순 “번역 프롬프트”로 충분하다.

#### 9.2.1 promptEn 정책(현행)
- **Brand-free 정책**: 현재 PromptRenderer는 브랜드명 제거/치환을 강제하지 않는다. (필요 시 별도 필터/지침 추가)
- **Tone(톤) 반영**: `styleKey/timeOfDayKey/moodKey` → fragment 매핑으로 톤을 반영한다(7.6.1-1 참고).
- **공통 금지 규칙**: `No text / no subtitles / no watermark / no logo` 문구를 항상 포함한다.

### 9.3 노드 타입별 promptEn 템플릿(권장 v0)

#### 합성 규칙(권장)
- `sceneTitle/sceneDescription` → 영어 1~2문장 요약(`sceneDescriptionEn`)
- `promptKo` → 영어 1문장 요약(`promptKoEn`, “what to show” 중심)
- 프리셋 키는 짧은 fragment로 변환(Style/Time/Mood/Camera)
- 공통 금지 규칙을 항상 포함: `No text / no watermark / no logo`

#### MASTER (Image)
- 목표: 씬의 기준 “룩”을 고정하는 1장 와이드샷(단일 스틸 프레임)
```
Scene: {sceneTitle}. {sceneDescriptionEn}.
Content: {promptKoEn}.
Wide establishing shot, single still frame.
Style: {styleFragment}. Time: {timeOfDayFragment}. Mood: {moodFragment}.
Keep character identity, outfits, lighting, and key props consistent.
No text, no subtitles, no watermark, no logo.
Aspect ratio: {aspectRatio}.
```

#### GRID — SHOT_VARIATIONS (Image, single grid image)
- 목표: 같은 순간/같은 장소를 유지한 채 “프레이밍만” 바꾼 패널들(시간 진행 아님)
```
Create ONE storyboard grid image with {layout} panels, all showing the SAME moment in the SAME scene (not sequential).
Base content: {promptKoEn}.
Panels must differ only by camera framing:
Panel 1: {shotType1En}
Panel 2: {shotType2En}
Panel 3: {shotType3En}
Panel 4: {shotType4En}
All panels share consistent characters, outfits, lighting, and location.
Style: {styleFragment}. Time: {timeOfDayFragment}. Mood: {moodFragment}.
Composition note: {compositionHintEn}.
No captions, no text, no watermark, no logo. Aspect ratio: {aspectRatio}.
```

#### GRID — STORY_BEATS (Image, single grid image)
```
Create ONE storyboard grid image with {layout} panels that depict a short sequence.
Base content: {promptKoEn}.
Panels 1..N are beats in order:
1) {beat1En}
2) {beat2En}
3) {beat3En}
4) {beat4En}
Keep the same characters, outfits, lighting, and location across panels.
Style: {styleFragment}. Time: {timeOfDayFragment}. Mood: {moodFragment}.
Continuity rules: {continuityRulesEn}.
No captions, no text, no watermark, no logo. Aspect ratio: {aspectRatio}.
```

#### SHOT (Image)
```
High-quality single cinematic frame based on storyboard cell #{gridCellIndex}.
Content: {promptKoEn}.
Camera framing: {shotTypeEn}. Facial expression: {expressionEn}.
Extra detail: {detailEn}.
Match the master look and character identity. No text, no watermark, no logo.
Aspect ratio: {aspectRatio}.
```

#### VIDEO (Veo 3.1, 4/6/8s)
- 핵심: “클립 내부는 단일 연속 샷”, 장면 전환은 **클립 경계에서만** (컷/점프 금지)
```
Generate a {duration}-second single continuous shot video from the provided start image. No cuts, no time jumps.
Action plan: {actionPlanEn}.
Camera motion: {cameraMotionEn}. {motionDescriptionEn}
Keep character identity, outfits, lighting, and location consistent. No text, no watermark, no logo.
Style: {styleFragment}. Time: {timeOfDayFragment}. Mood: {moodFragment}.
Hold the final pose for the last half-second.
```
트랜지션(`endShotNodeId`)이 있을 때만 한 줄 추가:
```
End should gently approach the end shot composition (no hard cut).
```

#### VIDEO: duration별 ActionPlanEn(권장 규칙)
> 목표: “길이가 길수록 일을 많이 시키는” 방향이 아니라, **자연스러운 호흡 + 컷 가능한 정지 구간**을 만드는 것.

- 4초: “짧은 비트”
  - 권장: 1개의 행동 + (있으면) 1개의 짧은 반응
  - 예: `He turns his head and gives a small nod.`
- 6초: “짧은 상호작용”
  - 권장: 1회 상호작용(행동 1~2비트)
  - 예: `They exchange a brief glance, then one person gestures subtly and the other reacts.`
- 8초: “미니 장면”
  - 권장: 2~3개의 작은 비트(행동/표정 변화). 과도한 사건 전개/전환은 금지.
  - 예: `She picks up the cup, looks toward the exit, then pauses on a neutral pose.`

#### VIDEO: 대화(선택) 표현 가이드(정책은 ‘소리 신경 X’)
> v0에서는 오디오/대사 품질을 목표로 하지 않는다. 다만 테스트용으로 대사를 포함하고 싶다면 아래처럼 “텍스트 렌더링” 위험을 줄인다.

- 따옴표(`"`)는 가급적 피한다(자막/텍스트로 렌더링될 위험 감소 목적).
- 형식은 아래처럼 짧게:
  - `The woman says: We should go. The man reacts with a small nod.`

### 9.4 promptKo 기본값(자동 채움) — 빈 프롬프트 방지용

> 목표: 사용자가 “프롬프트 생성” 버튼을 누르지 않아도, 기본 promptKo가 이미 채워져 있어 생성이 막히지 않게 한다.

- MASTER 기본 promptKo(예시):
  - “(장소/인물/상황) 씬의 대표 와이드샷. 인물과 소품을 명확히 보여주고 영화적인 조명/색감으로.”
- GRID(SHOT_VARIATIONS) 기본 promptKo(예시):
  - “같은 장면을 4칸 그리드로, 카메라 프레이밍만 다르게 보여줘. 인물/의상/배경/조명은 일관 유지.”
- GRID(STORY_BEATS) 기본 promptKo(예시):
  - “이 씬의 사건을 4단계 비트로 나눠 스토리보드로 보여줘. 연속성(인물/의상/조명/배경) 유지.”
- SHOT 기본 promptKo(예시):
  - “선택한 그리드 컷을 고품질 단일 이미지로 재생성. 인물 디테일/표정이 잘 보이게.”
- VIDEO 기본 promptKo(예시):
  - “선택한 샷에서 시작하는 단일 연속 숏 영상. 장면 전환 없이 자연스러운 움직임. 마지막에 잠깐 정지.”

---

---

## 10. 프론트엔드 구현 가이드 (Vue 3 + Pinia + Vue Flow)

### 10.1 “노드 선택 시 하이드레이트” 구현
- 트리 로드: `GET /api/scenes/{sceneId}/nodes` → Store에 UI 노드 생성
- 선택 시:
  1) Store에 `hydratedNodeIds: Set<string>`(또는 Map 캐시) 유지
  2) 선택된 노드가 `hydratedNodeIds`에 없고, 노드 타입이 `SCENE_HEADER`가 아니면
  3) `GET /api/nodes/{id}` 호출
  4) 응답의 `prompt/settings`를 Store의 노드 data에 병합
  5) `hydratedNodeIds`에 추가

> 목표: 새로고침해도 “패널 폼이 비어 보이는” 문제를 줄인다.

#### 10.1.1 하이드레이트 병합 규칙(필수)
- 요약(list)에서 이미 존재하는 값과 상세(detail) 값이 충돌할 때:
  - `status/contentUrl/position`은 요약을 우선(실시간성이 더 높을 수 있음)
  - `prompt/settings`는 상세를 우선(요약에는 없거나 기본값일 가능성 높음)
- 하이드레이트 이후, 패널 폼은 store 값과 1:1로 동기화되어야 한다.

### 10.2 UI 입력값은 “키(프리셋)”로 저장
- 예: mood는 UI 라벨(한글) 대신 `moodKey="NEUTRAL"`로 저장
- 장점: 서버에서 promptEn 렌더링이 쉬워지고, 문구 변경(A/B)이 가능

### 10.3 promptKo 흐름(유지)
- 패널의 텍스트 영역은 `promptKo`만 편집
- 승인 버튼은 `promptStatus=APPROVED`만 업데이트
- 생성 버튼은 `POST /api/nodes/{id}/generate`에 promptKo + settings를 전달

> 프론트는 promptEn을 직접 만들지 않는다(서버 단일 소스).

### 10.4 병렬 작업을 위한 프론트 체크리스트(완료 기준)
- [ ] 노드 선택 시 `GET /api/nodes/{id}` 하이드레이트 + 캐시
- [ ] 각 패널 폼 초기값이 “항상 채워짐”(promptKo 포함) → 빈 프롬프트로 generate 실패 방지
- [ ] VIDEO duration 옵션은 4/6/8만 노출
- [ ] aspectRatio는 16:9(기본) / 9:16(옵션)만 노출
- [ ] GRID(SHOT_VARIATIONS)는 기본 2x2 + 4 shotTypes 템플릿 제공

---

## 11. 백엔드 구현 가이드 (Spring Boot)

### 11.1 핵심: generate 시점에 promptEn 생성/저장
`NodeService.generateNode()`에서 다음을 수행한다.
1) 입력 `prompt`(promptKo)가 비었으면 400
2) defaults/상속을 적용하여 settings를 “완성”
3) `promptEn = PromptRenderer.render(nodeType, sceneInfo, promptKo, settings, start/end shot info)`
4) `data_json.promptEn = promptEn` 업데이트(캐시)
5) Job requestJson의 `prompt`에는 promptEn을 넣고 enqueue

#### 11.1.1 Job requestJson 포맷(권장)
> worker는 `job.request_json`을 파싱해 `prompt`와 `settings`만 사용한다.
```json
{
  "prompt": "영문 promptEn (필수)",
  "settings": {
    "aspectRatio": "16:9",
    "provider": "VEO_3_1",
    "duration": 4,
    "cameraMotionKey": "STATIC"
  }
}
```

### 11.2 PromptRenderer(신규 컴포넌트) 제안
- 책임:
  - 프리셋 키 → 영문 fragment 변환
  - 자유 텍스트(Ko) → En 번역/요약(LLM 1회)
  - 노드 타입 템플릿 합성
- 결과:
  - 빈 문자열이면 예외(400)

#### 11.2.1 VIDEO 전용: actionPlanEn 자동 생성(권장)
- 입력(가용한 것만 사용):
  - `promptKo`(한국어) + `motionDescriptionKo`(한국어) + start/end shot 정보(있으면)
  - `duration`
- 출력:
  - `actionPlanEn`(영문 1~2문장)
- 규칙:
  - 4초: 1비트 중심(짧게)
  - 6초: 1회 상호작용/행동 1~2비트
  - 8초: 2~3비트(하지만 “장면 전환/시간 점프”는 금지)
  - 항상 마지막은 “hold/pause”로 닫히도록 유도(템플릿에도 고정 문장 포함)

### 11.3 프리셋/기본값은 백엔드 단일 소스
- 예: `AiDefaultsProperties`(yml 기반) 또는 enum+매핑 테이블로 관리
- 프론트는 키만 저장/전송한다.

### 11.4 병렬 작업을 위한 백엔드 체크리스트(완료 기준)
- [ ] `POST /api/nodes/{id}/generate`에서 Job payload의 `prompt`가 promptEn으로 들어감
- [ ] `settings.promptEn`을 DB에 저장(캐시)하고, `GET /api/nodes/{id}`로 반환
- [ ] VIDEO: duration 검증(4/6/8), aspectRatio 검증(16:9/9:16)
- [ ] GRID: `layout`×`shotTypes.length` 자동 보정 또는 400 처리(DECISION 반영)
- [ ] VIDEO: `actionPlanEn` 생성 로직(최소 규칙 기반) 추가

---

## 12. 문서 정합성 작업(필수)

1) `docs/APIdocs.md`
   - 노드 생성 request 필드명을 백엔드 구현(`nodeType`)과 일치
   - settings에서 이미지 비율은 `ratio` 대신 `aspectRatio`로 표준화(서버 구현과 일치)
   - VIDEO duration은 4/6/8로 명시(모델 제약과 일치)
2) `docs/PRD_AI_Movie_Studio_v2.5.md`
   - “UI는 한글, 생성은 영어(promptEn)” 정책을 명시
   - GRID 모드 2개(SHOT_VARIATIONS / STORY_BEATS) 정의 및 단계(W3/W4) 명시

---

## 13. 단계별 구현 순서(MVP 추천)

### Phase 1 (필수: ‘결과만 보임’ 체감 해결)
1) 프론트: 노드 선택 시 `GET /api/nodes/{id}` 하이드레이트 + 캐시
2) 문서: APIdocs 필드명/키 정합성 수정(nodeType/aspectRatio 등)

### Phase 2 (필수: 영어 생성 프롬프트)
3) 백엔드: generate 시점 promptEn 렌더링 + data_json에 저장
4) 백엔드: VIDEO는 반드시 English promptEn 사용(빈 문자열 방지)

### Phase 3 (기능 확장)
5) GRID 모드 분리 + UI 토글 + 템플릿 추가
6) STORY_BEATS는 P1로 분리 가능(합의에 따라)

---

## 14. 오픈 이슈(합의 필요)

1) GRID 모드 2개를 W3에 다 넣을지, SHOT_VARIATIONS만 먼저 할지
2) promptKo 생성/개선에서 LLM 사용 범위(완전 자동 vs 템플릿 기반)
3) 오브젝트 시트(objectIds) 반영 방식(프롬프트에 “참조 이미지”까지 포함할지)

---

## 15. 병렬 작업 분할(권장 워크스트림)

### 15.1 Front Stream (UI/Store)
- 목표: “패널 폼 복원(하이드레이트) + 디폴트 주입 + UI 입력=프리셋 키 저장” 완성
- 주요 작업:
  - 노드 선택 시 `GET /api/nodes/{id}` 하이드레이트 + 캐시
  - 패널 폼 초기값(promptKo 포함) 자동 채움
  - duration/aspectRatio 제한 UI 반영(4/6/8, 16:9/9:16)
  - GRID(SHOT_VARIATIONS) 기본 2x2 + 4 shotTypes 제공
- 주요 파일 후보:
  - `itda-frontend/src/stores/sceneNode/index.ts`
  - `itda-frontend/src/components/scene-editor/panels/*.vue`

### 15.2 Backend Stream (PromptRenderer + generate)
- 목표: “generate 시 promptEn 렌더링 + Job에는 promptEn만 + settings에 캐시” 완성
- 주요 작업:
  - `NodeService.generateNode()`에서 promptEn 생성/저장 후 Job enqueue
  - PromptRenderer + VIDEO actionPlanEn 로직 구현
  - duration/aspectRatio/GRID 정합성 검증(DECISION 반영)
- 주요 파일 후보:
  - `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`
  - 신규: `itda-backend/src/main/java/com/itda/backend/ai/prompt/*`

### 15.3 Docs Stream (PRD/APIdocs 정합성)
- 목표: 구현과 문서가 1:1로 맞아 “문서만 보고 구현” 가능하게 유지
- 주요 작업:
  - `docs/APIdocs.md`: `nodeType`, `aspectRatio`, duration 4/6/8, GRID 모드/기본값 반영
  - `docs/PRD_AI_Movie_Studio_v2.5.md`: 한글 UI/영문 생성 정책, VIDEO 컷 정책, GRID 모드 정의/범위(P0/P1)
