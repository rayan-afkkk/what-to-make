// Turso (SQLite) tables. Safe to run repeatedly; `npm run setup` applies them.
export const SCHEMA = [
  `CREATE TABLE IF NOT EXISTS users (
     id             TEXT PRIMARY KEY,
     plan           TEXT NOT NULL DEFAULT 'free',
     pro_expires_at INTEGER,
     created_at     INTEGER NOT NULL
   )`,
  `CREATE TABLE IF NOT EXISTS sessions (
     token_hash   TEXT PRIMARY KEY,
     user_id      TEXT NOT NULL,
     created_at   INTEGER NOT NULL
   )`,
  `CREATE INDEX IF NOT EXISTS sessions_user ON sessions(user_id)`,
  `CREATE TABLE IF NOT EXISTS usage (
     user_id    TEXT NOT NULL,
     week_start INTEGER NOT NULL,
     used       INTEGER NOT NULL DEFAULT 0,
     PRIMARY KEY (user_id, week_start)
   )`,
  `CREATE TABLE IF NOT EXISTS purchases (
     purchase_token TEXT PRIMARY KEY,
     user_id        TEXT NOT NULL,
     product_id     TEXT NOT NULL,
     expires_at     INTEGER,
     created_at     INTEGER NOT NULL
   )`,
  `CREATE INDEX IF NOT EXISTS purchases_user ON purchases(user_id)`,
  `CREATE TABLE IF NOT EXISTS rate_limits (
     key        TEXT PRIMARY KEY,
     count      INTEGER NOT NULL,
     expires_at INTEGER NOT NULL
   )`,
];
