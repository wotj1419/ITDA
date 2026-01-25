# 백엔드 아키텍처 가이드

> 이 문서는 ITDA 프로젝트의 백엔드 아키텍처를 설명합니다.
> AI 이미지/영상 생성 작업의 전체 흐름과 각 컴포넌트의 역할을 이해할 수 있습니다.

---

## 목차

1. [용어 정리](#1-용어-정리)
2. [전체 아키텍처 개요](#2-전체-아키텍처-개요)
3. [Job 시스템](#3-job-시스템)
4. [Redis Streams 기반 작업 큐](#4-redis-streams-기반-작업-큐)
5. [Worker 컴포넌트](#5-worker-컴포넌트)
6. [AI 클라이언트](#6-ai-클라이언트)
7. [WebSocket 실시간 알림](#7-websocket-실시간-알림)
8. [전체 흐름 예시](#8-전체-흐름-예시)
9. [패키지 구조](#9-패키지-구조)

---

## 1. 용어 정리

백엔드 코드를 이해하기 위해 알아야 할 핵심 용어들입니다.

### 1.1 일반 용어

| 용어 | 설명 |
|------|------|
| **Job** | AI 생성 작업 하나를 의미합니다. 이미지 생성, 영상 생성, 병합 등의 작업이 Job으로 관리됩니다. |
| **Worker** | 실제로 AI API를 호출하거나 FFmpeg를 실행하는 컴포넌트입니다. |
| **Dispatcher** | Job을 생성하고 작업 큐(Redis Streams)에 메시지를 발행하는 역할입니다. |
| **Consumer** | 작업 큐에서 메시지를 읽어 Worker에게 전달하는 역할입니다. |
| **Asset** | 생성된 이미지/영상 파일의 메타데이터입니다. DB에 저장됩니다. |

### 1.2 Redis Streams 관련 용어

| 용어 | 설명 |
|------|------|
| **Stream** | Redis의 데이터 구조로, 메시지를 순서대로 저장하는 로그입니다. 카프카의 토픽과 비슷합니다. |
| **Consumer Group** | 여러 Consumer가 하나의 Stream을 나눠서 처리할 수 있게 해주는 그룹입니다. |
| **ACK (Acknowledge)** | "이 메시지 처리 완료했어요"라고 Redis에 알려주는 것입니다. |
| **Pending** | Consumer가 가져갔지만 아직 ACK하지 않은 메시지 목록입니다. |

### 1.3 Spring 관련 용어

| 용어 | 설명 |
|------|------|
| **@Service** | 비즈니스 로직을 담당하는 클래스에 붙이는 어노테이션입니다. |
| **@Component** | Spring이 관리하는 빈(Bean)으로 등록하는 어노테이션입니다. |
| **@Transactional** | 메서드 실행을 하나의 트랜잭션으로 묶어줍니다. 실패하면 롤백됩니다. |
| **@Profile** | 특정 환경(local, redis-streams 등)에서만 빈이 활성화되도록 합니다. |

### 1.4 WebSocket 관련 용어

| 용어 | 설명 |
|------|------|
| **WebSocket** | 서버와 클라이언트가 양방향으로 실시간 통신할 수 있는 프로토콜입니다. |
| **STOMP** | WebSocket 위에서 동작하는 메시지 프로토콜입니다. Spring에서 쉽게 사용할 수 있습니다. |
| **Topic** | 구독자들에게 메시지를 브로드캐스트하는 주소입니다. (예: `/topic/projects/123`) |
| **SimpMessagingTemplate** | Spring에서 STOMP 메시지를 보내는 데 사용하는 클래스입니다. |

---

## 2. 전체 아키텍처 개요

### 2.1 한 줄 요약

```
API 요청 → Job 생성 → Redis Streams 발행 → Consumer가 Worker 호출 → 결과 저장 → WebSocket 알림
```

### 2.2 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              [Frontend/Browser]                              │
│                                                                              │
│  1. POST /api/nodes/{id}/generate     ←──────────────────────────────────┐  │
│     "이미지 생성해줘"                                                     │  │
│                                         7. WebSocket: job.done            │  │
│                                            "생성 완료! 결과 URL 여기야"    │  │
└─────────────────────────────────────────────────────────────────────────────┘
          │                                             ▲
          ▼                                             │
┌─────────────────────────────────────────────────────────────────────────────┐
│                            [API Server: Spring Boot]                         │
│                                                                              │
│  ┌──────────────────────┐    ┌──────────────────────┐                       │
│  │     NodeController   │    │  WebSocketConfig     │                       │
│  │  (API 진입점)        │    │  (STOMP 설정)        │                       │
│  └──────────┬───────────┘    └──────────────────────┘                       │
│             │                                                                │
│             ▼                                                                │
│  ┌──────────────────────┐                                                   │
│  │     JobService       │  2. Job 생성 (DB INSERT)                          │
│  │  (Job 생성/관리)     │     - 상태: PENDING                               │
│  └──────────┬───────────┘     - idempotencyKey로 중복 방지                  │
│             │                                                                │
│             ▼                                                                │
│  ┌──────────────────────┐                                                   │
│  │ RedisStreamsDispatcher│ 3. Redis Streams에 메시지 발행                   │
│  │  (작업 큐 발행)       │    "ai:image" 스트림에 jobId 전송                │
│  └──────────────────────┘                                                   │
│                                                                              │
│  ┌──────────────────────┐                                                   │
│  │RedisStreamJobConsumer│ 4. 메시지 수신 → JobExecutor 호출                 │
│  │  (작업 큐 소비)       │                                                  │
│  └──────────┬───────────┘                                                   │
│             │                                                                │
│             ▼                                                                │
│  ┌──────────────────────┐                                                   │
│  │    JobExecutor       │  5. 상태 변경 (RUNNING) + Worker 호출             │
│  │  (Job 실행 진입점)   │                                                   │
│  └──────────┬───────────┘                                                   │
│             │                                                                │
│             ├──────────────────┬──────────────────┐                         │
│             ▼                  ▼                  ▼                         │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐                │
│  │ImageGenWorker   │ │VideoGenWorker   │ │MergeWorker      │                │
│  │(이미지 생성)    │ │(영상 생성)      │ │(FFmpeg 병합)    │                │
│  └────────┬────────┘ └────────┬────────┘ └────────┬────────┘                │
│           │                   │                   │                          │
│           ▼                   ▼                   ▼                          │
│  ┌─────────────────────────────────────────────────────────┐                │
│  │                    AI Clients / FFmpeg                   │                │
│  │  - GeminiImageClient (Google Gemini API)                 │                │
│  │  - VeoClient (Google Veo API)                            │                │
│  │  - FFmpeg (영상 병합)                                    │                │
│  └─────────────────────────────────────────────────────────┘                │
│                                                                              │
│  6. 결과 저장                                                                │
│     - 파일: /uploads/ai/images/...                                          │
│     - DB: assets 테이블에 메타데이터 저장                                   │
│     - Job 상태: SUCCEEDED                                                   │
│                                                                              │
│  ┌──────────────────────┐                                                   │
│  │WebSocketJobPublisher │  7. job.done 이벤트 발행                          │
│  │  (실시간 알림)       │     /topic/projects/{projectId}                   │
│  └──────────────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              [Infrastructure]                                │
│                                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────────────────┐            │
│  │    MySQL    │   │    Redis    │   │     Local Storage       │            │
│  │  - jobs     │   │  - Streams  │   │  /uploads/ai/images/    │            │
│  │  - assets   │   │  - ai:image │   │  /uploads/ai/videos/    │            │
│  │  - nodes    │   │  - ai:video │   │  /uploads/exports/      │            │
│  └─────────────┘   └─────────────┘   └─────────────────────────┘            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Job 시스템

### 3.1 Job이란?

Job은 **하나의 AI 생성 작업**을 나타내는 도메인 모델입니다.

**파일 위치**: `job/domain/Job.java`

```java
@Getter
@Builder
public class Job {
    private Long id;
    private Long projectId;      // 어느 프로젝트의 작업인지
    private Long sceneId;        // 어느 씬의 작업인지 (nullable)
    private Long nodeId;         // 어느 노드의 작업인지 (nullable)

    private JobType type;        // IMAGE_GENERATION, VIDEO_GENERATION, PROJECT_MERGE 등
    private JobStatus status;    // PENDING, RUNNING, SUCCEEDED, FAILED

    private String idempotencyKey;  // 중복 요청 방지 키
    private String requestJson;     // 요청 파라미터 (프롬프트, 설정 등)

    private Long resultAssetId;     // 성공 시 생성된 Asset ID
    private String errorMessage;    // 실패 시 에러 메시지

    private Integer retryCount;     // 재시도 횟수
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

### 3.2 Job 타입 (JobType)

```java
public enum JobType {
    IMAGE_GENERATION,   // 이미지 생성 (Gemini API)
    VIDEO_GENERATION,   // 영상 생성 (Veo API)
    SCENE_MERGE,        // 씬 내 영상 병합
    PROJECT_MERGE       // 프로젝트 전체 영상 병합
}
```

### 3.3 Job 상태 흐름 (JobStatus)

```
     ┌─────────────────────────────────────────────────┐
     │                                                 │
     ▼                                                 │
┌──────────┐      ┌──────────┐      ┌──────────────┐  │
│ PENDING  │ ───▶ │ RUNNING  │ ───▶ │  SUCCEEDED   │  │
│ (대기중) │      │ (실행중) │      │  (성공)      │  │
└──────────┘      └────┬─────┘      └──────────────┘  │
                       │                              │
                       │ 실패 시                      │ 재시도 (requeue)
                       ▼                              │
                 ┌──────────┐                         │
                 │  FAILED  │ ────────────────────────┘
                 │  (실패)  │
                 └──────────┘
```

### 3.4 Idempotency (멱등성)

**문제**: 사용자가 "생성" 버튼을 여러 번 클릭하면 같은 작업이 중복 실행됩니다.

**해결**: `idempotencyKey`를 사용해서 중복 요청을 방지합니다.

```java
// JobService.java
@Transactional
public Job createAndEnqueue(JobCreateRequest request, boolean requeueIfExisting) {
    String finalKey = resolveIdempotencyKey(request);

    // 1. 이미 같은 키로 Job이 존재하면?
    Job existing = jobMapper.findByIdempotencyKey(finalKey).orElse(null);
    if (existing != null) {
        // 기존 Job 반환 (새로 만들지 않음)
        return existing;
    }

    // 2. 없으면 새 Job 생성
    return createAndDispatch(...);
}
```

**idempotencyKey 생성 규칙** (JobIdempotencyKey.java):
```
{projectId}:{jobType}:{nodeId}:{sceneId}:{requestJson의 해시}
```

예시: `1:IMAGE_GENERATION:5::a1b2c3d4`

---

## 4. Redis Streams 기반 작업 큐

### 4.1 왜 Redis Streams를 사용하나요?

**문제**: AI API 호출은 오래 걸립니다 (10초~수 분). API 서버에서 직접 호출하면:
- 사용자가 응답을 오래 기다려야 함
- 서버가 다운되면 진행 중인 작업이 사라짐
- 동시 요청이 많으면 서버가 과부하됨

**해결**: 작업을 큐에 넣고, 별도의 Worker가 처리합니다.

```
[API 서버] ──(jobId)──▶ [Redis Streams] ──(jobId)──▶ [Consumer/Worker]
    │                                                      │
    │ 즉시 응답                                            │ 오래 걸리는 작업
    ▼                                                      ▼
"jobId: 123 받았어!"                              "이미지 생성 중..."
```

### 4.2 Redis Streams 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                        Redis Streams                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Stream: "ai:image"                                       │   │
│  │                                                          │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │ Consumer Group: "cg:ai-image"                      │  │   │
│  │  │                                                    │  │   │
│  │  │  Consumer: api-worker-uuid-1                       │  │   │
│  │  │  Consumer: api-worker-uuid-2                       │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  Messages:                                               │   │
│  │    [1-0] jobId=100, type=IMAGE_GENERATION               │   │
│  │    [2-0] jobId=101, type=IMAGE_GENERATION               │   │
│  │    [3-0] jobId=102, type=IMAGE_GENERATION               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Stream: "ai:video"                                       │   │
│  │  Consumer Group: "cg:ai-video"                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Stream: "media:merge"                                    │   │
│  │  Consumer Group: "cg:media-merge"                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 Dispatcher (작업 발행)

**파일 위치**: `job/dispatcher/RedisStreamsJobDispatcher.java`

```java
@Service
@Profile("redis-streams")  // redis-streams 프로필일 때만 활성화
public class RedisStreamsJobDispatcher implements JobDispatcher {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void enqueue(Job job) {
        // 1. Job 타입에 따라 스트림 키 결정
        String streamKey = resolveStreamKey(job.getType());
        // IMAGE_GENERATION → "ai:image"
        // VIDEO_GENERATION → "ai:video"
        // SCENE_MERGE, PROJECT_MERGE → "media:merge"

        // 2. 메시지 발행
        Map<String, String> payload = Map.of(
            "jobId", job.getId().toString(),
            "type", job.getType().name(),
            "projectId", job.getProjectId().toString()
        );

        redisTemplate.opsForStream()
            .add(StreamRecords.string(payload).withStreamKey(streamKey));
    }
}
```

### 4.4 Consumer (작업 소비)

**파일 위치**: `job/stream/RedisStreamJobConsumer.java`

```java
@Component
@Profile("redis-streams")
public class RedisStreamJobConsumer {

    @PostConstruct
    public void start() {
        // 1. Consumer Group 생성 (없으면)
        createGroupIfMissing("ai:image", "cg:ai-image");
        createGroupIfMissing("ai:video", "cg:ai-video");
        createGroupIfMissing("media:merge", "cg:media-merge");

        // 2. 각 스트림에 리스너 등록
        registerConsumer("ai:image", "cg:ai-image");
        registerConsumer("ai:video", "cg:ai-video");
        registerConsumer("media:merge", "cg:media-merge");

        container.start();
    }

    private void handleMessage(String streamKey, String group,
                               MapRecord<String, String, String> message) {
        try {
            // 3. 메시지에서 jobId 추출
            long jobId = Long.parseLong(message.getValue().get("jobId"));

            // 4. JobExecutor 호출 (실제 작업 수행)
            jobExecutor.execute(jobId);

        } finally {
            // 5. ACK - "이 메시지 처리 완료"
            redisTemplate.opsForStream()
                .acknowledge(streamKey, group, message.getId());
        }
    }
}
```

### 4.5 Stream 설정

**파일 위치**: `job/stream/JobStreamProperties.java`

```java
@ConfigurationProperties(prefix = "job.streams")
public class JobStreamProperties {
    private String imageStreamKey = "ai:image";       // 이미지 스트림 키
    private String videoStreamKey = "ai:video";       // 영상 스트림 키
    private String mergeStreamKey = "media:merge";    // 병합 스트림 키

    private String imageGroup = "cg:ai-image";        // 이미지 컨슈머 그룹
    private String videoGroup = "cg:ai-video";        // 영상 컨슈머 그룹
    private String mergeGroup = "cg:media-merge";     // 병합 컨슈머 그룹

    private String consumerPrefix = "api-worker";     // 컨슈머 이름 prefix
    private long pollTimeoutMs = 1000;                // 폴링 타임아웃
}
```

---

## 5. Worker 컴포넌트

### 5.1 JobExecutor (작업 실행 진입점)

**파일 위치**: `job/service/JobExecutor.java`

모든 작업 실행의 진입점입니다. Consumer가 이 클래스를 호출합니다.

```java
@Service
public class JobExecutor {

    public void execute(Long jobId) {
        // 1. Job 조회
        Job job = jobMapper.findById(jobId).orElse(null);

        // 2. 이미 완료된 경우 스킵 (중복 실행 방지)
        if (job.isSucceeded()) {
            return;
        }

        // 3. 실행 가능 상태 확인
        if (!job.isExecutable(maxRetryCount)) {
            return;
        }

        // 4. RUNNING으로 상태 변경 (낙관적 락)
        boolean started = markRunning(jobId);
        if (!started) {
            return;  // 다른 워커가 먼저 가져감
        }

        try {
            // 5. 노드 상태 업데이트 (RUNNING)
            updateNodeStatusIfApplicable(job, NodeStatus.RUNNING);

            // 6. 타입별 Worker 호출
            ExecutionResult result = executeByType(job);

            // 7. 성공 처리
            markSucceeded(jobId, result.resultAssetId());
            updateNodeStatusIfApplicable(job, NodeStatus.SUCCEEDED);

            // 8. WebSocket으로 완료 알림
            jobEventPublisher.publishDone(jobId);

        } catch (Exception e) {
            // 9. 실패 처리
            markFailed(jobId, e.getMessage());
            updateNodeStatusIfApplicable(job, NodeStatus.FAILED);
            jobEventPublisher.publishFailed(jobId);
        }
    }

    private ExecutionResult executeByType(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION -> imageWorker.execute(job);
            case VIDEO_GENERATION -> videoWorker.execute(job);
            case SCENE_MERGE, PROJECT_MERGE -> mergeWorker.execute(job);
        };
    }
}
```

### 5.2 ImageGenerationWorker (이미지 생성)

**파일 위치**: `worker/image/ImageGenerationWorker.java`

```java
@Component
public class ImageGenerationWorker {

    private final GeminiImageClient geminiImageClient;  // AI API 클라이언트
    private final LocalImageStorage localImageStorage;  // 파일 저장
    private final AssetRegistrar assetRegistrar;        // DB에 메타데이터 저장
    private final JobRequestParser jobRequestParser;    // JSON 파싱

    public ExecutionResult execute(Job job) {
        // 1. 요청 JSON 파싱 (프롬프트, 설정 추출)
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());
        // request.prompt() → "귀여운 고양이 그려줘"
        // request.settings() → { "aspectRatio": "16:9" }

        // 2. Gemini API 호출 → 이미지 바이트 반환
        GeminiImageResult result = geminiImageClient.generateImage(
            request.prompt(),
            request.settings()
        );

        // 3. 파일 저장 (로컬 디스크)
        // /uploads/ai/images/project-{projectId}/job-{jobId}.png
        StoredAsset storedAsset = localImageStorage.save(
            job.getProjectId(),
            job.getId(),
            result.bytes()
        );

        // 4. DB에 Asset 메타데이터 저장
        Long assetId = assetRegistrar.registerLocalAsset(
            job,
            storedAsset,
            AssetType.IMAGE,
            result.contentType()
        );

        // 5. 결과 반환
        return new ExecutionResult(assetId, storedAsset.storageKey());
    }
}
```

### 5.3 VideoGenerationWorker (영상 생성)

**파일 위치**: `worker/video/VideoGenerationWorker.java`

구조는 ImageGenerationWorker와 동일합니다.

```java
@Component
public class VideoGenerationWorker {

    public ExecutionResult execute(Job job) {
        // 1. 요청 파싱
        ParsedJobRequest request = jobRequestParser.parse(job.getRequestJson());

        // 2. Veo API 호출 → 영상 바이트 반환
        VeoResult result = veoClient.generateVideo(new VeoRequest(
            request.prompt(),
            request.settings()
        ));

        // 3. 파일 저장
        StoredAsset storedAsset = localVideoStorage.save(
            job.getProjectId(),
            job.getId(),
            result.bytes()
        );

        // 4. Asset 등록
        Long assetId = assetRegistrar.registerLocalAsset(
            job,
            storedAsset,
            AssetType.VIDEO,
            result.contentType()
        );

        return new ExecutionResult(assetId, storedAsset.storageKey());
    }
}
```

### 5.4 MergeWorker (영상 병합)

**파일 위치**: `worker/merge/MergeWorker.java`

FFmpeg를 사용해서 여러 영상을 하나로 합칩니다.

```java
@Component
public class MergeWorker {

    public ExecutionResult execute(Job job) {
        Long projectId = job.getProjectId();

        // 1. 확정(confirmed)된 VideoNode들의 영상 파일 경로 조회
        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesByProjectId(projectId);

        // 2. 입력 파일 경로 리스트
        List<Path> inputPaths = resolveInputPaths(rows);
        // [/uploads/ai/videos/node-1.mp4, /uploads/ai/videos/node-2.mp4, ...]

        // 3. 출력 파일 경로
        Path outputPath = resolveExportPath(projectId);
        // /uploads/exports/1/final.mp4

        // 4. FFmpeg concat 리스트 파일 생성
        Path concatList = createConcatListFile(inputPaths, outputPath.getParent());
        // concat-xxxx.txt:
        // file '/uploads/ai/videos/node-1.mp4'
        // file '/uploads/ai/videos/node-2.mp4'

        // 5. FFmpeg 실행
        runFfmpeg(concatList, outputPath, keepAudio);
        // ffmpeg -y -f concat -safe 0 -i concat.txt -c:v libx264 ... final.mp4

        return new ExecutionResult(null, null);  // 병합은 Asset 없음
    }
}
```

---

## 6. AI 클라이언트

### 6.1 GeminiImageClient (이미지 생성)

**파일 위치**: `ai/gemini/GeminiImageClient.java`

Google Gemini API를 사용해서 이미지를 생성합니다.

```java
@Component
public class GeminiImageClient {

    public GeminiImageResult generateImage(String prompt, Map<String, Object> settings) {
        // 1. Stub 모드 확인 (개발용 - 실제 API 호출 없이 테스트 이미지 반환)
        if (imageProperties.isStub()) {
            return new GeminiImageResult(AiStubAssets.stubPngBytes(), "image/png");
        }

        // 2. 요청 설정 빌드
        GenerateContentConfig config = GenerateContentConfig.builder()
            .responseModalities(List.of("TEXT", "IMAGE"))  // 이미지 응답 요청
            .httpOptions(HttpOptions.builder().timeout(60000).build())
            .imageConfig(ImageConfig.builder()
                .aspectRatio(settings.get("aspectRatio"))  // 예: "16:9"
                .build())
            .build();

        // 3. API 호출
        GenerateContentResponse response = clientProvider.getClient()
            .models
            .generateContent("gemini-2.0-flash-preview-image-generation", prompt, config);

        // 4. 응답에서 이미지 바이트 추출
        for (var part : response.parts()) {
            Blob blob = part.inlineData().orElse(null);
            if (blob != null) {
                byte[] data = blob.data().orElse(null);
                String mimeType = blob.mimeType().orElse("image/png");
                return new GeminiImageResult(data, mimeType);
            }
        }

        throw new AiProviderException("이미지 생성 실패");
    }
}
```

### 6.2 VeoClient (영상 생성)

**파일 위치**: `ai/veo/VeoClient.java`

Google Veo API를 사용해서 영상을 생성합니다.

```java
@Component
public class VeoClient {

    public VeoResult generateVideo(VeoRequest request) {
        // 1. Stub 모드 확인
        if (veoProperties.isStub()) {
            return new VeoResult(AiStubAssets.stubMp4Bytes(), "video/mp4");
        }

        // 2. 요청 설정 빌드
        GenerateVideosSource source = GenerateVideosSource.builder()
            .prompt(request.prompt())
            .build();

        GenerateVideosConfig config = GenerateVideosConfig.builder()
            .numberOfVideos(1)
            .durationSeconds(5)      // 영상 길이
            .aspectRatio("16:9")     // 화면 비율
            .build();

        // 3. 비동기 작업 시작
        GenerateVideosOperation operation = clientProvider.getClient()
            .models
            .generateVideos("veo-3.0-generate-001", source, config);

        // 4. 완료될 때까지 폴링
        GenerateVideosOperation completed = pollOperation(operation);

        // 5. 결과에서 영상 바이트 추출
        return parseOperationResult(completed);
    }

    private GenerateVideosOperation pollOperation(GenerateVideosOperation operation) {
        long deadline = System.currentTimeMillis() + 120000;  // 2분 타임아웃

        while (System.currentTimeMillis() < deadline) {
            // 상태 조회
            operation = clientProvider.getClient()
                .operations
                .getVideosOperation(operation);

            // 완료 확인
            if (operation.done().orElse(false)) {
                return operation;
            }

            Thread.sleep(2000);  // 2초 대기
        }

        throw new AiProviderException("영상 생성 타임아웃");
    }
}
```

### 6.3 Stub 모드

개발/테스트 시 실제 AI API를 호출하지 않고 미리 준비된 샘플 파일을 반환합니다.

**설정 (application.yml)**:
```yaml
ai:
  gemini:
    image:
      stub: true   # true면 실제 API 호출 안 함
  veo:
    stub: true
```

---

## 7. WebSocket 실시간 알림

### 7.1 왜 WebSocket을 사용하나요?

AI 생성 작업은 오래 걸립니다. 프론트엔드가 완료 여부를 알려면:

**폴링 방식** (비효율적):
```javascript
// 매 1초마다 서버에 물어봄
setInterval(() => {
    fetch(`/api/ai/jobs/${jobId}`)  // "아직 끝났어?"
}, 1000);
```

**WebSocket 방식** (효율적):
```javascript
// 서버가 완료되면 알려줌
stompClient.subscribe('/topic/projects/123', (message) => {
    if (message.type === 'job.done') {
        // 완료 처리
    }
});
```

### 7.2 WebSocket 설정

**파일 위치**: `global/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS 폴백 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 prefix: /topic/...
        registry.enableSimpleBroker("/topic");
        // 메시지 전송 prefix: /app/...
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### 7.3 이벤트 발행

**파일 위치**: `job/event/ProjectEventWebSocketPublisher.java`

```java
@Component
public class ProjectEventWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void jobDone(Long projectId, JobResponse response) {
        // /topic/projects/123 구독자들에게 메시지 전송
        publish(projectId, new JobEventMessage("job.done", response));
    }

    public void jobFailed(Long projectId, JobResponse response) {
        publish(projectId, new JobEventMessage("job.failed", response));
    }

    private void publish(Long projectId, JobEventMessage message) {
        messagingTemplate.convertAndSend(
            "/topic/projects/" + projectId,
            message
        );
    }
}
```

### 7.4 프론트엔드 연동 예시

```javascript
// WebSocket 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // 프로젝트 이벤트 구독
    stompClient.subscribe('/topic/projects/123', (message) => {
        const event = JSON.parse(message.body);

        if (event.type === 'job.done') {
            // 성공 - 결과 표시
            showResult(event.payload.resultUrl);
        } else if (event.type === 'job.failed') {
            // 실패 - 에러 표시
            showError(event.payload.errorMessage);
        }
    });
});
```

### 7.5 메시지 형식

```json
{
    "type": "job.done",
    "payload": {
        "id": 123,
        "type": "IMAGE_GENERATION",
        "status": "SUCCEEDED",
        "projectId": 1,
        "sceneId": 5,
        "nodeId": 10,
        "resultUrl": "/files/ai/images/project-1/job-123.png",
        "createdAt": "2024-01-15T10:30:00",
        "finishedAt": "2024-01-15T10:30:15"
    }
}
```

---

## 8. 전체 흐름 예시

### 8.1 이미지 생성 요청 ~ 결과 수신

```
┌────────────────────────────────────────────────────────────────────────────┐
│ 1. [Frontend] POST /api/nodes/10/generate                                  │
│    Body: { "prompt": "귀여운 고양이", "settings": { "aspectRatio": "1:1" } }│
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 2. [NodeController.generateNode]                                           │
│    - 사용자 권한 확인                                                       │
│    - NodeService.generateNode() 호출                                       │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 3. [NodeService.generateNode]                                              │
│    - 노드 조회 (type=MASTER → IMAGE_GENERATION)                            │
│    - JobService.createAndEnqueue() 호출                                    │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 4. [JobService.createAndEnqueue]                                           │
│    - idempotencyKey 생성: "1:IMAGE_GENERATION:10::abc123"                  │
│    - 중복 확인 → 없으면 Job INSERT (status=PENDING)                        │
│    - JobCreatedEventPublisher.publish(jobId) → 트랜잭션 커밋 후 이벤트     │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 5. [JobDispatchListener.onJobCreated]                                      │
│    - RedisStreamsJobDispatcher.enqueue(job)                                │
│    - Redis Stream "ai:image"에 메시지 발행                                 │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 6. [Response] 202 Accepted                                                 │
│    { "jobId": 123 }                                                        │
│    → 여기서 HTTP 요청/응답 완료 (빠르게 응답)                               │
└────────────────────────────────────────────────────────────────────────────┘

                    ───── 비동기 처리 시작 ─────

┌────────────────────────────────────────────────────────────────────────────┐
│ 7. [RedisStreamJobConsumer.handleMessage]                                  │
│    - "ai:image" 스트림에서 메시지 수신                                     │
│    - jobId 추출                                                            │
│    - JobExecutor.execute(123) 호출                                         │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 8. [JobExecutor.execute]                                                   │
│    - Job 조회                                                              │
│    - 상태 변경: PENDING → RUNNING                                          │
│    - 노드 상태 변경: RUNNING                                               │
│    - ImageGenerationWorker.execute(job) 호출                               │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 9. [ImageGenerationWorker.execute]                                         │
│    - 요청 JSON 파싱                                                        │
│    - GeminiImageClient.generateImage() → Gemini API 호출 (10초~)           │
│    - LocalImageStorage.save() → 파일 저장                                  │
│    - AssetRegistrar.registerLocalAsset() → DB 저장                         │
│    - return ExecutionResult(assetId, storageKey)                           │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 10. [JobExecutor - 성공 처리]                                              │
│     - 상태 변경: RUNNING → SUCCEEDED                                       │
│     - 노드 상태 변경: SUCCEEDED                                            │
│     - 노드 contentUrl 업데이트                                             │
│     - WebSocketJobEventPublisher.publishDone(123)                          │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 11. [ProjectEventWebSocketPublisher.jobDone]                               │
│     - /topic/projects/1 로 메시지 전송                                     │
│     - { type: "job.done", payload: { jobId: 123, resultUrl: "...", ... } } │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 12. [Frontend - WebSocket 수신]                                            │
│     - job.done 이벤트 수신                                                 │
│     - resultUrl로 이미지 표시                                              │
│     - 노드 상태 배지 업데이트                                              │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 패키지 구조

```
com.itda.backend
├── ai/                          # AI 관련
│   ├── config/                  # AI 설정
│   │   ├── AiVertexConfig.java      # Vertex AI 공통 설정
│   │   └── AiVertexProperties.java  # 설정 프로퍼티
│   ├── gemini/                  # Gemini (이미지 생성)
│   │   ├── GeminiImageClient.java   # API 클라이언트
│   │   ├── GeminiImageProperties.java
│   │   └── GeminiImageResult.java
│   ├── veo/                     # Veo (영상 생성)
│   │   ├── VeoClient.java           # API 클라이언트
│   │   ├── VeoProperties.java
│   │   ├── VeoRequest.java
│   │   └── VeoResult.java
│   ├── AiProviderException.java # AI 예외
│   ├── AiStubAssets.java        # 테스트용 더미 데이터
│   └── GenAiClientProvider.java # GenAI SDK 클라이언트 제공
│
├── job/                         # Job 시스템
│   ├── controller/              # API
│   │   ├── JobController.java       # Job 상태 조회 API
│   │   └── dto/
│   │       ├── JobResponse.java
│   │       └── JobAcceptedResponse.java
│   ├── domain/                  # 도메인
│   │   ├── Job.java                 # Job 엔티티
│   │   ├── JobType.java             # Job 타입 enum
│   │   └── JobStatus.java           # Job 상태 enum
│   ├── repository/              # DB 접근
│   │   └── JobMapper.java           # MyBatis Mapper
│   ├── service/                 # 비즈니스 로직
│   │   ├── JobService.java          # Job CRUD
│   │   ├── JobExecutor.java         # Job 실행 진입점
│   │   ├── JobDispatcher.java       # Dispatcher 인터페이스
│   │   ├── JobEventPublisher.java   # 이벤트 발행 인터페이스
│   │   ├── WebSocketJobEventPublisher.java  # WebSocket 구현체
│   │   ├── JobResultResolver.java   # 결과 URL 생성
│   │   └── JobExecutionProperties.java
│   ├── dispatcher/              # Dispatcher 구현
│   │   ├── LocalAsyncJobDispatcher.java     # @Async 방식 (개발용)
│   │   └── RedisStreamsJobDispatcher.java   # Redis Streams 방식
│   ├── stream/                  # Redis Streams
│   │   ├── JobStreamProperties.java     # 스트림 설정
│   │   └── RedisStreamJobConsumer.java  # 메시지 소비
│   └── event/                   # 이벤트
│       ├── JobCreatedEvent.java
│       ├── JobCreatedEventPublisher.java
│       ├── JobDispatchListener.java
│       ├── JobEventMessage.java
│       └── ProjectEventWebSocketPublisher.java
│
├── worker/                      # Worker
│   ├── image/                   # 이미지 생성 Worker
│   │   ├── ImageGenerationWorker.java
│   │   └── LocalImageStorage.java
│   ├── video/                   # 영상 생성 Worker
│   │   ├── VideoGenerationWorker.java
│   │   └── LocalVideoStorage.java
│   ├── merge/                   # 병합 Worker
│   │   └── MergeWorker.java
│   ├── ExecutionResult.java     # 실행 결과
│   ├── StoredAsset.java         # 저장된 파일 정보
│   ├── ParsedJobRequest.java    # 파싱된 요청
│   ├── JobRequestParser.java    # 요청 파서
│   ├── AssetRegistrar.java      # Asset DB 등록
│   └── LocalFileStorage.java    # 파일 저장 유틸
│
├── asset/                       # Asset 관리
│   ├── domain/
│   │   ├── Asset.java
│   │   └── AssetType.java
│   └── repository/
│       └── AssetMapper.java
│
├── global/                      # 공통
│   ├── config/
│   │   ├── WebSocketConfig.java     # WebSocket 설정
│   │   ├── AsyncConfig.java         # 비동기 설정
│   │   └── FileStorageProperties.java
│   └── ...
│
└── node/                        # Node (생성 트리거)
    ├── controller/
    │   └── NodeController.java      # POST /nodes/{id}/generate
    └── service/
        └── NodeService.java         # generateNode() → JobService 호출
```

---

## 마무리

이 문서를 읽고 나면:

1. **Job이 무엇인지** 이해할 수 있습니다
2. **API 요청부터 결과 수신까지의 전체 흐름**을 따라갈 수 있습니다
3. **Redis Streams가 왜 필요한지** 이해할 수 있습니다
4. **각 Worker가 무슨 일을 하는지** 알 수 있습니다
5. **WebSocket으로 어떻게 실시간 알림이 동작하는지** 이해할 수 있습니다

코드를 수정하거나 새 기능을 추가할 때 이 문서를 참고하세요.
