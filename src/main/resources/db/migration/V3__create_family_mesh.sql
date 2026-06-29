-- ── Family mesh (directed graph, bidirectional query in app layer) ─────────────
-- "owner" is who initiated the link; "member" is who was added.
-- Status becomes VERIFIED when member confirms.
-- canAccess query checks BOTH directions: (a→b) OR (b→a) with status=VERIFIED.
CREATE TABLE family_members (
    id             BIGSERIAL   PRIMARY KEY,
    owner_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status         VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at    TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(255),
    UNIQUE (owner_id, member_user_id),
    CONSTRAINT chk_no_self_add CHECK (owner_id <> member_user_id)
);

-- ── Share tokens (time-limited, per-record external access) ───────────────────
-- token_hash is SHA-256 of the raw token (raw token is returned to client once and never stored).
-- Anyone with the raw token can access the specific record without authenticating.
CREATE TABLE share_tokens (
    id            BIGSERIAL   PRIMARY KEY,
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    owner_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    record_type   VARCHAR(50) NOT NULL,   -- e.g. "MEDICAL_VISIT"
    record_id     BIGINT      NOT NULL,
    expires_at    TIMESTAMP   NOT NULL,
    is_revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Indexes ────────────────────────────────────────────────────────────────────
CREATE INDEX idx_family_members_owner      ON family_members(owner_id);
CREATE INDEX idx_family_members_member     ON family_members(member_user_id);
CREATE INDEX idx_share_tokens_hash         ON share_tokens(token_hash);
CREATE INDEX idx_share_tokens_owner_active ON share_tokens(owner_user_id, is_revoked)
    WHERE is_revoked = FALSE;
