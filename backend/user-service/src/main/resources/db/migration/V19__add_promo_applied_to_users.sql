ALTER TABLE users ADD COLUMN promo_applied BOOLEAN;
UPDATE users SET promo_applied = FALSE WHERE promo_applied IS NULL;

ALTER TABLE users ALTER COLUMN promo_applied SET DEFAULT FALSE;
ALTER TABLE users ALTER COLUMN promo_applied SET NOT NULL;