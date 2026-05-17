ALTER TABLE users ADD COLUMN easter_egg_claimed BOOLEAN;
UPDATE users SET easter_egg_claimed = FALSE WHERE easter_egg_claimed IS NULL;

ALTER TABLE users ALTER COLUMN easter_egg_claimed SET DEFAULT FALSE;
ALTER TABLE users ALTER COLUMN easter_egg_claimed SET NOT NULL;