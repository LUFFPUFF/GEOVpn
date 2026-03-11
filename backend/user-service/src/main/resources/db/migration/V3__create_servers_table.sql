CREATE TABLE servers (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL UNIQUE,
                         location VARCHAR(50) NOT NULL,
                         country_code CHAR(2) NOT NULL,
                         ip_address VARCHAR(45) NOT NULL,
                         port INTEGER NOT NULL,

                         reality_public_key VARCHAR(255),
                         reality_short_id VARCHAR(50),
                         reality_sni VARCHAR(255) DEFAULT 'www.google.com',

                         is_active BOOLEAN DEFAULT true,
                         max_connections INTEGER DEFAULT 1000,
                         current_connections INTEGER DEFAULT 0,

                         avg_latency_ms INTEGER,
                         health_score NUMERIC(5,2) DEFAULT 100.00,
                         last_health_check TIMESTAMP,

                         created_at TIMESTAMP DEFAULT NOW(),
                         updated_at TIMESTAMP DEFAULT NOW(),

                         CONSTRAINT positive_max_connections CHECK (max_connections > 0),
                         CONSTRAINT valid_health_score CHECK (health_score >= 0 AND health_score <= 100),
                         CONSTRAINT valid_port CHECK (port BETWEEN 1 AND 65535)
);

CREATE INDEX idx_servers_active ON servers(is_active);
CREATE INDEX idx_servers_location ON servers(country_code, location);
CREATE INDEX idx_servers_health ON servers(health_score DESC);

INSERT INTO servers (name, location, country_code, ip_address, port, reality_public_key, reality_short_id, max_connections)
VALUES
    ('Riga-1', 'Latvia', 'LV', '185.230.127.10', 443, 'test_public_key_1', '0123456789abcdef', 1000),
    ('Riga-2', 'Latvia', 'LV', '185.230.127.11', 443, 'test_public_key_2', '0123456789abcdef', 1000),
    ('Helsinki-1', 'Finland', 'FI', '95.216.24.100', 443, 'test_public_key_3', '0123456789abcdef', 1000),
    ('Helsinki-2', 'Finland', 'FI', '95.216.24.101', 443, 'test_public_key_4', '0123456789abcdef', 1000),
    ('Amsterdam-1', 'Netherlands', 'NL', '94.142.241.50', 443, 'test_public_key_5', '0123456789abcdef', 1000),
    ('Amsterdam-2', 'Netherlands', 'NL', '94.142.241.51', 443, 'test_public_key_6', '0123456789abcdef', 1000);