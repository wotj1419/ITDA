# Gen AI SDK 통일 리팩토링 계획서

> 작성일: 2026-01-25  
> 대상: `itda-backend` AI 텍스트/이미지/비디오 생성

## 목적

- Google Gen AI SDK로 통일하여 SDK/REST 혼용 제거
- 인증(ADC)·설정 일원화 및 유지보수성 개선
- 프롬프트/이미지/비디오 생성 흐름을 동일한 패턴으로 정리

## 현재 상태(요약)

- 텍스트: `VertexAiGeminiClient`가 `google-cloud-vertexai` SDK 사용
- 이미지: `GeminiImageClient`는 REST + `VertexAiAuthProvider`(ADC 토큰)
- 비디오: `VeoClient`는 REST + `VertexAiAuthProvider`(ADC 토큰)
- 워커: `ImageGenerationWorker`, `VideoGenerationWorker`가 각 클라이언트 호출
- 설정: `spring.ai.vertex.ai.gemini.*`, `ai.gemini.*`, `ai.veo.*` 혼재

## 목표

- `com.google.genai:google-genai` SDK로 텍스트/이미지/비디오 통일
- 인증은 **ADC**만 사용 (`GOOGLE_APPLICATION_CREDENTIALS`)
- 프로젝트/리전은 공통 설정 키로 정리
- 기존 API/DTO/응답 포맷은 변경 최소화
- Stub 모드(로컬/데모) 유지

## 결정 사항(안)

- SDK: `com.google.genai:google-genai`
- Vertex AI 사용 환경변수:
  - `GOOGLE_GENAI_USE_VERTEXAI=true`
  - `GOOGLE_CLOUD_PROJECT`
  - `GOOGLE_CLOUD_LOCATION`
- Stub 유지: `AI_GEMINI_STUB`, `AI_VEO_STUB`
- 타임아웃/폴링: 현 설정(`ai.*.timeout-ms`, `ai.veo.poll-interval-ms`) 유지

## 범위

### 포함
- `itda-backend` AI 클라이언트/워커/설정/테스트

### 제외
- 프론트엔드 변경
- API 계약 변경
- 비즈니스 로직 구조 재설계

## 단계별 작업 계획

### 1) 의존성/설정 정리
- `google-genai` 추가
- `google-cloud-vertexai` 제거
- 공통 설정 키 도입(예: `ai.vertex.project-id`, `ai.vertex.location`)
- `application-*.yml`, `env`에 공통 키 반영
- 기존 키는 일정 기간 **호환 읽기** 제공(옵션)

### 2) 클라이언트 구조 통일
- 현 클래스명 유지 후 내부 구현 교체(변경 최소화)
  - `VertexAiGeminiClient` → Gen AI SDK 텍스트 호출
  - `GeminiImageClient` → Gen AI SDK 이미지 생성
  - `VeoClient` → Gen AI SDK 비디오 생성(LRO)
- 또는 신규 클래스 도입 후 워커/서비스에서 교체(선택)

### 3) 텍스트 생성(프롬프트/시나리오)
- `VertexAiGeminiClient.generate()`를 Gen AI SDK로 교체
- 응답 텍스트 추출 방식 기존과 동일 유지

### 4) 이미지 생성
- `GeminiImageClient.generateImage()`를 Gen AI SDK로 교체
- 기존 요청 파라미터(`aspectRatio`) 매핑 유지

### 5) 비디오 생성(Veo)
- `VeoClient.generateVideo()`를 Gen AI SDK로 교체
- LRO 폴링/타임아웃 처리
- `duration`, `aspectRatio`, `cameraMotion`, `motionDescription` 매핑 유지

### 6) 인증 컴포넌트 제거
- `VertexAiAuthProvider` 및 REST 전용 유틸 제거
- Bearer 토큰 관련 코드 정리

### 7) 설정/테스트 업데이트
- 테스트 프로파일 설정 정리
- 기존 테스트가 깨지면 최소 수정
- Stub 모드 테스트 확인

## 리스크 및 대응

- **SDK 기능 지원 차이**: 영상/이미지 API의 Java 지원 범위 확인 필요  
  → 미지원 시 REST 백업 플로우 유지(옵션)
- **설정 마이그레이션**: 키 변경으로 환경 설정 누락 가능  
  → 일시적 호환 읽기 제공 권장
- **LRO 타임아웃**: Veo 생성 시간이 길어질 수 있음  
  → 타임아웃/폴링 파라미터 노출 유지

## 검증 체크리스트

- 텍스트 프롬프트 생성/개선 정상 동작
- 시나리오 프롬프트/플롯/씬 생성 정상 동작
- 이미지 생성(Stub/실연동) 정상 동작
- 비디오 생성(Stub/실연동) 정상 동작
- `./gradlew test` 통과

## 완료 기준

- 텍스트/이미지/비디오 모두 Gen AI SDK 기반
- REST 기반 `GeminiImageClient`/`VeoClient`와 `VertexAiAuthProvider` 제거
- 설정 키 통일 및 문서 반영
- 테스트 및 기본 시나리오 성공
