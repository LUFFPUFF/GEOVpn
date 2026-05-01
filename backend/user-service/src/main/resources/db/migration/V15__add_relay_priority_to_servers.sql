ALTER TABLE servers
    ADD COLUMN IF NOT EXISTS grpc_port       INTEGER,
    ADD COLUMN IF NOT EXISTS is_relay        BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS relay_sni       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS relay_public_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS relay_short_id  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS relay_priority  INTEGER     NOT NULL DEFAULT 0;

COMMENT ON COLUMN servers.is_relay        IS 'true = антиглушилка/relay, false = обычный VPS';
COMMENT ON COLUMN servers.relay_sni       IS 'SNI для relay-соединения (напр. eh.vk.com)';
COMMENT ON COLUMN servers.relay_priority  IS 'Порядок сортировки relay-серверов (меньше = выше)';

CREATE INDEX IF NOT EXISTS idx_servers_relay ON servers(is_relay, relay_priority)
