package com.satsrewards.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satsrewards.app.MainViewModel
import com.satsrewards.app.ads.AdMobManager
import com.satsrewards.app.toBtcString

@Composable
fun HomeScreen(vm: MainViewModel, activity: Activity) {
    val snackbar = remember { SnackbarHostState() }
    val msg = vm.lastRewardMsg
    LaunchedEffect(msg) {
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearRewardMsg()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sats Rewards", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (vm.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val me = vm.me
        val cfg = vm.config

        // Estimated earnings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Estimated earnings", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${me?.balanceSats?.toBtcString() ?: "0.00000000"} BTC",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("${me?.balanceSats ?: 0} sats")
            }
        }

        // Hashrate card (simulated display)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Cloud hashrate", style = MaterialTheme.typography.labelLarge)
                Text("${vm.simulatedHashrateMhs} MH/s", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Total earned: ${me?.totalEarnedSats ?: 0} sats")
            }
        }

        // Watch ad button
        val adsToday = me?.adsToday ?: 0
        val maxAds = cfg?.maxAdsPerDay ?: 30
        val reward = cfg?.rewardSatsPerAd ?: 0
        Button(
            onClick = {
                AdMobManager.show(
                    activity,
                    onRewarded = { vm.onAdRewarded() },
                    onDismissed = { /* preloaded next ad in manager */ },
                )
            },
            enabled = !vm.loading && adsToday < maxAds,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Watch ad  (+$reward sats)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("AD ($adsToday/$maxAds)")
            Text(if (AdMobManager.isReady()) "Ad ready" else "Loading ad...")
        }

        SnackbarHost(snackbar)
    }
}
