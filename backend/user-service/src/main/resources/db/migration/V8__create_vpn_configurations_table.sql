CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE vpn_configurations (
                                    id BIGSERIAL PRIMARY KEY,
                                    device_id BIGINT NOT NULL,
                                    user_id BIGINT NOT NULL,
                                    server_id INTEGER NOT NULL,

                                    vless_uuid UUID UNIQUE NOT NULL,
                                    vless_link TEXT NOT NULL,
                                    qr_code_base64 TEXT,

                                    protocol VARCHAR(20) NOT NULL DEFAULT 'VLESS',
                                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP DEFAULT NOW(),
                                    revoked_at TIMESTAMP,
                                    last_used_at TIMESTAMP,

                                    CONSTRAINT valid_protocol CHECK (protocol IN ('VLESS', 'HYSTERIA2', 'TROJAN', 'TUIC', 'SHADOWTLS')),
                                    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'SUSPENDED'))
);

CREATE INDEX idx_vpn_config_device ON vpn_configurations(device_id);
CREATE INDEX idx_vpn_config_user ON vpn_configurations(user_id);
CREATE INDEX idx_vpn_config_server ON vpn_configurations(server_id);
CREATE INDEX idx_vpn_config_uuid ON vpn_configurations(vless_uuid);
CREATE INDEX idx_vpn_config_status ON vpn_configurations(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_vpn_config_created ON vpn_configurations(created_at DESC);

CREATE TRIGGER update_vpn_configurations_updated_at
    BEFORE UPDATE ON vpn_configurations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE vpn_configurations IS 'История выданных VPN конфигураций';
COMMENT ON COLUMN vpn_configurations.vless_uuid IS 'UUID для VLESS протокола (уникальный идентификатор клиента)';
COMMENT ON COLUMN vpn_configurations.qr_code_base64 IS 'QR код в формате Base64 для мобильных приложений';