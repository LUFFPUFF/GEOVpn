CREATE TABLE IF NOT EXISTS traffic_usage (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             device_id BIGINT NOT NULL,
                                             server_id INTEGER NOT NULL,
                                             config_id BIGINT NOT NULL,

                                             bytes_in BIGINT NOT NULL DEFAULT 0,
                                             bytes_out BIGINT NOT NULL DEFAULT 0,
                                             bytes_total BIGINT GENERATED ALWAYS AS (bytes_in + bytes_out) STORED,

    cost_kopecks INTEGER NOT NULL DEFAULT 0,

    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_traffic_server FOREIGN KEY (server_id)
    REFERENCES servers(id) ON DELETE CASCADE
    );

CREATE INDEX idx_traffic_user_id ON traffic_usage(user_id);
CREATE INDEX idx_traffic_collected_at ON traffic_usage(collected_at);
CREATE INDEX idx_traffic_config_id ON traffic_usage(config_id);
CREATE INDEX idx_traffic_server_id ON traffic_usage(server_id);

COMMENT ON TABLE traffic_usage IS 'Traffic consumption records from Xray Stats API';
COMMENT ON COLUMN traffic_usage.bytes_in IS 'Upload traffic (client -> server)';
COMMENT ON COLUMN traffic_usage.bytes_out IS 'Download traffic (server -> client)';
COMMENT ON COLUMN traffic_usage.cost_kopecks IS 'Cost in kopecks (for PAYG billing)';