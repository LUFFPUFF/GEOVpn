ALTER TABLE blocked_domains
    ADD COLUMN match_type VARCHAR(20) DEFAULT 'EXACT' NOT NULL;

ALTER TABLE blocked_domains
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW() NOT NULL;

ALTER TABLE blocked_domains
    ALTER COLUMN is_blocked SET NOT NULL,
ALTER COLUMN auto_detected SET NOT NULL,
ALTER COLUMN source SET NOT NULL;

ALTER TABLE blocked_domains
ALTER COLUMN id TYPE BIGINT;

ALTER TABLE blocked_domains
    ADD CONSTRAINT valid_match_type CHECK (match_type IN ('EXACT', 'WILDCARD', 'REGEX'));

UPDATE blocked_domains SET domain = LOWER(domain);

ALTER TABLE blocked_domains
    ADD CONSTRAINT valid_domain_lowercase CHECK (domain = LOWER(domain));

DROP INDEX IF EXISTS idx_blocked_domains_active;
CREATE INDEX idx_blocked_domains_lookup ON blocked_domains(domain, match_type);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_blocked_domains_updated_at'
    ) THEN
CREATE TRIGGER update_blocked_domains_updated_at
    BEFORE UPDATE ON blocked_domains
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
END IF;
END
$$;