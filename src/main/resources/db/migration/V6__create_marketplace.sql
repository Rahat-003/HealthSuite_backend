-- ── Marketplace: Doctor profiles (registered through the app, separate from scraped doctors) ──

CREATE TABLE marketplace_doctor_profiles (
    id                   BIGSERIAL       PRIMARY KEY,
    user_id              BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name            VARCHAR(255)    NOT NULL,
    specialty            VARCHAR(100)    NOT NULL,
    qualifications       TEXT            NOT NULL,
    experience_years     INT             NOT NULL,
    license_number       VARCHAR(100)    NOT NULL UNIQUE,
    bio                  TEXT,
    hospital_affiliation VARCHAR(255),
    availability_note    VARCHAR(255),
    consultation_fee_bdt DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- PENDING | ACTIVE | SUSPENDED
    profile_photo_url    VARCHAR(1000),
    rating               DECIMAL(3, 2)   NOT NULL DEFAULT 0,
    total_consultations  INT             NOT NULL DEFAULT 0,
    rejection_reason     TEXT,
    created_at           TIMESTAMP,
    created_by           VARCHAR(255),
    updated_at           TIMESTAMP,
    updated_by           VARCHAR(255)
);

CREATE INDEX idx_mdp_user_id   ON marketplace_doctor_profiles(user_id);
CREATE INDEX idx_mdp_status    ON marketplace_doctor_profiles(status);
CREATE INDEX idx_mdp_specialty ON marketplace_doctor_profiles(specialty);

-- ── Consultation requests ──────────────────────────────────────────────────────

CREATE TABLE marketplace_consultation_requests (
    id                    BIGSERIAL       PRIMARY KEY,
    patient_id            BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_text          TEXT            NOT NULL,
    audio_url             VARCHAR(1000),
    refund_window_hours   INT             NOT NULL DEFAULT 3,
    status                VARCHAR(20)     NOT NULL DEFAULT 'QUEUED',  -- QUEUED | ACTIVE | COMPLETED | EXPIRED | REFUNDED
    upfront_amount_bdt    DECIMAL(10, 2)  NOT NULL DEFAULT 50,
    post_consult_amount_bdt DECIMAL(10, 2) NOT NULL DEFAULT 250,
    expires_at            TIMESTAMP,
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR(255)
);

CREATE INDEX idx_mcr_patient_id ON marketplace_consultation_requests(patient_id);
CREATE INDEX idx_mcr_status     ON marketplace_consultation_requests(status);

-- ── Consultation offers (one per doctor per request) ──────────────────────────

CREATE TABLE marketplace_consultation_offers (
    id                BIGSERIAL   PRIMARY KEY,
    request_id        BIGINT      NOT NULL REFERENCES marketplace_consultation_requests(id) ON DELETE CASCADE,
    doctor_profile_id BIGINT      NOT NULL REFERENCES marketplace_doctor_profiles(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | ACCEPTED | INVALIDATED | EXPIRED
    responded_at      TIMESTAMP,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    UNIQUE (request_id, doctor_profile_id)
);

CREATE INDEX idx_mco_request_id ON marketplace_consultation_offers(request_id);
CREATE INDEX idx_mco_doctor_id  ON marketplace_consultation_offers(doctor_profile_id);
