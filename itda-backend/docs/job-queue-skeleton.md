# Job Queue 스켈레톤 (Redis Streams)

출처: PRD AI Movie Studio v2.5 (비동기 Job용 Redis Streams, 상태
PENDING/RUNNING/SUCCEEDED/FAILED, image/video/merge 워커).

## 문서 개요

이 문서는 Redis Streams 기반 Job Queue의 "공통 계약"을 기록한 문서다.
Streams 토픽/스키마/컨슈머그룹 규칙을 한 곳에 모아 팀원이 같은 기준으로
Producer/Consumer를 구현하도록 돕는다. 구현 상세는 코드와 아키텍처 문서에서
관리한다.

## 언제 참고하나

- 신규 job type 또는 worker를 추가할 때
- Dispatcher가 stream에 넣을 필드를 정의/검토할 때
- Consumer(Worker) 읽기/ACK/재시도 로직을 구현할 때
- 운영/장애 대응 시 PEL, DLQ, retry 정책을 점검할 때

## 어떻게 참고하나 (간단 절차)

1. job type을 결정하고 대응하는 stream key를 선택한다.
2. Stream entry 스키마의 필수/선택 필드를 맞춘다.
3. Consumer group 규칙에 맞춰 읽기/ACK를 구현한다.
4. 재시도/리클레임/DLQ 규칙을 적용한다.

## 주의할 점 (Job Queue 초보자용)

- Streams는 at-least-once라 중복 처리가 가능하다. DB 상태 확인과 조건부
  업데이트로 중복 실행을 막는다.
- ACK는 결과 확정 후에만 한다. 먼저 ACK하면 작업이 유실될 수 있다.
- payload는 최소화하고 jobId로 DB를 조회한다.
- 재시도 폭주를 막기 위해 retryCount 상한, visibilityTimeout, DLQ를 함께 쓴다.
- PEL 누적은 장애 신호다. reclaim 규칙과 모니터링이 필요하다.
- 스키마 변경 시 schemaVersion을 올리고 구버전 호환을 고려한다.
- 필드명/키 네이밍(예: type vs jobType, env suffix)을 일관되게 유지한다.

## 용어 설명 (초보자용)

- Streams: Redis의 로그형 큐. 메시지가 스트림에 쌓이고 컨슈머가 읽는다.
- Consumer group: 여러 컨슈머가 작업을 나눠 처리하는 단위.
- PEL (Pending Entries List): 컨슈머가 읽었지만 ACK하지 않은 메시지 목록.
- XACK: 작업 완료 후 메시지를 처리 완료로 표시하는 명령.
- XCLAIM: 오래된 PEL 메시지를 다른 컨슈머가 가져오는 명령.
- visibilityTimeout: 메시지를 다른 컨슈머가 가져갈 수 있도록 기다리는 시간 기준.
- DLQ (Dead Letter Queue): 반복 실패한 메시지를 별도로 모아두는 스트림.
- at-least-once: 최소 1번은 처리됨. 중복 처리가 발생할 수 있다.

## Streams (토픽)

| Stream key  | 용도 | Job types | Producer | Consumer group |
|------------|------|-----------|----------|----------------|
| ai:image   | AI 이미지 생성 (Gemini) | IMAGE_GENERATION | API job dispatcher | cg:ai-image |
| ai:video   | AI 영상 생성 (Veo) | VIDEO_GENERATION | API job dispatcher | cg:ai-video |
| media:merge| FFmpeg 병합 (scene/project) | SCENE_MERGE, PROJECT_MERGE | API job dispatcher | cg:media-merge |

비고:
- job family 단위로 stream을 분리하고, worker는 jobId로 DB에서 상세를 조회한다.
- 환경 분리가 필요하면 suffix를 사용한다: `ai:image:dev`, `ai:image:prod`.

## Stream entry 스키마

필수 필드:
- jobId (string 또는 int)
- type (IMAGE_GENERATION | VIDEO_GENERATION | SCENE_MERGE | PROJECT_MERGE)
- projectId (string 또는 int)
- createdAt (ISO-8601)

선택 필드:
- nodeId (image/video)
- sceneId (scene merge)
- idempotencyKey
- retryCount (int, DB의 값과 동기)
- requestHash (hex)
- traceId
- schemaVersion (int)
- priority (0..n)

가이드:
- payload는 최소화하고 jobId를 키로 DB를 소스 오브 트루스로 유지한다.

## Consumer group 규칙

그룹 네이밍:
- 기본: `cg:{stream}` (예: `cg:ai-image`)
- 환경 분리 시: `cg:{stream}:{env}`

컨슈머 네이밍:
- `{workerType}-{hostname}-{pid}` (예: `image-worker-app01-1234`)

읽기:
- `XREADGROUP GROUP <group> <consumer> COUNT <n> BLOCK <ms> STREAMS <key> >`

ACK:
- DB 상태가 SUCCEEDED/FAILED로 전환된 이후에 XACK.

재시도/리클레임:
- worker 장애 시 메시지는 PEL에 남는다. `visibilityTimeout` 이후 `XPENDING` + `XCLAIM`으로 reclaim.
- 재시도 가능하면 DB retryCount 증가 후 동일 stream에 `XADD`로 재투입.
- retryCount >= MAX_RETRY_COUNT이면 DLQ로 이동하고 job은 FAILED로 확정.

멱등성:
- DB 상태가 SUCCEEDED면 작업을 스킵하고 XACK.
- RUNNING 전환은 낙관적 업데이트(조건부 UPDATE)로 중복 처리를 방지.

## DLQ (옵션, 권장)

Stream key: `dlq:jobs`

필드:
- jobId, type, projectId, sourceStream
- reason, failedAt, retryCount

## 예시 Redis 명령

```text
XGROUP CREATE ai:image cg:ai-image $ MKSTREAM
XGROUP CREATE ai:video cg:ai-video $ MKSTREAM
XGROUP CREATE media:merge cg:media-merge $ MKSTREAM

XADD ai:image * jobId 101 type IMAGE_GENERATION projectId 1 createdAt 2026-01-20T10:00:00Z

XREADGROUP GROUP cg:ai-image image-worker-app01-1234 COUNT 1 BLOCK 5000 STREAMS ai:image >
XACK ai:image cg:ai-image <message-id>
```

## TODO / 결정 필요

- visibilityTimeout (job type별)
- MAX_RETRY_COUNT
- schemaVersion 및 optional field 확정
- DLQ 처리 및 알림 기준
