# 07. Dispatcher / Queue / Worker 가이드 (Redis Streams)

> 목표: 팀원이 **각자 Worker를 만들 때 동일한 규칙으로** 구현하도록 “계약(contracts)”을 고정합니다.

---

## 0) 우리가 고정해야 하는 것(최소 계약)

### ✅ 고정 계약 3종
1) **Job DB 스키마(핵심 필드)**
2) **Streams 메시지 payload 스키마**
3) **상태 전이 규칙(pending→running→succeeded/failed)**

이 3개만 통일하면, Worker 구현은 서로 독립적으로 병렬 가능합니다.

---

## 1) Job 모델 (DB) — MVP 최소 필드

### 1.1 상태(enum)
- `pending`: 큐에 들어감 (아직 실행 안 함)
- `running`: Worker가 가져가 실행 중
- `succeeded`: 성공
- `failed`: 실패

### 1.2 Job 테이블(예시)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT/UUID | job 식별자 |
| project_id | BIGINT | 라우팅/권한/WS 룸에 사용 |
| type | VARCHAR | `IMAGE_GENERATION`, `VIDEO_GENERATION`, `SCENE_MERGE`, `PROJECT_MERGE` |
| status | VARCHAR | 위 enum |
| input_json | JSON | 요청 파라미터(프롬프트, nodeId...) |
| output_json | JSON | 결과(파일 URL, 메타데이터...) |
| error_message | TEXT | 실패 메시지 |
| retry_count | INT | 재시도 횟수 |
| created_at/updated_at | DATETIME | |

> idempotency(중복 실행 방지)를 위해 `input_json` 안에 **nodeId / sceneId / exportId** 같은 키를 반드시 포함하세요.

---

## 2) Streams 메시지 스키마 (통일)

### 2.1 Stream 이름
- `ai:image`
- `ai:video`
- `media:merge`

### 2.2 Consumer Group
- `image-workers`
- `video-workers`
- `merge-workers`

### 2.3 메시지 필드(최소)

| key | value 예 | 설명 |
|---|---|---|
| `jobId` | `12345` | Job 식별자(필수) |
| `projectId` | `77` | WS 룸 라우팅용 |
| `type` | `IMAGE_GENERATION` | 디버깅/분기용 |
| `createdAt` | `2026-01-19T10:11:12+09:00` | 디버깅 |

> Streams 메시지는 JSON이 아니라 key-value map 형태로 들어갑니다. (Spring에서는 Map으로 다룸)

---

## 3) Dispatcher란 무엇이고, 왜 굳이 만들나?

### 3.1 Dispatcher의 정의
- **Dispatcher = “Job을 실행시키는 게 아니라, Job을 Queue에 넣는 코드”**

### 3.2 왜 추상화가 필요한가?
- W3 초반: Lead 혼자 빠르게 검증할 때는 **Local Executor(메모리/스레드)**가 편함
- W3 중후반: 팀 병렬/운영을 위해 **Redis Streams**로 전환

Dispatcher를 인터페이스로 두면, 코드의 나머지(컨트롤러/서비스)는 그대로 두고
`Dispatcher` 구현만 바꿔서 실행 모드를 바꿀 수 있습니다.

---

## 4) Dispatcher 샘플 코드 (Spring)

> 아래 코드는 **개념 전달용 샘플**입니다. (패키지/예외/트랜잭션은 프로젝트 컨벤션에 맞추세요)

### 4.1 공통 인터페이스

```java
public interface JobDispatcher {
    void enqueue(Job job);
}
```

### 4.2 Redis Streams 구현

```java
@Service
public class RedisStreamsJobDispatcher implements JobDispatcher {

    private final StringRedisTemplate redis;

    public RedisStreamsJobDispatcher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void enqueue(Job job) {
        String streamKey = switch (job.getType()) {
            case IMAGE_GENERATION -> "ai:image";
            case VIDEO_GENERATION -> "ai:video";
            case SCENE_MERGE, PROJECT_MERGE -> "media:merge";
            default -> throw new IllegalArgumentException("unknown job type");
        };

        Map<String, String> fields = new HashMap<>();
        fields.put("jobId", job.getId().toString());
        fields.put("projectId", job.getProjectId().toString());
        fields.put("type", job.getType().name());
        fields.put("createdAt", OffsetDateTime.now().toString());

        // XADD streamKey * jobId ...
        redis.opsForStream().add(streamKey, fields);
    }
}
```

### 4.3 Local Executor 구현(@Async)

```java
@Service
public class LocalAsyncJobDispatcher implements JobDispatcher {

    private final LocalJobExecutor executor;

    public LocalAsyncJobDispatcher(LocalJobExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void enqueue(Job job) {
        executor.executeAsync(job.getId());
    }
}

@Service
public class LocalJobExecutor {

    private final JobService jobService;

    public LocalJobExecutor(JobService jobService) {
        this.jobService = jobService;
    }

    @Async
    public void executeAsync(Long jobId) {
        jobService.run(jobId); // 내부에서 type별로 처리
    }
}
```

> W3 초반(Lead 단독 검증)에는 `LocalAsyncJobDispatcher`, W3 중반부터는 `RedisStreamsJobDispatcher`로 전환하는 운영을 추천합니다.

---

## 5) Worker 기본 규칙 (꼭 지킬 것)

### 5.1 Worker는 “메시지 수신 → 상태 전이 → 처리 → 결과 저장 → ACK” 순서를 지켜야 함

1) 메시지 수신 (`XREADGROUP`)
2) DB 업데이트: status=running
3) 외부 API/FFmpeg 호출
4) 결과 저장(파일 업로드 + DB output 업데이트)
5) DB 업데이트: succeeded / failed
6) `XACK` (메시지 처리 완료)

### 5.2 재시도/중복 실행(중요)
- Streams는 장애/재시작 시 **pending** 메시지가 남을 수 있습니다.
- 따라서 Worker는 “중복 실행”을 대비해야 합니다.

권장 방어선:
- Job이 이미 `succeeded`면 **처리하지 말고 ACK**
- Job이 `running`인데 오래된 경우(timeout)만 재처리
- retry_count 제한(예: 3회)

---

## 6) Worker 샘플 코드 구조 (Spring)

### 6.1 Consumer(Streams Listener) — 개념 예시

```java
@Component
public class ImageWorkerListener {

    private final JobService jobService;

    public ImageWorkerListener(JobService jobService) {
        this.jobService = jobService;
    }

    // 실제 구현은 StreamMessageListenerContainer 등을 사용
    public void onMessage(String jobId) {
        jobService.runImageJob(Long.parseLong(jobId));
    }
}
```

### 6.2 Job 실행 서비스 — “상태 전이 + 결과 저장” 중심

```java
@Service
public class JobService {

    private final JobRepository jobRepo;
    private final JobEventPublisher eventPublisher; // WS로 보내는 주체는 API 서버

    public void runImageJob(Long jobId) {
        Job job = jobRepo.findById(jobId).orElseThrow();

        if (job.isSucceeded()) return;

        job.markRunning();
        jobRepo.save(job);

        try {
            // 1) 외부 호출
            // 2) 파일 업로드
            // 3) output 저장

            job.markSucceeded(/*outputJson*/);
            jobRepo.save(job);

            // ✅ Worker가 직접 WS를 보내지 않고, API 서버로 완료 콜백을 때리는 방식도 가능
            eventPublisher.notifyJobDone(job);

        } catch (Exception e) {
            job.markFailed(e.getMessage());
            jobRepo.save(job);

            eventPublisher.notifyJobFailed(job);
        }
    }
}
```

> 실제로는 Worker가 API 서버로 `POST /internal/jobs/{id}/complete` 같은 콜백을 호출하고,
> API 서버가 WS를 발행하는 형태가 운영/보안 상 깔끔합니다.

---

## 7) 운영 체크리스트 (Streams)

- [ ] consumer group 생성 여부(최초 1회)
- [ ] pending 메시지 모니터링(`XPENDING`)
- [ ] ACK 누락 방지(예외 시에도 처리가 끝났는지 판단)
- [ ] 재시도 정책(횟수/간격)
- [ ] 작업 시간 제한(timeout) / kill 전략

---

## 8) 팀 합의(필수) — 이 문서가 고정하는 것

- Stream 이름 / Consumer group 이름
- 메시지 최소 필드(jobId/projectId/type)
- Job status enum
- “WS는 API 서버가 전담”

> WS 이벤트 상세 스키마는 `04-WebSocket-Event-Schema.md`로 이동합니다.
