# AI Movie Studio — 이미지 기반 Node 워크플로우 구현 계획서 (v1)
> 목적: 이 문서만 보고도(프론트/백엔드/워커/QA가 병렬로) “마스터 → 그리드 → 샷 → 영상” 워크플로우를 **실제 이미지 입력 기반**으로 구현할 수 있도록, 현 상태/갭/결정사항/계약(API·Job Payload)/작업 항목/검증 기준을 한 번에 정리한다.

---

## 0. 문서 범위 / 전제

### 0.1 목표
1) **체감 일관성(시연 필수)**: 상위 노드 결과 이미지가 하위 노드 생성의 입력으로 사용되는 “이미지 기반 체인”을 만든다.
2) **병렬 개발 가능**: FE/BE/Worker/QA가 각자 진행해도 다시 맞추기 쉽도록 Contract를 고정한다.
3) **MVP 현실성**: 외부 업로드/외부 URL은 P0에서 제외하고, 현재 노드 결과(LOCAL/S3 저장)만 입력 이미지 소스로 사용한다.

### 0.2 현재 코드 기준(핵심 요약)
- 프롬프트 자동 생성(promptKo) 기능은 이미 존재: `/api/ai/prompts/generate` (사용자 수정/승인 후 생성).
- 노드 생성(Job)은 현재 `promptEn + settings`만 워커로 전달하며, **입력 이미지(first frame/reference image/last frame)가 전달되지 않음**.
- VIDEO는 `start_shot_node_id/end_shot_node_id`를 저장/검증하지만, 실제 Veo 요청에는 이미지가 포함되지 않음.
- 프론트는 노드 선택 시 `GET /api/nodes/{id}` 하이드레이트가 없어, 새로고침/재진입 시 폼이 비어 보이는 문제가 남아 있음.

### 0.3 참고 문서(요구사항/규칙)
- PRD: `docs/PRD_AI_Movie_Studio_v2.5.md`
  - P0: Image-to-Video(시작 샷 필수), P1: end shot(트랜지션) 선택
- Node Workflow & Prompt Spec(v0): `docs/node-workflow-prompt-spec-v0.md`
  - promptKo(한글 UI) / promptEn(영문 생성) 분리, 노드 선택 시 하이드레이트(상세 단일소스)

---

## 1. 용어(본 문서 기준)

- **promptKo**: 사용자 UI에서 편집/검토하는 한국어 문장(저장: `nodes.prompt`)
- **promptEn**: 모델 요청에 쓰는 영어/구조화 프롬프트(저장: `nodes.data_json.promptEn`)
- **입력 이미지(Input Image)**: 생성 요청 시 모델에 함께 전달되는 이미지(참조 이미지/first frame/last frame)
- **참조 이미지(referenceImage)**:
  - GRID: 부모 MASTER 결과 이미지(룩/캐릭터 일관성 유지)
  - SHOT: 부모 GRID 결과 이미지(특정 셀 기반 재생성)
- **firstFrame/lastFrame**:
  - VIDEO: 시작 샷(필수) / 끝 샷(P1, 선택)
- **contentKey**: 노드 결과가 저장된 경로 키(LOCAL/S3). 현재 `/api/nodes/{id}/content` 다운로드는 이 키를 사용해 로드한다.

---

## 2. P0/P1 범위 선언(Contract Freeze)

### 2.1 P0 (이번 범위)
1) MASTER 프롬프트 자동 생성(promptKo) 시 **씬 스토리 컨텍스트 포함**
2) 노드 선택 시 **하이드레이트(상세 1회 + 캐시)**
3) settings 키 정합(프론트↔백엔드) 최소한으로 맞춤
4) **이미지 기반 입력 체인**
   - MASTER → GRID: GRID 생성 시 부모 MASTER 결과 이미지를 reference로 사용
   - GRID → SHOT: SHOT 생성 시 부모 GRID 결과 이미지를 reference로 사용(+ gridCellIndex)
   - SHOT → VIDEO: VIDEO 생성 시 시작 샷 이미지를 firstFrame으로 사용(필수)
5) 스토리지: **LOCAL/S3 둘 다 지원** (S3 우선, 없으면 로컬)

### 2.2 P1 (다음 범위)
1) SHOT 2개 기반 트랜지션 VIDEO: end shot 이미지(lastFrame) 지원
2) GRID(STORY_BEATS) 모드(스펙 v0의 P1)
3) 그리드 셀 “직접 크롭” (PRD에서도 P1~P2 우선순위로 분리)

---

## 3. 결정해야 하는 사항(지금 문서에서 고정)

### 3.1 입력 이미지 전달 방식(원칙)
**DB/Job requestJson에 base64를 저장하지 않는다.**
- 대신 워커 실행 시점에 nodeId/parentId/startShotNodeId로 노드 결과를 로드하여 모델 요청에 포함한다.

### 3.2 이미지 소스(MVP)
P0에서는 **노드 결과 이미지(LOCAL/S3)만** 입력 이미지로 허용한다.
- 외부 업로드/외부 URL 입력은 제외(추후 P1).

### 3.3 실패 정책(UX + 안전)
프론트와 서버 모두 방어한다.
- **프론트**: 입력 이미지가 준비되지 않았으면 버튼 비활성화/안내(시연 안정성)
- **백엔드**: 그래도 요청이 오면 400/409로 명확한 에러 반환(안전장치)

권장 에러 케이스(서버):
- `VIDEO`: startShot 결과 이미지가 없으면 409(CONFLICT) 또는 400(INVALID_REQUEST)
- `SHOT`: parent GRID 결과 이미지가 없으면 409/400
- `GRID`: parent MASTER 결과 이미지가 없으면 409/400

### 3.5 상속/재생성 정책(Option A, P0 고정)
P0에서는 “설정값”이 아니라 **상위 노드의 생성 결과 이미지(SUCCEEDED)** 가 하위 노드 생성의 기준이다.
- 하위 노드(GRID/SHOT/VIDEO)는 **반드시 상위 결과 이미지가 존재**해야 생성 가능하다. (FE 버튼 비활성 + BE 409/400)
- MASTER의 style/time/tone 등 룩을 변경했더라도, **MASTER를 다시 생성하기 전에는** 하위 노드가 새 룩을 “따라가지 않는다”.
- 따라서 룩을 바꾸면 **MASTER 재생성 → (필요 시) 하위 노드 재생성** 흐름이 기본이다.

Acceptance Criteria:
- MASTER가 SUCCEEDED가 아니면 GRID 생성 버튼이 비활성이고, API도 실패한다.
- MASTER 룩만 변경(미생성)한 상태에서는 GRID/SHOT/VIDEO 생성 결과가 바뀌지 않는다(상위 결과 이미지가 동일하므로).

### 3.4 스토리지 로딩(LOCAL + S3)
워커는 사용자 인증 없이 내부 로딩이 가능해야 한다.
- `MediaFileService`는 userId 권한 체크가 있어 워커에서 직접 재사용하기 애매함.
- 워커 전용으로 **NodeContentLoader(내부 서비스)** 를 만들어 NodeMapper + 파일/S3 로더를 조합한다.

---

## 4. 계약(API/데이터/Job Payload)

### 4.1 노드 생성/수정/상세 조회(현재 유지)
- `GET /api/scenes/{sceneId}/nodes`: 요약(트리/상태/contentUrl/position 중심)
- `GET /api/nodes/{id}`: 상세(promptKo + settings JSON)
- `PUT /api/nodes/{id}`: prompt/settings 업데이트
- `POST /api/nodes/{id}/generate`: 생성(비동기 jobId)

### 4.2 프롬프트 자동 생성 API(현재 유지, 입력 강화)
- `POST /api/ai/prompts/generate`
  - P0에서 FE는 MASTER/GRID에도 `sceneOneLine`을 반드시 전달한다.
  - `sceneOneLine`은 “씬 제목/설명 + 노드 타입별 추가 정보(선택)”를 1~2문장으로 구성한다.

### 4.3 Job requestJson 스키마(확장)
현재: `{ "prompt": "<promptEn>", "settings": { ... } }`

P0 확장(권장):
```json
{
  "prompt": "promptEn",
  "settings": { "..." : "..." },
  "inputs": {
    "reference": { "mode": "PARENT_RESULT" },
    "firstFrame": { "mode": "START_SHOT_RESULT" }
  }
}
```

설명:
- P0에서는 **실제 식별자(nodeId/parentId/startShotNodeId)는 Job의 nodeId + DB 관계로 해석** 가능하므로,
  `inputs`는 “의도”만 표현하는 메타로 두고, 워커는 nodeId로 실데이터를 찾는다.
- P1에서는 `lastFrame`까지 사용:
```json
"inputs": {
  "firstFrame": { "mode": "START_SHOT_RESULT" },
  "lastFrame": { "mode": "END_SHOT_RESULT" }
}
```

> 이유: requestJson을 작게 유지하면서도, 워커가 “어떤 입력 이미지를 기대해야 하는지”를 명확히 할 수 있음.

---

## 5. 프론트엔드 구현 항목(Workstream A)

### A1. 노드 선택 시 하이드레이트 + 캐시
요구:
- 씬 진입 시에는 요약 노드만 로드(`GET /api/scenes/{sceneId}/nodes`)
- 노드 클릭(패널 오픈) 시 해당 노드만 상세 조회(`GET /api/nodes/{id}`)하여 store에 병합
- 동일 노드는 캐시하여 중복 호출 방지

Acceptance Criteria:
- 새로고침/재진입 후 노드 클릭 시, 패널 폼이 이전 값(prompt/settings)을 복원한다.

### A2. MASTER/GRID 프롬프트 생성 시 씬 컨텍스트(sceneOneLine) 포함
현 상태:
- SHOT/VIDEO는 `sceneOneLine`을 보내지만, MASTER/GRID는 누락 가능.

구현:
- MASTER 패널: 씬 헤더(title/description) + 선택값(style/time/mood/objectIds) 기반 `sceneOneLine` 구성
- GRID 패널: 씬 헤더 + layout/shotTypes/compositionHint 기반 `sceneOneLine` 구성

Acceptance Criteria:
- `/api/ai/prompts/generate` 요청 payload에 MASTER/GRID도 `sceneOneLine`이 포함된다.

### A3. settings 키 정합(최소)
현행 구현:
- 백엔드는 `styleKey/timeOfDayKey/moodKey`, `compositionHintKo`, `detailKo`, `cameraMotionKey`를 기준으로 처리한다.
- **레거시 호환**: `style/timeOfDay/mood` 라벨(한글 및 legacy 키)을 `GenerationSettingsResolver`가 매핑한다.  
  지원 종료일: **2026-02-28** (이후 제거 예정).

정합 목표(P0):
- **프론트는 “전송/저장용 키는 백엔드 key로 통일”**
  - MASTER: `styleKey`, `timeOfDayKey`, `moodKey`, `objectIds`
  - GRID: `gridMode=SHOT_VARIATIONS`, `layout`, `shotTypes`, `compositionHintKo`
  - SHOT: `gridCellIndex`, `shotType`, `expressionKey`, `detailKo`
  - VIDEO: `duration`, `cameraMotionKey`, `motionDescriptionKo`, `startShotNodeId`, `endShotNodeId`, `provider`
- UI 라벨(한글)↔key 매핑은 FE에서 관리(백엔드 PresetFragments enum과 정합)

Acceptance Criteria:
- 생성 요청(`/nodes/{id}/generate`) settings가 백엔드 `GenerationSettingsResolver`에 의해 기본값/검증/상속을 정상 통과한다.

### A4. 입력 이미지 준비 여부에 따른 UI 제어(시연 안정)
예시 규칙:
- GRID 생성 버튼: “부모 MASTER가 SUCCEEDED + 이미지 존재”일 때만 활성
- SHOT 생성 버튼: “부모 GRID가 SUCCEEDED + 이미지 존재”일 때만 활성
- VIDEO 생성 버튼: “startShot(SHOT) SUCCEEDED + 이미지 존재”일 때만 활성

Acceptance Criteria:
- 입력 이미지가 없으면 버튼이 disabled + 안내 문구 표시.
- (Option A) MASTER 룩 변경 후 MASTER가 재생성되기 전까지, 하위 노드 생성 버튼/결과는 “기존 상위 이미지 기준”으로 동작한다.

---

## 6. 백엔드/API 구현 항목(Workstream B)

### B1. 입력 이미지 존재 검증(Generate API)
NodeService.generateNode()에서 타입별로 사전 검증을 추가한다.

P0 규칙:
- GRID: parent MASTER의 contentUrl(또는 default contentKey)이 로드 가능해야 함
- SHOT: parent GRID의 contentUrl(또는 default contentKey)이 로드 가능해야 함
- VIDEO: startShotNodeId(SHOT)의 contentUrl(또는 default contentKey)이 로드 가능해야 함

응답:
- 실패 시 409/400 + “입력 이미지가 준비되지 않음” 메시지(프론트가 그대로 노출 가능)

### B2. 프롬프트 자동 생성 템플릿 보강(선택)
현재 AiPromptService는 “1~2문장 + 입력값 포함 강제” 정도로 단순함.

P0 권장 개선:
- nodeType별로 출력 가이드를 다르게(예: MASTER는 “와이드 establishing shot”, GRID는 “한 장의 storyboard grid, same moment” 등)
- 출력 제한: 너무 길어지지 않게(2~4문장) + 금지규칙(텍스트/자막/워터마크 금지) 포함

> 주의: 최종 promptEn은 PromptRenderer가 책임지므로, 여기서 과도하게 영문/구조화까지 할 필요는 없음.

---

## 7. 워커/AI Provider 구현 항목(Workstream C)

### C1. NodeContentLoader(내부 서비스) 추가
역할:
- `nodeId`를 받아 노드 결과 파일을 bytes로 로드한다.
- 로딩 우선순위: `contentUrl`이 절대 URL이면(현재는 remote 가능) **P0에서는 미지원**(실패 처리) 또는 다운로드(선택).
- 그렇지 않으면:
  1) S3 bucket에서 `contentKey`로 로드(HEAD + GET)
  2) 없으면 로컬 업로드 루트에서 파일 로드

입력:
- nodeId

출력:
- `bytes`, `contentType`, `contentKey`(로그용)

Acceptance Criteria:
- 같은 nodeId에 대해 `/api/nodes/{id}/content`가 정상 다운로드되는 환경이면, NodeContentLoader도 동일한 컨텐츠를 읽을 수 있다.

### C2. GRID/SHOT 이미지 생성: “참조 이미지 + 텍스트” 입력 지원
목표:
- GRID 생성 시: 부모 MASTER 결과 이미지를 참조로 전달
- SHOT 생성 시: 부모 GRID 결과 이미지를 참조로 전달(+ gridCellIndex는 promptEn에 이미 포함)

구현 선택지(결정 필요):
- (권장) Vertex AI 이미지 생성 모델이 “image + text” 입력을 지원하므로, GeminiImageClient를 멀티파트 입력으로 확장
- (대안) 이미지 입력이 SDK에서 난이도가 높다면, P0는 “promptEn + 상속룩”만으로 시연하고, 이미지 참조는 P1로 미룸(하지만 본 프로젝트 목표상 P0에서 이미지 기반이 필수라 권장하지 않음)

Acceptance Criteria:
- 동일 MASTER에서 GRID를 여러 번 생성할 때 캐릭터/룩/배경 일관성이 체감상 개선된다.

### C3. VIDEO 생성: firstFrame(필수) + lastFrame(P1) 지원
목표(P0):
- VIDEO 생성 요청 시 start shot 이미지가 Veo 요청에 포함되어야 한다.

구현:
1) VideoGenerationWorker가 startShotNodeId로 NodeContentLoader를 호출해 bytes를 얻는다.
2) VeoClient가 “firstFrame 이미지”를 포함한 요청을 보낸다.
3) P1에서 endShotNodeId도 동일하게 lastFrame으로 포함.

SDK/REST 선택:
- 현재 VeoClient는 google-genai SDK를 사용 중.
- SDK에서 first/last frame을 직접 지원하는 빌더가 없다면, VeoClient만 REST 호출로 교체(다른 코드 영향 최소).

Acceptance Criteria(P0):
- VIDEO 생성이 prompt-only가 아니라, startShot 이미지 기반으로 동작(시연 시 “샷 이미지에서 영상이 이어지는” 느낌).

---

## 8. 테스트/검증(Workstream D)

### D1. 단위 테스트(백엔드)
- NodeContentLoader: 로컬 파일 로드 케이스(테스트 리소스 파일로 검증)
- startShotNodeId 검증 로직: VIDEO에서 parent SHOT과 불일치 시 실패

### D2. 통합 시나리오(수동/QA)
1) MASTER 생성(SUCCEEDED) → GRID 생성 버튼 활성 확인
2) GRID 생성(SUCCEEDED) → SHOT 생성(특정 gridCellIndex) 버튼 활성 확인
3) SHOT 생성(SUCCEEDED) → VIDEO 생성 버튼 활성 확인
4) VIDEO 생성 결과가 “시작 샷과 유사한 첫 프레임”으로 시작하는지 육안 확인
5) (P1) end shot 지정 후 트랜지션 느낌 확인

### D3. 실패 케이스(시연 안전망)
- 상위 이미지가 없을 때 하위 생성 버튼이 비활성이고, API도 400/409로 명확히 실패하는지
- AI 장애/timeout 시 토스트/에러 메시지가 사용자에게 이해 가능하게 표시되는지

---

## 9. 작업 분해(병렬 진행을 위한 티켓 형태)

### FE 티켓
- FE-1: 노드 상세 하이드레이트 + 캐시(선택 시 1회)
- FE-2: MASTER/GRID promptKo 생성 시 sceneOneLine 포함
- FE-3: settings key 정합(라벨↔key 매핑) + generate/settings 전송 통일
- FE-4: 입력 이미지 준비 여부에 따른 버튼/안내 UX
- FE-5: (Option A) MASTER 룩 변경 시 “MASTER 재생성 필요” 안내 + 하위 노드 재생성 필요 상태 표시(간단 배지/문구)

### BE 티켓
- BE-1: generateNode 타입별 입력 이미지 준비 검증(409/400)
- BE-2: AiPromptService 템플릿 개선(nodeType별 가이드 강화) *(선택)*

### Worker 티켓
- WK-1: NodeContentLoader 구현(S3 우선 + 로컬 fallback)
- WK-2: ImageGenerationWorker: GRID/SHOT에서 parent result 로드 → GeminiImageClient에 참조 이미지 전달
- WK-3: VideoGenerationWorker: startShot 이미지 로드 → VeoClient firstFrame 전달
- WK-4(P1): endShot 이미지 로드 → VeoClient lastFrame 전달

### QA 티켓
- QA-1: 시나리오/실패 케이스 체크리스트 작성 및 검증

---

## 10. 리스크/대응

### 10.1 AI Provider 기능/SDK 제약
- SDK가 first/last frame 또는 image+text를 직접 지원하지 않을 수 있음.
  - 대응: VeoClient/GeminiImageClient를 REST 호출로 부분 대체(최소 영향)

### 10.2 S3/로컬 환경 차이
- 개발/데모 환경에서 저장소가 다를 수 있음.
  - 대응: NodeContentLoader가 S3 우선 + 로컬 fallback을 제공(동일 코드로 커버)

### 10.3 비용/지연(시연)
- 영상 생성은 지연이 크고 실패할 수 있음.
  - 대응: 프론트에서 상태/토스트/재시도 UX 강화 + stub 모드 활용(리허설)

---

## 11. “Done” 정의(시연 기준)

P0 완료 조건:
1) MASTER/GRID/SHOT/VIDEO 모두 promptKo를 AI가 생성하고 사용자가 수정/승인 가능
2) 노드 클릭 시 폼 값이 복원(하이드레이트)
3) GRID/SHOT/VIDEO 생성이 상위 이미지 준비 상태에 따라 UX/서버 모두 방어
4) VIDEO는 start shot 이미지 기반으로 생성되어 “샷에서 영상으로 이어지는” 시연이 가능
