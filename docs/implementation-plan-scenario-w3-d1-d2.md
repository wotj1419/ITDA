# W3 D1~D2 구현 계획: 시나리오 API v1 + Vertex AI Gemini 연동 PoC (김은서)

> 기준 일정: 2026-01-19(월) ~ 2026-01-20(화) 
> 참조 문서: `docs/ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md`, `docs/PRD_AI_Movie_Studio_v2.5.md`, `docs/APIdocs.md`, `docs/ai-movie-studio-md-pack-v3/06-api-and-events.md`  
> 범위: **SCEN-1 (Vertex AI Gemini 연동 PoC)**, **SCEN-2 (시나리오 API v1: 입력~줄거리 단계)**  

---

## 1) 목표/성공 기준

### D1 (01/19) — Vertex AI Gemini 연동 PoC
- Vertex AI Gemini 텍스트 생성 API 호출 성공(요청/응답 파싱 완료)
- 대표 오류 케이스 확인 및 에러 매핑 방식 확정
- API 키/모델/타임아웃 등 설정 방식 확정

### D2 (01/20) — 시나리오 API v1
- 시나리오 **입력 → 프롬프트 생성/저장/승인**까지 동작
- 프롬프트 기반 **줄거리 생성/저장/승인**까지 포함
- 프로젝트 멤버 권한 체크 및 기본 유효성 검증 포함

---

## 2) 범위 상세

### 포함
- 시나리오 입력값 저장
- 프롬프트 생성 API 및 승인 API
- 줄거리 생성 API 및 승인 API
- 시나리오 조회 API

### 제외 (D3 이후)
- 씬별 스토리 생성/저장/순서 변경 (SCEN-3에서 처리)
- 시나리오 → 씬 자동 생성 후 씬 CRUD 연동

---

## 3) API 설계 (v1)

> 공통: `Authorization: Bearer <JWT>`, `ApiResponse` 포맷 사용

### 3.1 시나리오 조회
`GET /api/projects/{projectId}/scenario`

Response (예시)
```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "version": 1,
    "currentStep": "PROMPT",
    "input": {
      "genre": "SF",
      "mood": "HOPEFUL",
      "sceneCount": 5,
      "keywords": "화성, 생존, 가족",
      "characterHints": "외로운 우주인",
      "backgroundHints": "화성 기지",
      "referenceStyle": "인터스텔라"
    },
    "prompt": { "text": "…", "status": "DRAFT" },
    "plot": { "text": "…", "status": "DRAFT" }
  }
}
```

### 3.2 프롬프트 생성 (저장)
`POST /api/projects/{projectId}/scenario/prompt/generate`

Request
```json
{
  "genre": "SF",
  "mood": "HOPEFUL",
  "sceneCount": 5,
  "keywords": "화성, 생존, 가족",
  "characterHints": "외로운 우주인",
  "backgroundHints": "화성 기지",
  "referenceStyle": "인터스텔라"
}
```

Response
```json
{
  "code": "SUCCESS",
  "data": {
    "prompt": { "text": "…", "status": "DRAFT" }
  }
}
```

### 3.3 프롬프트 저장/승인
`PUT /api/projects/{projectId}/scenario/prompt`

Request
```json
{ "text": "수정된 프롬프트", "status": "APPROVED" }
```

### 3.4 줄거리 생성
`POST /api/projects/{projectId}/scenario/plot/generate`

- 사전 조건: 프롬프트 `APPROVED`

Request: 없음 (서버에 저장된 승인 프롬프트 사용)

Response
```json
{
  "code": "SUCCESS",
  "data": {
    "plot": { "text": "…", "status": "DRAFT" }
  }
}
```

### 3.5 줄거리 저장/승인
`PUT /api/projects/{projectId}/scenario/plot`

Request
```json
{ "text": "수정된 줄거리", "status": "APPROVED" }
```

---

## 4) 데이터 모델 (D2 기준)

> **시나리오 1개 = 프로젝트 1개** (PK=project_id)

### 4.1 테이블 제안: `project_scenarios`
```sql
CREATE TABLE project_scenarios (
  project_id BIGINT PRIMARY KEY,
  input_genre VARCHAR(50) NOT NULL,
  input_mood VARCHAR(50) NOT NULL,
  input_scene_count INT NOT NULL,
  input_keywords VARCHAR(255),
  input_character_hints TEXT,
  input_background_hints TEXT,
  input_reference_style VARCHAR(100),
  prompt_text TEXT,
  prompt_status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
  plot_text TEXT,
  plot_status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
  current_step VARCHAR(10) NOT NULL DEFAULT 'INPUT',
  version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_scenario_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 4.2 상태값
- `prompt_status`, `plot_status`: `DRAFT | APPROVED`
- `current_step`: `INPUT | PROMPT | PLOT | SCENES` (FE 단계 표시용)

### 4.3 상태/버전 정책
- `prompt_text`, `plot_text`는 `NULL` 허용 (미생성 상태)
- `version`은 **낙관적 락** 용도: 업데이트 성공 시 +1 (WHERE version = ? 조건)
- 프롬프트 재생성 시 `plot_text`, `plot_status` 초기화 (DRAFT), `current_step = PROMPT`
- 줄거리 재생성 시 `plot_status`는 `DRAFT`, 씬 단계는 초기화 예정(SCEN-3)

---

## 5) 백엔드 구조/파일 변경

### 5.1 패키지 구조
```
com.itda.backend.scenario
  ├─ controller/ScenarioController.java
  ├─ controller/dto/request/GeneratePromptRequest.java
  ├─ controller/dto/request/UpdatePromptRequest.java
  ├─ controller/dto/request/GeneratePlotRequest.java
  ├─ controller/dto/request/UpdatePlotRequest.java
  ├─ controller/dto/response/ScenarioResponse.java
  ├─ controller/dto/response/ScenarioPromptResponse.java
  ├─ controller/dto/response/ScenarioPlotResponse.java
  ├─ service/ScenarioService.java
  ├─ domain/ProjectScenario.java
  ├─ repository/ScenarioMapper.java
  └─ repository/dto/ScenarioRecord.java
```

### 5.2 MyBatis XML
`itda-backend/src/main/resources/mapper/ScenarioMapper.xml`
- `findByProjectId`
- `insertScenario`
- `updateInputAndPrompt`
- `updatePrompt`
- `updatePlot`

### 5.3 구성/설정
기존 설정 키(`spring.ai.vertex.ai.gemini`)를 사용하고, **환경변수**로 주입
```yaml
spring.ai.vertex.ai.gemini:
  project-id: ${GCP_PROJECT_ID:}
  location: ${GCP_LOCATION:us-central1}

ai:
  scenario:
    model-text: ${VERTEX_TEXT_MODEL:}
    timeout-ms: ${VERTEX_TIMEOUT_MS:8000}
```
- 서비스 계정 키는 `GOOGLE_APPLICATION_CREDENTIALS` 환경변수로 경로 지정

---

## 6) Vertex AI Gemini 연동 PoC 설계 (D1)

### 6.1 최소 클라이언트
- `VertexAiGeminiClient` (패키지: `com.itda.backend.ai`)
- **Spring AI Vertex AI Gemini** 사용 (기존 설정과 일치)
- 요청/응답 DTO 분리

### 6.2 요청 흐름 (예시)
- Spring AI Gemini 모델 빈 구성
- `generate(promptText)` 호출
- 입력은 텍스트 프롬프트(2~3문장 생성 지시)

### 6.3 응답 파싱
- 모델 응답 텍스트 → 프롬프트/줄거리 텍스트
- 빈 응답 시 `INTERNAL_ERROR`로 처리

### 6.4 오류 케이스 체크리스트
- 인증 실패: 서비스 계정 누락/권한 부족
- 429: 쿼터 초과
- 5xx: Provider 장애
- 타임아웃: timeout 처리 및 에러 코드 매핑

---

## 7) 서비스 로직 상세 (D2)

### 7.1 `generatePrompt`
1. 프로젝트 멤버십 체크
2. 입력값 유효성 검증(genre, mood 필수, sceneCount 3~7, 길이 제한)
3. Vertex AI Gemini 호출 → 프롬프트 텍스트 획득
4. `project_scenarios` **upsert**
5. `current_step = PROMPT`, `prompt_status = DRAFT`
6. **재생성 시** `plot_text`, `plot_status` 초기화 (DRAFT)

### 7.2 `approvePrompt`
1. 프롬프트 존재 여부 확인
2. `prompt_status = APPROVED`, `current_step = PLOT`

### 7.3 `generatePlot`
1. 프롬프트 `APPROVED` 여부 확인
2. 저장된 프롬프트로 Vertex AI Gemini 호출 → 줄거리 텍스트 획득
3. `plot_status = DRAFT`

### 7.4 `approvePlot`
1. `plot_status = APPROVED`, `current_step = SCENES`

### 7.5 오류/응답 정책 (요약)
- 권한 없음: `FORBIDDEN`
- 프로젝트 없음: `PROJECT_NOT_FOUND`
- 사전 조건 위반(승인 전 줄거리 생성 등): `INVALID_REQUEST` 또는 신규 코드(`SCENARIO_INVALID_STATE`) 추가

---

## 8) 테스트/검증

### 8.1 유닛 테스트
- `ScenarioService` 입력 검증(필수값/sceneCount 범위)
- 프롬프트 승인 없이 줄거리 생성 요청 시 에러
- `VertexAiGeminiClient` 응답 파싱 테스트 (mock)
- 재생성 시 하위 단계 초기화 테스트

### 8.2 수동 테스트 시나리오
1. 로그인 → 프로젝트 생성
2. `POST /scenario/prompt/generate` 호출
3. `PUT /scenario/prompt` 승인
4. `POST /scenario/plot/generate` 호출
5. `GET /scenario`로 상태 확인

---

## 9) 산출물

- `VertexAiGeminiClient` + 환경설정
- 시나리오 API 컨트롤러/서비스/매퍼
- DB 스키마 업데이트 (`schema-local.sql` 포함)
- 기본 테스트 케이스

---

## 10) 결정 사항

1. **AI Provider**: **Vertex AI Gemini**로 통일 (텍스트/이미지/비디오 일관성 확보)
2. **D2 범위**: 프롬프트 + 줄거리 생성/승인까지 포함
3. **keywords 입력 형식**: FE는 문자열 입력을 사용 중이므로 **문자열(콤마 구분)**으로 수신 → 서버에서 trim/split하여 프롬프트 생성에 사용  
   - 참고: FE 타입(`itda-frontend/src/types/index.ts`)은 `keywords?: string[]`라서, **서버에서 문자열/배열 모두 수용**하도록 유연하게 파싱 권장
4. **시나리오 저장 방식**: **`project_scenarios` 단일 테이블로 시작**하고, 씬 테이블(`scenario_scenes`)은 SCEN-3 범위에서 추가

## 11) 추가 확인 필요 (값만 지정하면 됨)

- GCP 프로젝트 ID / 리전 (`GCP_PROJECT_ID`, `GCP_LOCATION`)
- 서비스 계정 키 파일 경로 (`GOOGLE_APPLICATION_CREDENTIALS`)
- 텍스트 모델 ID (`VERTEX_TEXT_MODEL`)  
  - W4 이후 이미지/비디오 모델 ID는 Worker 설계 시 별도 확정
