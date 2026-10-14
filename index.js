const express = require('express');
const crypto = require('crypto');
const db = require('./db');
const lightning = require('./lightning');
const fraud = require('./fraud');

const app = express();
app.use(express.json());

const REWARD_SATS_PER_AD = parseInt(process.env.REWARD_SATS_PER_AD || '5', 10);
const MIN_WITHDRAW_SATS = parseInt(process.env.MIN_WITHDRAW_SATS || '2000', 10);
const PORT = parseInt(process.env.PORT || '3000', 10);

function newToken() {
  return crypto.randomBytes(32).toString('hex');
}

function auth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  const user = token ? db.getUserByToken(token) : null;
  if (!user) return res.status(401).json({ error: 'unauthorized' });
  req.user = user;
  next();
}

// --- Public ---------------------------------------------------------------

// Register (or re-login) a device. Returns a bearer token.
app.post('/auth/register', (req, res) => {
  const deviceId = (req.body.device_id || '').trim();
  if (!deviceId || deviceId.length > 128) {
    return res.status(400).json({ error: 'device_id_required' });
  }
  let user = db.getUserByDevice(deviceId);
  if (!user) user = db.createUser(deviceId, newToken());
  res.json({ token: user.token, user_id: user.id });
});

app.get('/config', (req, res) => {
  res.json({
    reward_sats_per_ad: REWARD_SATS_PER_AD,
    max_ads_per_day: fraud.MAX_ADS_PER_DAY,
    min_ad_interval_sec: fraud.MIN_AD_INTERVAL_SEC,
    min_withdraw_sats: MIN_WITHDRAW_SATS,
    lightning_mode: lightning.MOCK ? 'mock' : 'lnbits',
  });
});

// --- Authenticated --------------------------------------------------------

app.get('/me', auth, (req, res) => {
  const u = db.getUserByToken(req.user.token);
  res.json({
    user_id: u.id,
    balance_sats: u.balance_sats,
    total_earned_sats: u.total_earned_sats,
    ads_watched_total: u.ads_watched_total,
    ads_today: db.countAdsToday(u.id),
  });
});

// Credit sats after the app confirms a rewarded ad was fully watched.
// NOTE: in production, validate Google AdMob SSV callback here instead of
// trusting the client. See README.
app.post('/rewards/ad', auth, (req, res) => {
  const check = fraud.checkAdAllowed(db, req.user.id);
  if (!check.ok) return res.status(429).json({ error: check.reason, detail: check.detail });

  const balance = db.creditSats(req.user.id, REWARD_SATS_PER_AD, 'ad_reward', {
    ad_unit: req.body.ad_unit || 'rewarded',
  });
  res.json({ credited_sats: REWARD_SATS_PER_AD, balance_sats: balance });
});

// Withdraw to a Lightning invoice provided by the user's wallet.
app.post('/withdraw', auth, async (req, res) => {
  const amount = parseInt(req.body.amount_sats, 10);
  const invoice = (req.body.invoice || '').trim();

  if (!Number.isInteger(amount) || amount < MIN_WITHDRAW_SATS) {
    return res.status(400).json({ error: 'amount_too_small', min_withdraw_sats: MIN_WITHDRAW_SATS });
  }
  if (!fraud.looksLikeInvoice(invoice)) {
    return res.status(400).json({ error: 'invalid_invoice' });
  }

  let withdrawalId;
  try {
    withdrawalId = db.debitForWithdrawal(req.user.id, amount, invoice);
  } catch (e) {
    if (e.message === 'insufficient_balance') {
      return res.status(400).json({ error: 'insufficient_balance' });
    }
    throw e;
  }

  const result = await lightning.payInvoice(invoice, amount);
  if (result.ok) {
    db.setWithdrawalStatus(withdrawalId, 'paid', result.ref);
    const u = db.getUserByToken(req.user.token);
    return res.json({ status: 'paid', ref: result.ref, balance_sats: u.balance_sats });
  }
  // Payment failed: refund the user automatically.
  db.refundWithdrawal(withdrawalId);
  const u = db.getUserByToken(req.user.token);
  return res.status(502).json({ status: 'failed', error: result.error, balance_sats: u.balance_sats });
});

app.get('/ledger', auth, (req, res) => {
  const limit = Math.min(parseInt(req.query.limit || '50', 10), 200);
  res.json({ entries: db.getLedger(req.user.id, limit) });
});

app.get('/health', async (req, res) => {
  res.json({ ok: true, lightning: await lightning.checkHealth() });
});

app.listen(PORT, () => {
  console.log(`sats-rewards backend on :${PORT} (lightning: ${lightning.MOCK ? 'MOCK' : 'LNBITS'})`);
});
