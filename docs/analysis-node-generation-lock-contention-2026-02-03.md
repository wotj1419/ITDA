# 이미지 생성 중 노드 추가 지연 이슈 분석 (2026-02-03)

## 1) 문제 요약
- 증상: 이미지/그리드 생성 중에 노드 추가/삭제 클릭 이벤트는 발생하지만, 즉시 반영되지 않고 나중에 한꺼번에 처리되는 현상.
- 영향: 협업 편집에서 "막힌 것처럼 보이는" UX 발생.

## 2) 원인 정리

### A. 근본 원인 (백엔드 DB 락 경합)
- `generateNode`가 트랜잭션 안에서 scene row를 `FOR UPDATE`로 조회함.
  - `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java:274,279`
  - `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java:423`
  - `itda-backend/src/main/resources/mapper/SceneMapper.xml:38-43`
- `createNode`(노드 추가)도 같은 scene row `FOR UPDATE` 경로를 사용함.
  - `itda-backend/src/main/java/com/itda/backend/node/service/NodeService.java:83-86`
- 즉, 생성 요청이 락을 잡고 있는 동안 추가/삭제 요청이 DB에서 대기 -> 락 해제 후 몰아서 처리됨.

### B. "오늘 변경이 원인인가?"에 대한 사실
- 2026-02-03 김은서 커밋은 프론트 중심 변경(패널 잠금/문구/재큐 옵션 전달)이며, `NodeService`/`SceneMapper` 락 로직은 변경하지 않음.
- `generateNode`가 `getSceneAndEnsureMemberForUpdate(...)`를 타는 구조는 기존부터 존재.
  - `generateNode` 도입 커밋: `ddc42e6` (2026-01-24 17:23:55+09:00)
  - `getSceneAndEnsureMemberForUpdate` 도입: `921a7a5` (2026-01-21 15:06:07+09:00)
  - `SceneMapper.findByIdForUpdate (FOR UPDATE)` 도입: `8d962a9` (2026-01-21 15:08:11+09:00)

## 3) "체감을 키운 트리거를 다시 없애면 원상복구되나?"
- 결론: **부분적으로는 좋아질 수 있지만, 근본 복구는 아님**.
- 이유:
  - UI 트리거(버튼 흐름/재시도 패턴)를 줄이면 동시 요청 수가 줄어 체감이 일시 개선될 수 있음.
  - 하지만 락 경합 자체(`generate` vs `add/delete`)는 그대로라, 특정 타이밍에서 재발 가능.

## 4) 두 방안 비교

### 방안 1: UI 재변경으로 트리거 완화
- 내용: 생성 중 일부 상호작용/자동 재시도/중복 액션을 줄여 동시 요청을 덜 발생시키는 방식.
- 장점: 구현/배포가 빠름, 리스크가 상대적으로 낮음.
- 단점: 근본 원인(DB 락 경합)을 해결하지 못함. 사용자 행동 패턴에 따라 재발.
- 평가: **임시 완화책**으로는 가능.

### 방안 2: `generate` 경로의 scene row 락 범위 축소
- 내용(핵심):
  1. `generateNode`에서 scene 조회를 non-lock 경로로 분리 검토 (`getSceneAndEnsureMember`).
  2. 번역/프롬프트 렌더링 등 시간이 걸릴 수 있는 구간을 락 밖으로 분리.
  3. 락이 꼭 필요한 `create/delete/reorder` 계열은 유지.
- 장점: 협업 환경에서 생성 중 추가/삭제 지연 문제를 근본적으로 줄임.
- 단점: 동시성 무결성 점검 필요(권한/정합성/순서 충돌 테스트 필수).
- 평가: **본 해결책**.

## 5) 권장 결론
- 협업 품질이 중요한 현재 요구사항에서는 **방안 2를 우선**하는 것이 맞음.
- 다만 서비스 안정성을 위해:
  - 단기: UI 트리거 완화(임시)
  - 중기: 백엔드 락 범위 축소(본 수정)
  - 순서로 병행하는 전략이 가장 안전함.

## 6) 수정 후 검증 체크리스트
- 이미지 생성 중에도 `마스터 추가/그리드 추가/샷 추가/삭제` 요청이 즉시 처리되는지 확인.
- 2명 이상 동시 편집 시 노드 정합성(부모-자식 관계, order index) 유지 확인.
- 생성 성공/실패/타임아웃 후 상태 전이(`RUNNING -> SUCCEEDED/FAILED`)가 정상 반영되는지 확인.
- 재큐잉(`requeueIfExisting`) 동작과 상관없이 편집 응답성이 유지되는지 확인.

