-- ── Medical visits (one per clinic visit) ─────────────────────────────────────
CREATE TABLE medical_visits (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visit_date       DATE         NOT NULL,
    doctor_name      VARCHAR(255) NOT NULL,
    doctor_specialty VARCHAR(255),
    hospital_name    VARCHAR(255),
    notes            TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(255)
);

-- ── Diagnoses / symptoms attached to a visit ──────────────────────────────────
CREATE TABLE diagnoses (
    id       BIGSERIAL    PRIMARY KEY,
    visit_id BIGINT       NOT NULL REFERENCES medical_visits(id) ON DELETE CASCADE,
    name     VARCHAR(255) NOT NULL,
    type     VARCHAR(20)  NOT NULL DEFAULT 'DISEASE',  -- DISEASE | SYMPTOM
    notes    TEXT
);

-- ── Medications per visit ─────────────────────────────────────────────────────
-- dosage_pattern examples: "1+0+1" (morning+night), "1+1+1", "10mg"
CREATE TABLE medications (
    id             BIGSERIAL    PRIMARY KEY,
    visit_id       BIGINT       NOT NULL REFERENCES medical_visits(id) ON DELETE CASCADE,
    drug_name      VARCHAR(255) NOT NULL,
    dosage_pattern VARCHAR(50)  NOT NULL,
    duration_days  INT,
    meal_timing    VARCHAR(20)  NOT NULL DEFAULT 'AFTER_MEAL',  -- BEFORE_MEAL | AFTER_MEAL
    notes          TEXT
);

-- ── Uploaded documents (prescriptions, lab reports, imaging) ──────────────────
CREATE TABLE visit_documents (
    id               BIGSERIAL     PRIMARY KEY,
    visit_id         BIGINT        NOT NULL REFERENCES medical_visits(id) ON DELETE CASCADE,
    file_url         VARCHAR(1000) NOT NULL,
    file_name        VARCHAR(255),
    file_type        VARCHAR(100),                        -- MIME type
    storage_provider VARCHAR(20)   NOT NULL DEFAULT 'S3', -- S3 | CLOUDINARY
    uploaded_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Indexes ────────────────────────────────────────────────────────────────────
CREATE INDEX idx_medical_visits_user_id   ON medical_visits(user_id);
CREATE INDEX idx_medical_visits_date      ON medical_visits(user_id, visit_date DESC);
CREATE INDEX idx_diagnoses_visit_id       ON diagnoses(visit_id);
CREATE INDEX idx_medications_visit_id     ON medications(visit_id);
CREATE INDEX idx_visit_documents_visit_id ON visit_documents(visit_id);
