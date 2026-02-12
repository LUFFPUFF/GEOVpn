CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       telegram_id BIGINT NOT NULL UNIQUE,
                       username VARCHAR(255),
                       first_name VARCHAR(255),
                       role VARCHAR(50) NOT NULL DEFAULT 'USER',

                       device_limit INT DEFAULT 3,

                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_telegram_id ON users(telegram_id);

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id UUID NOT NULL REFERENCES users(id),

                               status VARCHAR(50) NOT NULL,
                               plan_type VARCHAR(50),

                               starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_expires_at ON subscriptions(expires_at);

CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          user_id UUID NOT NULL REFERENCES users(id),

                          provider_payment_id VARCHAR(255),
                          amount DECIMAL(10, 2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
                          status VARCHAR(50) NOT NULL,

                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);