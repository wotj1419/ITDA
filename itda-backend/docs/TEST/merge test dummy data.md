```sql

-- TL-4a 스모크 테스트 더미 데이터 (sceneId=1, VIDEO nodes=6,7,8 고정)
-- 전제: sceneId=1 존재, nodes 6/7/8 존재 + VIDEO 타입

-- 확인(옵션)
SELECT id, scene_id, node_type, is_confirmed, content_url
FROM nodes
WHERE id IN (6,7,8);

-- project_id 가져오기
SET @scene_id := 1;
SET @project_id := (SELECT project_id FROM scenes WHERE id = @scene_id);

-- VIDEO 노드 confirm + content_url 부여
UPDATE nodes
SET is_confirmed = 1,
    content_url = CONCAT('https://example.com/video/', id, '.mp4')
WHERE scene_id = @scene_id
  AND id IN (6,7,8)
  AND node_type = 'VIDEO';

-- 기존 timeline_items 정리(중복 방지)
DELETE FROM timeline_items
WHERE scene_id = @scene_id;

-- scene_videos 생성 (프로젝트 merge 테스트용)
INSERT INTO scene_videos (scene_id, status, duration_ms, thumbnail_url)
VALUES (@scene_id, 'COMPLETED', 5000, 'https://example.com/thumb/scene1-1.jpg');
SET @sv1 := LAST_INSERT_ID();

INSERT INTO scene_videos (scene_id, status, duration_ms, thumbnail_url)
VALUES (@scene_id, 'COMPLETED', 6000, 'https://example.com/thumb/scene1-2.jpg');
SET @sv2 := LAST_INSERT_ID();

INSERT INTO scene_videos (scene_id, status, duration_ms, thumbnail_url)
VALUES (@scene_id, 'COMPLETED', 4500, 'https://example.com/thumb/scene1-3.jpg');
SET @sv3 := LAST_INSERT_ID();

-- timeline_items 삽입 (scene merge + project merge 통과용)
INSERT INTO timeline_items (
  project_id, scene_id, video_node_id, scene_video_id, order_index, created_by
) VALUES
(@project_id, @scene_id, 6, @sv1, 0, NULL),
(@project_id, @scene_id, 7, @sv2, 1, NULL),
(@project_id, @scene_id, 8, @sv3, 2, NULL);

```