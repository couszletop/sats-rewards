package com.satsrewards.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satsrewards.app.MainViewModel
import com.satsrewards.app.ads.AdMobManager

private data class Pack(
    val name: String,
    val hashrate: String,
    val price: String,
    val actionLabel: String,
    val enabled: Boolean,
)

@Composable
fun ShopScreen(vm: MainViewModel, activity: Activity) {
    val packs = listOf(
        Pack("Starter Miner", "2.0 GH/s", "Free", "Owned", enabled = false),
        Pack("Ad Boost", "+100 MH/s for 24h", "1 ad", "Boost now", enabled = true),
        // TODO: paid packs need Google Play Billing integration.
        Pack("Pro Miner", "50 GH/s", "Coming soon", "Coming soon", enabled = false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Shop", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Boost your cloud hashrate. Earnings are still paid in sats to your balance.",
            style = MaterialTheme.typography.bodyMedium,
        )
        packs.forEach { pack ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(pack.name, fontWeight = FontWeight.SemiBold)
                        Text(pack.hashrate, style = MaterialTheme.typography.bodyMedium)
                        Text(pack.price, style = MaterialTheme.typography.bodySmall)
                    }
                    if (pack.enabled) {
                        Button(onClick = {
                            AdMobManager.show(
                                activity,
                                onRewarded = { vm.onAdRewarded() },
                                onDismissed = {},
                            )
                        }) {
                            Text(pack.actionLabel)
                        }
                    } else {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text(pack.actionLabel)
                        }
                    }
                }
            }
        }
    }
}
