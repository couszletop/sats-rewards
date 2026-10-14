# Sats Rewards — backend

API for the watch-ads-earn-sats Android app: device accounts, ad-reward
credits, and Lightning withdrawals (LNbits, with a mock mode for testing).

## Run

```bash
cd backend
npm install
cp .env.example .env   # optional
npm start              # http://localhost:3000
```

Needs Node >= 22.5 (uses built-in `node:sqlite`, no native modules).

## Endpoints

| Method | Path              | Auth   | Description                              |
|--------|-------------------|--------|------------------------------------------|
| POST   | /auth/register    | no     | `{device_id}` → `{token, user_id}`       |
| GET    | /config           | no     | reward amounts, limits, lightning mode   |
| GET    | /me               | bearer | balance, totals, ads watched today       |
| POST   | /rewards/ad       | bearer | credit sats for one watched ad           |
| POST   | /withdraw         | bearer | `{amount_sats, invoice}` → Lightning pay |
| GET    | /ledger           | bearer | recent credit/debit history              |
| GET    | /health           | no     | server + Lightning wallet status         |

## Going live with real sats

1. Run an [LNbits](https://lnbits.com) instance (or use a hosted one) and fund
   its Lightning wallet with the starting float (~0.001–0.002 BTC is plenty).
2. Set `LNBITS_URL` and `LNBITS_API_KEY` in `.env` and restart.
3. Check `/health` — `lightning.mode` should be `lnbits`.

## Before publishing (important)

- **AdMob SSV**: `/rewards/ad` currently trusts the app. In production,
  validate Google's Server-Side Verification callback for rewarded ads and
  only credit on a signed, verified callback.
- **Play Integrity API**: verify the device verdict on register/reward calls
  to block emulators and rooted farms.
- **Auth**: the device-token scheme is fine for an MVP; consider Firebase Auth
  or similar once you have real users.
