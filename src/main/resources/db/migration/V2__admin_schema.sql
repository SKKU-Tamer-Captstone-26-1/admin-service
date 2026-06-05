CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE IF NOT EXISTS admin.places (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    address         TEXT        NOT NULL,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    business_hours  JSONB,
    contact         VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS admin.products (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id      UUID        NOT NULL REFERENCES admin.places(id),
    name          VARCHAR(200) NOT NULL,
    category      VARCHAR(50),
    image_url     TEXT,
    in_stock      BOOLEAN     NOT NULL DEFAULT TRUE,
    price         INT,
    display_order INT         NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_products_place_id ON admin.products(place_id);

CREATE TABLE IF NOT EXISTS admin.place_manager_mappings (
    place_id   UUID        NOT NULL REFERENCES admin.places(id),
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (place_id, user_id)
);

CREATE TABLE IF NOT EXISTS admin.change_requests (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id     UUID        NOT NULL,
    target_type      VARCHAR(30) NOT NULL,
    target_ref       UUID        NOT NULL,
    proposed_changes JSONB       NOT NULL,
    attachments      TEXT[]      NOT NULL DEFAULT '{}',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id      UUID,
    reviewed_at      TIMESTAMPTZ,
    review_comment   TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cr_requester_id ON admin.change_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_cr_status       ON admin.change_requests(status);

CREATE TABLE IF NOT EXISTS admin.audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID        NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    detail_json JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON admin.audit_logs(actor_id);
