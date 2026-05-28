CREATE TABLE vpn_ban_logs (
                              id SERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              device_id BIGINT NOT NULL,
                              banned_at TIMESTAMP NOT NULL,
                              reason VARCHAR(255) NOT NULL,
                              visited_domains TEXT,
                              traffic_consumed_mb BIGINT,
                              status VARCHAR(50) DEFAULT 'ACTIVE'
);