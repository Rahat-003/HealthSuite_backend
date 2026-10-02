-- Add a stable UUID to each user (used by consultation_media for user reference)
ALTER TABLE users ADD COLUMN user_uuid UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX idx_users_uuid ON users(user_uuid);

-- Stores media files (audio/video) attached to a consultation request.
-- Files are written to ~/HealthSuite/user_consult/{YYYYMMDD}/ on disk.
-- user_uuid references users(user_uuid) instead of the BIGINT PK.

CREATE TABLE consultation_media (
    id                  BIGSERIAL       PRIMARY KEY,
    consultation_id     BIGINT          NOT NULL REFERENCES marketplace_consultation_requests(id) ON DELETE CASCADE,
    user_uuid           UUID            NOT NULL REFERENCES users(user_uuid),
    file_name           VARCHAR(500)    NOT NULL,
    file_path           VARCHAR(1000)   NOT NULL,
    media_type          VARCHAR(10)     NOT NULL,   -- AUDIO | VIDEO
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cm_consultation_id ON consultation_media(consultation_id);
CREATE INDEX idx_cm_user_uuid       ON consultation_media(user_uuid);
