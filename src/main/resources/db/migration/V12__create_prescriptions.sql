-- Prescriptions: written by the accepted doctor at the end of a video consultation.
-- One prescription per consultation; submission completes the case.

CREATE TABLE marketplace_prescriptions (
    id                  BIGSERIAL PRIMARY KEY,
    consultation_id     BIGINT NOT NULL UNIQUE REFERENCES marketplace_consultation_requests(id) ON DELETE CASCADE,
    doctor_profile_id   BIGINT NOT NULL REFERENCES marketplace_doctor_profiles(id),
    patient_id          BIGINT NOT NULL REFERENCES users(id),
    diagnosis           TEXT,
    remarks             TEXT,
    follow_up_days      INT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE TABLE marketplace_prescription_medicines (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES marketplace_prescriptions(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    dosage          VARCHAR(100),
    frequency       VARCHAR(100),
    duration        VARCHAR(100),
    instructions    VARCHAR(300)
);

CREATE TABLE marketplace_prescription_tests (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES marketplace_prescriptions(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    note            VARCHAR(300)
);

-- Uploaded prescription copies (image/PDF). file_path points into the
-- local health-record store: {root}/{patientUserId}/{yyyyMMdd}/{file}
CREATE TABLE marketplace_prescription_files (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES marketplace_prescriptions(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(1000) NOT NULL,
    content_type    VARCHAR(100),
    size_bytes      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_prescriptions_patient ON marketplace_prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor  ON marketplace_prescriptions(doctor_profile_id);
