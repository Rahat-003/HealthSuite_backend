-- ── Concierge appointment booking tickets ─────────────────────────────────────
-- State machine: PENDING → PROCESSING (claimed by agent) → CONFIRMED | CANCELLED
-- @Version column drives optimistic locking; findPendingForClaim uses PESSIMISTIC_WRITE.
CREATE TABLE concierge_tickets (
    id                     BIGSERIAL    PRIMARY KEY,
    owner_id               BIGINT       NOT NULL REFERENCES users(id),         -- the patient
    created_by_user_id     BIGINT       NOT NULL REFERENCES users(id),         -- proxy or self
    assigned_support_id    BIGINT       REFERENCES users(id),                  -- ROLE_SUPPORT agent
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    doctor_name            VARCHAR(255) NOT NULL,
    doctor_specialty       VARCHAR(255),
    chamber_address        TEXT         NOT NULL,
    description            TEXT,
    serial_number          VARCHAR(50),                                         -- set on CONFIRMED
    estimated_arrival_time TIMESTAMP,                                          -- set on CONFIRMED
    cancellation_notes     TEXT,                                               -- required on CANCELLED
    version                BIGINT       NOT NULL DEFAULT 0,                    -- optimistic lock
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by             VARCHAR(255),
    updated_at             TIMESTAMP,
    updated_by             VARCHAR(255),

    -- DB-level enforcement of state machine invariants
    CONSTRAINT chk_confirmed_has_serial
        CHECK (status != 'CONFIRMED' OR serial_number IS NOT NULL),
    CONSTRAINT chk_confirmed_has_eta
        CHECK (status != 'CONFIRMED' OR estimated_arrival_time IS NOT NULL),
    CONSTRAINT chk_cancelled_has_notes
        CHECK (status != 'CANCELLED' OR cancellation_notes IS NOT NULL)
);

-- ── Indexes ────────────────────────────────────────────────────────────────────
CREATE INDEX idx_tickets_owner         ON concierge_tickets(owner_id);
CREATE INDEX idx_tickets_status        ON concierge_tickets(status);
CREATE INDEX idx_tickets_support_agent ON concierge_tickets(assigned_support_id);
-- Support queue: pending tickets ordered oldest-first
CREATE INDEX idx_tickets_pending_queue ON concierge_tickets(status, created_at ASC)
    WHERE status = 'PENDING';
