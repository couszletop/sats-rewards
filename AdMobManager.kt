package com.satsrewards.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// TODO: replace with your real AdMob rewarded ad unit id before release.
// This is Google's official test id: it only serves test ads.
const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

object AdMobManager {
    private var rewardedAd: RewardedAd? = null
    private var loading = false

    fun init(context: Context) {
        MobileAds.initialize(context) {}
        load(context.applicationContext)
    }

    fun load(context: Context) {
        if (loading || rewardedAd != null) return
        loading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    loading = false
                }
            },
        )
    }

    fun isReady(): Boolean = rewardedAd != null

    /** Shows the ad. Calls [onRewarded] if the user earned the reward,
     * then [onDismissed] in all cases. */
    fun show(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            return
        }
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                load(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                load(activity.applicationContext)
                onDismissed()
            }
        }
        ad.show(activity) { onRewarded() }
    }
}
