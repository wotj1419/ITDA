# Streams / Job Schema (MVP)

> 목적: Dispatcher/Worker가 **같은 규칙**으로 Job을 처리하도록 최소 계약을 고정합니다.  
> 기준 문서: `docs/ai-movie-studio-md-pack-v3/ai-movie-studio-md-pack/07-dispatcher-queue-worker-guide.md`, `docs/ai-movie-studio-md-pack-v3/ai-movie-studio-md-pack/16-glossary.md`

---

## 0) 역할 정의 (고정)

- **Dispatcher**: API 서버 내부 역할. 요청 검증 후 **Job을 만들고 Stream에 발행**한다.
- **Worker**: 별도 프로세스/컨테이너. Stream에서 Job을 가져와 **실제 작업을 수행**한다.

---

## 1) Job Status (enum)

- `pending` → `running` → `succeeded | failed`

---

## 2) Stream 이름 (topic)

- `ai:image`
- `ai:video`
- `media:merge`

> Stream = topic 역할 (Redis Streams).

---

## 3) Consumer Group

- `image-workers`
- `video-workers`
- `merge-workers`

---

## 4) Streams 메시지 (payload)

> Redis Streams는 **key-value map**으로 들어갑니다. (JSON 아님)

필수 필드
| key | type | 설명 |
| --- | --- | --- |
| jobId | String | Job 식별자 |
| projectId | String | WS 라우팅/권한 체크 |
| type | String | `IMAGE_GENERATION` \| `VIDEO_GENERATION` \| `SCENE_MERGE` \| `PROJECT_MERGE` |
| createdAt | String (ISO-8601) | 디버깅용 |

선택 필드
| key | type | 설명 |
| --- | --- | --- |
| idempotencyKey | String | 중복 실행 방지 키 |
| retryCount | Number | 재시도 횟수 (Job 기준) |

---

## 5) Idempotency / Retry (최소 규칙)

- Idempotency-Key 헤더가 들어오면 **그 값을 Job에 저장**하고 재요청 시 동일 jobId 반환.
- 헤더가 없으면 **target 기반**으로 idempotencyKey 생성  
  예) `projectId + type + targetId(nodeId/sceneId)` 해시
- Worker는 **중복 실행 대비**: 이미 `succeeded`면 처리하지 않고 ACK.

