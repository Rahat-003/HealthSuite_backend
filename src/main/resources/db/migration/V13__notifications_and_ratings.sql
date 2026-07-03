-- In-app notifications + post-consultation ratings

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,      -- CASE_ACCEPTED / NEW_CASE / PRESCRIPTION_READY / ...
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(500),
    link        VARCHAR(300),              -- in-app route to open
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read, created_at DESC);

CREATE TABLE marketplace_consultation_ratings (
    id                 BIGSERIAL PRIMARY KEY,
    consultation_id    BIGINT NOT NULL UNIQUE REFERENCES marketplace_consultation_requests(id) ON DELETE CASCADE,
    doctor_profile_id  BIGINT NOT NULL REFERENCES marketplace_doctor_profiles(id),
    patient_id         BIGINT NOT NULL REFERENCES users(id),
    stars              INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment            VARCHAR(1000),
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ratings_doctor ON marketplace_consultation_ratings(doctor_profile_id);

-- number of ratings backing marketplace_doctor_profiles.rating
ALTER TABLE marketplace_doctor_profiles ADD COLUMN rating_count INT NOT NULL DEFAULT 0;
