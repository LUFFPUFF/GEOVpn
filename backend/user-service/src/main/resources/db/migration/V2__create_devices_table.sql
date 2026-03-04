CREATE TABLE devices (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT REFERENCES users(telegram_id),
                         device_name VARCHAR(255) NOT NULL,
                         device_type VARCHAR(50) NOT NULL,
                         uuid UUID UNIQUE NOT NULL,
                         is_active BOOLEAN DEFAULT true,
                         last_connected_at TIMESTAMP,
                         created_at TIMESTAMP DEFAULT NOW(),

                         CONSTRAINT max_devices_per_user CHECK (
                             device_type IN ('ios', 'android', 'desktop', 'unknown')
                             )
);

CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_uuid ON devices(uuid);