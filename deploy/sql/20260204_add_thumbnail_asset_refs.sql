ALTER TABLE nodes
    ADD COLUMN thumbnail_asset_id BIGINT NULL AFTER asset_id,
    ADD INDEX idx_nodes_thumbnail_asset (thumbnail_asset_id),
    ADD CONSTRAINT fk_nodes_thumbnail_asset
        FOREIGN KEY (thumbnail_asset_id) REFERENCES assets (id) ON DELETE SET NULL;

ALTER TABLE scene_videos
    ADD COLUMN thumbnail_asset_id BIGINT NULL AFTER asset_id,
    ADD INDEX idx_scene_videos_thumbnail_asset (thumbnail_asset_id),
    ADD CONSTRAINT fk_scene_videos_thumbnail_asset
        FOREIGN KEY (thumbnail_asset_id) REFERENCES assets (id) ON DELETE SET NULL;

ALTER TABLE project_merges
    ADD COLUMN thumbnail_asset_id BIGINT NULL AFTER asset_id,
    ADD INDEX idx_project_merges_thumbnail_asset (thumbnail_asset_id),
    ADD CONSTRAINT fk_project_merges_thumbnail_asset
        FOREIGN KEY (thumbnail_asset_id) REFERENCES assets (id) ON DELETE SET NULL;
