ALTER TABLE devices DROP CONSTRAINT valid_device_type;
ALTER TABLE devices ADD CONSTRAINT valid_device_type CHECK (
    device_type IN ('IOS', 'ANDROID', 'DESKTOP', 'WINDOWS', 'MACOS', 'LINUX', 'UNKNOWN')
);
