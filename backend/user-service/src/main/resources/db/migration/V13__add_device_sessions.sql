CREATE TABLE device_sessions (
                                 id                  BIGSERIAL PRIMARY KEY,
                                 user_id             BIGINT        NOT NULL,
                                 device_fingerprint  VARCHAR(128)  NOT NULL,
                                 vless_uuid          UUID          NOT NULL,
                                 user_agent          VARCHAR(512),
                                 device_name         VARCHAR(256),
                                 last_ip             VARCHAR(45),
                                 is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
                                 first_seen_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
                                 last_seen_at        TIMESTAMP,

                                 CONSTRAINT uq_device_fingerprint UNIQUE (user_id, device_fingerprint)
);

CREATE INDEX idx_device_sessions_user_id ON device_sessions(user_id);
CREATE INDEX idx_device_sessions_active  ON device_sessions(user_id, is_active);