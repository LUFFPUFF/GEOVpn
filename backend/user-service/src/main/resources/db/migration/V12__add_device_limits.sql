CREATE TABLE device_limits (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT NOT NULL UNIQUE,
                               max_devices INTEGER NOT NULL DEFAULT 1,
                               plan_name   VARCHAR(50),
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at  TIMESTAMP,
                               expires_at  TIMESTAMP
);

CREATE INDEX idx_device_limits_user_id ON device_limits(user_id);

INSERT INTO device_limits (user_id, max_devices, plan_name)
SELECT DISTINCT user_id, 1, 'BASIC'
FROM vpn_configurations
    ON CONFLICT (user_id) DO NOTHING;