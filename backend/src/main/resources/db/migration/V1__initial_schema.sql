-- Схема питомника. Поля clients и reservations продолжают предметную область КР №1.
-- Дополнительные поля, теги, заметки, пользователи и аудит добавлены для REST API.

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    user_agent VARCHAR(500),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user ON refresh_session(user_id);
CREATE INDEX idx_refresh_expires ON refresh_session(expires_at);

CREATE TABLE clients (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_clients_name ON clients(full_name);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    animal_name VARCHAR(100) NOT NULL,
    comment VARCHAR(500),
    reservation_date DATE NOT NULL,
    client_id UUID NOT NULL REFERENCES clients(id),
    status VARCHAR(32) NOT NULL,
    deposit_amount NUMERIC(12,2) NOT NULL,
    needs_care BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(32) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,
    archived_by UUID REFERENCES app_user(id),
    created_by UUID REFERENCES app_user(id),
    updated_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_reservation_status CHECK (status IN ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_reservation_priority CHECK (priority IN ('NOT_APPLICABLE', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_reservation_deposit CHECK (deposit_amount >= 0 AND deposit_amount <= 1000000)
);

CREATE INDEX idx_reservations_animal ON reservations(animal_name);
CREATE INDEX idx_reservations_client ON reservations(client_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_archived ON reservations(archived);
CREATE INDEX idx_reservations_priority ON reservations(priority);
CREATE INDEX idx_reservations_date ON reservations(reservation_date);

CREATE TABLE reservation_tag (
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (reservation_id, tag)
);

CREATE INDEX idx_reservation_tag_value ON reservation_tag(tag);

CREATE TABLE reservation_note (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    body VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_note_reservation ON reservation_note(reservation_id, pinned DESC, created_at DESC);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(254),
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    summary VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(100),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    details_json TEXT
);

CREATE INDEX idx_audit_occurred ON audit_event(occurred_at DESC);
CREATE INDEX idx_audit_actor ON audit_event(actor_email);
CREATE INDEX idx_audit_entity ON audit_event(entity_type, entity_id);
