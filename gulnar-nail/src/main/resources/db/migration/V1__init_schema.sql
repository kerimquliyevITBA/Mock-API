-- Gulnar Nail schema — all objects live under this schema so they never mix
-- with anything else running on the same Supabase Postgres instance.

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    full_name     VARCHAR(120),
    phone         VARCHAR(30),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS services (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    duration_min INT           NOT NULL DEFAULT 60 CHECK (duration_min > 0),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS availability_days (
    id         BIGSERIAL PRIMARY KEY,
    day_date   DATE        NOT NULL UNIQUE,
    is_closed  BOOLEAN     NOT NULL DEFAULT FALSE,
    note       VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS availability_slots (
    id         BIGSERIAL PRIMARY KEY,
    day_id     BIGINT      NOT NULL REFERENCES availability_days(id) ON DELETE CASCADE,
    slot_time  TIME        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (day_id, slot_time)
);
CREATE INDEX IF NOT EXISTS ix_slots_day ON availability_slots(day_id);

CREATE TABLE IF NOT EXISTS reservations (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(20)  NOT NULL UNIQUE,
    customer_name     VARCHAR(120) NOT NULL,
    phone             VARCHAR(30)  NOT NULL,
    service_id        BIGINT       NOT NULL REFERENCES services(id),
    service_name      VARCHAR(120) NOT NULL,
    price_snapshot    NUMERIC(10,2) NOT NULL,
    reservation_date  DATE         NOT NULL,
    reservation_time  TIME         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cancelled_at      TIMESTAMPTZ,
    cancelled_by      VARCHAR(60),
    CONSTRAINT chk_res_status CHECK (status IN ('ACTIVE','CANCELLED','COMPLETED'))
);
CREATE INDEX IF NOT EXISTS ix_res_datetime ON reservations(reservation_date, reservation_time);
CREATE INDEX IF NOT EXISTS ix_res_phone    ON reservations(phone);
CREATE INDEX IF NOT EXISTS ix_res_status   ON reservations(status);

-- Only one ACTIVE reservation per (date, time)
CREATE UNIQUE INDEX IF NOT EXISTS ux_res_active_slot
    ON reservations(reservation_date, reservation_time)
    WHERE status = 'ACTIVE';

-- The same phone cannot hold two ACTIVE reservations on the same date+time
CREATE UNIQUE INDEX IF NOT EXISTS ux_res_active_phone_slot
    ON reservations(phone, reservation_date, reservation_time)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGSERIAL PRIMARY KEY,
    actor      VARCHAR(80),
    action     VARCHAR(60)  NOT NULL,
    entity     VARCHAR(60)  NOT NULL,
    entity_id  VARCHAR(80),
    payload    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_audit_created ON audit_log(created_at DESC);
