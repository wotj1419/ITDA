CREATE TABLE IF NOT EXISTS project_invites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,  -- ADMIN, EDITOR, VIEWER
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACCEPTED, DECLINED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_invite_receiver (project_id, receiver_id),
    KEY idx_project_invites_receiver (receiver_id),
    KEY idx_project_invites_project (project_id),
    CONSTRAINT fk_invites_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_invites_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_invites_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
