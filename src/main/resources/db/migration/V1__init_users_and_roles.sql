-- ── Roles ─────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(50)  NOT NULL UNIQUE
);

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255),                       -- NULL for social-only accounts
    phone_number   VARCHAR(20)  NOT NULL UNIQUE,       -- Validated BD format by app layer
    full_name      VARCHAR(255) NOT NULL,
    auth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    is_premium     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    fcm_token      VARCHAR(500),                       -- Firebase device token for push
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(255)
);

-- ── User ↔ Role join table ─────────────────────────────────────────────────────
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── Social accounts (Google / Facebook) ───────────────────────────────────────
CREATE TABLE social_accounts (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(255),
    UNIQUE (provider, provider_user_id)
);

-- ── Refresh tokens ─────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Seed roles ────────────────────────────────────────────────────────────────
INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_PREMIUM'),
    ('ROLE_SUPPORT'),
    ('ROLE_ADMIN'),
    ('ROLE_DOCTOR');

-- ── Indexes ────────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_users_phone        ON users(phone_number);
CREATE INDEX idx_social_accounts    ON social_accounts(provider, provider_user_id);
CREATE INDEX idx_refresh_tokens     ON refresh_tokens(token, is_revoked);
