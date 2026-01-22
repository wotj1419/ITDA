# 김은서 담당 계획: 시나리오 기반 씬 자동 생성

작성일: 2026-01-21  
범위: **시나리오 API v1(프롬프트/줄거리) 완료 이후, 시나리오 기반 씬 자동 생성 구현**

---

## 1) 현황 요약
- **완료**: 시나리오 API v1(프롬프트 생성/승인, 줄거리 생성/승인) 구현 완료.
- **다음 목표**: 승인된 줄거리 기반으로 **씬을 자동 생성**하고, 프로젝트 씬 목록과 **연동**.

---

## 2) 레퍼런스
- PRD v2.5.3  
  - 4.2.4 AI 시나리오 생성 4단계 플로우  
  - 8.6 AI 생성 API  
  - 8.4 씬 API  
  - 일정: W3 D3 `SCEN-3` (시나리오→씬 자동 생성)
- API 연동규격서  
  - `POST /api/projects/{id}/scenario/scenes/generate`  
  - `POST /api/projects/{id}/scenes` (씬 생성)
  - `PUT /api/projects/{id}/scenes/order` (씬 순서 변경)
  - `PUT /api/scenes/{sceneId}` (씬 수정)
  - `DELETE /api/scenes/{sceneId}` (씬 삭제)
- 개발 일정표 (W3~W6)  
  - W3 D3: SCEN-3 시나리오→씬 자동 생성 (scene CRUD 연동)

---

## 3) 범위 정의
### 포함
- 승인된 **줄거리 기반** 씬 자동 생성(Title/Description)  
- 생성된 씬 **N개를 `scenes`에 자동 생성 및 APPEND**  
- 기존 `SceneController/SceneService/SceneMapper` **로직 재사용**  
- 시나리오 상태 `SCENES` 단계 진입

### 제외 (후속)
- 씬 CRUD 구현/고도화(삭제/순서 이동/수정 등은 **기존 구현 사용**)  
- 씬별 노드 자동 생성/편집  
- 영상/이미지 생성 워커 연동

---

## 4) 요구사항 요약
1. **전제 조건**
   - 프롬프트 승인(`prompt_status=APPROVED`)
   - 줄거리 존재 + 승인(`plot_status=APPROVED`)
2. **출력**
   - 씬 N개(기본 3~7, `input_scene_count` 준수)
   - 각 씬: `order`, `title`, `description`
3. **저장**
   - **프로젝트 씬 테이블(`scenes`)에 직접 저장**
   - 시나리오 단계 진행 상태는 `project_scenarios.current_step`로 관리
4. **정렬**
   - **APPEND 정책**: 기존 마지막 `order_index` 이후로 순차 배치

---

## 5) 설계 결정(필수 확인)
1. **자동 생성 시 기존 씬 처리**
   - 기본: **기존 씬이 있어도 생성 진행, 마지막 뒤에 추가(APPEND)**  
   - 정렬: 기존 마지막 `order_index` 다음부터 1씩 증가  
   - 확장 여지: `mode=APPEND|REPLACE` 같은 옵션 파라미터로 추후 확장 가능
2. **씬 생성 시점**
   - 자동 생성 단계에서 즉시 `scenes` 레코드 생성  
   - **제안**: 즉시 생성(“씬 자동 생성” 요구 충족)
3. **AI 출력 포맷**
   - JSON 고정 형식 요구(파싱 안정성)  
   - 불완전 응답 시 1회 재시도(또는 내부 fallback 파서)

---

## 6) 데이터 모델(변경)
### 6.1 기존 테이블: `scenes` 활용
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | 씬 ID |
| project_id | BIGINT FK | 프로젝트 |
| order_index | INT | 씬 순서 |
| title | VARCHAR(200) | 씬 제목 |
| description | TEXT | 씬 설명 |
| created_at / updated_at | TIMESTAMP | 메타 |

**인덱스**
- `(project_id, order_index)` (이미 존재)

**정리**
- 시나리오 단계에서 생성되는 “씬 목록”은 **별도 테이블 없이 `scenes`에 직접 저장**.
- 시나리오 진행 상태는 `project_scenarios.current_step`로 관리.

### 6.2 schema-local.sql 반영
- **변경 없음** (신규 테이블 추가 X)

---

## 7) API/서비스 구현 계획
### 7.1 시나리오 씬 생성 (핵심)
**Endpoint**  
`POST /api/projects/{id}/scenario/scenes/generate`

**흐름**
1. 프로젝트 접근 권한 확인
2. 프롬프트/줄거리 승인 상태 확인
3. `input_scene_count` 기반 AI 프롬프트 생성
4. AI 결과 파싱(JSON)
5. 기존 `SceneService/SceneMapper` 로직 재사용하여 `scenes` 일괄 저장(APPEND)
6. 생성된 `scene_id` 반환
7. `project_scenarios.current_step = SCENES` 업데이트

**다건 APPEND 로직(정의)**
- `nextOrderIndex = sceneMapper.findNextOrderIndex(projectId)`
- AI 결과 N개에 대해 `order_index = nextOrderIndex, nextOrderIndex+1 ...` 순차 부여
- 일괄 insert(배치)로 저장  
  - 단건 insert 반복도 가능하지만, 성능/일관성 측면에서 batch insert 권장

**구현 방식**
- `POST /scenario/scenes/generate`는 **기존 scenes API를 HTTP로 호출하지 않고**,  
  **동일 서비스 레이어(`SceneService/SceneMapper`)를 직접 재사용**하여 처리

**응답 예시**
```
{
  "scenes": [
    { "sceneId": 201, "order": 1, "title": "...", "description": "..." }
  ],
  "currentStep": "SCENES"
}
```

### 7.2 시나리오 씬 저장/수정
**연동 범위**
- 씬 CRUD는 **기존 `SceneController`의 엔드포인트를 그대로 사용** (수정/삭제/순서 변경)
- 이번 범위에서는 **AI 생성/append까지만 구현**, 이후 노드 편집 기능으로 확장

**동기화 규칙**
- `scenes.order_index`는 항상 1..N 정규화.

---

## 8) AI 프롬프트/파싱 전략
### 8.1 프롬프트 포맷
- 입력: 줄거리 + 씬 개수  
- 출력: **JSON 배열 고정**
```
[
  { "order": 1, "title": "...", "description": "..." },
  { "order": 2, "title": "...", "description": "..." }
]
```
### 8.2 파싱 실패 대응
- 1차: JSON 파싱
- 실패 시: 단순 포맷 파서(라인 분리) 또는 **1회 재시도**
- 그래도 실패 시: `SCENARIO_INVALID_STATE` 또는 `AI_RESPONSE_INVALID` (신규 코드 필요 여부 확인)

---

## 9) 테스트 계획
1. **유효성**
   - 프롬프트 미승인 → 422
   - 줄거리 미승인 → 422
   - sceneCount 불일치 → 400
2. **정상 플로우**
   - 씬 N개 생성 + `scenes` 레코드 생성
3. **정렬 동기화**
   - APPEND 시 기존 마지막 `order_index` 이후로 순차 배치 확인
4. **중복 생성**
   - 기존 씬 존재 시 **append 동작** 및 `order_index` 증가 확인

---

## 10) 일정/작업 분해 (김은서)
### D3 (01/21) — SCEN-3 핵심 구현
- [ ] `scenes` 기반 시나리오 씬 자동 생성(APPEND) 설계 정리
- [ ] `POST /scenario/scenes/generate` 서비스 구현
- [ ] AI 프롬프트 & 파서 초안 구현

### D4 (01/22) — 연동/보완
- [ ] 시나리오 단계/상태 업데이트
- [ ] 에러 처리/재시도 정책 반영
- [ ] 기본 테스트(서비스 단위)

---

## 11) 산출물 체크리스트
- `scenes` 기반 생성/저장/순서 변경 API 완성  
- AI 프롬프트/파서  
- 기본 테스트 케이스  

---

## 12) 오픈 이슈
- AI 응답 포맷 실패 시 에러 코드 정의  
- 씬 승인/상태 값이 필요할지(필요 시 `scenes` 컬럼 확장 여부) 결정
