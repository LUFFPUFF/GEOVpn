CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,

                              amount INTEGER NOT NULL,
                              transaction_type VARCHAR(50) NOT NULL,
                              payment_method VARCHAR(50),

                              status VARCHAR(50) DEFAULT 'pending' NOT NULL,

                              external_transaction_id VARCHAR(255),
                              payment_provider VARCHAR(50),

                              description TEXT,

                              metadata JSONB,

                              created_at TIMESTAMP DEFAULT NOW(),
                              completed_at TIMESTAMP,

                              CONSTRAINT valid_transaction_type CHECK (
                                  transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'REFERRAL_BONUS', 'SUBSCRIPTION', 'USAGE_FEE', 'REFUND')
                                  ),
                              CONSTRAINT valid_status CHECK (
                                  status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')
                                  ),
                              CONSTRAINT valid_payment_method CHECK (
                                  payment_method IS NULL OR
                                  payment_method IN ('YOOMONEY', 'CRYPTO', 'CARD', 'BANK_TRANSFER', 'REFERRAL')
                                  )
);

CREATE INDEX idx_transactions_user ON transactions(user_id, created_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_external ON transactions(external_transaction_id);

CREATE VIEW user_transaction_stats AS
SELECT
    user_id,
    COUNT(*) AS total_transactions,
    SUM(CASE WHEN transaction_type = 'DEPOSIT' THEN amount ELSE 0 END) AS total_deposits,
    SUM(CASE WHEN transaction_type = 'USAGE_FEE' THEN amount ELSE 0 END) AS total_spent,
    SUM(CASE WHEN transaction_type = 'REFERRAL_BONUS' THEN amount ELSE 0 END) AS total_referral_earnings
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY user_id;