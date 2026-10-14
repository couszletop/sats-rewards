// Lightning payouts via LNbits (https://lnbits.com).
// Set LNBITS_URL + LNBITS_API_KEY to go live. Otherwise MOCK mode simulates
// payouts so the whole loop can be tested without real funds.

const MOCK = !process.env.LNBITS_URL || !process.env.LNBITS_API_KEY;

async function payInvoice(invoice, amountSats) {
  if (MOCK) {
    // Simulate a successful Lightning payment (test mode).
    await new Promise((r) => setTimeout(r, 300));
    return { ok: true, ref: 'mock-' + Date.now() };
  }
  const url = process.env.LNBITS_URL.replace(/\/$/, '') + '/api/v1/payments';
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Api-Key': process.env.LNBITS_API_KEY,
    },
    body: JSON.stringify({ out: true, bolt11: invoice }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    return { ok: false, error: data.detail || `lnbits_http_${res.status}` };
  }
  // LNbits returns a payment_hash immediately; settlement is async.
  // For simplicity we treat acceptance as success; check status via API in prod.
  if (typeof amountSats === 'number' && data.payment_hash) {
    return { ok: true, ref: data.payment_hash };
  }
  return { ok: true, ref: data.payment_hash || 'lnbits-accepted' };
}

async function checkHealth() {
  if (MOCK) return { mode: 'mock', ok: true };
  try {
    const url = process.env.LNBITS_URL.replace(/\/$/, '') + '/api/v1/wallet';
    const res = await fetch(url, { headers: { 'X-Api-Key': process.env.LNBITS_API_KEY } });
    const data = await res.json().catch(() => ({}));
    return { mode: 'lnbits', ok: res.ok, balance_msat: data.balance };
  } catch (e) {
    return { mode: 'lnbits', ok: false, error: String(e.message || e) };
  }
}

module.exports = { payInvoice, checkHealth, MOCK };
