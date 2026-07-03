-- Auto-filed visits know which consultation produced them, so the PHR can
-- surface the full prescription (sheet, attached copies, PDF) with the visit —
-- including for family members the visit is shared with.
ALTER TABLE medical_visits
    ADD COLUMN consultation_id BIGINT REFERENCES marketplace_consultation_requests(id) ON DELETE SET NULL;

CREATE INDEX idx_medical_visits_consultation ON medical_visits(consultation_id)
    WHERE consultation_id IS NOT NULL;
