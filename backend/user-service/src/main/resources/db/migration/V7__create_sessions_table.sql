CREATE TABLE sessions (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,

                          session_token VARCHAR(255) UNIQUE NOT NULL,
                          refresh_token VARCHAR(255),

                          device_id BIGINT REFERENCES devices(id) ON DELETE SET NULL,
                          ip_address VARCHAR(45),
                          user_agent TEXT,

                          created_at TIMESTAMP DEFAULT NOW(),
                          expires_at TIMESTAMP NOT NULL,
                          last_activity_at TIMESTAMP DEFAULT NOW(),

                          is_active BOOLEAN DEFAULT true,

                          CONSTRAINT valid_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_token ON sessions(session_token);
CREATE INDEX idx_sessions_expiry ON sessions(expires_at);

CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS void AS $$
BEGIN
UPDATE sessions
SET is_active = false
WHERE expires_at < NOW() AND is_active = true;
END;
$$ LANGUAGE plpgsql;