-- Credential documents attached to a doctor application (certificates, headshot).
-- Files are written to ~/HealthSuite/doctor_docs/{YYYYMMDD}/ on disk.

CREATE TABLE doctor_documents (
    id                  BIGSERIAL       PRIMARY KEY,
    doctor_profile_id   BIGINT          NOT NULL REFERENCES marketplace_doctor_profiles(id) ON DELETE CASCADE,
    doc_type            VARCHAR(20)     NOT NULL,   -- CERTIFICATE | HEADSHOT
    file_name           VARCHAR(500)    NOT NULL,
    file_path           VARCHAR(1000)   NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dd_doctor_profile_id ON doctor_documents(doctor_profile_id);
