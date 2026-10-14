// SQLite storage using Node's built-in node:sqlite (no native deps).
const { DatabaseSync } = require('node:sqlite');
const path = require('path');

const dbPath = process.env.DB_PATH || path.join(__dirname, '..', 'data.sqlite');
const db = new DatabaseSync(dbPath);

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    token TEXT UNIQUE NOT NULL,
    balance_sats INTEGER NOT NULL DEFAULT 0,
    total_earned_sats INTEGER NOT NULL DEFAULT 0,
    ads_watched_total INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );
  CREATE TABLE IF NOT EXISTS ledger (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    type TEXT NOT NULL,              -- 'ad_reward' | 'withdraw' | 'adjust'
    amount_sats INTEGER NOT NULL,    -- positive = credit, negative = debit
    meta TEXT,                       -- JSON
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );
  CREATE TABLE IF NOT EXISTS ad_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );
  CREATE TABLE IF NOT EXISTS withdrawals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    amount_sats INTEGER NOT NULL,
    invoice TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',  -- pending | paid | failed
    provider_ref TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );
  CREATE INDEX IF NOT EXISTS idx_ledger_user ON ledger(user_id);
  CREATE INDEX IF NOT EXISTS idx_ad_events_user_time ON ad_events(user_id, created_at);
`);

function transaction(fn) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const result = fn();
    db.exec('COMMIT');
    return result;
  } catch (e) {
    try { db.exec('ROLLBACK'); } catch (_) {}
    throw e;
  }
}

function getUserByToken(token) {
  return db.prepare('SELECT * FROM users WHERE token = ?').get(token) || null;
}

function getUserByDevice(deviceId) {
  return db.prepare('SELECT * FROM users WHERE device_id = ?').get(deviceId) || null;
}

function createUser(deviceId, token) {
  db.prepare('INSERT INTO users (device_id, token) VALUES (?, ?)').run(deviceId, token);
  return getUserByDevice(deviceId);
}

function creditSats(userId, amountSats, type, meta) {
  transaction(() => {
    db.prepare(
      'UPDATE users SET balance_sats = balance_sats + ?, total_earned_sats = total_earned_sats + ?, ads_watched_total = ads_watched_total + 1 WHERE id = ?'
    ).run(amountSats, amountSats, userId);
    db.prepare('INSERT INTO ledger (user_id, type, amount_sats, meta) VALUES (?, ?, ?, ?)')
      .run(userId, type, amountSats, JSON.stringify(meta || {}));
    db.prepare('INSERT INTO ad_events (user_id) VALUES (?)').run(userId);
  });
  return db.prepare('SELECT balance_sats FROM users WHERE id = ?').get(userId).balance_sats;
}

function debitForWithdrawal(userId, amountSats, invoice) {
  return transaction(() => {
    const row = db.prepare('SELECT balance_sats FROM users WHERE id = ?').get(userId);
    if (!row || row.balance_sats < amountSats) throw new Error('insufficient_balance');
    db.prepare('UPDATE users SET balance_sats = balance_sats - ? WHERE id = ?').run(amountSats, userId);
    db.prepare("INSERT INTO ledger (user_id, type, amount_sats, meta) VALUES (?, 'withdraw', ?, ?)")
      .run(userId, -amountSats, JSON.stringify({ invoice: invoice.slice(0, 40) + '...' }));
    const r = db.prepare(
      "INSERT INTO withdrawals (user_id, amount_sats, invoice, status) VALUES (?, ?, ?, 'pending')"
    ).run(userId, amountSats, invoice);
    return r.lastInsertRowid;
  });
}

function setWithdrawalStatus(id, status, providerRef) {
  db.prepare('UPDATE withdrawals SET status = ?, provider_ref = ? WHERE id = ?')
    .run(status, providerRef || null, id);
}

function refundWithdrawal(withdrawalId) {
  return transaction(() => {
    const w = db.prepare('SELECT * FROM withdrawals WHERE id = ?').get(withdrawalId);
    if (!w || w.status !== 'pending') return false;
    db.prepare('UPDATE users SET balance_sats = balance_sats + ? WHERE id = ?').run(w.amount_sats, w.user_id);
    db.prepare("INSERT INTO ledger (user_id, type, amount_sats, meta) VALUES (?, 'adjust', ?, ?)")
      .run(w.user_id, w.amount_sats, JSON.stringify({ reason: 'withdraw_refund', withdrawal_id: withdrawalId }));
    db.prepare("UPDATE withdrawals SET status = 'failed' WHERE id = ?").run(withdrawalId);
    return true;
  });
}

function countAdsToday(userId) {
  return db.prepare(
    "SELECT COUNT(*) AS c FROM ad_events WHERE user_id = ? AND created_at >= datetime('now', '-1 day')"
  ).get(userId).c;
}

function lastAdAt(userId) {
  const r = db.prepare(
    'SELECT created_at AS t FROM ad_events WHERE user_id = ? ORDER BY id DESC LIMIT 1'
  ).get(userId);
  return r ? new Date(r.t + 'Z').getTime() : 0;
}

function getLedger(userId, limit) {
  return db.prepare('SELECT type, amount_sats, created_at FROM ledger WHERE user_id = ? ORDER BY id DESC LIMIT ?')
    .all(userId, limit);
}

module.exports = {
  db,
  getUserByToken,
  getUserByDevice,
  createUser,
  creditSats,
  debitForWithdrawal,
  setWithdrawalStatus,
  refundWithdrawal,
  countAdsToday,
  lastAdAt,
  getLedger,
};
