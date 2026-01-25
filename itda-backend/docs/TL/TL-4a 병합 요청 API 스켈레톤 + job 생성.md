# TL-4a 병합 요청 API 스켈레톤 가이드
대상: 3주차 D4 / TL-4a  
브랜치: `feat/TL-4a-merge-request-skeleton`

---

## 1) 목표/범위
**목표**
- 병합 요청 API(`/merge`) 스켈레톤 구현
- 요청 즉시 **job 생성** + `jobId`/`status` 반환
- 실제 병합(FFmpeg/Worker)은 D5에서 구현 → **여기서는 하지 않음**

**범위(In scope)**
- `/api/scenes/{id}/merge` (씬 병합 요청)
- `/api/projects/{id}/merge` (프로젝트 병합 요청)
- DTO/Controller/Service/Mapper 스켈레톤
- job 생성 로직
- Swagger/문서 예시 정리(간단)

**범위(Out of scope)**
- 실제 병합 처리(Worker/FFmpeg)
- 파일 생성/저장/URL 발급
- 타임라인 계산 상세 로직 (이미 TL‑1에서 읽기 구현)

---

## 2) 참고 문서/규칙
- 일정: `02-schedule-w3-w6.md` / `03-team-plan-by-person.md`
- PRD: `PRD_AI_Movie_Studio_v2.5.md`
- APIdocs: `APIdocs.md`

**핵심 규칙 (PRD/APIdocs 기준)**
- **씬 병합**: 씬 타임라인 순서 기준 병합  
- **프로젝트 병합**: 프로젝트 타임라인 순서 기준 병합  
- **P1 스코프**: 영상만 병합 (오디오/트랜지션 제외)

---

## 3) API 요약
### 3.1 씬 병합 요청
- `POST /api/scenes/{id}/merge`
- 설명: 씬 내부 확정 VIDEO들을 병합 요청
- 응답: `202 ACCEPTED` + `jobId`, `status`

**요청 바디**
```json
{
  "includeMusic": false
}
```
- 요청 바디 optional (미전송 시 `includeMusic=false`)
- 현재 P1에서는 `false` 고정 가능

### 3.2 프로젝트 병합 요청
- `POST /api/projects/{id}/merge`
- 설명: 씬 병합 결과(씬 비디오)를 프로젝트 타임라인 순서 기준으로 병합 요청
- 응답: `202 ACCEPTED` + `jobId`, `status`

---

## 4) 구현 가이드 (Skeleton)

### 4.1 DTO
- `MergeRequest`
  - `Boolean includeMusic` (nullable)
- `MergeResponse`
  - `Long jobId`
  - `JobStatus status`

### 4.2 Service 로직 (핵심 흐름)

**씬 병합**
1. `requireScene(sceneId)` + `ensureMember(userId, projectId)`
2. confirmed VIDEO 존재 여부 최소 검증(없으면 400)
3. `job` 생성  
   - type: `SCENE_MERGE`  
   - payload: `{ includeMusic }`
4. `MergeResponse(jobId, status)` 반환

**프로젝트 병합**
1. `requireProject(projectId)` + `ensureMember(userId, projectId)`
2. sceneVideoId 존재 여부 최소 검증(없으면 400)
3. `job` 생성  
   - type: `PROJECT_MERGE`  
   - payload: `{ includeMusic }`
4. `MergeResponse(jobId, status)` 반환

### 4.3 Mapper/Repository
- `JobService.createAndEnqueue(...)` 사용 (Job 도메인 재사용)
- payload JSON은 ObjectMapper로 직렬화

---

## 5) 에러/응답 가이드
- `404` PROJECT_NOT_FOUND / SCENE_NOT_FOUND  
- `403` FORBIDDEN (멤버 아님)  
- `400` INVALID_REQUEST (확정 VIDEO 없음 등)  
- `202` ACCEPTED (job 생성 성공)

> 기존 `ProjectService`, `SceneService`, `NodeService`의 에러 코드/응답 포맷과 동일하게 맞춘다.

---

## 6) 문서/Swagger 정리
- APIdocs 기준 설명 유지
- 응답 예시는 `jobId`, `status` 포함으로 갱신
- 요청 바디 optional + `includeMusic=false` 기본값 명시
- “순서 기준 병합” 문구 유지(씬/프로젝트 타임라인 기준)

---

## 7) 체크리스트
- [ ] MergeRequest/Response DTO 작성  
- [ ] Controller endpoint 생성(씬/프로젝트)  
- [ ] Service skeleton + job 생성 로직  
- [ ] JobService 연동 (createAndEnqueue)  
- [ ] 최소 검증 로직(confirmed VIDEO / sceneVideoId)  
- [ ] 문서/Swagger 예시 업데이트  

---

## 8) 커밋 단위(권장)
1) **Feat : Add merge request DTO/controller skeleton**  
2) **Feat : Add merge job creation in service**  
3) **Docs : Update merge request notes (jobId response)**  

---

## 9) 테스트 시나리오(간단)
- **씬 병합 요청**: confirmed VIDEO 없으면 400  
- **씬 병합 요청**: confirmed VIDEO 있으면 202 + jobId/status  
- **프로젝트 병합 요청**: sceneVideoId 없으면 400  
- **프로젝트 병합 요청**: sceneVideoId 있으면 202 + jobId/status  

---

## 10) 비고
- 실제 병합 결과(contentUrl/sceneVideoId 생성)는 **D5 Worker 단계**
- D4에서는 **job 생성/응답 안정화**가 목표

---

## 11) 작업 기록
- **MergeRequest**: 요청 바디 optional, `includeMusic` 기본 false 처리
  - (Reason: P1/P2 확장 대비 Future Compatibility)
- **MergeResponse**: `jobId` + `status` 반환
  - (Reason: Long-running Task 비동기 처리)
- **Controller**: 씬/프로젝트 병합 요청 엔드포인트 추가, `202 Accepted` 응답
- **Service**: JobService로 job 생성 + payload JSON 직렬화


## 12) 테스트 기록

- 테스트 환경: local / itda_local
- 사전 조건:
  - sceneId=1
  - VIDEO 노드 6/7/8 confirm 완료
  - timeline_items + scene_videos 더미 데이터 삽입
- POST /api/scenes/1/merge -> 202 ACCEPTED, jobId/status 반환 확인
- GET /api/scenes/1/timeline -> items 6/7/8, order 0/1/2 확인 (totalDuration=0)
- POST /api/projects/1/merge -> 202 ACCEPTED, jobId/status 반환 확인
- GET /api/projects/1/timeline -> sceneVideoId/thumbnail/duration/order 반환 확인