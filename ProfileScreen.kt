package com.satsrewards.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satsrewards.app.MainViewModel

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val me = vm.me
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Your stats", fontWeight = FontWeight.SemiBold)
                Text("Total earned: ${me?.totalEarnedSats ?: 0} sats")
                Text("Ads watched: ${me?.adsWatchedTotal ?: 0}")
                Text("Ads today: ${me?.adsToday ?: 0}")
            }
        }

        Button(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh")
        }

        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text(
            "Sats Rewards pays real sats earned from ads, sent over Lightning. " +
                "The hashrate shown in the app is a simulated display.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
