# PROJ-2: 프로젝트 CRUD 완성 구현 계획서

> **담당자**: 강보승  
> **작성일**: 2026-01-20  
> **일정**: D2 (01/20 화요일)  
> **Story Point**: 2 SP  
> **상태**: 구현 대기

---

## 목차

1. [개요](#1-개요)
2. [API 명세](#2-api-명세)
3. [구현 상세](#3-구현-상세)
4. [권한 체크 로직](#4-권한-체크-로직)
5. [삭제 정책](#5-삭제-정책)
6. [테스트 시나리오](#6-테스트-시나리오)
7. [검증 계획](#7-검증-계획)

---

## 1. 개요

### 1.1 목표

프로젝트 CRUD API를 완성합니다. 현재 **Create, List, Detail** API가 구현되어 있으며, **Update(수정)** 와 **Delete(삭제)** API를 추가 구현합니다.

### 1.2 현재 상태 분석

| API | 메서드 | 경로 | 상태 |
|-----|--------|------|------|
| 프로젝트 생성 | `POST` | `/api/projects` | ✅ 완료 |
| 프로젝트 목록 조회 | `GET` | `/api/projects` | ✅ 완료 |
| 프로젝트 상세 조회 | `GET` | `/api/projects/{id}` | ✅ 완료 |
| **프로젝트 수정** | `PUT` | `/api/projects/{id}` | ⏳ **구현 필요** |
| **프로젝트 삭제** | `DELETE` | `/api/projects/{id}` | ⏳ **구현 필요** |

### 1.3 구현 순서

```
Step 0: 하드 삭제 정책에 맞춘 FK ON DELETE 규칙 반영 (schema-local.sql / 운영 DB DDL)
    ↓
Step 1: UpdateProjectRequest.java DTO 생성/검증 보강
    ↓
Step 2: ProjectMapper.java에 메서드 시그니처 변경 (update/delete row count 반환)
    ↓
Step 3: ProjectMapper.xml에 SQL 추가/수정 (update/delete)
    ↓
Step 4: ProjectService.java에 비즈니스 로직 추가 (row count 체크 포함)
    ↓
Step 5: ProjectController.java에 엔드포인트 추가
    ↓
Step 6: 빌드 및 테스트
```

---

## 2. API 명세

### 2.1 프로젝트 수정 API

```
경로: PUT /api/projects/{id}
보안: Bearer Token (Owner만 가능)
설명: 프로젝트의 메타데이터(제목, 설명, 장르)를 수정합니다.
```

#### Request

```json
{
  "title": "수정된 제목",
  "description": "수정된 설명",
  "genre": "COMEDY"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 | 설명 |
|------|------|------|-------------|------|
| title | String | 선택 | max 200자, 공백 불가 | 변경할 프로젝트 제목 |
| description | String | 선택 | max 2000자 | 변경할 프로젝트 설명 |
| genre | String | 선택 | max 100자, 공백 불가 | 변경할 프로젝트 장르 |

> **Note**: 모든 필드가 선택이지만, **최소 1개 이상의 필드가 존재**해야 합니다.  
> **Note**: title/genre는 공백 문자열을 허용하지 않습니다.

#### Response (성공 - 200 OK)

```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "title": "수정된 제목",
    "description": "수정된 설명",
    "genre": "COMEDY",
    "myRole": "OWNER",
    "ownerId": 1,
    "createdAt": "2026-01-15T12:00:00"
  }
}
```

> **정합성 Note**: APIdocs의 프로젝트 상세 응답에는 `members` 요약이 포함되어 있으나, 현재 `ProjectDetailResponse`는 해당 필드를 포함하지 않습니다.  
> 본 작업 범위에서는 기존 응답 스펙을 유지하며, 필요 시 APIdocs 수정 또는 응답 확장을 별도 작업으로 진행합니다.

#### 에러 응답

| 상황 | ErrorCode | HTTP Status |
|------|-----------|-------------|
| 프로젝트 없음 | `PROJECT_NOT_FOUND` | 404 |
| 권한 없음 (Owner 아님) | `FORBIDDEN` | 403 |
| 빈 요청 (필드 없음) | `INVALID_REQUEST` | 400 |

---

### 2.2 프로젝트 삭제 API

```
경로: DELETE /api/projects/{id}
보안: Bearer Token (Owner만 가능)
설명: 프로젝트를 영구적으로 삭제합니다. (Hard Delete + 연관 데이터 삭제)
```

#### Response (성공 - 200 OK)

```json
{
  "code": "SUCCESS"
}
```

#### 에러 응답

| 상황 | ErrorCode | HTTP Status |
|------|-----------|-------------|
| 프로젝트 없음 | `PROJECT_NOT_FOUND` | 404 |
| 권한 없음 (Owner 아님) | `FORBIDDEN` | 403 |

---

## 3. 구현 상세

### 3.1 파일 목록

| 파일 | 작업 | 경로 |
|------|------|------|
| `schema-local.sql` | **수정 (FK ON DELETE 규칙 반영)** | `itda-backend/src/main/resources/sql/` |
| `UpdateProjectRequest.java` | **신규 생성** | `project/controller/dto/request/` |
| `ProjectMapper.java` | 수정 | `project/repository/` |
| `ProjectMapper.xml` | 수정 | `resources/mapper/` |
| `ProjectService.java` | 수정 | `project/service/` |
| `ProjectController.java` | 수정 | `project/controller/` |

---

### 3.2 Step 0: 스키마 FK ON DELETE 규칙 반영

**경로**: `itda-backend/src/main/resources/sql/schema-local.sql`  
**목표**: Hard Delete 시 프로젝트 삭제가 연관 데이터까지 안정적으로 정리되도록 FK에 ON DELETE 규칙을 부여합니다.

**핵심 변경안 (요약 표)**  
| 참조 테이블 | FK 대상 | 규칙 | 이유 |
|---|---|---|---|
| project_members.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 멤버십 제거 |
| scenes.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 씬 제거 |
| nodes.scene_id | scenes.id | ON DELETE CASCADE | 씬 삭제 시 노드 제거 |
| nodes.parent_node_id | nodes.id | ON DELETE CASCADE | 노드 삭제 시 자식 노드 제거 (PRD 정합) |
| scenes.active_master_node_id | nodes.id | ON DELETE SET NULL | 노드 삭제 시 씬 FK 제약 회피 |
| video_clips.shot_node_id | nodes.id | ON DELETE CASCADE | 노드 삭제 시 클립 정리 |
| video_clips.asset_id | assets.id | ON DELETE SET NULL | 에셋 삭제 시 클립 보존 가능 |
| assets.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 에셋 정리 |
| generation_jobs.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 작업 정리 |
| timeline_items.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 타임라인 정리 |
| upload_requests.project_id | projects.id | ON DELETE CASCADE | 프로젝트 삭제 시 업로드 요청 정리 |

> **운영 DB 적용**: 로컬 스키마 변경 외에 운영/개발 DB에도 동일한 DDL을 적용해야 합니다.  
> **주의**: FK 변경은 기존 제약을 DROP 후 재생성해야 하므로 적용 순서 검토가 필요합니다.

---

### 3.3 Step 1: UpdateProjectRequest.java 생성

**경로**: `itda-backend/src/main/java/com/itda/backend/project/controller/dto/request/UpdateProjectRequest.java`

**전체 파일 내용:**

```java
package com.itda.backend.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Project update request")
public record UpdateProjectRequest(

        @Schema(description = "Project title", example = "Updated Title")
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be 200 characters or less")
        String title,

        @Schema(description = "Project description", example = "Updated description")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Project genre", example = "COMEDY")
        @NotBlank(message = "genre must not be blank")
        @Size(max = 100, message = "genre must be 100 characters or less")
        String genre
) {
    /**
     * 최소 하나의 필드가 존재하는지 확인
     * Update 요청 시 빈 body는 허용하지 않음
     */
    @AssertTrue(message = "at least one field must be provided")
    public boolean isAnyFieldPresent() {
        return title != null || description != null || genre != null;
    }
}
```

---

### 3.4 Step 2: ProjectMapper.java 수정

**경로**: `itda-backend/src/main/java/com/itda/backend/project/repository/ProjectMapper.java`

**추가할 메서드 (기존 메서드 아래에 추가):**

```java
int updateProject(@Param("id") Long id,
                  @Param("title") String title,
                  @Param("description") String description,
                  @Param("genre") String genre);

int deleteProject(@Param("id") Long id);
```

**수정 후 전체 파일:**

```java
package com.itda.backend.project.repository;

import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.dto.ProjectSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMapper {

    void insertProject(Project project);

    Optional<Project> findById(@Param("id") Long id);

    List<ProjectSummary> findAllByUserId(@Param("userId") Long userId);

    int updateProject(@Param("id") Long id,
                      @Param("title") String title,
                      @Param("description") String description,
                      @Param("genre") String genre);

    int deleteProject(@Param("id") Long id);
}
```

---

### 3.5 Step 3: ProjectMapper.xml 수정

**경로**: `itda-backend/src/main/resources/mapper/ProjectMapper.xml`

**추가할 SQL (기존 `</mapper>` 태그 바로 위에 추가):**

```xml
    <!-- updateProject: 동적 업데이트 (null이 아닌 필드만 업데이트) -->
    <update id="updateProject">
        UPDATE projects
        <set>
            <if test="title != null">
                title = #{title},
            </if>
            <if test="description != null">
                description = #{description},
            </if>
            <if test="genre != null">
                genre = #{genre},
            </if>
            updated_at = CURRENT_TIMESTAMP
        </set>
        WHERE id = #{id}
    </update>

    <!-- deleteProject: Hard Delete -->
    <delete id="deleteProject">
        DELETE FROM projects
        WHERE id = #{id}
    </delete>
```

**추가 수정 사항:**
- `findById`, `findAllByUserId`에서 `deleted_at IS NULL` 조건 제거 (Hard Delete 전환)

**수정 후 전체 파일:**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.itda.backend.project.repository.ProjectMapper">

    <resultMap id="ProjectResultMap" type="com.itda.backend.project.domain.Project">
        <id property="id" column="id"/>
        <result property="ownerId" column="owner_id"/>
        <result property="title" column="title"/>
        <result property="description" column="description"/>
        <result property="genre" column="genre"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        <result property="deletedAt" column="deleted_at"/>
    </resultMap>

    <resultMap id="ProjectSummaryMap" type="com.itda.backend.project.repository.dto.ProjectSummary">
        <id property="projectId" column="project_id"/>
        <result property="title" column="title"/>
        <result property="myRole" column="my_role"/>
        <result property="memberCount" column="member_count"/>
        <result property="sceneCount" column="scene_count"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <insert id="insertProject" parameterType="com.itda.backend.project.domain.Project"
            useGeneratedKeys="true" keyProperty="id" keyColumn="id">
        INSERT INTO projects (owner_id, title, description, genre, status)
        VALUES (#{ownerId}, #{title}, #{description}, #{genre}, #{status})
    </insert>

    <select id="findById" resultMap="ProjectResultMap">
        SELECT id, owner_id, title, description, genre, status, created_at, updated_at, deleted_at
        FROM projects
        WHERE id = #{id}
    </select>

    <select id="findAllByUserId" resultMap="ProjectSummaryMap">
        SELECT
            p.id AS project_id,
            p.title,
            pm.role AS my_role,
            (SELECT COUNT(*) FROM project_members pm2 WHERE pm2.project_id = p.id) AS member_count,
            (SELECT COUNT(*) FROM scenes s WHERE s.project_id = p.id) AS scene_count,
            p.updated_at
        FROM projects p
        JOIN project_members pm ON pm.project_id = p.id
        WHERE pm.user_id = #{userId}
        ORDER BY p.updated_at DESC
    </select>

    <!-- updateProject: 동적 업데이트 (null이 아닌 필드만 업데이트) -->
    <update id="updateProject">
        UPDATE projects
        <set>
            <if test="title != null">
                title = #{title},
            </if>
            <if test="description != null">
                description = #{description},
            </if>
            <if test="genre != null">
                genre = #{genre},
            </if>
            updated_at = CURRENT_TIMESTAMP
        </set>
        WHERE id = #{id}
    </update>

    <!-- deleteProject: Hard Delete -->
    <delete id="deleteProject">
        DELETE FROM projects
        WHERE id = #{id}
    </delete>

</mapper>
```

---

### 3.6 Step 4: ProjectService.java 수정

**경로**: `itda-backend/src/main/java/com/itda/backend/project/service/ProjectService.java`

**추가할 import (기존 import 섹션에 추가):**

```java
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
```

**추가할 메서드 (기존 메서드 아래에 추가):**

```java
@Transactional
public ProjectDetailResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
    // 1. 프로젝트 존재 여부 확인
    Project project = projectMapper.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    // 2. 권한 확인 (Owner만 수정 가능)
    String role = projectMemberMapper.findRole(projectId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

    if (!ROLE_OWNER.equals(role)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    // 3. 요청에 업데이트할 필드가 없으면 에러
    if (!request.isAnyFieldPresent()) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    // 4. 업데이트 수행
    int updated = projectMapper.updateProject(
            projectId,
            request.title(),
            request.description(),
            request.genre()
    );
    if (updated == 0) {
        throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
    }

    // 5. 업데이트된 프로젝트 조회 및 반환
    Project updatedProject = projectMapper.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    return ProjectDetailResponse.from(updatedProject, role);
}

@Transactional
public void deleteProject(Long userId, Long projectId) {
    // 1. 프로젝트 존재 여부 확인
    Project project = projectMapper.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    // 2. 권한 확인 (Owner만 삭제 가능)
    String role = projectMemberMapper.findRole(projectId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

    if (!ROLE_OWNER.equals(role)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    // 3. Hard Delete 수행
    int deleted = projectMapper.deleteProject(projectId);
    if (deleted == 0) {
        throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
    }
}
```

**수정 후 전체 파일:**

```java
package com.itda.backend.project.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.controller.dto.response.ProjectSummaryResponse;
import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.project.repository.dto.ProjectSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    @Transactional
    public ProjectCreateResponse createProject(Long userId, CreateProjectRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Project project = Project.builder()
                .ownerId(userId)
                .title(request.title())
                .description(request.description())
                .genre(request.genre())
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        projectMapper.insertProject(project);
        projectMemberMapper.insertMember(project.getId(), userId, ROLE_OWNER);

        return new ProjectCreateResponse(
                project.getId(),
                project.getTitle(),
                ROLE_OWNER,
                project.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ProjectListResponse listProjects(Long userId, int page, int size) {
        List<ProjectSummary> summaries = projectMapper.findAllByUserId(userId);
        int total = summaries.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<ProjectSummaryResponse> items = summaries.subList(fromIndex, toIndex).stream()
                .map(ProjectSummaryResponse::from)
                .toList();

        return new ProjectListResponse(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long userId, Long projectId) {
        Project project = projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        String role = projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        return ProjectDetailResponse.from(project, role);
    }

    @Transactional
    public ProjectDetailResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        // 1. 프로젝트 존재 여부 확인
        Project project = projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 2. 권한 확인 (Owner만 수정 가능)
        String role = projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        if (!ROLE_OWNER.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 요청에 업데이트할 필드가 없으면 에러
        if (!request.isAnyFieldPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 4. 업데이트 수행
        int updated = projectMapper.updateProject(
                projectId,
                request.title(),
                request.description(),
                request.genre()
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 5. 업데이트된 프로젝트 조회 및 반환
        Project updatedProject = projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        return ProjectDetailResponse.from(updatedProject, role);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        // 1. 프로젝트 존재 여부 확인
        Project project = projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 2. 권한 확인 (Owner만 삭제 가능)
        String role = projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        if (!ROLE_OWNER.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. Hard Delete 수행
        int deleted = projectMapper.deleteProject(projectId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }
}
```

---

### 3.7 Step 5: ProjectController.java 수정

**경로**: `itda-backend/src/main/java/com/itda/backend/project/controller/ProjectController.java`

**추가할 import (기존 import 섹션에 추가):**

```java
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

**추가할 메서드 (기존 메서드 아래에 추가):**

```java
@Operation(summary = "Update project", description = "프로젝트 메타데이터를 수정합니다. Owner만 가능합니다.")
@PutMapping("/{projectId}")
public ResponseEntity<ApiResponse<ProjectDetailResponse>> updateProject(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long projectId,
        @Valid @RequestBody UpdateProjectRequest request) {
    ProjectDetailResponse response = projectService.updateProject(
            userDetails.getUserId(), projectId, request);
    return ApiResponse.success(response);
}

@Operation(summary = "Delete project", description = "프로젝트를 삭제합니다. Owner만 가능합니다.")
@DeleteMapping("/{projectId}")
public ResponseEntity<ApiResponse<Void>> deleteProject(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long projectId) {
    projectService.deleteProject(userDetails.getUserId(), projectId);
    return ApiResponse.success();
}
```

**수정 후 전체 파일:**

```java
package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.project.controller.dto.request.CreateProjectRequest;
import com.itda.backend.project.controller.dto.request.UpdateProjectRequest;
import com.itda.backend.project.controller.dto.response.ProjectCreateResponse;
import com.itda.backend.project.controller.dto.response.ProjectDetailResponse;
import com.itda.backend.project.controller.dto.response.ProjectListResponse;
import com.itda.backend.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Projects", description = "Project APIs")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project", description = "새로운 프로젝트를 생성합니다. 생성자는 자동으로 Owner가 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectCreateResponse response = projectService.createProject(userDetails.getUserId(), request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "List projects", description = "내가 참여 중인 프로젝트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> listProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProjectListResponse response = projectService.listProjects(userDetails.getUserId(), page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get project detail", description = "프로젝트 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ProjectDetailResponse response = projectService.getProjectDetail(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Update project", description = "프로젝트 메타데이터를 수정합니다. Owner만 가능합니다.")
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> updateProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectDetailResponse response = projectService.updateProject(
                userDetails.getUserId(), projectId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Delete project", description = "프로젝트를 삭제합니다. Owner만 가능합니다.")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        projectService.deleteProject(userDetails.getUserId(), projectId);
        return ApiResponse.success();
    }
}
```

---

## 4. 권한 체크 로직

### 4.1 권한 체크 흐름

```
┌─────────────────────────────────────────────────────────────┐
│  권한 체크 흐름 (Update/Delete)                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 프로젝트 존재 확인                                        │
│     └─ findById(projectId)                                  │
│     └─ 없으면 → PROJECT_NOT_FOUND (404)                     │
│                                                             │
│  2. 멤버십 확인                                              │
│     └─ findRole(projectId, userId)                          │
│     └─ 없으면 → FORBIDDEN (403)                             │
│                                                             │
│  3. Owner 권한 확인                                          │
│     └─ role == "OWNER"?                                     │
│     └─ 아니면 → FORBIDDEN (403)                             │
│                                                             │
│  4. 작업 수행                                                │
│     └─ Update: 동적 필드 업데이트                            │
│     └─ Delete: Hard Delete (DB Cascade)                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 권한 정의

| 역할 | 수정 | 삭제 | 비고 |
|------|------|------|------|
| OWNER | ✅ | ✅ | 모든 권한 |
| EDITOR | ❌ | ❌ | 프로젝트 메타데이터 수정 불가 |
| VIEWER | ❌ | ❌ | 읽기 전용 |

---

## 5. 삭제 정책

### 5.1 결정: Hard Delete

PRD/APIdocs의 삭제 정책에 맞춰 **Hard Delete**로 구현합니다.

- PRD: 삭제 정책(MVP) = Hard Delete
- APIdocs: 프로젝트 삭제 시 포함된 씬/노드까지 삭제됨

> **Note**: `deleted_at` 컬럼은 현재 Hard Delete 정책 하에서는 사용하지 않으며, 향후 정책 변경 시 활용 가능합니다.

### 5.2 연관 데이터 처리 (DB Cascade)

Step 0에서 정의한 **FK ON DELETE 규칙**을 적용하여, 프로젝트 삭제 시 연관 데이터가 DB 레벨에서 정리되도록 합니다.

- 서비스에서는 `projects` 레코드만 삭제
- 연관 데이터(멤버십/씬/노드/자식노드/클립/타임라인/잡/업로드 요청 등)는 FK 규칙에 의해 삭제 또는 NULL 처리
  
**삭제 후 접근 응답 정책 (정책 선택 필요):**
- 현재 구조 기준: 멤버십이 삭제되므로 `403 FORBIDDEN`이 반환될 가능성이 큼
- 만약 `404 PROJECT_NOT_FOUND`가 필요하면, 씬/노드 서비스에서 프로젝트 존재 여부를 선검증하는 로직 추가 필요

### 5.3 운영 적용/리스크

- FK 변경은 운영 DB에도 동일하게 적용되어야 합니다.
- 대량 데이터 프로젝트 삭제 시 락/성능 이슈가 발생할 수 있어 사전 검증이 필요합니다.

---

## 6. 테스트 시나리오

### 6.1 프로젝트 수정 API 테스트

| # | 시나리오 | 예상 결과 |
|---|----------|-----------|
| 1 | Owner가 프로젝트 수정 (모든 필드) | 200 OK, 수정된 프로젝트 반환 |
| 2 | Owner가 프로젝트 수정 (title만) | 200 OK, title만 수정됨 |
| 3 | Owner가 프로젝트 수정 (description만) | 200 OK, description만 수정됨 |
| 4 | Editor가 프로젝트 수정 시도 | 403 FORBIDDEN |
| 5 | Viewer가 프로젝트 수정 시도 | 403 FORBIDDEN |
| 6 | 비멤버가 프로젝트 수정 시도 | 403 FORBIDDEN |
| 7 | 존재하지 않는 프로젝트 수정 | 404 PROJECT_NOT_FOUND |
| 8 | 빈 요청 body로 수정 시도 | 400 INVALID_REQUEST |
| 9 | title/genre 공백 문자열로 수정 시도 | 400 INVALID_REQUEST |
| 10 | 삭제된 프로젝트 수정 시도 | 404 PROJECT_NOT_FOUND |

### 6.2 프로젝트 삭제 API 테스트

| # | 시나리오 | 예상 결과 |
|---|----------|-----------|
| 1 | Owner가 프로젝트 삭제 | 200 OK |
| 2 | 삭제된 프로젝트 조회 시도 | 404 PROJECT_NOT_FOUND |
| 3 | 삭제된 프로젝트 목록에서 제외 확인 | 목록에 표시 안됨 |
| 4 | 삭제된 프로젝트의 씬/노드 조회 시도 | 404 또는 FORBIDDEN (API 정책에 따름) |
| 5 | Editor가 프로젝트 삭제 시도 | 403 FORBIDDEN |
| 6 | Viewer가 프로젝트 삭제 시도 | 403 FORBIDDEN |
| 7 | 비멤버가 프로젝트 삭제 시도 | 403 FORBIDDEN |
| 8 | 존재하지 않는 프로젝트 삭제 | 404 PROJECT_NOT_FOUND |
| 9 | 같은 프로젝트 두 번 삭제 시도 | 404 PROJECT_NOT_FOUND (두 번째) |

---

## 7. 검증 계획

### 7.1 빌드 확인

```bash
cd itda-backend
./gradlew build
```

빌드 성공 시 다음 단계로 진행합니다.

**추가 검증 (DB 스키마):**
- `schema-local.sql`에 FK ON DELETE 규칙 반영 여부 확인
- 운영/개발 DB에도 동일 DDL 반영 여부 확인 (예: `SHOW CREATE TABLE ...`)

### 7.2 Swagger UI 테스트

서버 실행 후 `http://localhost:8080/swagger-ui.html` 접속하여 다음 테스트를 수행합니다.

#### 테스트 1: 프로젝트 수정 (성공)

```
PUT /api/projects/{projectId}
Authorization: Bearer {owner_token}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description"
}
```

예상 응답: 200 OK

#### 테스트 2: 프로젝트 수정 (권한 없음)

```
PUT /api/projects/{projectId}
Authorization: Bearer {editor_token}
Content-Type: application/json

{
  "title": "Updated Title"
}
```

예상 응답: 403 FORBIDDEN

#### 테스트 3: 프로젝트 삭제 (성공)

```
DELETE /api/projects/{projectId}
Authorization: Bearer {owner_token}
```

예상 응답: 200 OK

#### 테스트 4: 삭제된 프로젝트 조회

```
GET /api/projects/{projectId}
Authorization: Bearer {owner_token}
```

예상 응답: 404 PROJECT_NOT_FOUND

#### 테스트 5: 삭제된 프로젝트의 씬/노드 접근

```
GET /api/projects/{projectId}/scenes
Authorization: Bearer {owner_token}
```

예상 응답: 404 또는 FORBIDDEN (정책 기준으로 결정)

---

## 체크리스트

- [ ] **Step 0**: FK ON DELETE 규칙 반영 (`schema-local.sql` + 운영 DB DDL)
- [ ] **Step 1**: `UpdateProjectRequest.java` 생성
- [ ] **Step 2**: `ProjectMapper.java` 수정
- [ ] **Step 3**: `ProjectMapper.xml` 수정
- [ ] **Step 4**: `ProjectService.java` 수정
- [ ] **Step 5**: `ProjectController.java` 수정
- [ ] **Step 6**: 빌드 성공 확인 (`./gradlew build`)
- [ ] **Step 7**: Swagger UI 테스트
  - [ ] 프로젝트 수정 (Owner) - 성공
  - [ ] 프로젝트 수정 (Editor) - 403 실패
  - [ ] 프로젝트 삭제 (Owner) - 성공
  - [ ] 삭제된 프로젝트 조회 - 404 실패

---

## 참고 문서

- [APIdocs.md](../APIdocs.md) - API 명세서
- [PRD_AI_Movie_Studio_v2.5.md](../PRD_AI_Movie_Studio_v2.5.md) - 프로젝트 관리 기능 정의
- [02-schedule-w3-w6.md](./ai-movie-studio-md-pack-v3/02-schedule-w3-w6.md) - PROJ-2 태스크 정의
