# Job 시스템 공통 뼈대 구현 계획서

> **작성일**: 2026-01-19  
> **개정일**: 2026-01-20  
> **담당자**: 강보승  
> **목표**: 팀원들이 병렬 작업할 수 있도록 Job 시스템 공통 뼈대 작성

---

## 0. 맥락 요약 (대화 히스토리)

### 배경
- AI Movie Studio 프로젝트의 3주차 MVP 개발 시작
- 강보승은 **Lead/Full-stack**으로 공통 뼈대 담당
- 팀원들(이용호-이미지Worker, 김은서-영상Worker, 장현준-병합Worker)이 병렬 작업하려면 **공통 계약**이 필요

### 완료된 작업
1. **Schema 통합 완료** (`sql/schema-local.sql`)
   - `schema-reference.sql` 기반으로 통합
   - 토큰 테이블 제거 (Redis 사용)
   - `users` 테이블에 `status` 추가
   - `generation_jobs` 테이블에 `node_id`, `retry_count` 추가
   - `video_clips` 테이블에 `is_confirmed` 추가

2. **아키텍처 결정사항**
   - Dispatcher 패턴: `LocalAsyncJobDispatcher` → `RedisStreamsJobDispatcher` 전환 가능 구조
   - Worker 방식: 단일 앱 + 프로파일 분리 (B 방식)
   - AI 호출: **Vertex AI Java SDK** 사용 (동기 호출 OK, Worker가 이미 비동기 구조)

### 참고 문서
- `docs/ai-movie-studio-md-pack-v3/04-architecture-core.md` - 코어 아키텍처
- `docs/ai-movie-studio-md-pack-v3/07-dispatcher-queue-worker-guide.md` - Dispatcher/Worker 가이드

---

## 1. 목표

### MVP 최소 요구사항
- **Job 생성**: API에서 Job 생성 → DB 저장 → Dispatcher로 전달
- **Job 상태 관리**: `PENDING` → `RUNNING` → `SUCCEEDED`/`FAILED`
- **Idempotency**: `Idempotency-Key`로 중복 요청 시 동일 Job 반환
- **완료 알림**: API 서버가 `job.done`/`job.failed` WS 이벤트 발행
- **팀원 unblock**: Worker 담당자들이 Job 모델/인터페이스를 사용해 각자 Worker 구현 가능

### 1.1 결정 사항 (2026-01-20)
- **Idempotency 키 정책**: 클라이언트가 키를 제공하지 않으면  
  `{projectId}:{jobType}:{target}:{requestHash}`로 자동 생성  
  → 같은 대상이라도 요청(JSON)이 다르면 새 Job 생성 허용  
  → requestJson은 JSON 정규화 후 해시하여 키 안정성 확보
- **중복 요청 처리**: 동일 키 Job이 있으면 기본은 기존 Job 반환  
  단, `PENDING` 또는 `FAILED` 상태면 **사용자 요청 시 재큐잉** 가능
- **API 노출 방식**: 같은 Job 재실행은 `/api/ai/jobs/{jobId}/requeue`  
  노드 재시도는 `/api/nodes/{id}/regenerate`로 새 버전 생성
- **재시도 정책**: **자동 재시도 없음**(비용 이슈)  
  `maxRetryCount`는 옵션(0 이하 = 제한 없음, 추후 정책 확정 시 적용)
- **상태 전이 안전성**: `RUNNING` 상태에서만 `SUCCEEDED/FAILED`로 전이하도록 조건부 업데이트 적용

### 1.2 리뷰/리팩터링 반영 사항
- **트랜잭션 경계 분리**: Worker 실행은 트랜잭션 밖에서 수행, 상태 업데이트는 짧은 트랜잭션으로 처리
- **성공/실패 업데이트 조건화**: 상태 덮어쓰기 방지 (RUNNING → SUCCEEDED/FAILED만 허용)
- **실패 처리 원자화**: 실패 시 `retry_count` 증가와 상태 변경을 단일 SQL로 처리
- **중복 키 경쟁 상황 처리**: Insert 충돌 시 기존 Job 재조회 후 반환

### 1.2.1 Idempotency 중복 경쟁 + 격리 수준 이슈 (중요)

#### 문제 요약
동일한 `idempotencyKey`로 동시에 요청이 들어오면 한 요청만 INSERT 성공,
다른 요청은 `DuplicateKeyException`이 발생한다.  
현재 구현은 catch에서 다시 조회해 기존 Job을 반환하려고 하지만,
DB 격리 수준이 `REPEATABLE_READ`인 경우 같은 트랜잭션 내 재조회가
새로 커밋된 행을 보지 못해 재조회가 실패할 수 있다.

#### 발생 시나리오 (요약)
1) 요청 A/B가 동시에 `findByIdempotencyKey` 실행 → 둘 다 없음
2) A insert 성공, B insert 실패 (`DuplicateKeyException`)
3) B가 같은 트랜잭션에서 재조회 → 스냅샷이라 A의 row가 안 보일 수 있음
4) 결국 예외 전파 → idempotency 보장이 깨짐

#### 영향
- 중복 요청에 대해 기존 Job 반환 대신 500 발생 가능
- 멱등성 보장 실패 (클라이언트 재시도 시도에도 실패 가능)

#### 권장 해결책
- **권장**: `DuplicateKeyException` catch 블록의 재조회만  
  `REQUIRES_NEW + READ_COMMITTED`로 분리하여 최신 커밋을 보장
- 대안: `createAndEnqueue` 전체를 `READ_COMMITTED`로 낮춤
- 고급 대안: idempotency 전용 테이블/락 또는 DB upsert 전략

### 1.3 Idempotency Key 개념 및 필요성

#### 1.3.1 Idempotency Key란?
**Idempotency Key (멱등성 키)**는 "똑같은 요청을 여러 번 보내도 결과가 한 번만 적용되도록" 보장하는 안전장치이다.
네트워크 지연, 사용자 버튼 연타, 클라이언트 재시도 등으로 동일 요청이 중복 전송될 때 **서버가 이를 감지하고 중복 처리를 방지**한다.

#### 1.3.2 왜 필요한가? (문제 상황)

**시나리오**: 사용자가 "이미지 생성" 버튼을 눌렀는데 응답이 느려서 3번 연타함

| Idempotency Key 없음 | Idempotency Key 있음 |
|---|---|
| 서버가 요청 3개를 모두 처리 | 첫 요청만 처리, 나머지는 기존 Job 반환 |
| AI API 3번 호출 (비용 3배 💸) | AI API 1번만 호출 |
| DB에 중복 Job 3개 생성 | DB에 Job 1개만 존재 |

#### 1.3.3 동작 방식

```
[첫 번째 요청]
  클라이언트 → POST /api/nodes/5/generate (키: abc-123)
  서버: "abc-123 키가 DB에 없음 → 새 Job 생성"
  응답: { jobId: 100, status: PENDING }

[두 번째 요청 (연타)]
  클라이언트 → POST /api/nodes/5/generate (키: abc-123)
  서버: "abc-123 키가 이미 있음 → 기존 Job 반환"
  응답: { jobId: 100, status: PENDING }  ← 같은 Job
```

#### 1.3.4 키 생성 규칙 (`JobIdempotencyKey`)

클라이언트가 `Idempotency-Key` 헤더를 제공하지 않으면 서버가 자동 생성:

```
{projectId}:{jobType}:{target}:{requestHash}

예시: 1:IMAGE_GENERATION:node:5:a8f9c7e3...
     └─프로젝트 └─타입       └─노드ID └─요청JSON 해시
```

- **같은 노드, 같은 설정** → 같은 키 → 중복 방지
- **같은 노드, 다른 설정** → 다른 키 → 새 Job 생성 허용
※ requestJson은 JSON 정규화 후 해시하여 키 순서/공백 차이를 흡수

### 팀원별 의존성
| 팀원 | 역할 | 이 작업에서 필요한 것 |
|---|---|---|
| 이용호 | Image Worker | Job 모델, JobDispatcher, Streams 스키마 |
| 김은서 | Video Worker | Job 모델, JobDispatcher |
| 장현준 | Merge Worker, Node/Scene CRUD | Job 모델, Project/Scene/Node 엔티티 |

---

## 2. 패키지 구조

```
src/main/java/com/itda/backend/
├── auth/                    # ✅ 이미 있음
├── global/                  # ✅ 이미 있음
│
├── job/                     # 🆕 신규 - 강보승 담당
│   ├── domain/
│   │   ├── Job.java
│   │   ├── JobType.java
│   │   └── JobStatus.java
│   ├── repository/
│   │   └── JobMapper.java
│   ├── service/
│   │   ├── JobService.java
│   │   └── JobDispatcher.java (인터페이스)
│   │   ├── JobIdempotencyKey.java
│   │   └── JobResultResolver.java
│   │   ├── JobEventPublisher.java
│   │   └── WebSocketJobEventPublisher.java
│   ├── dispatcher/
│   │   ├── LocalAsyncJobDispatcher.java
│   │   └── RedisStreamsJobDispatcher.java (나중에)
│   ├── event/
│   │   ├── JobCreatedEvent.java
│   │   ├── JobCreatedEventPublisher.java
│   │   └── JobDispatchListener.java
│   └── controller/
│       ├── JobController.java
│       └── dto/
│           └── JobResponse.java
│
├── worker/                  # 🆕 신규 - 각 팀원 담당
│   ├── common/
│   │   └── WorkerProperties.java
│   ├── image/               # 이용호
│   │   └── ImageGenerationWorker.java
│   ├── video/               # 김은서
│   │   └── VideoGenerationWorker.java
│   └── merge/               # 장현준
│       └── MergeWorker.java
│
├── project/                 # 🆕 신규 - 장현준 담당 (스켈레톤만 강보승)
├── scene/                   # 🆕 신규 - 장현준 담당 (스켈레톤만 강보승)
└── node/                    # 🆕 신규 - 강보승 담당
```

---

## 3. 상세 구현 가이드

### 3.1 Job 도메인

#### 3.1.1 JobStatus.java
```java
package com.itda.backend.job.domain;

public enum JobStatus {
    PENDING,    // 큐에 들어감 (아직 실행 안 함)
    RUNNING,    // Worker가 가져가 실행 중
    SUCCEEDED,  // 성공
    FAILED      // 실패
}
```

#### 3.1.2 JobType.java
```java
package com.itda.backend.job.domain;

public enum JobType {
    IMAGE_GENERATION,   // 이미지 생성 (Imagen)
    VIDEO_GENERATION,   // 영상 생성 (Veo)
    SCENE_MERGE,        // 씬 병합 (FFmpeg)
    PROJECT_MERGE       // 프로젝트 전체 병합 (FFmpeg)
}
```

#### 3.1.3 Job.java
```java
package com.itda.backend.job.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    private Long id;
    private Long projectId;
    private Long sceneId;
    private Long nodeId;
    
    private JobType type;

    private String idempotencyKey; // 중복 요청 방지 키 (optional)
    
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;
    
    private String requestJson;      // 입력 파라미터 (JSON 문자열)
    private Long resultAssetId;      // 결과 파일 ID
    private String errorMessage;     // 실패 시 에러 메시지
    
    @Builder.Default
    private Integer retryCount = 0;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    // === 상태 전이 메서드 ===
    
    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }
    
    public void markSucceeded(Long resultAssetId) {
        this.status = JobStatus.SUCCEEDED;
        this.resultAssetId = resultAssetId;
        this.finishedAt = LocalDateTime.now();
    }
    
    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
    
    public void incrementRetry() {
        this.retryCount++;
    }
    
    public boolean isSucceeded() {
        return this.status == JobStatus.SUCCEEDED;
    }
    
    public boolean canRetry(int maxRetryCount) {
        return this.retryCount < maxRetryCount && this.status == JobStatus.FAILED;
    }
}
```

---

### 3.2 JobMapper (MyBatis)

#### 3.2.1 JobMapper.java (인터페이스)
```java
package com.itda.backend.job.repository;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface JobMapper {
    
    void insert(Job job);
    
    Optional<Job> findById(@Param("id") Long id);

    Optional<Job> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    
    List<Job> findByProjectId(@Param("projectId") Long projectId);
    
    List<Job> findByStatus(@Param("status") JobStatus status);
    
    int updateStatusIfExpected(@Param("id") Long id,
                               @Param("expectedStatuses") List<JobStatus> expectedStatuses,
                               @Param("status") JobStatus status,
                               @Param("errorMessage") String errorMessage);
    
    int updateResultIfRunning(@Param("id") Long id,
                              @Param("resultAssetId") Long resultAssetId);
    
    int updateFailureIfRunning(@Param("id") Long id,
                               @Param("errorMessage") String errorMessage);
}
```

#### 3.2.2 JobMapper.xml
파일 위치: `src/main/resources/mapper/JobMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.itda.backend.job.repository.JobMapper">

    <resultMap id="JobResultMap" type="com.itda.backend.job.domain.Job">
        <id property="id" column="id"/>
        <result property="projectId" column="project_id"/>
        <result property="sceneId" column="scene_id"/>
        <result property="nodeId" column="node_id"/>
        <result property="type" column="job_type" 
                javaType="com.itda.backend.job.domain.JobType"/>
        <result property="idempotencyKey" column="idempotency_key"/>
        <result property="status" column="status" 
                javaType="com.itda.backend.job.domain.JobStatus"/>
        <result property="requestJson" column="request_json"/>
        <result property="resultAssetId" column="result_asset_id"/>
        <result property="errorMessage" column="error_message"/>
        <result property="retryCount" column="retry_count"/>
        <result property="createdAt" column="created_at"/>
        <result property="startedAt" column="started_at"/>
        <result property="finishedAt" column="finished_at"/>
    </resultMap>

    <insert id="insert" parameterType="com.itda.backend.job.domain.Job"
            useGeneratedKeys="true" keyProperty="id" keyColumn="id">
        INSERT INTO generation_jobs (
            project_id, scene_id, node_id, job_type, idempotency_key, status,
            request_json, retry_count
        ) VALUES (
            #{projectId}, #{sceneId}, #{nodeId}, #{type}, #{idempotencyKey}, #{status},
            #{requestJson}, #{retryCount}
        )
    </insert>

    <select id="findById" resultMap="JobResultMap">
        SELECT * FROM generation_jobs WHERE id = #{id}
    </select>

    <select id="findByIdempotencyKey" resultMap="JobResultMap">
        SELECT * FROM generation_jobs WHERE idempotency_key = #{idempotencyKey}
    </select>

    <select id="findByProjectId" resultMap="JobResultMap">
        SELECT * FROM generation_jobs 
        WHERE project_id = #{projectId}
        ORDER BY created_at DESC
    </select>

    <select id="findByStatus" resultMap="JobResultMap">
        SELECT * FROM generation_jobs 
        WHERE status = #{status}
        ORDER BY created_at ASC
    </select>

    <update id="updateStatusIfExpected">
        UPDATE generation_jobs
        SET status = #{status},
            error_message = #{errorMessage},
            started_at = CASE WHEN #{status} = 'RUNNING' THEN NOW() ELSE started_at END,
            finished_at = CASE WHEN #{status} IN ('SUCCEEDED', 'FAILED') THEN NOW() ELSE finished_at END
        WHERE id = #{id}
          AND status IN
          <foreach collection="expectedStatuses" item="s" open="(" separator="," close=")">
            #{s}
          </foreach>
    </update>

    <update id="updateResultIfRunning">
        UPDATE generation_jobs
        SET result_asset_id = #{resultAssetId},
            status = 'SUCCEEDED',
            finished_at = NOW()
        WHERE id = #{id}
          AND status = 'RUNNING'
    </update>

    <update id="updateFailureIfRunning">
        UPDATE generation_jobs
        SET status = 'FAILED',
            error_message = #{errorMessage},
            retry_count = retry_count + 1,
            finished_at = NOW()
        WHERE id = #{id}
          AND status = 'RUNNING'
    </update>

</mapper>
```

---

### 3.3 JobDispatcher (핵심 인터페이스)

#### 3.3.1 JobDispatcher.java (인터페이스)
```java
package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;

/**
 * Job을 실행 큐에 넣는 역할
 * 구현체: LocalAsyncJobDispatcher (개발용), RedisStreamsJobDispatcher (운영용)
 */
public interface JobDispatcher {
    
    /**
     * Job을 큐에 추가
     * @param job 실행할 Job (이미 DB에 저장된 상태)
     */
    void enqueue(Job job);
}
```

#### 3.3.2 LocalAsyncJobDispatcher.java (초기 개발용)
```java
package com.itda.backend.job.dispatcher;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.service.JobDispatcher;
import com.itda.backend.job.service.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 로컬 개발용 Dispatcher
 * @Async로 비동기 실행
 * 나중에 RedisStreamsJobDispatcher로 교체 가능
 */
@Slf4j
@Service
@Profile("!redis-streams")  // redis-streams 프로파일이 아닐 때 활성화
@RequiredArgsConstructor
public class LocalAsyncJobDispatcher implements JobDispatcher {

    private final JobExecutor jobExecutor;

    @Override
    @Async("jobExecutorPool")
    public void enqueue(Job job) {
        log.info("[LocalDispatcher] Job enqueued: id={}, type={}", job.getId(), job.getType());
        jobExecutor.execute(job.getId());
    }
}
```

#### 3.3.3 AsyncConfig.java (스레드풀 설정)
```java
package com.itda.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "jobExecutorPool")
    public Executor jobExecutorPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-executor-");
        executor.initialize();
        return executor;
    }
}
```

#### 3.3.4 JobExecutionProperties (재시도 설정)
`application.yml` 예시:
```yaml
job:
  execution:
    max-retry-count: 0  # 0 이하: 제한 없음 (추후 정책 확정 시 설정)
```

#### 3.3.5 JobCreatedEvent + DispatchListener (AFTER_COMMIT)
DB 트랜잭션이 커밋되기 전에 enqueue하면 **DB 롤백 + 큐 메시지 발행** 불일치가 생길 수 있으므로,
Job 생성 이후 **AFTER_COMMIT**에 Dispatcher가 실행되도록 분리합니다.

```java
package com.itda.backend.job.event;

public record JobCreatedEvent(Long jobId) {}
```

```java
package com.itda.backend.job.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobCreatedEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Long jobId) {
        publisher.publishEvent(new JobCreatedEvent(jobId));
    }
}
```

```java
package com.itda.backend.job.event;

import com.itda.backend.job.repository.JobMapper;
import com.itda.backend.job.service.JobDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobDispatchListener {

    private final JobMapper jobMapper;
    private final JobDispatcher jobDispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(JobCreatedEvent event) {
        jobMapper.findById(event.jobId()).ifPresent(job -> {
            jobDispatcher.enqueue(job);
            log.info("[JobDispatchListener] Job enqueued: id={}, type={}", job.getId(), job.getType());
        });
    }
}
```

#### 3.3.6 JobIdempotencyKey (헤더 없을 때 키 생성)
```java
package com.itda.backend.job.service;

import com.itda.backend.job.domain.JobType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class JobIdempotencyKey {
    private JobIdempotencyKey() {}

    public static String of(Long projectId, JobType type, Long nodeId, Long sceneId, String requestJson) {
        String target = nodeId != null
                ? "node:" + nodeId
                : (sceneId != null ? "scene:" + sceneId : "project");
        String requestHash = sha256Hex(requestJson == null ? "" : requestJson);
        return projectId + ":" + type.name() + ":" + target + ":" + requestHash;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}
```

---

### 3.4 JobExecutor (Worker 연결점)

> **정책 반영 포인트**
> - Worker 실행은 트랜잭션 밖에서 수행
> - 상태 업데이트는 짧은 트랜잭션으로 처리
> - `RUNNING` 상태에서만 `SUCCEEDED/FAILED`로 전이

```java
package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.repository.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job 실행의 진입점
 * 각 Worker는 이 클래스를 통해 호출됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutor {

    private final JobMapper jobMapper;
    private final JobEventPublisher jobEventPublisher;
    private final JobExecutionProperties jobExecutionProperties;
    // private final ImageGenerationWorker imageWorker;  // 이용호 구현
    // private final VideoGenerationWorker videoWorker;  // 김은서 구현
    // private final MergeWorker mergeWorker;            // 장현준 구현

    @Transactional
    public void execute(Long jobId) {
        Job job = jobMapper.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // 이미 완료된 경우 스킵 (중복 실행 방지)
        if (job.isSucceeded()) {
            log.info("[JobExecutor] Job already succeeded, skipping: id={}", jobId);
            return;
        }

        // 실행 가능 상태 확인 (PENDING 또는 재시도 가능한 FAILED)
        int maxRetryCount = jobExecutionProperties.getMaxRetryCount();
        if (!job.isExecutable(maxRetryCount)) {
            log.info("[JobExecutor] Job not executable, skipping: id={}", jobId);
            return;
        }

        // RUNNING 전환 (중복 실행 방지 - 기대 상태 체크)
        int updated = jobMapper.updateStatusIfExpected(
                jobId,
                List.of(JobStatus.PENDING, JobStatus.FAILED),
                JobStatus.RUNNING,
                null
        );
        if (updated == 0) {
            log.info("[JobExecutor] Job not eligible to run, skipping: id={}", jobId);
            return;
        }
        log.info("[JobExecutor] Job started: id={}, type={}", jobId, job.getType());

        try {
            // TODO: 타입별 Worker 호출 (팀원들이 구현)
            Long resultAssetId = executeByType(job);
            
            if (resultAssetId == null) {
                throw new IllegalStateException("Worker returned null resultAssetId");
            }

            // 성공 처리 (RUNNING 상태에서만 전이)
            int updated = jobMapper.updateResultIfRunning(jobId, resultAssetId);
            if (updated == 0) {
                log.warn("[JobExecutor] Job success ignored (status changed): id={}", jobId);
                return;
            }
            log.info("[JobExecutor] Job succeeded: id={}", jobId);
            jobEventPublisher.publishDone(jobId);
            
        } catch (Exception e) {
            // 실패 처리
            log.error("[JobExecutor] Job failed: id={}, error={}", jobId, e.getMessage(), e);
            int updated = jobMapper.updateFailureIfRunning(jobId, e.getMessage());
            if (updated == 0) {
                log.warn("[JobExecutor] Job failure ignored (status changed): id={}", jobId);
                return;
            }
            jobEventPublisher.publishFailed(jobId);
        }
    }

    private Long executeByType(Job job) {
        // TODO: 각 Worker 구현 후 주석 해제
        return switch (job.getType()) {
            case IMAGE_GENERATION -> {
                // yield imageWorker.execute(job);
                log.warn("[JobExecutor] IMAGE_GENERATION not implemented yet");
                yield null;
            }
            case VIDEO_GENERATION -> {
                // yield videoWorker.execute(job);
                log.warn("[JobExecutor] VIDEO_GENERATION not implemented yet");
                yield null;
            }
            case SCENE_MERGE, PROJECT_MERGE -> {
                // yield mergeWorker.execute(job);
                log.warn("[JobExecutor] MERGE not implemented yet");
                yield null;
            }
        };
    }
}
```

---

### 3.5 JobEventPublisher (WS 이벤트 브릿지)
API 서버가 **job.done/job.failed** 이벤트를 발행하는 책임을 갖습니다.
LocalAsync 실행 시엔 `JobExecutor`가 직접 호출하고,
Redis Streams 전환 시에는 Worker가 완료 콜백을 호출하면 동일 로직을 재사용합니다.

```java
package com.itda.backend.job.service;

public interface JobEventPublisher {
    void publishDone(Long jobId);
    void publishFailed(Long jobId);
}
```

```java
package com.itda.backend.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketJobEventPublisher implements JobEventPublisher {

    private final JobMapper jobMapper;
    private final JobResultResolver jobResultResolver;
    // private final ProjectEventWebSocketPublisher wsPublisher; // 실제 WS 브릿지 구현

    @Override
    public void publishDone(Long jobId) {
        Job job = jobMapper.findById(jobId).orElseThrow();
        String resultUrl = jobResultResolver.resolve(job);
        // wsPublisher.jobDone(job, resultUrl);
    }

    @Override
    public void publishFailed(Long jobId) {
        Job job = jobMapper.findById(jobId).orElseThrow();
        // wsPublisher.jobFailed(job);
    }
}
```

> **WebSocket 채널 규칙**
> - STOMP 엔드포인트: `/ws`
> - 구독 토픽: `/topic/projects/{projectId}`
> - 메시지 형식: `{ event: "job.done|job.failed", data: JobResponse }`

> Redis Streams 전환 시 Worker는 `POST /internal/jobs/{id}/complete` / `.../fail` 같은 내부 콜백을 호출하고,
> API 서버가 동일 로직으로 DB 업데이트 + WS 이벤트 발행을 수행한다.

---

### 3.6 JobService (비즈니스 로직)

> **정책 반영 포인트**
> - Idempotency Key는 `requestJson` 해시 포함 (JSON 정규화 후 해시)
> - 중복 키 경쟁 상황 발생 시 기존 Job 재조회 후 반환
> - `requeueIfExisting` 옵션으로 PENDING/FAILED Job 재큐잉 가능 (재시도 한도는 옵션)

```java
package com.itda.backend.job.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.repository.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobMapper jobMapper;
    private final JobCreatedEventPublisher jobCreatedEventPublisher;

    /**
     * 새 Job 생성 (Idempotency 적용) + 큐 전달 (AFTER_COMMIT)
     */
    @Transactional
    public Job createAndEnqueue(JobType type,
                                Long projectId,
                                Long sceneId,
                                Long nodeId,
                                String requestJson,
                                String idempotencyKey) {

        if (idempotencyKey != null) {
            return jobMapper.findByIdempotencyKey(idempotencyKey)
                    .orElseGet(() -> createAndDispatch(type, projectId, sceneId, nodeId, requestJson, idempotencyKey));
        }

        // 헤더가 없으면 target 기반으로 키를 만들어 중복 요청을 방지
        String derivedKey = JobIdempotencyKey.of(projectId, type, nodeId, sceneId, requestJson);
        return jobMapper.findByIdempotencyKey(derivedKey)
                .orElseGet(() -> createAndDispatch(type, projectId, sceneId, nodeId, requestJson, derivedKey));
    }

    private Job createAndDispatch(JobType type,
                                  Long projectId,
                                  Long sceneId,
                                  Long nodeId,
                                  String requestJson,
                                  String idempotencyKey) {
        Job job = Job.builder()
                .type(type)
                .projectId(projectId)
                .sceneId(sceneId)
                .nodeId(nodeId)
                .idempotencyKey(idempotencyKey)
                .requestJson(requestJson)
                .status(JobStatus.PENDING)
                .build();

        jobMapper.insert(job);
        log.info("[JobService] Job created: id={}, type={}", job.getId(), type);

        // 커밋 이후에 Dispatcher가 enqueue하도록 이벤트 발행
        jobCreatedEventPublisher.publish(job.getId());

        return job;
    }

    /**
     * Job 상태 조회
     */
    @Transactional(readOnly = true)
    public Job getJob(Long jobId) {
        return jobMapper.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
    }

    /**
     * 프로젝트의 모든 Job 조회
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByProject(Long projectId) {
        return jobMapper.findByProjectId(projectId);
    }

    /**
     * 특정 상태의 Job 목록 조회 (모니터링용)
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByStatus(JobStatus status) {
        return jobMapper.findByStatus(status);
    }
}
```

---

### 3.7 JobController (API)

```java
package com.itda.backend.job.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.job.controller.dto.JobResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Jobs", description = "AI 작업 상태 조회 API")
@RestController
@RequestMapping("/api/ai/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobResultResolver jobResultResolver;

    @Operation(summary = "Job 상태 조회", description = "Job ID로 작업 상태를 조회합니다.")
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable Long jobId) {
        Job job = jobService.getJob(jobId);
        String resultUrl = jobResultResolver.resolve(job);
        return ApiResponse.success(JobResponse.from(job, resultUrl));
    }

    @PostMapping("/{jobId}/requeue")
    public ResponseEntity<ApiResponse<JobResponse>> requeueJob(@PathVariable Long jobId) {
        Job job = jobService.requeueIfExecutable(jobId);
        String resultUrl = jobResultResolver.resolve(job);
        return ApiResponse.success(JobResponse.from(job, resultUrl));
    }
}
```

---

### 3.8 JobResultResolver (결과 URL 해석)
Job 결과는 `result_asset_id`로 저장되므로, API 응답에서는 presigned URL로 변환해 제공합니다.

```java
package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import org.springframework.stereotype.Component;

@Component
public class JobResultResolver {

    public String resolve(Job job) {
        if (job.getResultAssetId() == null) {
            return null;
        }
        // TODO: AssetService에서 presigned URL 생성
        return null;
    }
}
```

---

### 3.9 DTO

#### JobResponse.java
```java
package com.itda.backend.job.controller.dto;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;

import java.time.LocalDateTime;

public record JobResponse(
        Long jobId,
        JobType type,
        JobStatus status,
        Integer progress,     // MVP: 항상 null
        JobTarget target,
        String resultUrl,     // 성공 시 presigned URL
        JobError error,
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {
    public static JobResponse from(Job job, String resultUrl) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                null,
                JobTarget.from(job),
                resultUrl,
                JobError.from(job),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }
}

public record JobTarget(String type, Long id) {
    public static JobTarget from(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION, VIDEO_GENERATION -> new JobTarget("NODE", job.getNodeId());
            case SCENE_MERGE -> new JobTarget("SCENE", job.getSceneId());
            case PROJECT_MERGE -> new JobTarget("PROJECT", job.getProjectId());
        };
    }
}

public record JobError(String code, String message) {
    public static JobError from(Job job) {
        return job.getErrorMessage() == null ? null
                : new JobError("JOB_EXECUTION_FAILED", job.getErrorMessage());
    }
}
```

---

### 3.10 ErrorCode 추가

`global/response/ErrorCode.java`에 추가:

```java
// Job 관련
JOB_NOT_FOUND(404, "JOB_NOT_FOUND", "Job을 찾을 수 없습니다."),
JOB_ALREADY_RUNNING(409, "JOB_ALREADY_RUNNING", "이미 실행 중인 Job입니다."),
JOB_EXECUTION_FAILED(500, "JOB_EXECUTION_FAILED", "Job 실행 중 오류가 발생했습니다."),
```

---

## 4. 파일 생성 체크리스트

### 신규 파일 (강보승 담당)
- [ ] `job/domain/Job.java`
- [ ] `job/domain/JobType.java`
- [ ] `job/domain/JobStatus.java`
- [ ] `job/repository/JobMapper.java`
- [ ] `job/service/JobService.java`
- [ ] `job/service/JobDispatcher.java`
- [ ] `job/service/JobExecutor.java`
- [ ] `job/service/JobIdempotencyKey.java`
- [ ] `job/service/JobResultResolver.java`
- [ ] `job/service/JobEventPublisher.java`
- [ ] `job/service/WebSocketJobEventPublisher.java`
- [ ] `job/dispatcher/LocalAsyncJobDispatcher.java`
- [ ] `job/event/JobCreatedEvent.java`
- [ ] `job/event/JobCreatedEventPublisher.java`
- [ ] `job/event/JobDispatchListener.java`
- [ ] `job/controller/JobController.java`
- [ ] `job/controller/dto/JobResponse.java`
- [ ] `resources/mapper/JobMapper.xml`
- [ ] `global/config/AsyncConfig.java`

### 수정 파일
- [ ] `global/response/ErrorCode.java` - Job 관련 에러 코드 추가
- [ ] `src/main/resources/sql/schema-local.sql` - `idempotency_key` 컬럼/인덱스 추가

### Worker 스켈레톤 (선택 - 팀원용 가이드)
- [ ] `worker/common/WorkerProperties.java`
- [ ] `worker/image/ImageGenerationWorker.java` (이용호)
- [ ] `worker/video/VideoGenerationWorker.java` (김은서)
- [ ] `worker/merge/MergeWorker.java` (장현준)

---

## 5. 의존성 추가 (build.gradle.kts)

```kotlin
// Vertex AI (나중에 Worker 구현 시 필요)
implementation("com.google.cloud:google-cloud-vertexai:1.0.0")
```

---

## 6. 테스트 시나리오

### 6.1 단위 테스트
```java
@SpringBootTest
class JobServiceTest {
    
    @Autowired
    private JobService jobService;
    
    @Test
    void createAndEnqueue_ShouldCreateJob() {
        // given
        JobType type = JobType.IMAGE_GENERATION;
        Long projectId = 1L;
        String requestJson = "{\"prompt\": \"test\"}";
        String idempotencyKey = "proj1:IMAGE_GENERATION:node:10";
        
        // when
        Job job = jobService.createAndEnqueue(type, projectId, null, 10L, requestJson, idempotencyKey);
        
        // then
        assertThat(job.getId()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void createAndEnqueue_ShouldReturnSameJob_WhenIdempotencyKeyMatches() {
        // given
        String idempotencyKey = "proj1:IMAGE_GENERATION:node:10";
        jobService.createAndEnqueue(JobType.IMAGE_GENERATION, 1L, null, 10L, "{\"prompt\":\"a\"}", idempotencyKey);

        // when
        Job job = jobService.createAndEnqueue(JobType.IMAGE_GENERATION, 1L, null, 10L, "{\"prompt\":\"a\"}", idempotencyKey);

        // then
        assertThat(job.getId()).isNotNull();
    }
}
```

### 6.2 통합 테스트 (Swagger에서)
1. Job 생성 API 호출 (아직 없음 - Node API에서 호출)
2. `GET /api/ai/jobs/{jobId}`로 상태 확인
3. 상태가 `PENDING` → `RUNNING` → `SUCCEEDED`/`FAILED` 전이 확인
4. 프로젝트 WS에서 `job.done`/`job.failed` 이벤트 수신 확인

---

## 7. 다음 단계 (D2~D3)

### 강보승
- [ ] Node CRUD API 스켈레톤 (`/api/scenes/{sceneId}/nodes`)
- [ ] Node에서 Job 생성 연동 (`POST /api/nodes/{nodeId}/generate`)

### 이용호
- [ ] Redis Streams 설정
- [ ] `ImageGenerationWorker` 구현 (Gemini/Imagen 호출)

### 김은서
- [ ] `VideoGenerationWorker` 구현 (Veo 호출)

### 장현준
- [ ] Scene CRUD 완성
- [ ] `MergeWorker` 구현 (FFmpeg 호출)

---

## 8. 참고: Redis Streams 전환 시

### RedisStreamsJobDispatcher.java (나중에)
```java
@Slf4j
@Service
@Profile("redis-streams")
@RequiredArgsConstructor
public class RedisStreamsJobDispatcher implements JobDispatcher {

    private final StringRedisTemplate redis;

    @Override
    public void enqueue(Job job) {
        String streamKey = switch (job.getType()) {
            case IMAGE_GENERATION -> "ai:image";
            case VIDEO_GENERATION -> "ai:video";
            case SCENE_MERGE, PROJECT_MERGE -> "media:merge";
        };

        Map<String, String> fields = new HashMap<>();
        fields.put("jobId", job.getId().toString());
        fields.put("projectId", job.getProjectId().toString());
        fields.put("type", job.getType().name());
        fields.put("createdAt", OffsetDateTime.now().toString());
        if (job.getIdempotencyKey() != null) {
            fields.put("idempotencyKey", job.getIdempotencyKey());
        }
        fields.put("retryCount", job.getRetryCount().toString());

        redis.opsForStream().add(streamKey, fields);
        log.info("[RedisDispatcher] Job enqueued to {}: id={}", streamKey, job.getId());
    }
}
```

### 8.1 Job Queue 스켈레톤 (Streams/스키마/컨슈머그룹 규칙)

#### Streams (토픽)

| Stream key  | 용도 | Job types | Producer | Consumer group |
|------------|------|-----------|----------|----------------|
| ai:image   | AI 이미지 생성 (Gemini) | IMAGE_GENERATION | API job dispatcher | cg:ai-image |
| ai:video   | AI 영상 생성 (Veo) | VIDEO_GENERATION | API job dispatcher | cg:ai-video |
| media:merge| FFmpeg 병합 (scene/project) | SCENE_MERGE, PROJECT_MERGE | API job dispatcher | cg:media-merge |

메모:
- stream은 job family 단위로 분리, worker는 jobId로 DB에서 requestJson/파라미터 조회
- 환경 분리 필요 시 suffix 사용: `ai:image:dev`, `ai:image:prod`

#### Stream entry 스키마

필수 필드:
- jobId
- type (IMAGE_GENERATION | VIDEO_GENERATION | SCENE_MERGE | PROJECT_MERGE)
- projectId
- createdAt (ISO-8601)

선택 필드:
- nodeId (image/video)
- sceneId (scene merge)
- idempotencyKey
- retryCount
- requestHash
- traceId
- schemaVersion
- priority

가이드:
- payload는 최소화하고, jobId 기반으로 DB가 소스 오브 트루스가 되도록 유지

#### Consumer group 규칙

- Group naming: `cg:{stream}` (환경 분리 시 `cg:{stream}:{env}`)
- Consumer naming: `{workerType}-{hostname}-{pid}`
- Read: `XREADGROUP GROUP <group> <consumer> COUNT <n> BLOCK <ms> STREAMS <key> >`
- Ack: DB 상태가 SUCCEEDED/FAILED로 전환된 후 XACK

Retry / reclaim:
- worker 장애 시 PEL에 남은 메시지를 `XPENDING` + `XCLAIM`으로 reclaim
- 재시도 가능 시 DB retryCount 증가 후 동일 stream에 XADD 재투입
- retryCount >= MAX_RETRY_COUNT이면 DLQ로 이동하고 job은 FAILED로 확정

Idempotency:
- DB 상태가 SUCCEEDED면 작업 스킵 후 XACK
- RUNNING 전환은 낙관적 업데이트(조건부 UPDATE)로 중복 처리 방지

#### DLQ (옵션)

Stream key: `dlq:jobs`

필드:
- jobId, type, projectId, sourceStream
- reason, failedAt, retryCount

#### 예시 Redis 명령

```text
XGROUP CREATE ai:image cg:ai-image $ MKSTREAM
XGROUP CREATE ai:video cg:ai-video $ MKSTREAM
XGROUP CREATE media:merge cg:media-merge $ MKSTREAM

XADD ai:image * jobId 101 type IMAGE_GENERATION projectId 1 createdAt 2026-01-20T10:00:00Z

XREADGROUP GROUP cg:ai-image image-worker-app01-1234 COUNT 1 BLOCK 5000 STREAMS ai:image >
XACK ai:image cg:ai-image <message-id>
```

#### TODO / 결정 필요

- visibilityTimeout (job type별)
- MAX_RETRY_COUNT
- schemaVersion, optional field 확정
- DLQ 처리 및 알림 기준

---

## 부록: 코드 컨벤션 (현재 코드베이스 기준)

| 항목 | 패턴 |
|---|---|
| 응답 포맷 | `ApiResponse<T>` record |
| 예외 처리 | `BusinessException` + `ErrorCode` enum |
| DTO | record 사용 |
| Swagger | `@Tag`, `@Operation`, `@ApiResponses` |
| 서비스 | 인터페이스 없이 직접 구현 |
| Repository | MyBatis Mapper |
| Lombok | `@Getter`, `@Builder`, `@RequiredArgsConstructor` |
