// Basic anti-fraud rules for ad rewards.
// These stop casual abuse. For production, add:
//  - Google Play Integrity API verdict check (device genuine, not emulator)
//  - per-IP / per-device velocity limits, VPN/proxy detection
//  - AdMob SSV (Server-Side Verification) callback validation

const MAX_ADS_PER_DAY = parseInt(process.env.MAX_ADS_PER_DAY || '30', 10);
const MIN_AD_INTERVAL_SEC = parseInt(process.env.MIN_AD_INTERVAL_SEC || '45', 10);

function checkAdAllowed(db, userId) {
  const adsToday = db.countAdsToday(userId);
  if (adsToday >= MAX_ADS_PER_DAY) {
    return { ok: false, reason: 'daily_cap_reached', detail: `max ${MAX_ADS_PER_DAY}/day` };
  }
  const elapsed = (Date.now() - db.lastAdAt(userId)) / 1000;
  if (elapsed < MIN_AD_INTERVAL_SEC) {
    return {
      ok: false,
      reason: 'too_fast',
      detail: `wait ${Math.ceil(MIN_AD_INTERVAL_SEC - elapsed)}s`,
    };
  }
  return { ok: true };
}

// Very rough invoice sanity check: Lightning BOLT11 invoices start with lnbc/lntb/lnbcrt.
function looksLikeInvoice(invoice) {
  return typeof invoice === 'string' && /^(lnbc|lntb|lnbcrt)[0-9a-z]+$/i.test(invoice.trim());
}

module.exports = { checkAdAllowed, looksLikeInvoice, MAX_ADS_PER_DAY, MIN_AD_INTERVAL_SEC };
