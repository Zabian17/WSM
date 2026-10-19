CREATE TABLE mobile_api_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash BINARY(32) NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile_api_session_token_hash (token_hash),
    KEY idx_mobile_api_session_user_expiry (user_id, expires_at),
    CONSTRAINT fk_mobile_api_session_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE mobile_api_login_attempt (
    attempt_key BINARY(32) NOT NULL,
    window_started_at DATETIME NOT NULL,
    attempts INT NOT NULL,
    PRIMARY KEY (attempt_key),
    KEY idx_mobile_api_login_attempt_window (window_started_at)
);
