# Vertex AI (Gemini / Imagen / Veo) 모델·API·비용 정리

> 기준일: 2026-01-26 (USD, Vertex AI Generative AI pricing 기준)

## 1) 이 레포에서 현재 쓰는 모델/호출 방식 (As-Is)

### 공통 (Vertex 설정)
- 설정 파일: `itda-backend/src/main/resources/application-*.yml`
- 필수 값
  - `ai.vertex.project-id` (`GCP_PROJECT_ID`)
  - `ai.vertex.location` (`GCP_LOCATION`, 기본 `us-central1`)
  - `ai.vertex.api-version` (기본 `v1`)
- 인증: ADC(Application Default Credentials) 사용
  - 코드: `itda-backend/src/main/java/com/itda/backend/ai/GenAiClientProvider.java`
  - 스코프: `https://www.googleapis.com/auth/cloud-platform`

### 텍스트(시나리오) — Gemini `generateContent`
- 코드: `itda-backend/src/main/java/com/itda/backend/ai/VertexAiGeminiClient.java`
- 모델 설정
  - `ai.scenario.model-text` (env: `VERTEX_TEXT_MODEL`)
  - 기본값(로컬): `gemini-2.5-flash`

### 이미지 — Gemini 2.5 Flash Image `generateContent`
- 코드: `itda-backend/src/main/java/com/itda/backend/ai/gemini/GeminiImageClient.java`
- 모델 설정
  - `ai.gemini.image-model` (env: `GEMINI_IMAGE_MODEL` 또는 `VERTEX_IMAGE_MODEL`)
  - 기본값(로컬): `gemini-2.5-flash-image`

### 비디오 — Veo `predictLongRunning` (SDK에서는 `generateVideos`)
- 코드: `itda-backend/src/main/java/com/itda/backend/ai/veo/VeoClient.java`
- 모델 설정
  - `ai.veo.model` (env: `VEO_MODEL`)
  - 기본값(로컬): `veo-3.1-generate-001`

## 2) 모델 ID(추천/대체) 목록

### 모델 ID 규칙 (중요)
- Vertex AI Generative AI에서 보통 “모델 별칭(짧은 ID)”를 그대로 씁니다.
  - 예: `gemini-2.5-flash`, `gemini-2.5-flash-image`, `gemini-3-flash-preview`
- 문서의 “모델 이름(사람 친화적)”(예: *Gemini 3 Pro Preview*)과, 실제 호출에 넣는 “Vertex 모델 ID”(예: `gemini-3-pro-preview`)를 혼동하면 `404 NOT_FOUND`/`400`이 나기 쉽습니다.

### Gemini (텍스트)
- `gemini-2.5-flash` (현재 사용)
- 대체 후보: `gemini-2.5-pro`, `gemini-2.5-flash-lite`
- (Preview) Gemini 3 (Vertex 모델 ID)
  - `gemini-3-flash-preview`
  - `gemini-3-pro-preview`
  - 주의: Gemini 3는 “Global endpoint”를 사용하므로 `locations/global`로 호출해야 합니다.
- 모델 수명/alias(자동 업데이트) 참고:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/learn/model-versions

### Gemini (이미지 생성/편집)
- `gemini-2.5-flash-image` (현재 사용)
  - 모델 카드: https://cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-flash-image
- (Preview) Gemini 3 이미지 (Vertex 모델 ID)
  - `gemini-3-pro-image-preview`
  - 모델 카드: https://cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/3-pro-image-preview

지원 여부 메모
- 이미지 출력(= `responseModalities`에 `IMAGE`)은 `gemini-2.5-flash-image`, `gemini-3-pro-image-preview`에서 지원됩니다.
- `gemini-2.5-flash`, `gemini-3-pro-preview`는 이미지 “입력”은 가능하지만 이미지 “출력”은 지원하지 않습니다.

### Imagen (텍스트→이미지 전용)
> Gemini 이미지 출력 대신 “Imagen API”로도 이미지 생성 가능 (가격 단위가 “이미지 1장” 기준이라 예산 예측이 쉬운 편)

- GA(권장): `imagen-4.0-generate-001`, `imagen-4.0-fast-generate-001`, `imagen-4.0-ultra-generate-001`
- 기타: `imagen-3.0-generate-002`, `imagen-3.0-generate-001`, `imagen-3.0-fast-generate-001`, `imagen-3.0-capability-001`
- 목록/주의(Preview 모델 retire 등):
  - https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/imagen-api
  - https://cloud.google.com/vertex-ai/generative-ai/docs/deprecations

### Veo (비디오)
- `veo-3.1-generate-001` (현재 사용), `veo-3.1-fast-generate-001`
- `veo-3.0-generate-001`, `veo-3.0-fast-generate-001`
- 모델 카드:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/models/veo/3-1-generate
  - https://cloud.google.com/vertex-ai/generative-ai/docs/models/veo/3-0-generate

## 3) Vertex AI REST API 엔드포인트(모델 API)

> 레포는 `com.google.genai:google-genai` SDK를 쓰지만, 장애/디버깅/권한 확인용으로 REST 형태도 같이 정리합니다.

### 3.1 Gemini — `generateContent`
- REST(공식):
  - `POST https://aiplatform.googleapis.com/v1/{model}:generateContent`
  - `{model}` 예시: `projects/{PROJECT}/locations/{LOCATION}/publishers/google/models/gemini-2.5-flash`
  - (Gemini 3 예시): `projects/{PROJECT}/locations/global/publishers/google/models/gemini-3-pro-preview`
- 참고:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.publishers.models/generateContent

#### Gemini로 이미지 출력할 때(중요)
- `responseModalities`에 `IMAGE`가 포함되어야 이미지가 나옵니다.
- 참고:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/image/generate-images

### 3.2 Imagen — `models.predict`
- REST(공식):
  - `POST https://aiplatform.googleapis.com/v1/{endpoint}:predict`
  - `{endpoint}` 예시: `projects/{PROJECT}/locations/{LOCATION}/publishers/google/models/imagen-4.0-fast-generate-001`
- 참고:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.publishers.models/predict
  - https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/imagen-api

### 3.3 Veo — `predictLongRunning` + 결과 조회
- 생성:
  - `POST https://aiplatform.googleapis.com/v1/{endpoint}:predictLongRunning`
  - `{endpoint}` 예시: `projects/{PROJECT}/locations/{LOCATION}/publishers/google/models/veo-3.1-generate-001`
- 결과 조회:
  - `GET  https://aiplatform.googleapis.com/v1/{operationName}`
- 참고:
  - https://cloud.google.com/vertex-ai/generative-ai/docs/video/generate-videos
  - https://cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.publishers.models/predictLongRunning

## 4) 비용(Price) 요약

> 가장 최신/정확한 값은 항상 공식 Pricing 페이지를 기준으로 확인하세요. (가격은 종종 변경됩니다)
>
> 공식: https://cloud.google.com/vertex-ai/generative-ai/pricing

### 4.1 Gemini 3 (Preview, 토큰 기반)
> 모델 ID: `gemini-3-flash-preview`, `gemini-3-pro-preview` (둘 다 `locations/global`)

| 모델 | 과금 단위 | 입력(≤200K) | 입력(>200K) | 출력 텍스트(≤200K) | 출력 텍스트(>200K) |
|---|---:|---:|---:|---:|---:|
| Gemini 3 Flash Preview | 1M tokens | $0.50 | $0.50 | $3.00 | $3.00 |
| Gemini 3 Pro Preview | 1M tokens | $2.00 | $4.00 | $12.00 | $18.00 |

이미지 출력(토큰) — `gemini-3-pro-image-preview`
| 모델 | 과금 단위 | 출력 이미지(≤200K) | 출력 이미지(>200K) |
|---|---:|---:|---:|
| Gemini 3 Pro Image Preview | 1M tokens | $120.00 | $150.00 |

메모
- 프롬프트가 200K 토큰을 초과하면 “전체 토큰”이 장문(>200K) 단가로 과금됩니다(공식 Pricing 페이지 참고).
- 상세(캐시/배치/오디오 입력 등)는 공식 Pricing 표를 그대로 확인 권장: https://cloud.google.com/vertex-ai/generative-ai/pricing

### 4.2 Gemini 2.5 (토큰 기반)
| 모델 | 과금 단위 | 입력 | 출력(텍스트) | 출력(이미지) |
|---|---:|---:|---:|---:|
| Gemini 2.5 Flash | 1M tokens | $0.30 | $2.50 | (해당 없음) |
| Gemini 2.5 Flash Image (`gemini-2.5-flash-image`) | 1M tokens | $0.30 | $2.50 | $30.00 |
| Gemini 2.5 Pro | 1M tokens | $1.25 | $10.00 | (해당 없음) |

메모
- 위 가격의 근거는 공식 Pricing 표(입력/출력 텍스트/이미지 출력 토큰)입니다: https://cloud.google.com/vertex-ai/generative-ai/pricing

메모
- 텍스트는 대략 “4 chars ≒ 1 token”(공식 페이지의 참고값).
- 이미지 출력은 “image output tokens”로 별도 과금(모델/해상도에 따라 토큰 소모량이 달라짐).

### 4.3 Imagen (이미지 1장 단위)
| 모델 | 기능 | 가격 |
|---|---|---:|
| Imagen 4 Fast | 이미지 생성 | $0.02 / image |
| Imagen 4 | 이미지 생성 | $0.04 / image |
| Imagen 4 Ultra | 이미지 생성 | $0.06 / image |
| Imagen 3 | 이미지 생성 | $0.03 / image |
| Imagen 3 Fast | 이미지 생성 | $0.01 / image |

추가(업스케일)
- Imagen 4 Fast / Imagen 4: Upscale 2x = $0.01 / image, Upscale 4x = $0.02 / image
추가(편집/커스텀)
- Imagen Editing = $0.04 / image
- Imagen Customization = $10 / hour

### 4.4 Veo (생성된 “영상 길이(초)” 단위)
| 모델 | 출력 | 해상도 | 가격 |
|---|---|---|---:|
| Veo 3.1 | Video | 720p/1080p | $0.20 / sec |
| Veo 3.1 | Video + Audio | 720p/1080p | $0.40 / sec |
| Veo 3.1 Fast | Video | 720p/1080p | $0.10 / sec |
| Veo 3.1 Fast | Video + Audio | 720p/1080p | $0.15 / sec |
| Veo 3 | Video | 720p/1080p | $0.20 / sec |
| Veo 3 Fast | Video | 720p/1080p | $0.10 / sec |
| Veo 2 | Video | 720p | $0.50 / sec |

4K 관련(필요 시)
- Veo 3.1/3.1 Fast는 4K 옵션이 표에 별도 단가로 존재(공식 pricing 참고).

## 5) “깨질 때” 빠른 체크리스트

1. **모델 ID 오타/retire 여부**
   - Model versions/lifecycle에서 확인: https://cloud.google.com/vertex-ai/generative-ai/docs/learn/model-versions
2. **location 지원 여부**
   - 모델/기능마다 지원 리전이 다를 수 있음(특히 Preview/Media 모델)
   - Gemini 3는 `locations/global`을 요구하는 반면, Imagen/Veo는 보통 리전(`us-central1` 등)을 씁니다.
     - 이 레포처럼 “단일 Vertex 클라이언트(location 고정)” 구조면, Gemini 3를 위해 `location=global`로 바꾸는 순간 Veo/Imagen 호출이 깨질 수 있습니다.
     - 해결: 모델(또는 기능)별로 **location을 분리한 Client**를 만들거나, 호출 시 endpoint를 명시적으로 분기합니다.
3. **Vertex AI API + Billing 활성화**
4. **ADC 권한**
   - `roles/aiplatform.user`(또는 필요한 최소 권한) 및 프로젝트 설정
5. **API version 혼용**
   - 이 레포는 기본 `v1`로 동작. 문서/샘플이 `v1beta1`인 경우 주의.
