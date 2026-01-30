# AI 프롬프트 품질 개선 구현 명세서

> **목표**: "키워드 나열이 아닌 장면 서술" 원칙에 맞게 프롬프트 품질을 개선하고,
> 1) 비용이 큰 이미지/영상 생성 전에 **최종 영어 프롬프트(promptEnFinal)** 를 확인/수정할 수 있게 하며,
> 2) **영어 원본(promptEnBase)** + **한국어 번역(promptKo)** 를 함께 제공하고,
> 3) 기본 룩은 **CINEMATIC_MODERN**(영화 톤)으로 맞춘다. (추후 Film Look 옵션 확장 가능)

---

## 0. 맥락 / 용어 / 현재 파이프라인

### 0.1 현재 파이프라인 (AS-IS)

- 프론트 "프롬프트 생성" 버튼 -> `POST /api/ai/prompts/generate` -> `AiPromptService`가 **한국어(promptKo)** 생성/반환
- 프론트는 노드의 `prompt`에 promptKo를 저장
- 실제 생성(비용 발생): `POST /api/nodes/{id}/generate`
  - `NodeService.generateNode()`가 settings 상속/정규화(`GenerationSettingsResolver`) 후
  - `PromptRenderer`로 **최종 영어(promptEnFinal)** 조립 -> job `requestJson.prompt`로 저장 -> 워커가 사용

### 0.2 문제 (WHY)

- 이미지/영상 생성 호출은 비용이 크므로, **생성 전에 최종 영어 프롬프트(promptEnFinal)를 확인**하고 싶다.
- 현재는 promptKo(한국어) 중심이라, 한->영 번역 과정에서 **디테일 손실/요약** 및 **물리적 일관성(시간대/광원) 충돌**이 발생하기 쉽다.
- 단일 `PHOTO_REAL`을 "실사"로만 쓰면 결과가 너무 평범하거나(혹은 수식어 과다로 합성 느낌) 흔들릴 수 있어, 기본 룩을 **영화 톤(CINEMATIC_MODERN)** 으로 맞추고 싶다.

### 0.3 용어 정의 (TO-BE)

| 용어 | 의미 | 기본 편집 권한 | 생성 시 사용 |
|---|---|---:|---:|
| `promptEnBase` | 사용자가 편집하는 **영어 원본**(장면 서술 중심) | 편집 가능 | 간접(렌더링 입력) |
| `promptKo` | `promptEnBase`의 **한국어 번역/프리뷰** | 기본 읽기 전용 | 아니오 |
| `promptEnFinal` | settings(스타일/시간/무드/비율/룩)까지 결합한 **최종 영어** | 읽기 전용 | 예(기본) |
| `promptEnFinalOverride` | 고급 토글에서 사용자가 직접 덮어쓰는 **최종 영어 오버라이드** | 편집 가능(고급) | 예(우선) |

### 0.4 편집/동기화 정책 (TO-BE)

- **영어가 원본(Source of Truth)**: 기본 편집 대상은 `promptEnBase`.
- `promptKo`는 기본적으로 **표시용 번역**이며 자동 동기화는 비용/루프 위험이 있으므로, 명시적 버튼으로 동기화한다.
  - 영어 수정 후: **[EN -> KO 번역]** 버튼으로 promptKo 갱신
  - 한국어를 수정할 수도 있게 열어두되(선택), 한국어 수정 후에는 **[KO -> EN 반영(재작성)]** 버튼으로 promptEnBase 갱신
- **고급 토글(Advanced)**: `promptEnFinal`은 기본 읽기 전용으로 보여주고, 필요 시 `promptEnFinalOverride`를 켜서 사용자가 최종 프롬프트를 직접 통제한다.

### 0.5 룩(look) 확장 전략

- `styleKey`는 렌더링 스타일(실사/애니/3D/수채화/유화)로 유지한다.
- 영화 느낌은 별도 축으로 **`filmLookKey`** 를 도입한다. (초기값: `CINEMATIC_MODERN`)
- 렌즈/심도(DoF)/보케 같은 촬영 요소는 `styleKey`에 고정하지 않고, **노드 타입/샷 타입에 따라 `PromptRenderer`에서 결정**해 충돌을 줄인다.

## 목차

0. [맥락 / 용어 / 현재 파이프라인](#0-맥락--용어--현재-파이프라인)
1. [변경 개요](#1-변경-개요)
2. [변경 1: PresetFragments.java — 프래그먼트 상세화 (+ filmLookKey)](#2-변경-1-presetfragmentsjava--프래그먼트-상세화--filmlookkey)
3. [변경 2: AiPromptService.java — 영어 원본(promptEnBase) 생성 + 한국어 번역(promptKo)](#3-변경-2-aipromptservicejava--영어-원본promptenbase-생성--한국어-번역promptko)
4. [변경 3: VideoActionPlanGenerator.java — 액션 플랜 개선](#4-변경-3-videoactionplangeneratorjava--액션-플랜-개선)
5. [변경 4: Translator(EN-KO) — 번역/재작성 제약 재정의](#5-변경-4-translatoren-ko--번역재작성-제약-재정의)
6. [변경 5: PromptRenderer.java — film look 반영 + 최종 promptEnFinal 조립](#6-변경-5-promptrendererjava--film-look-반영--최종-promptenfinal-조립)
7. [변경 6: 최종 영어 프롬프트(promptEnFinal) 프론트엔드 노출 + 고급 오버라이드](#7-변경-6-최종-영어-프롬프트promptenfinal-프론트엔드-노출--고급-오버라이드)
8. [테스트 업데이트](#8-테스트-업데이트)
9. [구현 순서 및 검증](#9-구현-순서-및-검증)

---

## 1. 변경 개요

### 변경 대상 파일

| # | 파일 경로 | 역할 | 영향도 |
|---|----------|------|--------|
| 1 | `itda-backend/src/main/java/com/itda/backend/ai/prompt/PresetFragments.java` | style/time/mood/shot/camera + **filmLookKey** 프래그먼트 | **높음** |
| 2 | `itda-backend/src/main/java/com/itda/backend/node/generation/GenerationSettingsResolver.java` | 기본값/상속/정규화에 `filmLookKey` 반영 | **중간** |
| 3 | `itda-backend/src/main/java/com/itda/backend/ai/service/AiPromptService.java` | **영어 원본(promptEnBase)** 생성 + 한국어 번역(promptKo) | **중간** |
| 4 | `itda-backend/src/main/java/com/itda/backend/ai/controller/AiPromptController.java` | generate/improve/translate 응답 스키마 정리 | **낮음** |
| 5 | `itda-backend/src/main/java/com/itda/backend/ai/controller/dto/response/AiPromptResponse.java` | `promptEnBase`, `promptKo` 포함 | **낮음** |
| 6 | `itda-backend/src/main/java/com/itda/backend/ai/prompt/*Translator*.java` | EN-KO 번역/재작성(1~3문장 허용) | **중간** |
| 7 | `itda-backend/src/main/java/com/itda/backend/ai/prompt/PromptRenderer.java` | film look + 촬영 요소 반영, **promptEnFinal** 조립 | **높음** |
| 8 | `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java` | prompt preview + override 우선순위 적용 | **높음** |
| 9 | `itda-backend/src/main/java/com/itda/backend/node/controller/NodeController.java` | `prompt-preview` 엔드포인트 추가 | **중간** |
| 10 | `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/GenerateNodeRequest.java` | `promptEnFinalOverride`(또는 동등 필드) 추가 | **중간** |
| 11 | `itda-frontend/src/types/ui/sceneNodes.ts` | 노드 입력/표시용 prompt 필드 확장 | **중간** |
| 12 | `itda-frontend/src/types/api/ai.ts` | prompt 생성/번역 응답 타입 확장 | **낮음** |
| 13 | `itda-frontend/src/services/api/ai.ts` | generate/improve/translate + prompt preview 호출 | **낮음** |
| 14 | `itda-frontend/src/composables/useNodeGeneration.ts` | promptEnBase/promptKo/promptEnFinal 상태 연결 | **낮음** |
| 15 | `itda-frontend/src/components/scene-editor/panels/*Panel.vue` | 영어/한국어/최종영어 UI + 고급 토글 | **중간** |
| 16 | `itda-backend/src/test/java/com/itda/backend/ai/prompt/PromptRendererTest.java` | renderer 결과 변경에 따른 테스트 업데이트 | **낮음** |

### 핵심 변경

1. **기본 룩을 CINEMATIC_MODERN으로**: `filmLookKey` 도입 + 기본값 적용 (추후 룩 옵션 확장 용이)
2. **영어 원본(promptEnBase) 중심으로 전환**: 생성/편집의 기준을 영어로 두고, 한국어는 번역 프리뷰로 제공
3. **최종 영어(promptEnFinal) 미리보기 + 고급 오버라이드**: 비용 큰 생성 호출 전에 확인/수정 가능
4. **프롬프트 품질 개선**: 프래그먼트 상세화 + renderer 조립 개선 + 액션 플랜 개선 + 번역/재작성 정책 정리

---

## 2. 변경 1: PresetFragments.java — 프래그먼트 상세화 (+ filmLookKey)

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/prompt/PresetFragments.java`

현재 프래그먼트가 너무 짧아서 모델이 시각적 디테일을 충분히 반영하지 못한다.

추가로, "영화 같은 느낌"은 `styleKey(실사/애니 등)`만으로 표현하기보다 별도 축인 **`filmLookKey`** 로 분리해야
향후 선택지를 늘려도(예: NOIR/DOCUMENTARY/ANAMORPHIC) 충돌이 적고 확장성이 좋다.

> 주의: 렌즈/심도(DoF)/보케 같은 촬영 요소는 `styleKey`/`moodKey`에 하드코딩하면 노드 타입(특히 MASTER 와이드)과 충돌하기 쉬우므로,
> 최종 조립 단계(`PromptRenderer`)에서 nodeType/shotType에 맞춰 붙이는 방향으로 정리한다.

### 2.1 FilmLookKey (신규)

> **핵심**: 기본값은 `CINEMATIC_MODERN`. (초기에는 1개만 제공하고, 추후 확장)

```java
// (신규) film look 프래그먼트 축
public static String filmLookFragment(Object filmLookKey) { ... }

public enum FilmLookKey {
  CINEMATIC_MODERN("cinematic modern film look, filmic color grading, subtle film grain, natural highlight roll-off, gentle lens bloom");
}
```

### 2.2 StyleKey

```java
// ====== 현재 ======
PHOTO_REAL("photo-realistic"),
ANIME_2D("2D anime illustration, clean line art"),
STYLIZED_3D("stylized 3D animated feature film look"),
WATERCOLOR_ILLUSTRATION("watercolor illustration, soft washes, subtle paper texture"),
OIL_PAINT_ILLUSTRATION("oil paint illustration, textured brush strokes");

// ====== 변경 ======
PHOTO_REAL("photorealistic, realistic skin texture, natural material surfaces, physically plausible lighting"),
ANIME_2D("2D anime illustration, clean ink line art, cel-shaded flat colors, vibrant saturated palette, subtle rim lighting on characters"),
STYLIZED_3D("stylized 3D animated feature-film look, smooth subsurface-scattered skin, soft ambient occlusion, rounded appealing shapes"),
WATERCOLOR_ILLUSTRATION("watercolor illustration, soft washes, subtle paper texture, loose wet-on-wet edges, visible pigment granulation, delicate color bleeds"),
OIL_PAINT_ILLUSTRATION("oil paint illustration, textured brush strokes, thick impasto highlights, rich color mixing, canvas weave texture visible in shadow areas");
```

### 2.3 TimeOfDayKey

```java
// ====== 현재 ======
DAWN("dawn"),
DAY("daytime"),
DUSK("golden hour, sunset"),
NIGHT("night");

// ====== 변경 ======
DAWN("early dawn, pale pink-orange sky gradient, soft diffused pre-sunrise light, long blue-tinted shadows"),
DAY("bright midday, clear overhead sunlight, short crisp shadows, neutral white balance"),
DUSK("golden hour, warm amber sunset light raking at a low angle, long stretched shadows, rich orange-pink sky"),
NIGHT("nighttime, deep blue-black sky, cool moonlight with isolated warm practical light sources, visible ambient glow");
```

### 2.4 MoodKey

무드 프래그먼트는 "색보정/조명/콘트라스트" 중심으로 두고, 프레이밍/DoF 같은 촬영 요소는 ShotType/PromptRenderer 단계로 이동한다.

```java
// ====== 현재 ======
NEUTRAL("natural color grade, balanced lighting, moderate contrast"),
COZY("warm color grade, soft diffused lighting, gentle contrast"),
LONELY("cooler tones, slightly desaturated, more negative space, calm atmosphere"),
TENSE("low-key lighting, higher contrast, cooler grade, subtle shadow emphasis"),
HOPEFUL("bright high-key lighting, vibrant but natural colors, soft highlights"),
DARK("desaturated cool palette, soft low contrast, overcast or dim ambience");

// ====== 변경 ======
NEUTRAL("natural color grade, balanced lighting, moderate contrast"),
COZY("warm color grade, soft diffused lighting, gentle contrast, soft highlights"),
LONELY("cooler tones, slightly desaturated, more negative space, calm atmosphere"),
TENSE("low-key lighting, higher contrast, cooler grade, subtle shadow emphasis, subtle film grain"),
HOPEFUL("bright high-key lighting, vibrant but natural colors, soft highlights, gentle lens flare"),
DARK("desaturated cool palette, dim ambience, muted color palette, hazy atmosphere, vignette edges");
```

### 2.5 ShotTypeKey

```java
// ====== 현재 ======
WIDE("wide shot"),
MEDIUM("medium shot"),
CLOSE_UP("close-up"),
EXTREME_CLOSE_UP("extreme close-up"),
OTS("over-the-shoulder shot"),
POV("POV shot"),
HIGH_ANGLE("high-angle shot"),
LOW_ANGLE("low-angle shot");

// ====== 변경 ======
WIDE("wide shot, full environment visible, subject occupies less than a third of the frame"),
MEDIUM("medium shot, waist-up framing, balanced subject-to-environment ratio"),
CLOSE_UP("close-up, head and shoulders tightly framed, background softly blurred"),
EXTREME_CLOSE_UP("extreme close-up, single facial feature or object detail fills the entire frame"),
OTS("over-the-shoulder shot, foreground shoulder softly blurred, subject in sharp focus"),
POV("POV first-person perspective, hands or held object visible in foreground"),
HIGH_ANGLE("high-angle shot, camera looking down at the subject, subject appears smaller in the environment"),
LOW_ANGLE("low-angle shot, camera looking up at the subject, subject appears powerful and dominant");
```

### 2.6 CameraMotionKey

```java
// ====== 현재 ======
STATIC("static camera"),
SLOW_ZOOM_IN("slow zoom in"),
ZOOM_OUT("zoom out"),
PAN_LR("pan left to right"),
TILT_UP("tilt up");

// ====== 변경 ======
STATIC("static locked-off camera, no movement, rock-steady frame"),
SLOW_ZOOM_IN("slow gradual zoom in, gently narrowing the frame over the full duration"),
ZOOM_OUT("steady zoom out, slowly revealing more of the surrounding environment"),
PAN_LR("smooth pan from left to right at a constant speed, following the action"),
TILT_UP("smooth tilt upward, gradually revealing the scene from bottom to top");
```

### 2.7 ExpressionKey — 변경 없음 (현재 수준 적절)

---

## 3. 변경 2: AiPromptService.java — 영어 원본(promptEnBase) 생성 + 한국어 번역(promptKo)

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/service/AiPromptService.java`

이 단계에서 만드는 프롬프트는 **promptEnFinal(최종 생성용)** 이 아니라, 사용자가 편집하는 **영어 원본(promptEnBase)** 이다.
promptEnFinal은 `PromptRenderer + effective settings`로 조립되므로 별도 preview 엔드포인트에서 계산한다. (변경 6)

### 3.1 buildGeneratePrompt(): promptEnBase 생성(EN)

핵심 원칙:
- 키워드 나열이 아니라 **하나의 장면을 서술**
- **물리적으로 일관된 조명/시간대** (단일 시간대/단일 광원) 유지
- 스타일/무드/시간대는 settings에서 제어되므로, base prompt에서는 **콘텐츠(피사체/행동/환경) 중심**

```java
private String buildGeneratePrompt(AiPromptGenerateRequest request) {
    List<String> lines = new ArrayList<>();

    lines.add("Write an English prompt for image/video generation.");
    lines.add("- Describe ONE coherent scene as 2 to 4 sentences (no keyword list).");
    lines.add("- Output ONLY the prompt text (no bullets, no numbering, no quotes, no JSON, no code, no 'prompt:' prefix).");
    lines.add("- Focus on subject appearance, action, environment, and camera framing/angle.");
    lines.add("- Keep lighting physically plausible: single time of day, single dominant light source; avoid contradictory color/lighting instructions.");
    lines.add("- Do not mention watermarks, subtitles, captions, or logos.");
    lines.add("");

    NodeType nodeType = request == null ? null : request.nodeType();
    if (nodeType != null) {
        lines.add(switch (nodeType) {
            case MASTER -> "Guide: wide establishing shot; include environment, layout, and key props.";
            case GRID -> "Guide: describe the shared scene moment; avoid sequencing; keep details consistent across panels.";
            case SHOT -> "Guide: focus on a single frame with clear subject pose, gaze, hands, and foreground/background relation.";
            case VIDEO -> "Guide: single continuous shot; describe a natural motion arc from start to end (no cuts).";
            case SCENE_HEADER -> "Guide: summarize the scene context briefly.";
        });
        lines.add("");
    }

    lines.add("Inputs:");
    lines.add("nodeType: " + safe(nodeType));
    lines.add("sceneOneLine: " + safe(request == null ? null : request.sceneOneLine()));
    // style/time/mood are resolved later by settings + PromptRenderer; keep them as constraints only.
    lines.add("style(optional constraint): " + safe(request == null ? null : request.style()));
    lines.add("timeOfDay(optional constraint): " + safe(request == null ? null : request.timeOfDay()));
    lines.add("mood(optional constraint): " + safe(request == null ? null : request.mood()));
    if (request != null && request.objects() != null && !request.objects().isEmpty()) {
        lines.add("objects: " + String.join(", ", request.objects()));
    } else {
        lines.add("objects: none");
    }

    return String.join("\\n", lines);
}
```

### 3.2 buildImprovePrompt(): promptEnBase 개선(EN)

`improve`는 한국어를 개선하는 것이 아니라, **영어 원본(promptEnBase)** 을 개선한다.

```java
private String buildImprovePrompt(AiPromptImproveRequest request) {
    List<String> lines = new ArrayList<>();
    lines.add("Improve the following English image/video prompt.");
    lines.add("- Keep the core content, but make it more concrete and visually specific.");
    lines.add("- Output 2 to 4 sentences, English only.");
    lines.add("- Output ONLY the prompt text (no bullets, no numbering, no JSON, no code).");
    lines.add("- Keep lighting physically plausible and internally consistent.");
    lines.add("");
    lines.add("nodeType: " + safe(request.nodeType()));
    if (request.instruction() != null && !request.instruction().isBlank()) {
        lines.add("userFeedback: " + request.instruction().trim());
    }
    lines.add("promptEnBase: " + safe(request.prompt()));
    return String.join("\\n", lines);
}
```

### 3.3 응답/엔드포인트 스키마 (요약)

`/api/ai/prompts/generate` 및 `/api/ai/prompts/improve`는 아래를 반환한다:
- `promptEnBase`: 영어 원본(편집 대상)
- `promptKo`: 영어 원본의 번역 프리뷰(표시용)

추가로 번역/재작성 전용 엔드포인트를 둔다(자동 동기화 대신 버튼 기반):
- `POST /api/ai/prompts/translate` (EN->KO): 영어 수정 후 한국어 갱신
- `POST /api/ai/prompts/rewrite` (KO->EN): 한국어 수정 후 영어 원본 갱신(단순 번역이 아니라 "프롬프트 재작성"에 가깝게)

---

## 4. 변경 3: VideoActionPlanGenerator.java — 액션 플랜 개선

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/prompt/VideoActionPlanGenerator.java`

현재 6초/8초 템플릿이 내용과 무관하게 동일한 제네릭 문구를 붙인다. 추가 API 호출 없이 정적 템플릿만 개선한다.

```java
// ====== 현재 (라인 19~23) ======
return switch (durationSeconds) {
    case 4 -> normalizedBase;
    case 6 -> normalizedBase + " Then a brief follow-up reaction completes the moment.";
    case 8 -> normalizedBase + " Then two small follow-up beats happen, and the action settles into a neutral pose.";
    default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO duration must be 4, 6, or 8");
};

// ====== 변경 ======
return switch (durationSeconds) {
    case 4 -> normalizedBase;
    case 6 -> normalizedBase
        + " The subject holds this action briefly, then naturally settles back"
        + " with a subtle follow-through movement.";
    case 8 -> normalizedBase
        + " The motion unfolds gradually over the first half;"
        + " in the second half, the subject completes the action"
        + " and eases into a relaxed neutral pose with gentle residual movement.";
    default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO duration must be 4, 6, or 8");
};
```

---

## 5. 변경 4: Translator(EN-KO) — 번역/재작성 제약 재정의

**대상 파일(예시)**:
- `itda-backend/src/main/java/com/itda/backend/ai/prompt/*Translator*.java`

이제 영어가 원본(promptEnBase)이므로, 필요한 번역은 2가지다:
- EN -> KO: 영어 원본을 한국어로 **표시/공유**하기 위한 번역
- KO -> EN: 사용자가 한국어를 수정했을 때 영어 원본으로 **반영(재작성)** 하기 위한 변환

> 주의: KO -> EN은 "직역"보다 "프롬프트 재작성"에 가까우며, 물리적 일관성(시간대/광원)과 서술형 구조를 유지해야 한다.

### 5.1 EN -> KO (translate)

```text
Translate the following English image/video prompt into natural Korean.
Rules:
- Preserve all visual details and spatial relationships.
- Keep 2 to 4 sentences.
- Output ONLY Korean text (no quotes, no bullet points, no JSON, no code).
- Do not add information not present in the source.
Text:
{promptEnBase}
```

### 5.2 KO -> EN (rewrite for promptEnBase)

```text
Rewrite the following Korean description into a fluent English image/video generation prompt.
Rules:
- 2 to 4 sentences, descriptive paragraph (no keyword list).
- Output ONLY English prompt text (no quotes, no bullets, no JSON, no code).
- Preserve all concrete visual details; do not add new information.
- Keep lighting physically plausible and internally consistent (single time of day, single dominant light source).
Text:
{promptKo}
```

### 5.3 normalizeText()

- 모델 응답의 줄바꿈/불릿/접두어를 제거하고, 1개의 문단으로 정리한다.
- 따옴표로 전체가 감싸져 있으면 제거한다.

---

## 6. 변경 5: PromptRenderer.java — film look 반영 + 최종 promptEnFinal 조립

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/prompt/PromptRenderer.java`

Gemini 가이드의 핵심 원칙: **"Describe the scene, don't just list keywords."**

TO-BE에서 `PromptRenderer`는 아래를 책임진다:
- 입력: `promptEnBase`(영어 원본) + scene 컨텍스트 + settings(styleKey/timeOfDayKey/moodKey/filmLookKey/ratio/shotType...)
- 출력: **`promptEnFinal`** (실제 생성에 사용)
- 충돌 방지: 렌즈/심도(DoF) 같은 촬영 요소는 `styleKey`/`moodKey`가 아니라 **nodeType/shotType 기준으로 결정**
- 포맷: 라벨 나열을 최소화하고, 자연어 지시문 형태로 구성(여러 줄이어도 OK)

### 6.1 renderMaster() (라인 53~78)

```java
// ====== TO-BE (요약) ======
List<String> lines = new ArrayList<>();
lines.add("A " + safeOrNone(filmLook) + " " + safeOrNone(style) + " wide establishing shot of " + requireEn(promptEnBase) + ".");
lines.add("Set in the scene \"" + safeOrNone(sceneTitleEn) + "\" — " + safeOrNone(sceneDescriptionEn) + ".");
lines.add("Lighting: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
lines.add("Camera: 24-35mm wide lens, deep focus (establishing shot).");
lines.add("Maintain consistent character identity, outfits, lighting, and key props across all shots.");
lines.add("No text, no subtitles, no watermark, no logo.");
lines.add("Aspect ratio: " + safeOrNone(aspectRatio) + ".");
```

**TO-BE 출력 예시:**
```
A cinematic modern film look, photorealistic wide establishing shot of A young woman sits by the window of an old wooden cafe, holding a warm mug with both hands and gazing outside.
Set in the scene "Morning in the cafe" — A quiet neighborhood cafe.
Lighting: bright midday, clear overhead sunlight... Mood: natural color grade, balanced lighting...
Camera: 24-35mm wide lens, deep focus (establishing shot).
Maintain consistent character identity, outfits, lighting, and key props across all shots.
No text, no subtitles, no watermark, no logo.
Aspect ratio: 16:9.
```

### 6.2 renderShot() (라인 147~171)

```java
// ====== 현재 ======
List<String> lines = new ArrayList<>();
int safeGridCellIndex = gridCellIndex == null ? 0 : Math.max(0, gridCellIndex);
int cellNumberHuman = safeGridCellIndex + 1;
lines.add("High-quality single cinematic frame based on storyboard cell #" + cellNumberHuman + ".");
lines.add("Content: " + requireEn(promptEnBase) + ".");
lines.add("Camera framing: " + safeOrNone(shotTypeEn) + ". Facial expression: " + safeOrNone(expressionEn) + ".");
lines.add("Extra detail: " + safeOrNone(detailEn) + ".");
lines.add("Match the master look and character identity. No text, no watermark, no logo.");
lines.add("Aspect ratio: " + safeOrNone(aspectRatio) + ".");

// ====== 변경 ======
List<String> lines = new ArrayList<>();
int safeGridCellIndex = gridCellIndex == null ? 0 : Math.max(0, gridCellIndex);
int cellNumberHuman = safeGridCellIndex + 1;
lines.add("A " + safeOrNone(filmLook) + " " + safeOrNone(style) + " high-quality single cinematic frame based on storyboard cell #" + cellNumberHuman + ":");
lines.add(requireEn(promptEnBase) + ".");
lines.add("Captured as a " + safeOrNone(shotTypeEn) + ", with the subject showing a " + safeOrNone(expressionEn) + " expression.");
lines.add("Camera: lens and depth of field appropriate for the framing (e.g., 24-35mm wide/deep focus; 85mm close-up/shallow DoF).");
if (!safeOrNone(detailEn).equals(DEFAULT_NONE)) {
    lines.add(safeOrNone(detailEn) + ".");
}
lines.add("This frame matches the established master look with consistent character identity and visual continuity.");
lines.add("No text, no watermark, no logo. Aspect ratio: " + safeOrNone(aspectRatio) + ".");
```

### 6.3 renderVideo() (라인 173~211)

```java
// ====== 현재 ======
List<String> lines = new ArrayList<>();
lines.add("Generate a " + (duration == null ? 4 : duration) + "-second single continuous shot video from the provided start image. No cuts, no time jumps.");
lines.add("Action plan: " + requireEn(actionPlanEn));
String cameraLine = "Camera motion: " + safeOrNone(cameraMotionEn) + ".";
if (!motionDescriptionEn.isBlank()) {
    cameraLine += " " + ensurePeriod(motionDescriptionEn);
}
lines.add(cameraLine);
lines.add("Keep character identity, outfits, lighting, and location consistent. No text, no watermark, no logo.");
lines.add("Style: " + safeOrNone(filmLook) + " " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
lines.add("Hold the final pose for the last half-second.");
if (read(settings, "endShotNodeId") != null) {
    lines.add("End should gently approach the end shot composition (no hard cut).");
}

// ====== 변경 ======
List<String> lines = new ArrayList<>();
lines.add("Generate a " + (duration == null ? 4 : duration) + "-second single continuous shot video from the provided start image, with no cuts or time jumps.");
lines.add(requireEn(actionPlanEn));
String cameraLine = "The camera uses " + safeOrNone(cameraMotionEn) + ".";
if (!motionDescriptionEn.isBlank()) {
    cameraLine += " " + ensurePeriod(motionDescriptionEn);
}
lines.add(cameraLine);
lines.add("The scene has a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
lines.add("Maintain character identity, outfits, and location consistency throughout. No text, no watermark, no logo.");
lines.add("Hold the final pose steadily for the last half-second.");
if (read(settings, "endShotNodeId") != null) {
    lines.add("End should gently approach the end shot composition (no hard cut).");
}
```

### 6.4 renderGridShotVariations() (라인 80~109)

`Style: X. Time: Y. Mood: Z.` 라인(104)을 서술형으로 변경:

```java
// ====== 현재 (라인 104) ======
lines.add("Style: " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");

// ====== 변경 ======
lines.add("Rendered in a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
```

### 6.5 renderGridStoryBeats() (라인 111~145)

동일하게 라인 140 변경:

```java
// ====== 현재 (라인 140) ======
lines.add("Style: " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");

// ====== 변경 ======
lines.add("Rendered in a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
```

---

## 7. 변경 6: 최종 영어 프롬프트(promptEnFinal) 프론트엔드 노출 + 고급 오버라이드

목표는 3가지를 동시에 만족하는 것이다:
- 사용자는 **영어 원본(promptEnBase)** 을 편집한다.
- 사용자는 한국어도 함께 보되, 한국어는 기본적으로 **번역 프리뷰(promptKo)** 다.
- 비용 큰 생성 호출 전에, 실제 생성에 쓰일 **최종 영어(promptEnFinal)** 를 보고 필요하면 고급 모드로 직접 덮어쓴다.

### 7.1 백엔드: AiPromptResponse.java (generate/improve 응답)

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/controller/dto/response/AiPromptResponse.java`

```java
public record AiPromptResponse(
    @Schema(description = "English base prompt (user editable source of truth)", example = "A young woman sits by the window of an old wooden cafe...")
    String promptEnBase,

    @Schema(description = "Korean translation preview for UI", example = "낡은 목조 카페 창가에 앉은 젊은 여성이...")
    String promptKo
) {}
```

### 7.2 백엔드: AiPromptController.java (generate/improve/translate/rewrite)

**파일**: `itda-backend/src/main/java/com/itda/backend/ai/controller/AiPromptController.java`

- `/generate`: 입력값 기반으로 promptEnBase 생성 -> promptKo 번역 -> 둘 다 반환
- `/improve`: promptEnBase 개선 -> promptKo 재번역 -> 둘 다 반환
- `/translate` (EN->KO): 영어 수정 후 한국어 갱신 버튼용
- `/rewrite` (KO->EN): 한국어 수정 후 영어 원본 반영 버튼용

### 7.3 백엔드: prompt preview 엔드포인트 (진짜 최종 promptEnFinal)

**파일(예시)**:
- `itda-backend/src/main/java/com/itda/backend/node/controller/NodeController.java`
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`

새 엔드포인트를 추가한다(작업 enqueue 없음):
- `POST /api/nodes/{id}/prompt-preview`

요구사항:
- `NodeService`가 effective settings를 계산하고(`GenerationSettingsResolver` + active master 상속)
- `PromptRenderer`로 **promptEnFinal** 을 생성해 반환
- 고급 오버라이드(`promptEnFinalOverride`)가 있으면, preview는 "사용될 프롬프트"를 기준으로 반환한다

응답 예시(요약):
```json
{
  "promptEnFinal": "...",
  "source": "RENDERED" // or "OVERRIDE"
}
```

### 7.4 백엔드: GenerateNodeRequest에 오버라이드 전달

**파일**: `itda-backend/src/main/java/com/itda/backend/node/controller/dto/request/GenerateNodeRequest.java`

```java
public record GenerateNodeRequest(
    // 기존 필드...
    String promptEnFinalOverride
) {}
```

생성 시 우선순위:
1) request의 `promptEnFinalOverride` (고급 모드)
2) 없으면 `PromptRenderer`가 만든 `promptEnFinal`

### 7.5 프론트엔드: 타입/서비스/컴포저블

**파일(예시)**:
- `itda-frontend/src/types/api/ai.ts`
- `itda-frontend/src/services/api/ai.ts`
- `itda-frontend/src/composables/useNodeGeneration.ts`

프론트는 다음을 다룬다:
- prompt 생성 결과: `promptEnBase` + `promptKo`
- prompt preview 결과: `promptEnFinal` (+ source)
- 고급 override 입력: `promptEnFinalOverride`

저장/동기화(권장, DB 변경 최소화):
- `Node.prompt` 컬럼을 **promptEnBase(영어 원본)** 로 사용
- `Node.dataJson(settings)`에 `promptKo`, `promptEnFinalOverride`를 함께 저장
- `promptEnFinal`은 기본적으로 캐시하지 않고 `prompt-preview`로 계산(필요하면 캐시 키 추가 가능)

`useNodeGeneration` 흐름(요약):
1) generatePrompt 클릭 -> `aiService.generatePrompt()`로 `promptEnBase/promptKo` 수신 -> 노드 state 업데이트
2) 이어서 `nodeService.previewPrompt()` 호출 -> `promptEnFinal` 표시(읽기 전용)
3) 생성 버튼 클릭(runGeneration) 시:
   - 고급 override가 있으면 `GenerateNodeRequest.promptEnFinalOverride`에 실어 보냄
   - 없으면 기존처럼 promptEnFinal을 backend가 조립하여 job enqueue

### 7.6 프론트엔드: 패널 UI (영/한/최종영어 + 고급 토글)

권장 UI 구성:
- **English (Editable)**: promptEnBase textarea
  - 버튼: `[EN -> KO 번역]` (promptKo 갱신)
- **Korean (Preview)**: promptKo read-only(기본) + 옵션으로 editable
  - 버튼(선택): `[KO -> EN 반영]` (promptEnBase 갱신)
- **Final English (Used for generation)**: promptEnFinal (read-only)
  - **Advanced 토글**: 켜면 promptEnFinalOverride textarea 노출 + "Override 적용됨" 배지 표시

> 라벨 주의: promptEnBase/promptKo는 "프롬프트 생성" 결과이고, promptEnFinal은 "실제 생성에 사용"이다. UI에서 오해가 없게 명확히 구분한다.

---

## 8. 테스트 업데이트

**파일**: `itda-backend/src/test/java/com/itda/backend/ai/prompt/PromptRendererTest.java`

### 8.1 PromptRendererTest (핵심 변경 포인트)

- `VideoActionPlanGenerator` 변경에 맞춰, 6초/8초 확장 문구 assertion 업데이트
- `filmLookKey`가 prompt에 반영되는지(예: "cinematic modern") 확인하는 assertion 추가(선택)
- `promptEnBase`가 renderer 입력으로 들어가는 경우, 더 이상 `EN( ... )` 래핑에 의존하지 않도록 테스트 스텁/입력값 정리

### 8.2 GenerationSettingsResolver 기본값 테스트(선택)

- `filmLookKey` 기본값이 `CINEMATIC_MODERN`으로 들어가는지 검증(예: `effectiveSettings.get("filmLookKey")`)

### 8.3 NodeService prompt-preview 테스트(가능하면 추가)

- `promptEnFinalOverride`가 있으면 preview가 OVERRIDE를 반환하는지
- override가 없으면 renderer 결과(RENDERED)를 반환하는지

---

## 9. 구현 순서 및 검증

### 구현 순서

```
Phase 1 (렌더링 기반 정리):
  ├─ [변경 1] PresetFragments.java — filmLookKey 추가 + 프래그먼트 상세화
  ├─ [변경 1-추가] GenerationSettingsResolver.java — filmLookKey 기본값(CINEMATIC_MODERN) 반영
  └─ [변경 3] VideoActionPlanGenerator.java — 액션 플랜 개선

Phase 2 (프롬프트 파이프라인 전환):
  ├─ [변경 4] Translator(EN-KO) — translate/rewrite 구현
  ├─ [변경 2] AiPromptService/Controller — promptEnBase 생성 + promptKo 번역 + translate/rewrite 엔드포인트
  └─ [변경 5] PromptRenderer.java — film look 반영 + promptEnFinal 조립(촬영 요소 충돌 방지)

Phase 3 (미리보기/오버라이드 + UI 노출):
  ├─ [변경 6-백엔드] NodeService/NodeController — prompt-preview + promptEnFinalOverride 지원
  └─ [변경 6-프론트] 타입/서비스/컴포저블/패널 — 영/한/최종영어 + 고급 토글

Phase 4 (테스트/빌드):
  └─ PromptRendererTest/preview 관련 테스트 업데이트 + 빌드 확인
```

### 검증 방법

1. **단위 테스트**: `./gradlew test` — `PromptRendererTest` 통과 확인
2. **로컬 서버 실행 후 기능 테스트**:
   - "프롬프트 생성" 클릭 -> `promptEnBase(편집 가능)` + `promptKo(번역)` + `promptEnFinal(읽기 전용)` 표시 확인
   - 영어 수정 -> `[EN->KO 번역]` 버튼으로 한국어 갱신 확인(자동 번역 아님)
   - (선택) 한국어 수정 -> `[KO->EN 반영]` 버튼으로 영어 원본 갱신 확인
   - 고급 토글 ON -> `promptEnFinalOverride` 입력 -> preview/source=OVERRIDE 확인 -> 생성 요청 시 override가 사용되는지 확인
3. **프롬프트 품질 확인**:
   - `filmLookKey=CINEMATIC_MODERN`이 promptEnFinal에 반영되는지 확인
   - 시간대/무드/스타일이 서로 충돌하지 않게 조립되는지 확인(특히 MASTER 와이드의 DoF/렌즈)
4. **미리보기 정확성 확인**:
   - 실제 생성 워커가 사용하는 prompt와 preview promptEnFinal이 일치하는지(override 포함)
