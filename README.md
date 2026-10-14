# Sats Rewards — Android app

Native Kotlin + Jetpack Compose app: Home (earnings + watch ads), Shop
(hashrate packs), Wallet (Lightning withdrawals), Profile.

## Build

1. Open the `android/` folder in Android Studio (Koala or newer).
2. Let Gradle sync. You need the Android SDK (API 34) — Android Studio
   installs it for you.
3. Run on an emulator or your phone, or **Build > Build App Bundle(s)** for
   the release AAB to upload to the Play Console.

The emulator reaches a backend running on your PC via `10.0.2.2:3000`
(already set in `Config.kt`).

## Before release — replace the test ids

| What | File | Test value → real value |
|---|---|---|
| AdMob app id | `AndroidManifest.xml` | `ca-app-pub-3940256099942544~3347511713` → your AdMob app id |
| Rewarded ad unit | `ads/AdMobManager.kt` (`REWARDED_AD_UNIT_ID`) | `.../5224354917` → your rewarded ad unit id |
| Backend URL | `Config.kt` (`BACKEND_URL`) | `http://10.0.2.2:3000` → `https://your-api-domain.com` |

## Notes

- The hashrate shown in the UI is a **simulated display**, like other
  cloud-mining apps. Real value flows: rewarded ads → backend credits →
  Lightning withdrawals.
- Paid hashrate packs need Google Play Billing integration (marked TODO in
  `ShopScreen.kt`). The MVP ships with free + ad-boosted packs only.
- For production, validate AdMob **Server-Side Verification (SSV)**
  callbacks in the backend before crediting sats, and check the
  **Play Integrity API** verdict to block emulators.
