CREATE TABLE connections (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
                             device_id BIGINT REFERENCES devices(id) ON DELETE SET NULL,
                             server_id INTEGER REFERENCES servers(id) ON DELETE SET NULL,

                             protocol VARCHAR(20) NOT NULL,

                             connected_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             disconnected_at TIMESTAMP,

                             bytes_sent BIGINT DEFAULT 0,
                             bytes_received BIGINT DEFAULT 0,

                             duration_seconds INTEGER,

                             avg_latency_ms INTEGER,
                             packet_loss_percentage NUMERIC(5,2),

                             CONSTRAINT positive_traffic CHECK (bytes_sent >= 0 AND bytes_received >= 0),
                             CONSTRAINT valid_protocol CHECK (protocol IN ('VLESS', 'HYSTERIA2', 'TROJAN', 'TUIC', 'SHADOWTLS')),
                             CONSTRAINT valid_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX idx_connections_user ON connections(user_id, connected_at DESC);
CREATE INDEX idx_connections_device ON connections(device_id);
CREATE INDEX idx_connections_server ON connections(server_id);
CREATE INDEX idx_connections_active ON connections(user_id) WHERE disconnected_at IS NULL;

CREATE VIEW active_connections AS
SELECT
    c.id,
    c.user_id,
    u.username,
    d.device_name,
    s.name AS server_name,
    s.location AS server_location,
    c.protocol,
    c.connected_at,
    EXTRACT(EPOCH FROM (NOW() - c.connected_at))::INTEGER AS duration_seconds,
    c.bytes_sent,
    c.bytes_received
FROM connections c
         JOIN users u ON c.user_id = u.telegram_id
         LEFT JOIN devices d ON c.device_id = d.id
         LEFT JOIN servers s ON c.server_id = s.id
WHERE c.disconnected_at IS NULL;