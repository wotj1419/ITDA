# 노드 CRUD API 스켈레톤 구현 계획

> **담당자**: 강보승  
> **날짜**: 2026-01-21 (D3)  
> **Sprint Task ID**: NODE-3  
> **Story Points**: 3

---

## 1. 목표

3주차 D3 일정에 따라 **노드 CRUD API 스켈레톤**을 구현합니다.
PRD/APIdocs와 정합성을 맞추어 **scene_header는 가상 노드**로 처리합니다.

### 핵심 기능
| HTTP | 엔드포인트 | 설명 |
|------|-----------|------|
| POST | `/api/scenes/{sceneId}/nodes` | 노드 생성 |
| GET | `/api/scenes/{sceneId}/nodes` | 씬 노드 트리 조회 (scene_header 포함) |
| GET | `/api/nodes/{id}` | 노드 상세 조회 |
| PUT | `/api/nodes/{id}` | 노드 수정 |
| DELETE | `/api/nodes/{id}` | 노드 삭제 |
| PUT | `/api/scenes/{sceneId}/nodes/positions` | 노드 위치 일괄 저장 |

> 참고: regenerate/activate/confirm/export 등은 후속 태스크 범위로 유지합니다.

---

## 2. 주요 설계 결정

### 2.1 SCENE_HEADER는 가상 노드 (DB 저장 안 함)

> [!IMPORTANT]
> PRD/APIdocs 기준으로 **scene_header는 DB에 저장하지 않는 가상 노드**입니다.
> `GET /api/scenes/{sceneId}/nodes` 응답에만 합성하여 포함합니다.

**구현 위치**: `NodeService.listNodes()`
- Scene 정보 기반으로 `SCENE_HEADER` 응답 DTO를 합성
- `nodeId`는 음수 규칙(예: `-sceneId`)로 생성
- `position`은 고정값(예: `x=0, y=-200`)으로 반환

**요청 처리 규칙**
- `SCENE_HEADER` 생성/수정/삭제/이동 요청은 금지 (400/403)

### 2.2 data_json 처리 방식 (settings 전용)

> [!NOTE]
> `data_json`은 **settings 전용(JSON 문자열)**으로 사용하고,
> 상태/확정/활성/콘텐츠 URL 등은 명시 컬럼으로 분리합니다.

**근거**:
- APIdocs/PRD에서 status/is_active/is_confirmed/content_url 등을 명시
- 제약/인덱스/검증을 컬럼 기반으로 처리해야 함
- `Job.requestJson`과 동일하게 `String + ObjectMapper` 패턴 사용

### 2.3 상태/활성/확정 소스 오브 트루스
- `nodes.status`: Job 완료 시 업데이트
- `nodes.is_active` + `scenes.active_master_node_id`: Active Master 유지
- `nodes.is_confirmed`: shot당 1개 확정 (서비스 트랜잭션 보장)

### 2.4 연결 규칙 검증 (P0 필수)
- master → grid → shot → video만 허용
- `VIDEO.parentNodeId`는 `settings.startShotNodeId`와 반드시 동일

---

## 3. 제약사항 (API 문서 기반)

| 노드 타입 | 제약 |
|----------|------|
| `SCENE_HEADER` | 생성/수정/삭제/이동 불가 (가상 노드) |
| `MASTER` | 씬당 최대 3개, Active 1개 |
| `VIDEO` | SHOT당 확정 1개만 |

**연결 규칙**
- master → grid → shot → video만 허용
- video는 `parentNodeId == startShotNodeId` 필수

---

## 4. 파일 변경 목록

### 4.1 스키마 수정

#### [MODIFY] `schema-local.sql`
- `nodes` 테이블에 명시 컬럼 추가  
  - `status VARCHAR(20)` (PENDING/RUNNING/SUCCEEDED/FAILED)
  - `is_active TINYINT(1)` (master 전용)
  - `is_confirmed TINYINT(1)` (video 전용)
  - `content_url VARCHAR(500)`
  - `prompt TEXT`
  - `start_shot_node_id BIGINT`
  - `end_shot_node_id BIGINT`
- `position_x/y`를 `FLOAT`로 변경 (캔버스 좌표)
- `start_shot_node_id`/`end_shot_node_id` FK 추가 (nodes → nodes)

---

### 4.2 Domain 계층

#### [NEW] `node/domain/Node.java`
```java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class Node {
    private Long id;
    private Long sceneId;
    private NodeType nodeType;
    private Long parentNodeId;
    private Integer orderIndex;
    private Float positionX;
    private Float positionY;
    private String prompt;
    private String dataJson;  // settings JSON 문자열
    private NodeStatus status;
    private Boolean isActive;
    private Boolean isConfirmed;
    private String contentUrl;
    private Long startShotNodeId;
    private Long endShotNodeId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### [NEW] `node/domain/NodeType.java`
```java
public enum NodeType {
    SCENE_HEADER, MASTER, GRID, SHOT, VIDEO
}
```

#### [NEW] `node/domain/NodeStatus.java`
```java
public enum NodeStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED
}
```

---

### 4.3 Repository 계층

#### [NEW] `node/repository/NodeMapper.java`
```java
@Mapper
public interface NodeMapper {
    void insertNode(Node node);
    Optional<Node> findById(Long id);
    List<Node> findAllBySceneId(Long sceneId);
    void updateNode(Node node);
    void deleteById(Long id);
    int countMasterNodesBySceneId(Long sceneId);
    void clearActiveMasterBySceneId(Long sceneId);
    void setActiveMaster(Long nodeId);
    void clearConfirmedVideosByShotId(Long shotNodeId);
    void setConfirmedVideo(Long nodeId);
}
```

#### [NEW] `resources/mapper/NodeMapper.xml`
- INSERT, SELECT, UPDATE, DELETE 쿼리 정의
- `dataJson` ↔ `data_json` 컬럼 매핑
- `status/is_active/is_confirmed/content_url/start_shot_node_id/end_shot_node_id` 매핑

---

### 4.4 DTO 계층

| 파일 | 용도 |
|------|------|
| `dto/request/CreateNodeRequest.java` | 생성 요청 (type, parentNodeId, prompt, settings) |
| `dto/request/UpdateNodeRequest.java` | 수정 요청 (prompt, settings) |
| `dto/response/NodeCreateResponse.java` | 생성 응답 |
| `dto/response/NodeDetailResponse.java` | 상세 응답 |
| `dto/response/NodeSummaryResponse.java` | 목록 항목 |
| `dto/response/NodeTreeResponse.java` | 트리 응답 |
| `dto/response/NodePosition.java` | 위치 정보 |
| `dto/response/SceneHeaderNodeResponse.java` | 가상 노드 응답 |

---

### 4.5 Service 계층

#### [NEW] `node/service/NodeService.java`

| 메서드 | 설명 |
|--------|------|
| `createNode()` | 노드 생성 (SCENE_HEADER 금지, MASTER 제한, 연결 규칙 검증) |
| `listNodes()` | 씬 노드 목록 조회 (가상 scene_header 포함) |
| `getNodeDetail()` | 노드 상세 조회 |
| `updateNode()` | 노드 수정 (SCENE_HEADER 수정 차단) |
| `deleteNode()` | 노드 삭제 (SCENE_HEADER 삭제 차단, 자식 노드 cascade) |
| `updatePositions()` | 노드 위치 일괄 저장 (SCENE_HEADER 무시) |

**의존성**: `NodeMapper`, `SceneMapper`, `ProjectMemberMapper`, `ObjectMapper`

---

### 4.6 Controller 계층

#### [NEW] `node/controller/NodeController.java`
- Swagger 어노테이션 포함
- `@AuthenticationPrincipal` 인증 처리
- 위치 일괄 저장 API 포함

---

### 4.7 기존 파일 수정

#### [MODIFY] `scene/service/SceneService.java`
- 변경 없음 (scene_header는 가상 노드)

#### [MODIFY] `global/response/ErrorCode.java`
```java
MASTER_NODE_LIMIT_EXCEEDED(409, "마스터 노드는 씬당 최대 3개까지 생성할 수 있습니다"),
SCENE_HEADER_NOT_MODIFIABLE(403, "씬 헤더 노드는 수정/삭제/이동할 수 없습니다"),
INVALID_NODE_RELATION(400, "노드 연결 규칙이 올바르지 않습니다"),
CONFIRMED_VIDEO_CONFLICT(409, "동일 샷에 이미 확정된 영상이 있습니다")
```

---

## 5. 파일 요약

| 구분 | 개수 | 파일 |
|------|------|------|
| **신규** | 13개 | Node.java, NodeType.java, NodeStatus.java, NodeMapper.java, NodeMapper.xml, NodeController.java, 8개 DTO |
| **수정** | 2개 | schema-local.sql, ErrorCode.java |

---

## 6. 검증 계획

### 테스트 시나리오

1. **씬 생성 → scene_header 가상 노드 응답 포함 확인**
2. **MASTER 노드 생성** (성공)
3. **4번째 MASTER 생성** (실패: MASTER_NODE_LIMIT_EXCEEDED)
4. **SCENE_HEADER 생성/수정/삭제/이동 시도** (실패: SCENE_HEADER_NOT_MODIFIABLE)
5. **연결 규칙 위반 생성** (실패: INVALID_NODE_RELATION)
6. **노드 목록/상세 조회** (성공, status/isActive/isConfirmed/contentUrl 필드 확인)
7. **노드 수정/삭제** (성공, 하위 노드 삭제 확인)
8. **노드 위치 일괄 저장** (SCENE_HEADER 무시 확인)

### 테스트 명령어 예시
```bash
# 씬 생성 (SCENE_HEADER 자동 생성됨)
curl -X POST http://localhost:8080/api/projects/1/scenes \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"title": "테스트 씬"}'

# 노드 목록 조회 (SCENE_HEADER 포함)
curl http://localhost:8080/api/scenes/1/nodes \
  -H "Authorization: Bearer {token}"
```

---

## 7. 스코프 외 (후속 태스크)

| ID | 태스크 | 담당 |
|----|--------|------|
| NODE-4 | Active Master / Confirm 유일성 제약 | 장현준 |
| NODE-6 | 영상 확정 API (confirm/unconfirm) | 장현준 |
| AI-4 | Job 상태 조회 API | 이용호 |
