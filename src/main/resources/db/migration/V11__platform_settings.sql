-- Singleton row of platform-wide, admin-configurable settings.
CREATE TABLE platform_settings (
    id                              BIGINT          PRIMARY KEY DEFAULT 1,
    max_doctors_per_consultation    INT             NOT NULL DEFAULT 3,
    consultation_cancel_wait_hours  INT             NOT NULL DEFAULT 2,
    CONSTRAINT chk_platform_settings_singleton CHECK (id = 1)
);

INSERT INTO platform_settings (id, max_doctors_per_consultation, consultation_cancel_wait_hours)
VALUES (1, 3, 2);
