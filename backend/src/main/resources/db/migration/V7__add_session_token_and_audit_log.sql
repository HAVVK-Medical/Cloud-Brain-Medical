CREATE TABLE session_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    patient_id BIGINT,
    doctor_id BIGINT,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    token_hash VARCHAR(128) NOT NULL,
    refresh_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    revoked_at TIMESTAMP,
    logout_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_session_token_token_id UNIQUE (token_id),
    CONSTRAINT uk_session_token_token_hash UNIQUE (token_hash),
    CONSTRAINT uk_session_token_refresh_hash UNIQUE (refresh_hash)
);

CREATE INDEX idx_session_token_user_status ON session_token (user_id, status);
CREATE INDEX idx_session_token_refresh_hash ON session_token (refresh_hash);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    actor_role VARCHAR(32),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id BIGINT,
    trace_id VARCHAR(64),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    message TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_action_time ON audit_log (action, occurred_at DESC);
