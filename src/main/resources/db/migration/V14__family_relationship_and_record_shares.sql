-- ── Family flow completion ─────────────────────────────────────────────────────
-- 1. Relationship label on the tie (set by the initiator: MOTHER, FATHER, ...)
ALTER TABLE family_members ADD COLUMN relationship VARCHAR(30);

-- 2. Record-share grants: each user explicitly controls what a verified family
--    member can see — everything (ALL) or hand-picked visits (SELECTED).
--    No row = nothing shared. A verified tie alone no longer exposes records.
CREATE TABLE family_record_shares (
    id              BIGSERIAL   PRIMARY KEY,
    grantor_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    grantee_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope           VARCHAR(20) NOT NULL,   -- ALL | SELECTED
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    UNIQUE (grantor_user_id, grantee_user_id),
    CONSTRAINT chk_share_no_self CHECK (grantor_user_id <> grantee_user_id)
);

CREATE TABLE family_record_share_visits (
    share_id BIGINT NOT NULL REFERENCES family_record_shares(id) ON DELETE CASCADE,
    visit_id BIGINT NOT NULL REFERENCES medical_visits(id) ON DELETE CASCADE,
    PRIMARY KEY (share_id, visit_id)
);

CREATE INDEX idx_record_shares_grantee ON family_record_shares(grantee_user_id);
