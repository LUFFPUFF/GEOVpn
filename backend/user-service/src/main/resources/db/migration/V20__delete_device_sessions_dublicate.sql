DELETE FROM device_sessions a
    USING device_sessions b
WHERE a.id < b.id
  AND a.user_id = b.user_id
  AND a.vless_uuid = b.vless_uuid;

ALTER TABLE device_sessions
    ADD CONSTRAINT uq_user_vless_config UNIQUE (user_id, vless_uuid);