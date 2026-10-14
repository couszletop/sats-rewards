# Sats Rewards — watch ads, earn real sats

A complete starter kit for an Android rewards app: users watch rewarded ads,
earn satoshis, and withdraw them over Lightning. In the UI it looks like the
popular "cloud mining" apps (estimated earnings, hashrate, shop) — the money
flow underneath is honest: ad revenue in, sats out.

```
sats-rewards/
  backend/   Node.js API (accounts, ad rewards, Lightning withdrawals) — tested
  android/   Kotlin + Compose app (Home, Shop, Wallet, Profile) + AdMob
```

## How the money works

1. User watches a rewarded ad → AdMob pays you ~$0.005–0.015 per view.
2. Backend credits the user a few sats (configurable, default 5/ad, max 30/day).
3. User withdraws to their own Lightning wallet once they reach the minimum
   (default 2000 sats). You fund the Lightning wallet; ad revenue refills it.

Keep the payout ratio under ~50% of ad revenue and the unit economics stay
positive from the first user. See the backend README for the anti-fraud
checklist — fraud is what kills these apps, not the code.

## Going live — checklist

### 1. Backend on a VPS (~$5/month)
- Any small VPS (Hetzner, DigitalOcean, OVH...), Ubuntu 24.04, Node 22+.
- Copy `backend/`, run `npm install`, create `.env` from `.env.example`.
- Run behind a reverse proxy with HTTPS (Caddy or nginx) — the app must use
  `https://` in production.
- Keep it running with `systemd` or `pm2`.

### 2. Lightning wallet (the payout float)
- Install [LNbits](https://lnbits.com) on the VPS (or use a hosted instance),
  open a Lightning channel / fund it with **0.001–0.002 BTC** to start.
- Set `LNBITS_URL` + `LNBITS_API_KEY` in backend `.env`, restart, and check
  `/health` shows `"mode": "lnbits"`.
- Rule of thumb: keep 1–2 months of expected payouts in the hot wallet.

### 3. AdMob
- Create an AdMob account, add the Android app, create a **Rewarded** ad unit.
- Replace the test ids (see `android/README.md`) and set the real backend URL
  in `Config.kt`.

### 4. Google Play
- Google Play Console account ($25 one-time).
- In Android Studio: **Build > Generate Signed Bundle** → upload the AAB.
- Fill in the store listing, content rating questionnaire, and the
  "financial features" declaration (the app deals with cryptocurrency rewards).
- Note: on-device crypto mining is banned; this app does none — rewards are
  credited server-side. Apps whose *primary purpose* is serving ads can be
  rejected, so make sure the app has real standalone value (wallet, history,
  shop) beyond the ad button.

### 5. Before scaling
- Validate AdMob **SSV callbacks** server-side before crediting (backend
  README has the details).
- Enforce **Play Integrity API** verdicts on `/auth/register` and
  `/rewards/ad`.
- Start with a small trusted group, watch the fraud patterns, then open up.

## Project status

- Backend: written and tested end-to-end locally (register → ad reward →
  rate limits → withdrawal in mock Lightning mode).
- Android app: complete source, **not compiled here** (no Android SDK on this
  machine) — open in Android Studio to build the APK/AAB.
