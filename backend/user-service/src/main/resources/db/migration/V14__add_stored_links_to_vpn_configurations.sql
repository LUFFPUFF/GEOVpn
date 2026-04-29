ALTER TABLE vpn_configurations
    ADD COLUMN IF NOT EXISTS vless_links_json  JSONB,

    ADD COLUMN IF NOT EXISTS relay_links_json  JSONB,

    ADD COLUMN IF NOT EXISTS hy2_link          TEXT,

    ADD COLUMN IF NOT EXISTS device_os         VARCHAR(50),

    ADD COLUMN IF NOT EXISTS device_name       VARCHAR(255),

    ADD COLUMN IF NOT EXISTS links_built_at    TIMESTAMP;

COMMENT ON COLUMN vpn_configurations.vless_links_json IS
    '[{"serverId":1,"serverName":"Helsinki-1","countryCode":"FI","link":"vless://...","avgLatencyMs":30}]';
COMMENT ON COLUMN vpn_configurations.relay_links_json IS
    '[{"serverId":10,"serverName":"RU-Relay-1","countryCode":"RU","link":"vless://...","relayPriority":1}]';
COMMENT ON COLUMN vpn_configurations.hy2_link IS
    'hy2://uuid@host:port?...#Name';
COMMENT ON COLUMN vpn_configurations.device_os IS
    'iOS | Android | Windows | macOS | Linux | Unknown';

CREATE INDEX IF NOT EXISTS idx_vpn_configs_vless_links
    ON vpn_configurations USING GIN (vless_links_json);

CREATE INDEX IF NOT EXISTS idx_vpn_configs_user_status
    ON vpn_configurations(user_id, status);