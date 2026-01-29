CREATE DATABASE IF NOT EXISTS itda_local;
USE itda_local;

-- ============================================
-- Drop order (FK-safe)
-- ============================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS timeline_items;
DROP TABLE IF EXISTS generation_jobs;
DROP TABLE IF EXISTS video_clips;
DROP TABLE IF EXISTS nodes;
DROP TABLE IF EXISTS scene_objects;
DROP TABLE IF EXISTS scene_videos;
DROP TABLE IF EXISTS scenes;
DROP TABLE IF EXISTS project_scenarios;
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS objects;
DROP TABLE IF EXISTS upload_requests;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. 사용자 (Users)
-- ============================================


CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Local seed user (email: test@gmail.com, password: qweqwe123)
INSERT INTO users (email, password_hash, name, profile_image_url, role)
VALUES (
    'test@gmail.com',
    '$2a$10$wOct./8vg0zB5Fy.KtrPleexXvWS4FXul34r3c.m.dg.WiH5vr.Eu',
    'Test User',
    NULL,
    'USER'
);

-- ============================================
-- 2. 프로젝트 (Projects)
-- ============================================


CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    genre VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. 프로젝트 멤버 (Project Members) - W4
-- ============================================


CREATE TABLE project_members (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,  -- OWNER, EDITOR, VIEWER
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    KEY idx_pm_user (user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. 오브젝트 시트 (Objects)
-- ============================================


CREATE TABLE objects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,         -- CHARACTER, PROP, ETC
    description TEXT NOT NULL,
    style VARCHAR(100),
    sheet_image_url VARCHAR(500),
    sheet_image_asset_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, SUCCEEDED, FAILED
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_objects_project (project_id),
    KEY idx_objects_status (status),
    CONSTRAINT fk_objects_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_objects_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. 씬 (Scenes)
-- ============================================


CREATE TABLE scenes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_index INT NOT NULL DEFAULT 0,
    active_master_node_id BIGINT,  -- FK는 nodes 생성 후 ALTER로 추가
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_scenes_project_order (project_id, order_index),
    CONSTRAINT fk_scenes_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5-1. 씬-오브젝트 연결 (Scene Objects)
-- ============================================


CREATE TABLE scene_objects (
    scene_id BIGINT NOT NULL,
    object_id BIGINT NOT NULL,
    PRIMARY KEY (scene_id, object_id),
    KEY idx_scene_objects_object (object_id),
    CONSTRAINT fk_scene_objects_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_objects_object FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5-2. 시나리오 (Project Scenarios)
-- ============================================


CREATE TABLE project_scenarios (
    project_id BIGINT PRIMARY KEY,
    input_genre VARCHAR(50) NOT NULL,
    input_mood VARCHAR(50) NOT NULL,
    input_scene_count INT NOT NULL,
    input_keywords VARCHAR(255),
    input_character_hints TEXT,
    input_background_hints TEXT,
    input_reference_style VARCHAR(100),
    prompt_text TEXT,
    prompt_status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    plot_text TEXT,
    plot_status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    current_step VARCHAR(10) NOT NULL DEFAULT 'INPUT',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scenario_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. 에셋 (Assets) - 파일 저장
-- ============================================


CREATE TABLE assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT,
    project_id BIGINT,
    asset_type VARCHAR(10) NOT NULL,  -- IMAGE, VIDEO, AUDIO
    storage_provider VARCHAR(20) NOT NULL DEFAULT 'S3',
    storage_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    KEY idx_assets_project (project_id),
    CONSTRAINT fk_assets_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_assets_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 6. 노드 (Nodes) - Vue Flow
-- ============================================


CREATE TABLE nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    node_type VARCHAR(20) NOT NULL,  -- SCENE_HEADER, MASTER, GRID, SHOT, VIDEO
    parent_node_id BIGINT,
    order_index INT NOT NULL DEFAULT 0,
    position_x FLOAT,                -- 캔버스 X 좌표
    position_y FLOAT,                -- 캔버스 Y 좌표
    prompt TEXT,                     -- AI 프롬프트
    data_json JSON,                  -- settings JSON (스타일, 비율 등)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, SUCCEEDED, FAILED
    is_active TINYINT(1) NOT NULL DEFAULT 0,        -- MASTER용 활성 플래그
    is_confirmed TINYINT(1) NOT NULL DEFAULT 0,     -- VIDEO용 확정 플래그
    content_url VARCHAR(500),        -- 생성 결과 URL
    start_shot_node_id BIGINT,       -- VIDEO 시작 샷 노드 ID
    end_shot_node_id BIGINT,         -- VIDEO 종료 샷 노드 ID
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_nodes_scene (scene_id),
    KEY idx_nodes_parent (parent_node_id),
    KEY idx_nodes_scene_order (scene_id, order_index),
    KEY idx_nodes_status (status),
    KEY idx_nodes_start_shot (start_shot_node_id),
    CONSTRAINT fk_nodes_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_nodes_parent FOREIGN KEY (parent_node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_nodes_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_nodes_start_shot FOREIGN KEY (start_shot_node_id) REFERENCES nodes(id) ON DELETE SET NULL,
    CONSTRAINT fk_nodes_end_shot FOREIGN KEY (end_shot_node_id) REFERENCES nodes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- scenes.active_master_node_id FK 추가
ALTER TABLE scenes
    ADD CONSTRAINT fk_scenes_active_master
    FOREIGN KEY (active_master_node_id) REFERENCES nodes(id) ON DELETE SET NULL;

-- ============================================
-- 7. 영상 클립 (Video Clips)
-- ============================================


CREATE TABLE video_clips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shot_node_id BIGINT NOT NULL,
    asset_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',  -- QUEUED, GENERATING, COMPLETED, FAILED
    duration_ms INT,
    is_confirmed TINYINT(1) NOT NULL DEFAULT 0,  -- 확정 여부
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_video_shot FOREIGN KEY (shot_node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scene_videos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    asset_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',  -- QUEUED, GENERATING, COMPLETED, FAILED
    duration_ms INT,
    thumbnail_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_scene_videos_scene (scene_id),
    CONSTRAINT fk_scene_videos_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_videos_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 8. 생성 작업 (Generation Jobs) - Worker용
-- ============================================


CREATE TABLE generation_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    scene_id BIGINT,
    node_id BIGINT,  -- 추가: 어떤 노드의 작업인지
    job_type VARCHAR(20) NOT NULL,  -- IMAGE_GENERATION, VIDEO_GENERATION, SCENE_MERGE, PROJECT_MERGE
    idempotency_key VARCHAR(128),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, SUCCEEDED, FAILED
    request_json JSON,  -- 입력 파라미터
    result_asset_id BIGINT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    finished_at DATETIME,
    KEY idx_jobs_project (project_id),
    KEY idx_jobs_scene (scene_id),
    KEY idx_jobs_status (status),
    KEY idx_jobs_node (node_id),
    UNIQUE KEY uk_jobs_idempotency (idempotency_key),
    CONSTRAINT fk_jobs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_node FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_result_asset FOREIGN KEY (result_asset_id) REFERENCES assets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 9. 타임라인 (Timeline Items)
-- ============================================


CREATE TABLE timeline_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    video_node_id BIGINT,
    scene_video_id BIGINT,
    order_index INT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_timeline_project_order (project_id, order_index),
    KEY idx_timeline_scene_order (scene_id, order_index),
    CONSTRAINT fk_timeline_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_timeline_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_timeline_video_node FOREIGN KEY (video_node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_timeline_scene_video FOREIGN KEY (scene_video_id) REFERENCES scene_videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_timeline_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 10. 업로드 요청 (Upload Requests) - W4
-- ============================================


CREATE TABLE upload_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    owner_id BIGINT,
    asset_type VARCHAR(10) NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED, EXPIRED
    presigned_key VARCHAR(512),
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_upload_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_upload_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
