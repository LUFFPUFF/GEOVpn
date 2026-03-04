CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       telegram_id BIGINT UNIQUE NOT NULL,
                       username VARCHAR(255),
                       first_name VARCHAR(255),
                       balance INTEGER DEFAULT 0,
                       subscription_type VARCHAR(50) DEFAULT 'payg',
                       subscription_expires_at TIMESTAMP,
                       referral_code VARCHAR(20) UNIQUE,
                       referred_by BIGINT REFERENCES users(telegram_id),
                       created_at TIMESTAMP DEFAULT NOW(),
                       last_active_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_telegram ON users(telegram_id);
CREATE INDEX idx_users_referral ON users(referral_code);

