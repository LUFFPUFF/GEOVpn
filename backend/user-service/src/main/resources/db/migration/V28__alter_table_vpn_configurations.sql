ALTER TABLE vpn_configurations DROP CONSTRAINT valid_status;

ALTER TABLE vpn_configurations ADD CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'BANNED', 'LIMIT_EXCEEDED'));