CREATE TABLE devices (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
                         device_name VARCHAR(255) NOT NULL,
                         device_type VARCHAR(50) NOT NULL,
                         uuid UUID UNIQUE NOT NULL,
                         is_active BOOLEAN DEFAULT true,
                         last_connected_at TIMESTAMP,
                         created_at TIMESTAMP DEFAULT NOW(),

                         CONSTRAINT valid_device_type CHECK (
                             device_type IN ('IOS', 'ANDROID', 'DESKTOP', 'UNKNOWN')
                             )
);

CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_uuid ON devices(uuid);
CREATE INDEX idx_devices_active ON devices(user_id, is_active);