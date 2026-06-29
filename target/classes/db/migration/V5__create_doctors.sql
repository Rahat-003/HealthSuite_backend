-- ── Doctor directory (populated by scraper, BMDC-based dedup) ────────────────
-- Upsert rule: match on bmdc_number first; fall back to (full_name + specialty + chamber_address).
-- If match found → update. If no match → insert new row.
CREATE TABLE doctors (
    id                 BIGSERIAL      PRIMARY KEY,
    bmdc_number        VARCHAR(50)    UNIQUE,              -- authoritative dedup key
    full_name          VARCHAR(255)   NOT NULL,
    specialty          VARCHAR(255),
    chamber_address    TEXT,
    hospital_name      VARCHAR(255),
    consultation_fee   DECIMAL(10, 2),
    available_slots    TEXT,                               -- free-text or JSON from source
    phone_number       VARCHAR(20),
    profile_url        VARCHAR(1000),
    is_active          BOOLEAN        NOT NULL DEFAULT TRUE,
    last_scraped_at    TIMESTAMP,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP
);

-- ── Indexes ────────────────────────────────────────────────────────────────────
CREATE INDEX idx_doctors_bmdc         ON doctors(bmdc_number) WHERE bmdc_number IS NOT NULL;
CREATE INDEX idx_doctors_name         ON doctors(full_name);
CREATE INDEX idx_doctors_specialty    ON doctors(specialty);
CREATE INDEX idx_doctors_name_spec_ch ON doctors(full_name, specialty, chamber_address);
