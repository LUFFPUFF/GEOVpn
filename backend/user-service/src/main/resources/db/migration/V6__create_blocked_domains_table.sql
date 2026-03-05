CREATE TABLE blocked_domains (
                                 id SERIAL PRIMARY KEY,
                                 domain VARCHAR(255) UNIQUE NOT NULL,

                                 is_blocked BOOLEAN DEFAULT true,

                                 auto_detected BOOLEAN DEFAULT false,
                                 source VARCHAR(100),

                                 last_checked TIMESTAMP DEFAULT NOW(),
                                 created_at TIMESTAMP DEFAULT NOW(),

                                 category VARCHAR(50),

                                 CONSTRAINT valid_domain CHECK (LENGTH(domain) > 0)
);

CREATE INDEX idx_blocked_domains_active ON blocked_domains(domain) WHERE is_blocked = true;
CREATE INDEX idx_blocked_domains_category ON blocked_domains(category) WHERE category IS NOT NULL;
CREATE INDEX idx_blocked_domains_source ON blocked_domains(source);

CREATE INDEX idx_blocked_domains_search ON blocked_domains USING gin(to_tsvector('english', domain));

INSERT INTO blocked_domains (domain, is_blocked, source, category)
VALUES
    ('facebook.com', true, 'roskomnadzor', 'social'),
    ('twitter.com', true, 'roskomnadzor', 'social'),
    ('instagram.com', true, 'roskomnadzor', 'social'),
    ('youtube.com', false, 'test', 'video'),
    ('linkedin.com', true, 'roskomnadzor', 'professional');