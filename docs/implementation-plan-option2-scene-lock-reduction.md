# Option 2 구현안: generate 경로의 Scene Row Lock 축소

## 1. 문제 정의
- 현재 `generateNode` 요청이 scene row를 `FOR UPDATE`로 조회하면서, 같은 scene에 대한 **락 사용 경로(특히 create, 일부 상태변경)** 요청이 DB에서 대기합니다.
- 결과적으로 협업 중 "클릭 이벤트는 발생하지만 반영이 늦고 한꺼번에 처리"되는 현상이 발생합니다.
- 단, `delete`는 현재 코드상 scene `FOR UPDATE`를 직접 사용하지 않으므로, 실제 대기 원인은 별도 계측으로 확인이 필요합니다.

## 2. 왜 UI 해제만으로는 부족한가
- UI 잠금 해제는 요청 "발생"만 복구합니다.
- 병목은 서버 트랜잭션 락 경합이므로, 근본적으로는 백엔드의 락 범위를 줄여야 합니다.

## 3. 목표
- 이미지/그리드/샷/영상 생성 중에도 같은 scene의 노드 추가/삭제 요청이 즉시 처리되도록 개선.
- 생성 품질/기능(재큐잉, 프롬프트 생성/승인 흐름)은 유지.

## 4. 현재 락 경합 지점
- `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`
  - `generateNode(...)` 내부에서 `getSceneAndEnsureMemberForUpdate(...)` 호출
  - `createNode(...)`도 동일 메서드 사용
- `itda-backend/src/main/resources/mapper/SceneMapper.xml`
  - `findByIdForUpdate`가 `FOR UPDATE` 수행

## 5. 핵심 해결 전략 (Option 2)

### 5.1 원칙
- `create/delete/reorder`는 기존 락 유지 (정합성 보수적 유지)
- `generate` 경로만 scene `FOR UPDATE`를 제거/축소

### 5.2 1차 최소 변경 (핫픽스 성격)
1. `generateNode(...)`에서 scene 조회를 `getSceneAndEnsureMemberForUpdate(...)` -> `getSceneAndEnsureMember(...)`로 변경
2. 나머지 로직은 그대로 유지 (요청 포맷, `requeueIfExisting`, job enqueue, 상태 갱신)

> 기대효과: scene row lock 경합이 즉시 감소해, 생성 중 add/delete 응답성 개선

### 5.3 2차 안정화 (권장)
`generateNode`를 "준비 단계"와 "쓰기 단계"로 분리:

#### A) 준비 단계 (Read 중심, 락 없음)
- prompt 정규화/번역/렌더링
- settings 합성
- 입력 이미지 준비 검증

#### B) 쓰기 단계 (짧은 트랜잭션)
- generation input 저장
- job 생성/재큐잉
- node 상태 `PENDING/RUNNING` 갱신
- 이벤트 발행

> 핵심은 외부 AI/번역 호출 가능 구간을 긴 트랜잭션 밖으로 빼서 체류시간을 줄이는 것

#### C) 트랜잭션 분리 구현 시 주의사항 (중요)
- 같은 클래스 내부 private 메서드 분리만으로는 `@Transactional` 경계가 분리되지 않을 수 있음 (self-invocation).
- 아래 둘 중 하나로 **명시적 분리** 필요:
  1. `prepare`는 트랜잭션 밖에서 실행하고, `persistAndEnqueueGeneration`을 별도 `@Service`로 분리해 `@Transactional` 적용
  2. `TransactionTemplate`로 쓰기 구간만 감싸기
- 목표는 `generateNode` 전체가 아닌 **쓰기 구간만 짧게 트랜잭션 유지**하는 것.

## 6. 코드 수정 포인트 (구체)

### 6.1 NodeService
- 파일: `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java`

#### 변경 항목
1. `generateNode(...)`의 scene 조회 메서드 교체
   - Before: `getSceneAndEnsureMemberForUpdate(node.getSceneId(), userId)`
   - After: `getSceneAndEnsureMember(node.getSceneId(), userId)`

2. (2차) 내부 메서드 분리
   - `prepareGenerationContext(...)` (read-only)
   - `persistAndEnqueueGeneration(...)` (transactional, 짧게)
   - 구현 시 self-invocation 회피(별도 빈/TransactionTemplate) 필수
   - 권장 구조:
     - `NodeGenerationOrchestrator`(비트랜잭션): prompt 정규화/번역/렌더링, settings 합성, 입력 검증, requestJson/idempotency 계산
     - `NodeGenerationWriter`(`@Transactional`): node 재확인, generation input 저장, job enqueue, status 갱신, 이벤트 발행
   - 트랜잭션 경계 원칙:
     - 외부 호출(번역/AI)은 트랜잭션 밖
     - DB write + enqueue만 트랜잭션 안

3. 재검증 최소화
   - 쓰기 단계 진입 시 `node` 존재/타입/권한만 한 번 더 확인
   - `updateGenerationInputs` 결과 row count = 0 이면 `NODE_NOT_FOUND`로 즉시 실패 처리
   - job 생성 시 FK/무결성 예외 발생 시 `NODE_NOT_FOUND` 또는 재시도 불가 에러로 매핑

### 6.2 Mapper/Repository
- `SceneMapper.xml`의 `findByIdForUpdate`는 유지
  - 단, `generate` 경로에서는 호출하지 않도록 service 레벨에서 제어

### 6.3 JobService
- 현재 `idempotency + requeueIfExisting` 로직 유지
- 변경 불필요 (회귀 리스크 최소화)

## 7. 동시성/정합성 고려
- `generate` 중 scene의 active master가 바뀔 수 있음
  - 허용 가능한 eventual consistency로 처리 (다음 generate에서 반영)
- 생성 직전 node가 삭제될 수 있음
  - 쓰기 단계 재조회 + `updateGenerationInputs` 0-row + job insert FK 예외를 모두 `NODE_NOT_FOUND`로 안전 실패
- 중복 생성은 기존 idempotency 키 정책으로 제어

### 7.1 레이스 케이스별 처리 방침 (추가)
1. `prepare` 후 `persist` 직전 노드 삭제  
   - 재조회 실패 또는 update 0-row → 실패 반환 (`NODE_NOT_FOUND`)
2. node update 성공 후 job insert 시점에 노드 삭제  
   - FK 예외 캐치 후 비즈니스 예외로 변환 (클라이언트에 명확한 실패 전달)
3. active master 변경과 동시 generate  
   - 요청 시점 스냅샷 기준으로 실행, 이후 요청부터 최신 master 반영

## 8. 테스트 계획 (필수)

### 8.1 통합 테스트
1. Thread A: `generateNode` 호출 (의도적으로 지연 유발)
2. Thread B: 같은 scene에 `createNode` 호출
3. 검증:
   - 변경 전: B가 긴 대기
   - 변경 후: B가 즉시 완료(또는 유의미하게 단축)
4. 합격 기준(예시):
   - `createNode` p95 < 300ms
   - DB lock wait p95 < 100ms
   - 타임아웃/에러율 증가 없음

### 8.2 회귀 테스트
- prompt 생성/승인/재생성 흐름
- `requeueIfExisting=true` 경로
- 실패/타임아웃 시 node 상태 전이 (`RUNNING -> FAILED`)
- race 케이스:
  - generate 중 node 삭제 시 에러 코드 일관성(`NODE_NOT_FOUND`)
  - update 0-row, FK 예외 매핑 검증

### 8.3 수동 검증 시나리오
1. 마스터 이미지 생성 시작
2. 생성 중 `마스터 추가`, `그리드 추가`, `노드 삭제` 실행
3. "이벤트만 쌓였다가 나중에 반영" 현상 해소 확인

### 8.4 사전 계측 체크 (문제 재현 근거)
- 엔드포인트별로 lock wait/응답시간 로그를 분리 수집:
  - `generateNode`, `createNode`, `deleteNode`
- 특히 `deleteNode` 지연의 원인이 scene lock인지 FK 대기인지 구분해서 기록

## 9. 배포 전략
1. 1차 최소 변경 먼저 배포
2. 락 대기 지표/사용자 체감 모니터링
   - p95 응답시간, DB lock wait, 에러율, 재시도율
3. 필요 시 2차(prepare/write 분리) 추가 배포

## 10. 롤백 계획
- 서비스 레벨 1줄 복귀:
  - `getSceneAndEnsureMember(...)` -> `getSceneAndEnsureMemberForUpdate(...)`
- DB 스키마 변경이 없으므로 롤백 비용이 낮음
- 2차 분리 적용 시 롤백도 단계화:
  1. 분리 로직 feature toggle OFF (가능한 경우)
  2. `generateNode` 단일 트랜잭션 경로로 복귀
  3. 필요 시 1차 롤백(ForUpdate 복귀)까지 수행

## 11. 권장 커밋 단위
1. `fix(backend): generate 경로에서 scene FOR UPDATE 제거`
2. `refactor(backend): generate 준비/쓰기 단계 분리로 트랜잭션 축소`
3. `test(backend): generate-create 동시성 회귀 테스트 추가`

---

## 결론
- 협업 환경에서의 체감 이슈를 근본 해결하려면 **Option 2(백엔드 락 범위 축소)**가 맞습니다.
- 우선 1차 최소 변경으로 빠르게 개선하고, 2차 분리 리팩터링으로 안정성을 높이는 2-step 접근을 권장합니다.
